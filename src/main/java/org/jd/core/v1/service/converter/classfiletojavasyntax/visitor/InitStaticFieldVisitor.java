/*
 * Copyright (c) 2008, 2019 Emmanuel Dupuy.
 * This project is distributed under the GPLv3 license.
 * This is a Copyleft license that gives the user the right to use,
 * copy and modify the code freely for non-commercial purposes.
 */

package org.jd.core.v1.service.converter.classfiletojavasyntax.visitor;

import org.apache.bcel.Const;
import org.jd.core.v1.model.javasyntax.AbstractJavaSyntaxVisitor;
import org.jd.core.v1.model.javasyntax.declaration.AnnotationDeclaration;
import org.jd.core.v1.model.javasyntax.declaration.BodyDeclaration;
import org.jd.core.v1.model.javasyntax.declaration.ClassDeclaration;
import org.jd.core.v1.model.javasyntax.declaration.ConstructorDeclaration;
import org.jd.core.v1.model.javasyntax.declaration.EnumDeclaration;
import org.jd.core.v1.model.javasyntax.declaration.ExpressionVariableInitializer;
import org.jd.core.v1.model.javasyntax.declaration.FieldDeclaration;
import org.jd.core.v1.model.javasyntax.declaration.FieldDeclarator;
import org.jd.core.v1.model.javasyntax.declaration.InterfaceDeclaration;
import org.jd.core.v1.model.javasyntax.declaration.LocalVariableDeclarator;
import org.jd.core.v1.model.javasyntax.declaration.MethodDeclaration;
import org.jd.core.v1.model.javasyntax.declaration.StaticInitializerDeclaration;
import org.jd.core.v1.model.javasyntax.expression.Expression;
import org.jd.core.v1.model.javasyntax.expression.FieldReferenceExpression;
import org.jd.core.v1.model.javasyntax.expression.LocalVariableReferenceExpression;
import org.jd.core.v1.model.javasyntax.expression.ObjectTypeReferenceExpression;
import org.jd.core.v1.service.converter.classfiletojavasyntax.model.javasyntax.expression.ClassFileLocalVariableReferenceExpression;
import org.jd.core.v1.model.javasyntax.statement.BaseStatement;
import org.jd.core.v1.model.javasyntax.statement.Statement;
import org.jd.core.v1.model.javasyntax.statement.Statements;
import org.jd.core.v1.model.javasyntax.type.PrimitiveType;
import org.jd.core.v1.service.converter.classfiletojavasyntax.model.javasyntax.declaration.ClassFileBodyDeclaration;
import org.jd.core.v1.service.converter.classfiletojavasyntax.model.javasyntax.declaration.ClassFileConstructorOrMethodDeclaration;
import org.jd.core.v1.service.converter.classfiletojavasyntax.model.javasyntax.declaration.ClassFileFieldDeclaration;
import org.jd.core.v1.service.converter.classfiletojavasyntax.model.javasyntax.declaration.ClassFileStaticInitializerDeclaration;
import org.jd.core.v1.service.converter.classfiletojavasyntax.model.localvariable.LocalVariableReference;
import org.jd.core.v1.service.converter.classfiletojavasyntax.util.Utils;
import org.jd.core.v1.util.DefaultList;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class InitStaticFieldVisitor extends AbstractJavaSyntaxVisitor {
    private final SearchFirstLineNumberVisitor searchFirstLineNumberVisitor = new SearchFirstLineNumberVisitor();
    private final SearchLocalVariableReferenceVisitor searchLocalVariableReferenceVisitor = new SearchLocalVariableReferenceVisitor();
    private final ForwardFieldReferenceVisitor forwardFieldReferenceVisitor = new ForwardFieldReferenceVisitor();
    private final CollectLocalVariableIndicesVisitor collectLocalVariableIndicesVisitor = new CollectLocalVariableIndicesVisitor();
    private String internalTypeName;
    private final Map<String, FieldDeclarator> fields = new HashMap<>();
    private final Map<String, Integer> fieldPositions = new HashMap<>();
    private int fieldPositionCounter;
    private List<ClassFileConstructorOrMethodDeclaration> methods;
    private Boolean deleteStaticDeclaration;

    void setInternalTypeName(String internalTypeName) {
        this.internalTypeName = internalTypeName;
    }

    @Override
    public void visit(AnnotationDeclaration declaration) {
        this.internalTypeName = declaration.getInternalTypeName();
        safeAccept(declaration.getBodyDeclaration());
    }

    @Override
    public void visit(ClassDeclaration declaration) {
        this.internalTypeName = declaration.getInternalTypeName();
        safeAccept(declaration.getBodyDeclaration());
    }

    @Override
    public void visit(EnumDeclaration declaration) {
        this.internalTypeName = declaration.getInternalTypeName();
        safeAccept(declaration.getBodyDeclaration());
    }

    @Override
    public void visit(InterfaceDeclaration declaration) {
        this.internalTypeName = declaration.getInternalTypeName();
        safeAccept(declaration.getBodyDeclaration());
    }

    @Override
    public void visit(BodyDeclaration declaration) {
        ClassFileBodyDeclaration bodyDeclaration = (ClassFileBodyDeclaration)declaration;

        // Store field declarations
        fields.clear();
        fieldPositions.clear();
        fieldPositionCounter = 0;
        safeAcceptListDeclaration(bodyDeclaration.getFieldDeclarations());

        if (!fields.isEmpty()) {
            methods = bodyDeclaration.getMethodDeclarations();

            if (methods != null) {
                deleteStaticDeclaration = null;

                for (int i=0, len=methods.size(); i<len; i++) {
                    methods.get(i).accept(this);

                    if (deleteStaticDeclaration != null) {
                        if (deleteStaticDeclaration) {
                            methods.remove(i);
                        }
                        break;
                    }
                }
            }
        }

        safeAcceptListDeclaration(bodyDeclaration.getInnerTypeDeclarations());
    }

    @Override
    public void visit(FieldDeclarator declaration) {
        fields.put(declaration.getName(), declaration);
        fieldPositions.put(declaration.getName(), fieldPositionCounter++);
    }

    @Override
    public void visit(ConstructorDeclaration declaration) {}

    @Override
    public void visit(MethodDeclaration declaration) {}

    @Override
    public void visit(StaticInitializerDeclaration declaration) {
        ClassFileStaticInitializerDeclaration sid = (ClassFileStaticInitializerDeclaration) declaration;

        BaseStatement statements = sid.getStatements();

        if (statements != null) {
            if (statements.isList()) {
                DefaultList<Statement> list = statements.getList();

                // Multiple statements
                if (!list.isEmpty() && isAssertionsDisabledStatement(list.getFirst())) {
                    // Remove assert initialization statement
                    list.removeFirst();
                }

                for (int i=0, len=list.size(); i<len; i++) {
                    if (i > 0 && wouldBreakLocalVariableScope(list, i)) {
                        // Splitting here would separate a local variable declaration
                        // from a later use of that variable in another 'static' block.
                        continue;
                    }
                    if (setStaticFieldInitializer(list.get(i))) {
                        if (i > 0) {
                            // Split 'static' block
                            BaseStatement newStatements;

                            List<Statement> subList = null;
                            if (i == 1) {
                                newStatements = list.getFirst();
                            } else {
                                subList = list.subList(0, i);
                                newStatements = new Statements(subList);
                            }

                            int firstLineNumber = getFirstLineNumber(newStatements);
                            if (firstLineNumber != -1) {
                                // Removes statements from original list
                                i = 0;
                                len -= newStatements.size();
                                if (newStatements.size() == 1) {
                                    list.removeFirst();
                                } else if (subList != null){
                                    subList.clear();
                                }
                                addStaticInitializerDeclaration(sid, firstLineNumber, newStatements);
                            }
                        }
                        // Remove field initialization statement
                        list.remove(i--);
                        len--;
                    }
                }
            } else {
                // Single statement
                if (isAssertionsDisabledStatement(statements.getFirst())) {
                    // Remove assert initialization statement
                    statements = null;
                }
                if (statements != null && setStaticFieldInitializer(statements.getFirst())) {
                    // Remove field initialization statement
                    statements = null;
                }
            }

            if (Utils.isEmpty(statements)) {
                deleteStaticDeclaration = Boolean.TRUE;
            } else {
                int firstLineNumber = getFirstLineNumber(statements);
                sid.setFirstLineNumber(firstLineNumber==-1 ? 0 : firstLineNumber);
                deleteStaticDeclaration = Boolean.FALSE;
            }
        }
    }

    protected boolean isAssertionsDisabledStatement(Statement statement) {
        Expression expression = statement.getExpression();

        if (expression.getLeftExpression().isFieldReferenceExpression()) {
            FieldReferenceExpression fre = (FieldReferenceExpression) expression.getLeftExpression();

            if (fre.getType() == PrimitiveType.TYPE_BOOLEAN && fre.getInternalTypeName().equals(internalTypeName) && "$assertionsDisabled".equals(fre.getName())) {
                return true;
            }
        }

        return false;
    }

    protected boolean setStaticFieldInitializer(Statement statement) {
        Expression expression = statement.getExpression();

        if (expression.getLeftExpression().isFieldReferenceExpression()) {
            FieldReferenceExpression fre = (FieldReferenceExpression) expression.getLeftExpression();

            if (fre.getInternalTypeName().equals(internalTypeName)) {
                FieldDeclarator fdr = fields.get(fre.getName());

                if (fdr != null && fdr.getVariableInitializer() == null) {
                    FieldDeclaration fdn = fdr.getFieldDeclaration();

                    if ((fdn.getFlags() & Const.ACC_STATIC) != 0 && fdn.getType().getDescriptor().equals(fre.getDescriptor())) {
                        expression = expression.getRightExpression();

                        if (!searchLocalVariableReferenceVisitor.containsExternalReference(expression)) {
                            // JLS 8.3.3: a simple-name reference to a field declared later in the same
                            // class is illegal even from within a lambda; qualify it with the class name.
                            Integer currentPosition = fieldPositions.get(fre.getName());
                            if (currentPosition != null) {
                                forwardFieldReferenceVisitor.init(internalTypeName, fieldPositions, currentPosition);
                                expression.accept(forwardFieldReferenceVisitor);
                            }
                            fdr.setVariableInitializer(new ExpressionVariableInitializer(expression));
                            ((ClassFileFieldDeclaration)fdr.getFieldDeclaration()).setFirstLineNumber(expression.getLineNumber());
                            return true;
                        }
                    }
                }
            }
        }

        return false;
    }

    protected boolean wouldBreakLocalVariableScope(DefaultList<Statement> list, int splitIndex) {
        Set<Integer> before = collectLocalVariableIndices(list.subList(0, splitIndex));

        if (before.isEmpty()) {
            return false;
        }

        for (int i = splitIndex + 1, len = list.size(); i < len; i++) {
            collectLocalVariableIndicesVisitor.init();
            list.get(i).accept(collectLocalVariableIndicesVisitor);

            for (Integer index : collectLocalVariableIndicesVisitor.getIndices()) {
                if (before.contains(index)) {
                    return true;
                }
            }
        }

        return false;
    }

    private Set<Integer> collectLocalVariableIndices(List<Statement> statements) {
        Set<Integer> indices = new HashSet<>();

        for (Statement statement : statements) {
            collectLocalVariableIndicesVisitor.init();
            statement.accept(collectLocalVariableIndicesVisitor);
            indices.addAll(collectLocalVariableIndicesVisitor.getIndices());
        }

        return indices;
    }

    protected int getFirstLineNumber(BaseStatement baseStatement) {
        searchFirstLineNumberVisitor.init();
        baseStatement.accept(searchFirstLineNumberVisitor);
        return searchFirstLineNumberVisitor.getLineNumber();
    }

    protected void addStaticInitializerDeclaration(ClassFileStaticInitializerDeclaration sid, int lineNumber, BaseStatement statements) {
        if (!sid.getClassFile().isInterface()) {
	        methods.add(new ClassFileStaticInitializerDeclaration(
	            sid.getBodyDeclaration(), sid.getClassFile(), sid.getMethod(), sid.getBindings(),
	            sid.getTypeBounds(), lineNumber, statements));
        }
    }

    private static class ForwardFieldReferenceVisitor extends AbstractJavaSyntaxVisitor {
        private String internalTypeName;
        private Map<String, Integer> fieldPositions;
        private int currentPosition;

        void init(String internalTypeName, Map<String, Integer> fieldPositions, int currentPosition) {
            this.internalTypeName = internalTypeName;
            this.fieldPositions = fieldPositions;
            this.currentPosition = currentPosition;
        }

        @Override
        public void visit(FieldReferenceExpression expression) {
            if (internalTypeName.equals(expression.getInternalTypeName())
                    && expression.getExpression() instanceof ObjectTypeReferenceExpression otr
                    && !otr.isExplicit()) {
                Integer targetPosition = fieldPositions.get(expression.getName());
                if (targetPosition != null && targetPosition >= currentPosition) {
                    // Field declared at or after the current position: a simple name would be an illegal forward reference
                    otr.setExplicit(true);
                }
            }
            super.visit(expression);
        }
    }

    private static class CollectLocalVariableIndicesVisitor extends AbstractJavaSyntaxVisitor {
        private final Set<Integer> indices = new HashSet<>();

        void init() {
            indices.clear();
        }

        Set<Integer> getIndices() {
            return indices;
        }

        @Override
        public void visit(LocalVariableReferenceExpression expression) {
            indices.add(((ClassFileLocalVariableReferenceExpression) expression).getLocalVariable().getIndex());
        }

        @Override
        public void visit(LocalVariableDeclarator declarator) {
            if (declarator instanceof LocalVariableReference reference) {
                indices.add(reference.getLocalVariable().getIndex());
            }
            super.visit(declarator);
        }
    }
}
