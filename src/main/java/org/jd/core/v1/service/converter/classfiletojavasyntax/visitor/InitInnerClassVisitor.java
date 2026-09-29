/*
 * Copyright (c) 2008, 2019 Emmanuel Dupuy.
 * This project is distributed under the GPLv3 license.
 * This is a Copyleft license that gives the user the right to use,
 * copy and modify the code freely for non-commercial purposes.
 */
package org.jd.core.v1.service.converter.classfiletojavasyntax.visitor;

import org.apache.bcel.Const;
import org.apache.bcel.classfile.ConstantNameAndType;
import org.apache.bcel.classfile.ConstantPool;
import org.apache.bcel.classfile.EnclosingMethod;
import org.apache.bcel.classfile.Method;
import org.jd.core.v1.model.classfile.ClassFile;
import org.jd.core.v1.model.javasyntax.AbstractJavaSyntaxVisitor;
import org.jd.core.v1.model.javasyntax.declaration.AnnotationDeclaration;
import org.jd.core.v1.model.javasyntax.declaration.BaseFormalParameter;
import org.jd.core.v1.model.javasyntax.declaration.BodyDeclaration;
import org.jd.core.v1.model.javasyntax.declaration.ClassDeclaration;
import org.jd.core.v1.model.javasyntax.declaration.ConstructorDeclaration;
import org.jd.core.v1.model.javasyntax.declaration.EnumDeclaration;
import org.jd.core.v1.model.javasyntax.declaration.FieldDeclaration;
import org.jd.core.v1.model.javasyntax.declaration.FieldDeclarator;
import org.jd.core.v1.model.javasyntax.declaration.FormalParameter;
import org.jd.core.v1.model.javasyntax.declaration.InterfaceDeclaration;
import org.jd.core.v1.model.javasyntax.declaration.LocalVariableDeclaration;
import org.jd.core.v1.model.javasyntax.declaration.LocalVariableDeclarator;
import org.jd.core.v1.model.javasyntax.declaration.MethodDeclaration;
import org.jd.core.v1.model.javasyntax.declaration.StaticInitializerDeclaration;
import org.jd.core.v1.model.javasyntax.declaration.TypeDeclaration;
import org.jd.core.v1.model.javasyntax.expression.BaseExpression;
import org.jd.core.v1.model.javasyntax.expression.ConstructorInvocationExpression;
import org.jd.core.v1.model.javasyntax.expression.Expression;
import org.jd.core.v1.model.javasyntax.expression.FieldReferenceExpression;
import org.jd.core.v1.model.javasyntax.expression.NewExpression;
import org.jd.core.v1.model.javasyntax.expression.ObjectTypeReferenceExpression;
import org.jd.core.v1.model.javasyntax.expression.SuperConstructorInvocationExpression;
import org.jd.core.v1.model.javasyntax.statement.BaseStatement;
import org.jd.core.v1.model.javasyntax.statement.ForEachStatement;
import org.jd.core.v1.model.javasyntax.statement.LocalVariableDeclarationStatement;
import org.jd.core.v1.model.javasyntax.statement.Statement;
import org.jd.core.v1.model.javasyntax.statement.Statements;
import org.jd.core.v1.model.javasyntax.statement.TypeDeclarationStatement;
import org.jd.core.v1.model.javasyntax.statement.TryStatement.CatchClause;
import org.jd.core.v1.model.javasyntax.type.BaseType;
import org.jd.core.v1.model.javasyntax.type.ObjectType;
import org.jd.core.v1.model.javasyntax.type.Type;
import org.jd.core.v1.service.converter.classfiletojavasyntax.model.javasyntax.declaration.ClassFileBodyDeclaration;
import org.jd.core.v1.service.converter.classfiletojavasyntax.model.javasyntax.declaration.ClassFileClassDeclaration;
import org.jd.core.v1.service.converter.classfiletojavasyntax.model.javasyntax.declaration.ClassFileConstructorDeclaration;
import org.jd.core.v1.service.converter.classfiletojavasyntax.model.javasyntax.declaration.ClassFileConstructorOrMethodDeclaration;
import org.jd.core.v1.service.converter.classfiletojavasyntax.model.javasyntax.declaration.ClassFileMemberDeclaration;
import org.jd.core.v1.service.converter.classfiletojavasyntax.model.javasyntax.declaration.ClassFileMethodDeclaration;
import org.jd.core.v1.service.converter.classfiletojavasyntax.model.javasyntax.declaration.ClassFileStaticInitializerDeclaration;
import org.jd.core.v1.service.converter.classfiletojavasyntax.model.javasyntax.declaration.ClassFileTypeDeclaration;
import org.jd.core.v1.service.converter.classfiletojavasyntax.model.javasyntax.expression.ClassFileConstructorInvocationExpression;
import org.jd.core.v1.service.converter.classfiletojavasyntax.model.javasyntax.expression.ClassFileLocalVariableReferenceExpression;
import org.jd.core.v1.service.converter.classfiletojavasyntax.model.javasyntax.expression.ClassFileNewExpression;
import org.jd.core.v1.service.converter.classfiletojavasyntax.model.javasyntax.expression.ClassFileSuperConstructorInvocationExpression;
import org.jd.core.v1.service.converter.classfiletojavasyntax.model.localvariable.AbstractLocalVariable;
import org.jd.core.v1.service.converter.classfiletojavasyntax.util.TypeMaker;
import org.jd.core.v1.service.converter.classfiletojavasyntax.util.Utils;
import org.jd.core.v1.util.DefaultList;

import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import static org.apache.bcel.Const.ACC_SYNTHETIC;
import static org.jd.core.v1.model.javasyntax.declaration.Declaration.FLAG_ANONYMOUS;

public class InitInnerClassVisitor extends AbstractJavaSyntaxVisitor {
    private static final String OUTER_THIS_PREFIX = "this$";
    private static final Pattern OUTER_THIS_PARAMETER_NAME = Pattern.compile("this\\$\\d+");
    private final UpdateFieldDeclarationsAndReferencesVisitor updateFieldDeclarationsAndReferencesVisitor = new UpdateFieldDeclarationsAndReferencesVisitor();
    private final DefaultList<String> syntheticInnerFieldNames = new DefaultList<>();
    private String outerTypeFieldName;
    private boolean outerInstanceParameter;

    @Override
    public void visit(AnnotationDeclaration declaration) {
        safeAccept(declaration.getBodyDeclaration());
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

    @Override
    public void visit(BodyDeclaration declaration) {
        ClassFileBodyDeclaration bodyDeclaration = (ClassFileBodyDeclaration)declaration;

        // Init attributes
        outerTypeFieldName = null;
        outerInstanceParameter = false;
        syntheticInnerFieldNames.clear();
        // Visit methods
        safeAcceptListDeclaration(bodyDeclaration.getMethodDeclarations());
        // Init values
        bodyDeclaration.setOuterTypeFieldName(outerTypeFieldName);
        bodyDeclaration.setOuterInstanceParameter(outerInstanceParameter || outerTypeFieldName != null);

        if (!syntheticInnerFieldNames.isEmpty()) {
            bodyDeclaration.setSyntheticInnerFieldNames(new DefaultList<>(syntheticInnerFieldNames));
        }

        if (outerTypeFieldName != null || !syntheticInnerFieldNames.isEmpty()) {
            updateFieldDeclarationsAndReferencesVisitor.visit(bodyDeclaration);
        }
    }

    @Override
    public void visit(ConstructorDeclaration declaration) {
        ClassFileConstructorOrMethodDeclaration cfcd = (ClassFileConstructorOrMethodDeclaration)declaration;
        ClassFile classFile = cfcd.getClassFile();
        ClassFile outerClassFile = classFile.getOuterClassFile();
        boolean removeFirstParameter = false;

        syntheticInnerFieldNames.clear();

        // Search synthetic field initialization
        if (cfcd.getStatements().isList()) {
            Iterator<Statement> iterator = cfcd.getStatements().iterator();

            Statement statement;
            while (iterator.hasNext()) {
                statement = iterator.next();

                if (statement.isExpressionStatement()) {
                    Expression expression = statement.getExpression();

                    if (expression.isSuperConstructorInvocationExpression()) {
                        // 'super(...)'
                        break;
                    }

                    if (expression.isConstructorInvocationExpression()) {
                        // 'this(...)'
                        if (outerClassFile != null && !classFile.isStatic()) {
                            // Inner non-static class --> First parameter is the synthetic outer reference
                            removeFirstParameter = true;
                        }
                        break;
                    }

                    if (expression.isBinaryOperatorExpression()) {
                        Expression e = expression.getLeftExpression();

                        if (e.isFieldReferenceExpression()) {
                            String name = e.getName();

                            if (name.startsWith(OUTER_THIS_PREFIX)) {
                                outerTypeFieldName = name;
                                removeFirstParameter = true;
                            } else if (name.startsWith("val$")) {
                                syntheticInnerFieldNames.add(name);

                                Expression value = expression.getRightExpression();
                                String capturedName = name.substring(4);
                                if (value.isLocalVariableReferenceExpression() && !declaresParameter(cfcd.getFormalParameters(), capturedName)) {
                                    // javac 22+ reads the captured parameter (not the 'val$' field) in the constructor body
                                    ((ClassFileLocalVariableReferenceExpression) value).getLocalVariable().setName(capturedName);
                                }
                            }
                        }
                    }
                }

                iterator.remove();
            }
        }

        BaseFormalParameter parameters = cfcd.getFormalParameters();

        if (!removeFirstParameter && parameters != null && outerClassFile != null && !classFile.isStatic()) {
            // javac 18+ does not store an unused outer instance in 'this$N': the parameter is still passed, named 'this$N'
            FormalParameter firstParameter = parameters.getFirst();

            if (OUTER_THIS_PARAMETER_NAME.matcher(firstParameter.getName()).matches() && firstParameter.getType() instanceof ObjectType firstParameterType
                    && firstParameterType.getInternalName().equals(outerClassFile.getInternalTypeName())
                    && !isDeclaredInStaticMethod(classFile, outerClassFile)) {
                outerInstanceParameter = true;
                removeFirstParameter = true;
            }
        }

        // Remove synthetic parameters

        if (parameters != null) {
            if (parameters.isList()) {
                List<FormalParameter> list = parameters.getList();

                if (removeFirstParameter) {
                    // Remove outer this
                    list.remove(0);
                }

                int count = syntheticInnerFieldNames.size();

                if (count > 0) {
                    // Remove outer local variable reference
                    int size = list.size();
                    if (size > 0 && count <= size) {
                        list.subList(size - count, size).clear();
                    }
                }
            } else if (removeFirstParameter || !syntheticInnerFieldNames.isEmpty()) {
                // Remove outer this and outer local variable reference
                cfcd.setFormalParameters(null);
            }
        }

        // Anonymous class constructor ?
        if (outerClassFile != null) {
            String outerTypeName = outerClassFile.getInternalTypeName();
            String internalTypeName = cfcd.getClassFile().getInternalTypeName();
            int min;

            if (internalTypeName.startsWith(outerTypeName + '$')) {
                min = outerTypeName.length() + 1;
            } else {
                min = internalTypeName.lastIndexOf('$') + 1;
            }

            if (Character.isDigit(internalTypeName.charAt(min))) {
                int i = internalTypeName.length();
                boolean anonymousFlag = true;

                while (--i > min) {
                    if (!Character.isDigit(internalTypeName.charAt(i))) {
                        anonymousFlag = false;
                        break;
                    }
                }

                if (anonymousFlag) {
                    // Mark anonymous class constructor
                    cfcd.setFlags(cfcd.getFlags() | FLAG_ANONYMOUS);
                    cfcd.getBodyDeclaration().setAnonymous(anonymousFlag);
                }
            }
        }
    }

    private static boolean isDeclaredInStaticMethod(ClassFile classFile, ClassFile outerClassFile) {
        EnclosingMethod enclosingMethod = classFile.getAttribute(Const.ATTR_ENCLOSING_METHOD);

        if (enclosingMethod == null || enclosingMethod.getEnclosingMethodIndex() == 0) {
            return false;
        }

        ConstantPool constants = classFile.getConstantPool();
        ConstantNameAndType nameAndType = enclosingMethod.getEnclosingMethod();
        String name = nameAndType.getName(constants);
        String signature = nameAndType.getSignature(constants);

        for (Method method : outerClassFile.getMethods()) {
            if (method.getName().equals(name) && method.getSignature().equals(signature)) {
                return method.isStatic();
            }
        }
        return false;
    }

    private static boolean declaresParameter(BaseFormalParameter parameters, String name) {
        if (parameters == null) {
            return false;
        }
        for (FormalParameter parameter : parameters) {
            if (name.equals(parameter.getName())) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void visit(MethodDeclaration declaration) {}
    @Override
    public void visit(StaticInitializerDeclaration declaration) {}

    protected class UpdateFieldDeclarationsAndReferencesVisitor extends AbstractUpdateExpressionVisitor {
        private ClassFileBodyDeclaration bodyDeclaration;
        private boolean syntheticField;

        @Override
        public void visit(BodyDeclaration declaration) {
            bodyDeclaration = (ClassFileBodyDeclaration)declaration;
            safeAcceptListDeclaration(bodyDeclaration.getFieldDeclarations());
            safeAcceptListDeclaration(bodyDeclaration.getMethodDeclarations());
        }

        @Override
        public void visit(FieldDeclaration declaration) {
            syntheticField = false;
            declaration.getFieldDeclarators().accept(this);

            if (syntheticField) {
                declaration.setFlags(declaration.getFlags()|ACC_SYNTHETIC);
            }
        }

        @Override
        public void visit(FieldDeclarator declarator) {
            String name = declarator.getName();

            if (name.equals(outerTypeFieldName) || syntheticInnerFieldNames.contains(name)) {
                syntheticField = true;
            }
        }

        @Override
        public void visit(StaticInitializerDeclaration declaration) {}

        @Override
        public void visit(MethodDeclaration declaration) {
            safeAccept(declaration.getStatements());
        }

        @Override
        public void visit(NewExpression expression) {
            if (expression.getParameters() != null) {
                expression.setParameters(updateBaseExpression(expression.getParameters()));
                expression.getParameters().accept(this);
            }
            safeAccept(expression.getBodyDeclaration());
        }

        @Override
        public void visit(FieldReferenceExpression expression) {
            if (expression.getName().startsWith(OUTER_THIS_PREFIX)) {
                if (expression.getInternalTypeName().equals(bodyDeclaration.getInternalTypeName())) {
                    if (expression.getName().equals(outerTypeFieldName)) {
                        ObjectType objectType = (ObjectType)expression.getType();
                        Expression exp = expression.getExpression() == null ? expression : expression.getExpression();
                        expression.setExpression(new ObjectTypeReferenceExpression(exp.getLineNumber(), objectType.createType(null)));
                        expression.setName("this");
                    }
                } else {
                    ClassFileTypeDeclaration typeDeclaration = bodyDeclaration.getInnerTypeDeclaration(expression.getInternalTypeName());

                    if (typeDeclaration != null && typeDeclaration.isClassDeclaration()) {
                        ClassFileBodyDeclaration cfbd = (ClassFileBodyDeclaration) typeDeclaration.getBodyDeclaration();
                        String outerInternalTypeName = cfbd.getOuterBodyDeclaration().getInternalTypeName();
                        ObjectType objectType = (ObjectType)expression.getType();

                        if (outerInternalTypeName.equals(objectType.getInternalName())) {
                            Expression exp = expression.getExpression() == null ? expression : expression.getExpression();
                            expression.setExpression(new ObjectTypeReferenceExpression(exp.getLineNumber(), objectType.createType(null)));
                            expression.setName("this");
                        }
                    }
                }
            } else if (expression.getName().startsWith("val$")) {
                expression.setName(expression.getName().substring(4));
                expression.setExpression(null);
            } else {
                super.visit(expression);
            }
        }

        @Override
        protected Expression updateExpression(Expression expression) {
            if (expression.isLocalVariableReferenceExpression() && expression.getName() != null && expression.getName().equals(outerTypeFieldName) && expression.getType().isObjectType()) {
                ObjectType objectType = (ObjectType)expression.getType();
                if (bodyDeclaration.getOuterBodyDeclaration() != null && bodyDeclaration.getOuterBodyDeclaration().getInternalTypeName().equals(objectType.getInternalName())) {
                    return new FieldReferenceExpression(objectType, new ObjectTypeReferenceExpression(expression.getLineNumber(), objectType.createType(null)), objectType.getInternalName(), "this", objectType.getDescriptor());
                }
            }

            return expression;
        }
    }

    public static class UpdateNewExpressionVisitor extends AbstractJavaSyntaxVisitor {
        private final TypeMaker typeMaker;
        private ClassFileBodyDeclaration bodyDeclaration;
        private ClassFile classFile;
        private final Map<String, String> finalLocalVariableNameMap = new HashMap<>();
        private final DefaultList<ClassFileClassDeclaration> localClassDeclarations = new DefaultList<>();
        private final Set<NewExpression> newExpressions = new HashSet<>();
        private int lineNumber;

        public UpdateNewExpressionVisitor(TypeMaker typeMaker) {
            this.typeMaker = typeMaker;
        }

        @Override
        public void visit(BodyDeclaration declaration) {
            bodyDeclaration = (ClassFileBodyDeclaration)declaration;
            safeAcceptListDeclaration(bodyDeclaration.getMethodDeclarations());
        }

        @Override
        public void visit(ConstructorDeclaration declaration) {
            classFile = ((ClassFileConstructorOrMethodDeclaration)declaration).getClassFile();
            finalLocalVariableNameMap.clear();
            localClassDeclarations.clear();

            safeAccept(declaration.getStatements());

            if (! finalLocalVariableNameMap.isEmpty()) {
                UpdateParametersAndLocalVariablesVisitor visitor = new UpdateParametersAndLocalVariablesVisitor();

                declaration.getStatements().accept(visitor);

                if (declaration.getFormalParameters() != null) {
                    declaration.getFormalParameters().accept(visitor);
                }
            }

            if (! localClassDeclarations.isEmpty()) {
                localClassDeclarations.sort(Comparator.comparing(ClassFileMemberDeclaration::getFirstLineNumber));
                declaration.accept(new AddLocalClassDeclarationVisitor());
            }
        }

        @Override
        public void visit(MethodDeclaration declaration) {
            finalLocalVariableNameMap.clear();
            localClassDeclarations.clear();
            safeAccept(declaration.getStatements());

            if (! finalLocalVariableNameMap.isEmpty()) {
                UpdateParametersAndLocalVariablesVisitor visitor = new UpdateParametersAndLocalVariablesVisitor();

                declaration.getStatements().accept(visitor);

                if (declaration.getFormalParameters() != null) {
                    declaration.getFormalParameters().accept(visitor);
                }
            }

            if (! localClassDeclarations.isEmpty()) {
                localClassDeclarations.sort(Comparator.comparing(ClassFileMemberDeclaration::getFirstLineNumber));
                declaration.accept(new AddLocalClassDeclarationVisitor());
            }
        }

        @Override
        public void visit(StaticInitializerDeclaration declaration) {
            finalLocalVariableNameMap.clear();
            localClassDeclarations.clear();
            safeAccept(declaration.getStatements());

            if (! finalLocalVariableNameMap.isEmpty()) {
                declaration.getStatements().accept(new UpdateParametersAndLocalVariablesVisitor());
            }

            if (! localClassDeclarations.isEmpty()) {
                localClassDeclarations.sort(Comparator.comparing(ClassFileMemberDeclaration::getFirstLineNumber));
                declaration.accept(new AddLocalClassDeclarationVisitor());
            }
        }

        @Override
        public void visit(Statements list) {
            if (!list.isEmpty()) {
                ListIterator<Statement> iterator = list.listIterator();

                Statement s;
                while (iterator.hasNext()) {
                    //iterator.next().accept(this);
                    s = iterator.next();
                    s.accept(this);

                    if (lineNumber == Expression.UNKNOWN_LINE_NUMBER && !localClassDeclarations.isEmpty()) {
                        iterator.previous();

                        for (TypeDeclaration typeDeclaration : localClassDeclarations) {
                            iterator.add(new TypeDeclarationStatement(typeDeclaration));
                        }

                        localClassDeclarations.clear();
                        iterator.next();
                    }
                }
            }
        }

        @Override
        public void visit(NewExpression expression) {
            if (newExpressions.add(expression)) {
                ClassFileNewExpression ne = (ClassFileNewExpression)expression;
                ClassFileBodyDeclaration cfbd = null;

                if (ne.getBodyDeclaration() == null) {
                    ObjectType type = ne.getObjectType();
                    String internalName = type.getInternalName();
                    ClassFileTypeDeclaration typeDeclaration = bodyDeclaration.getInnerTypeDeclaration(internalName);

                    if (typeDeclaration == null) {
                        for (ClassFileBodyDeclaration bd = bodyDeclaration; bd != null; bd = bd.getOuterBodyDeclaration()) {
                            if (bd.getInternalTypeName().equals(internalName)) {
                                cfbd = bd;
                                break;
                            }
                        }
                    } else if (typeDeclaration.isClassDeclaration()) {
                        ClassFileClassDeclaration cfcd = (ClassFileClassDeclaration) typeDeclaration;
                        cfbd = (ClassFileBodyDeclaration) cfcd.getBodyDeclaration();

                        if (type.getQualifiedName() == null && type.getName() != null) {
                            // Local class
                            cfcd.setFlags(cfcd.getFlags() & ~ACC_SYNTHETIC);
                            localClassDeclarations.add(cfcd);
                            bodyDeclaration.removeInnerType(internalName);
                            lineNumber = ne.getLineNumber();
                        }
                    }
                } else {
                    // Anonymous class
                    cfbd = (ClassFileBodyDeclaration) ne.getBodyDeclaration();
                }

                if (cfbd != null) {
                    BaseExpression parameters = ne.getParameters();
                    BaseType parameterTypes = ne.getParameterTypes();

                    if (parameters != null) {
                        // Remove synthetic parameters
                        DefaultList<String> syntheticInnerFieldNames = cfbd.getSyntheticInnerFieldNames();

                        if (parameters.isList()) {
                            DefaultList<Expression> list = parameters.getList();
                            DefaultList<Type> types = parameterTypes.getList();

                            if (cfbd.hasOuterInstanceParameter()) {
                                // Remove outer this
                                list.removeFirst();
                                types.removeFirst();
                            }

                            if (syntheticInnerFieldNames != null) {
                                // Remove outer local variable reference
                                int size = list.size();
                                int count = syntheticInnerFieldNames.size();
                                List<Expression> lastParameters = list.subList(size - count, size);
                                Iterator<Expression> parameterIterator = lastParameters.iterator();
                                Iterator<String> syntheticInnerFieldNameIterator = syntheticInnerFieldNames.iterator();

                                Expression param;
                                String syntheticInnerFieldName;
                                while (parameterIterator.hasNext()) {
                                    param = parameterIterator.next();
                                    syntheticInnerFieldName = syntheticInnerFieldNameIterator.next();

                                    if (param.isCastExpression()) {
                                        param = param.getExpression();
                                    }

                                    if (param.isLocalVariableReferenceExpression()) {
                                        AbstractLocalVariable lv = ((ClassFileLocalVariableReferenceExpression) param).getLocalVariable();
                                        String localVariableName = syntheticInnerFieldName.substring(4);
                                        finalLocalVariableNameMap.put(lv.getName(), localVariableName);
                                    }
                                }

                                lastParameters.clear();
                                types.subList(size - count, size).clear();
                            }
                        } else if (cfbd.hasOuterInstanceParameter()) {
                            // Remove outer this
                            ne.setParameters(null);
                            ne.setParameterTypes(null);
                        } else if (syntheticInnerFieldNames != null) {
                            // Remove outer local variable reference
                            Expression param = parameters.getFirst();

                            if (param.isCastExpression()) {
                                param = param.getExpression();
                            }

                            if (param.isLocalVariableReferenceExpression()) {
                                AbstractLocalVariable lv = ((ClassFileLocalVariableReferenceExpression) param).getLocalVariable();
                                String localVariableName = syntheticInnerFieldNames.getFirst().substring(4);
                                finalLocalVariableNameMap.put(lv.getName(), localVariableName);
                                ne.setParameters(null);
                                ne.setParameterTypes(null);
                            }
                        }

                        // Is the last parameter synthetic ?
                        parameters = ne.getParameters();

                        if (!Utils.isEmpty(parameters) && parameters.getLast().isNullExpression()) {
                            parameterTypes = ne.getParameterTypes();

                            if (parameterTypes.getLast().getName() == null) {
                                // Yes. Remove it.
                                if (parameters.isList()) {
                                    parameters.getList().removeLast();
                                    parameterTypes.getList().removeLast();
                                } else {
                                    ne.setParameters(null);
                                    ne.setParameterTypes(null);
                                }
                            }
                        }
                    }
                }
            }

            safeAccept(expression.getParameters());
        }

        @Override
        public void visit(SuperConstructorInvocationExpression expression) {
            ClassFileSuperConstructorInvocationExpression scie = (ClassFileSuperConstructorInvocationExpression)expression;
            BaseExpression parameters = scie.getParameters();

            if (!Utils.isEmpty(parameters)) {
                // Remove outer 'this' reference parameter
                Type firstParameterType = parameters.getFirst().getType();

                if (firstParameterType.isObjectType() && !classFile.isStatic() && bodyDeclaration.hasOuterInstanceParameter()) {
                    TypeMaker.TypeTypes superTypeTypes = typeMaker.makeTypeTypes(classFile.getSuperTypeName());

                    if (superTypeTypes != null && superTypeTypes.getThisType().isInnerObjectType() && typeMaker.isRawTypeAssignable(superTypeTypes.getThisType().getOuterType(), (ObjectType)firstParameterType)) {
                        scie.setParameters(removeFirstItem(parameters));
                        scie.setParameterTypes(removeFirstItem(scie.getParameterTypes()));
                    }
                }

                // Remove last synthetic parameter
                expression.setParameters(removeLastSyntheticParameter(scie.getParameters(), scie.getParameterTypes()));
            }
        }

        @Override
        public void visit(ConstructorInvocationExpression expression) {
            ClassFileConstructorInvocationExpression cie = (ClassFileConstructorInvocationExpression)expression;
            BaseExpression parameters = cie.getParameters();

            if (!Utils.isEmpty(parameters)) {
                // Remove outer this reference parameter
                if (bodyDeclaration.hasOuterInstanceParameter()) {
                    cie.setParameters(removeFirstItem(parameters));
                    cie.setParameterTypes(removeFirstItem(cie.getParameterTypes()));
                }

                // Remove last synthetic parameter
                cie.setParameters(removeLastSyntheticParameter(cie.getParameters(), cie.getParameterTypes()));
            }

            safeAccept(parameters);
        }

        protected BaseExpression removeFirstItem(BaseExpression parameters) {
            if (parameters.isList()) {
                parameters.getList().removeFirst();
            } else {
                parameters = null;
            }

            return parameters;
        }

        protected BaseType removeFirstItem(BaseType types) {
            if (types.isList()) {
                types.getList().removeFirst();
            } else {
                types = null;
            }

            return types;
        }

        protected BaseExpression removeLastSyntheticParameter(BaseExpression parameters, BaseType parameterTypes) {
            // Is the last parameter synthetic ?
            if (!Utils.isEmpty(parameters) && parameters.getLast().isNullExpression() && parameterTypes.getLast().getName() == null) {
                // Yes. Remove it.
                if (parameters.isList()) {
                    parameters.getList().removeLast();
                } else {
                    parameters = null;
                }
            }

            return parameters;
        }

        protected class UpdateParametersAndLocalVariablesVisitor extends AbstractJavaSyntaxVisitor {
            private boolean fina1;

            @Override
            public void visit(FormalParameter declaration) {
                if (finalLocalVariableNameMap.containsKey(declaration.getName())) {
                    declaration.setFinal(true);
                    declaration.setName(finalLocalVariableNameMap.get(declaration.getName()));
                }
            }

            @Override
            public void visit(CatchClause statement) {
                if (finalLocalVariableNameMap.containsKey(statement.getName())) {
                    statement.setFinal(true);
                }
                super.visit(statement);
            }

            @Override
            public void visit(ForEachStatement statement) {
                if (finalLocalVariableNameMap.containsKey(statement.getName())) {
                    statement.setFinal(true);
                }
                super.visit(statement);
            }

            @Override
            public void visit(LocalVariableDeclarationStatement statement) {
                fina1 = false;
                statement.getLocalVariableDeclarators().accept(this);
                statement.setFinal(fina1);
            }

            @Override
            public void visit(LocalVariableDeclaration declaration) {
                fina1 = false;
                declaration.getLocalVariableDeclarators().accept(this);
                declaration.setFinal(fina1);
            }

            @Override
            public void visit(LocalVariableDeclarator declarator) {
                if (finalLocalVariableNameMap.containsKey(declarator.getName())) {
                    fina1 = true;
                    declarator.setName(finalLocalVariableNameMap.get(declarator.getName()));
                }
            }
        }

        protected class AddLocalClassDeclarationVisitor extends AbstractJavaSyntaxVisitor {
            private final SearchFirstLineNumberVisitor searchFirstLineNumberVisitor = new SearchFirstLineNumberVisitor();
            private int firstLineNumber = Expression.UNKNOWN_LINE_NUMBER;

            @Override
            public void visit(ConstructorDeclaration declaration) {
                ClassFileConstructorDeclaration cfcd = (ClassFileConstructorDeclaration)declaration;
                cfcd.setStatements(addLocalClassDeclarations(cfcd.getStatements()));
            }

            @Override
            public void visit(MethodDeclaration declaration) {
                ClassFileMethodDeclaration cfmd = (ClassFileMethodDeclaration)declaration;
                cfmd.setStatements(addLocalClassDeclarations(cfmd.getStatements()));
            }

            @Override
            public void visit(StaticInitializerDeclaration declaration) {
                ClassFileStaticInitializerDeclaration cfsid = (ClassFileStaticInitializerDeclaration)declaration;
                cfsid.setStatements(addLocalClassDeclarations(cfsid.getStatements()));
            }

            protected BaseStatement addLocalClassDeclarations(BaseStatement statements) {
                if (!localClassDeclarations.isEmpty()) {
                    if (statements.isStatements()) {
                        statements.accept(this);
                    } else {
                        ClassFileClassDeclaration declaration = localClassDeclarations.get(0);

                        searchFirstLineNumberVisitor.init();
                        statements.accept(searchFirstLineNumberVisitor);

                        if (searchFirstLineNumberVisitor.getLineNumber() != -1) {
                            firstLineNumber = searchFirstLineNumberVisitor.getLineNumber();
                        }

                        if (declaration.getFirstLineNumber() <= firstLineNumber) {
                            Statements list = new Statements();
                            Iterator<ClassFileClassDeclaration> declarationIterator = localClassDeclarations.iterator();

                            list.add(new TypeDeclarationStatement(declaration));
                            declarationIterator.next();
                            declarationIterator.remove();

                            while (declarationIterator.hasNext() && (declaration = declarationIterator.next()).getFirstLineNumber() <= firstLineNumber) {
                                list.add(new TypeDeclarationStatement(declaration));
                                declarationIterator.remove();
                            }

                            if (statements.isList()) {
                                list.addAll(statements.getList());
                            } else {
                                list.add(statements.getFirst());
                            }
                            statements = list;
                        } else {
                            statements.accept(this);
                        }
                    }
                }

                return statements;
            }

            @Override
            public void visit(Statements list) {
                if (!localClassDeclarations.isEmpty() && !list.isEmpty()) {
                    ListIterator<Statement> statementIterator = list.listIterator();
                    Iterator<ClassFileClassDeclaration> declarationIterator = localClassDeclarations.iterator();
                    ClassFileClassDeclaration declaration = declarationIterator.next();

                    Statement statement;
                    while (statementIterator.hasNext()) {
                        statement = statementIterator.next();

                        searchFirstLineNumberVisitor.init();
                        statement.accept(searchFirstLineNumberVisitor);

                        if (searchFirstLineNumberVisitor.getLineNumber() != -1) {
                            firstLineNumber = searchFirstLineNumberVisitor.getLineNumber();
                        }

                        while (declaration.getFirstLineNumber() <= firstLineNumber) {
                            statementIterator.previous();
                            statementIterator.add(new TypeDeclarationStatement(declaration));
                            statementIterator.next();
                            declarationIterator.remove();

                            if (!declarationIterator.hasNext()) {
                                return;
                            }

                            declaration = declarationIterator.next();
                        }
                    }
                }
            }
        }
    }
}
