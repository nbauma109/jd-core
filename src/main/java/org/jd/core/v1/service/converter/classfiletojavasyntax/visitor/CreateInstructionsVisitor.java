/*
 * Copyright (c) 2008, 2019 Emmanuel Dupuy.
 * This project is distributed under the GPLv3 license.
 * This is a Copyleft license that gives the user the right to use,
 * copy and modify the code freely for non-commercial purposes.
 */

package org.jd.core.v1.service.converter.classfiletojavasyntax.visitor;

import org.apache.bcel.classfile.Code;
import org.apache.bcel.classfile.Method;
import org.jd.core.v1.model.classfile.ClassFile;
import org.jd.core.v1.model.javasyntax.AbstractJavaSyntaxVisitor;
import org.jd.core.v1.model.javasyntax.declaration.AnnotationDeclaration;
import org.jd.core.v1.model.javasyntax.declaration.BodyDeclaration;
import org.jd.core.v1.model.javasyntax.declaration.ClassDeclaration;
import org.jd.core.v1.model.javasyntax.declaration.ConstructorDeclaration;
import org.jd.core.v1.model.javasyntax.declaration.EnumDeclaration;
import org.jd.core.v1.model.javasyntax.declaration.FieldDeclaration;
import org.jd.core.v1.model.javasyntax.declaration.InterfaceDeclaration;
import org.jd.core.v1.model.javasyntax.declaration.MethodDeclaration;
import org.jd.core.v1.model.javasyntax.declaration.StaticInitializerDeclaration;
import org.jd.core.v1.model.javasyntax.statement.Statements;
import org.jd.core.v1.parser.util.ASTUtilities;
import org.jd.core.v1.service.converter.classfiletojavasyntax.model.javasyntax.declaration.ClassFileBodyDeclaration;
import org.jd.core.v1.service.converter.classfiletojavasyntax.model.javasyntax.declaration.ClassFileConstructorOrMethodDeclaration;
import org.jd.core.v1.service.converter.classfiletojavasyntax.util.ByteCodeWriter;
import org.jd.core.v1.service.converter.classfiletojavasyntax.util.ControlFlowGraphReducer;
import org.jd.core.v1.service.converter.classfiletojavasyntax.util.ExceptionUtil;
import org.jd.core.v1.service.converter.classfiletojavasyntax.util.LocalVariableMaker;
import org.jd.core.v1.service.converter.classfiletojavasyntax.util.StatementMaker;
import org.jd.core.v1.service.converter.classfiletojavasyntax.util.TypeMaker;
import org.jd.core.v1.service.converter.classfiletojavasyntax.util.cfg.DuplicateMergeCFGReducer;

import java.util.List;
import java.util.Set;

import static org.apache.bcel.Const.ACC_ABSTRACT;
import static org.apache.bcel.Const.ACC_BRIDGE;
import static org.apache.bcel.Const.ACC_PUBLIC;
import static org.apache.bcel.Const.ACC_STATIC;
import static org.apache.bcel.Const.ACC_SYNTHETIC;

public class CreateInstructionsVisitor extends AbstractJavaSyntaxVisitor {
    /** Bounds retries that force one more unresolved merge target to duplicate per predecessor at a time. */
    private static final int MAX_LABEL_RESOLUTION_RETRIES = 8;

    private final TypeMaker typeMaker;
    private final FixHoistedCatchThrowVisitor fixHoistedCatchThrowVisitor = new FixHoistedCatchThrowVisitor();
    private final FixMissingNullGuardVisitor fixMissingNullGuardVisitor = new FixMissingNullGuardVisitor();

    public CreateInstructionsVisitor(TypeMaker typeMaker) {
        this.typeMaker = typeMaker;
    }

    @Override
    public void visit(AnnotationDeclaration declaration) {
        safeAccept(declaration.getBodyDeclaration());
    }

    @Override
    public void visit(BodyDeclaration declaration) {
        ClassFileBodyDeclaration bodyDeclaration = (ClassFileBodyDeclaration)declaration;

        // Parse byte code
        List<ClassFileConstructorOrMethodDeclaration> methods = bodyDeclaration.getMethodDeclarations();

        for (ClassFileConstructorOrMethodDeclaration method : methods) {
            if ((method.getFlags() & (ACC_SYNTHETIC|ACC_BRIDGE)) != 0) {
                method.accept(this);
            } else if (((method.getFlags() & (ACC_STATIC|ACC_BRIDGE)) == ACC_STATIC) && method.getMethod().getName().startsWith("access$")) {
                // Accessor -> bridge method
                method.setFlags(method.getFlags() | ACC_BRIDGE);
                method.accept(this);
            }
        }
        for (ClassFileConstructorOrMethodDeclaration method : methods) {
            if ((method.getFlags() & (ACC_SYNTHETIC|ACC_BRIDGE)) == 0) {
                method.accept(this);
            }
        }
    }

    @Override
    public void visit(FieldDeclaration declaration) {}

    @Override
    public void visit(ConstructorDeclaration declaration) {
        createParametersVariablesAndStatements((ClassFileConstructorOrMethodDeclaration)declaration, true);
    }

    @Override
    public void visit(MethodDeclaration declaration) {
        createParametersVariablesAndStatements((ClassFileConstructorOrMethodDeclaration)declaration, false);
    }

    @Override
    public void visit(StaticInitializerDeclaration declaration) {
        createParametersVariablesAndStatements((ClassFileConstructorOrMethodDeclaration)declaration, false);
    }

    public void createParametersVariablesAndStatements(ClassFileConstructorOrMethodDeclaration comd, boolean constructor) {
        ClassFile classFile = comd.getClassFile();
        Method method = comd.getMethod();
        Code attributeCode = method.getCode();
        LocalVariableMaker localVariableMaker = new LocalVariableMaker(typeMaker, comd, constructor);

        if (attributeCode == null) {
            localVariableMaker.make(false, typeMaker);
        } else {
            StatementMaker statementMaker = new StatementMaker(typeMaker, localVariableMaker, comd);
            boolean containsLineNumber = attributeCode.getLineNumberTable() != null;

            List<ControlFlowGraphReducer> preferredReducers = ControlFlowGraphReducer.getPreferredReducers();

            boolean reduced = false;
            for (ControlFlowGraphReducer controlFlowGraphReducer : preferredReducers) {
                try {
                    if (controlFlowGraphReducer.reduce(method)) {
                        boolean madeStatements = false;

                        if (comd.getStatements() instanceof Statements stmts) {
                            if (stmts.isEmpty()) {
                                comd.setStatements(statementMaker.make(controlFlowGraphReducer.getControlFlowGraph(), stmts));
                                madeStatements = true;
                            }
                        } else {
                            comd.setStatements(statementMaker.make(controlFlowGraphReducer.getControlFlowGraph(), new Statements()));
                            madeStatements = true;
                        }

                        if (madeStatements && controlFlowGraphReducer instanceof DuplicateMergeCFGReducer duplicateMergeCFGReducer) {
                            Set<Integer> unresolved = statementMaker.getUnresolvedLabelTargets();
                            int retries = 0;

                            while (!unresolved.isEmpty() && retries++ < MAX_LABEL_RESOLUTION_RETRIES
                                    && duplicateMergeCFGReducer.addForcedDuplicateOffsets(unresolved)) {
                                try {
                                    if (!duplicateMergeCFGReducer.reduce(method)) {
                                        break;
                                    }

                                    LocalVariableMaker retryLocalVariableMaker = new LocalVariableMaker(typeMaker, comd, constructor);
                                    StatementMaker retryStatementMaker = new StatementMaker(typeMaker, retryLocalVariableMaker, comd);
                                    Statements retryStatements = retryStatementMaker.make(duplicateMergeCFGReducer.getControlFlowGraph(), new Statements());

                                    comd.setStatements(retryStatements);
                                    localVariableMaker = retryLocalVariableMaker;
                                    statementMaker = retryStatementMaker;
                                    unresolved = retryStatementMaker.getUnresolvedLabelTargets();
                                } catch (Exception | StackOverflowError e) {
                                    // Keep whatever the last successful attempt produced (still fully valid,
                                    // just with an unresolved jump or two left as a 'goto' comment) rather than
                                    // letting a failed retry take down a method that already reduced fine.
                                    break;
                                }
                            }
                        }

                        reduced = true;
                        break;
                    }
                } catch (Exception | StackOverflowError e) {
                    assert ExceptionUtil.printStackTrace(e);
                }
            }
            if (!reduced) {
                System.err.println("Could not reduce control flow graph in method " + method.getName() + method.getSignature() + " from class " + classFile.getInternalTypeName());
                comd.setStatements(ASTUtilities.toBaseStatement(ByteCodeWriter.getLineNumberTableAsStatements(method)));
            }

            localVariableMaker.make(containsLineNumber, typeMaker);

            if (comd.getStatements() instanceof Statements methodStatements) {
                methodStatements.accept(fixHoistedCatchThrowVisitor);
                methodStatements.accept(fixMissingNullGuardVisitor);
            }
        }

        comd.setFormalParameters(localVariableMaker.getFormalParameters());

        if (classFile.isInterface()) {
            comd.setFlags(comd.getFlags() & ~(ACC_PUBLIC|ACC_ABSTRACT));
        }
    }

    @Override
    public void visit(ClassDeclaration declaration) {
        safeAccept(declaration.getBodyDeclaration());
    }

    @Override
    public void visit(EnumDeclaration declaration) {
        safeAccept(declaration.getBodyDeclaration());
    }

    @Override
    public void visit(InterfaceDeclaration declaration) {
        safeAccept(declaration.getBodyDeclaration());
    }
}
