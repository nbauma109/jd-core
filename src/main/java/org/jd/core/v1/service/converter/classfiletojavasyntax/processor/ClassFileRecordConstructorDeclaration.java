/*
 * Copyright (c) 2025 GPLv3.
 * This project is distributed under the GPLv3 license.
 * This is a Copyleft license that gives the user the right to use,
 * copy and modify the code freely for non-commercial purposes.
 */

package org.jd.core.v1.service.converter.classfiletojavasyntax.processor;

import org.apache.bcel.classfile.Method;
import org.jd.core.v1.model.classfile.ClassFile;
import org.jd.core.v1.model.javasyntax.declaration.BaseFormalParameter;
import org.jd.core.v1.model.javasyntax.declaration.RecordConstructorDeclaration;
import org.jd.core.v1.model.javasyntax.reference.BaseAnnotationReference;
import org.jd.core.v1.model.javasyntax.statement.BaseStatement;
import org.jd.core.v1.model.javasyntax.type.BaseType;
import org.jd.core.v1.model.javasyntax.type.BaseTypeParameter;
import org.jd.core.v1.model.javasyntax.type.Type;
import org.jd.core.v1.model.javasyntax.type.TypeArgument;
import org.jd.core.v1.service.converter.classfiletojavasyntax.model.javasyntax.declaration.ClassFileBodyDeclaration;
import org.jd.core.v1.service.converter.classfiletojavasyntax.model.javasyntax.declaration.ClassFileConstructorOrMethodDeclaration;

import java.util.Map;

public class ClassFileRecordConstructorDeclaration extends RecordConstructorDeclaration
        implements ClassFileConstructorOrMethodDeclaration {

    private final ClassFileBodyDeclaration bodyDeclaration;
    private final ClassFile classFile;
    private final Method method;
    private final BaseType parameterTypes;
    private final Map<String, TypeArgument> bindings;
    private final Map<String, BaseType> typeBounds;
    private int firstLineNumber;

    public ClassFileRecordConstructorDeclaration(ClassFileBodyDeclaration bodyDeclaration, ClassFile classFile, Method method, BaseAnnotationReference annotationReferences,
            BaseTypeParameter typeParameters, BaseType parameterTypes, BaseType exceptionTypes,
            Map<String, TypeArgument> bindings, Map<String, BaseType> typeBounds, int firstLineNumber) {
        super(annotationReferences, method.getAccessFlags(), typeParameters, null, exceptionTypes, method.getSignature(), null);
        this.bodyDeclaration = bodyDeclaration;
        this.classFile = classFile;
        this.method = method;
        this.parameterTypes = parameterTypes;
        this.bindings = bindings;
        this.typeBounds = typeBounds;
        this.firstLineNumber = firstLineNumber;
    }

    @Override
    public void setFormalParameters(BaseFormalParameter formalParameters) {
        this.formalParameters = formalParameters;
    }

    @Override
    public void setStatements(BaseStatement statements) {
        this.statements = statements;
    }

    @Override
    public ClassFileBodyDeclaration getBodyDeclaration() {
        return bodyDeclaration;
    }

    @Override
    public ClassFile getClassFile() {
        return classFile;
    }

    @Override
    public Method getMethod() {
        return method;
    }

    @Override
    public BaseType getParameterTypes() {
        return parameterTypes;
    }

    @Override
    public Type getReturnedType() {
        return null;
    }

    public Map<String, TypeArgument> getBindings() {
        return bindings;
    }

    public Map<String, BaseType> getTypeBounds() {
        return typeBounds;
    }

    public int getFirstLineNumber() {
        return firstLineNumber;
    }

    public void setFirstLineNumber(int firstLineNumber) {
        this.firstLineNumber = firstLineNumber;
    }

    @Override
    public String toString() {
        return "ClassFileRecordConstructorDeclaration{" + classFile.getInternalTypeName() + ' ' + descriptor + ", firstLineNumber=" + firstLineNumber + "}";
    }

}
