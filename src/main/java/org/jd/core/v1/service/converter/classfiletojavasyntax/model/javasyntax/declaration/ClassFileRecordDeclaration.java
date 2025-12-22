/*
 * Copyright (c) 2025 GPLv3.
 * This project is distributed under the GPLv3 license.
 * This is a Copyleft license that gives the user the right to use,
 * copy and modify the code freely for non-commercial purposes.
 */

package org.jd.core.v1.service.converter.classfiletojavasyntax.model.javasyntax.declaration;

import org.jd.core.v1.model.javasyntax.declaration.RecordDeclaration;
import org.jd.core.v1.model.javasyntax.reference.BaseAnnotationReference;
import org.jd.core.v1.model.javasyntax.type.BaseType;
import org.jd.core.v1.model.javasyntax.type.BaseTypeParameter;

import java.util.List;
import java.util.Optional;

public class ClassFileRecordDeclaration extends RecordDeclaration implements ClassFileTypeDeclaration {
    private final int firstLineNumber;

    public ClassFileRecordDeclaration(BaseAnnotationReference annotationReferences, int flags, String internalName, String name, BaseTypeParameter typeParameters, List<RecordComponent> recordComponents, BaseType interfaces, ClassFileBodyDeclaration bodyDeclaration) {
        super(annotationReferences, flags, internalName, name, typeParameters, recordComponents, interfaces, bodyDeclaration);
        this.firstLineNumber = Optional.ofNullable(bodyDeclaration).map(ClassFileMemberDeclaration::getFirstLineNumber).orElse(0);
    }

    @Override
    public int getFirstLineNumber() {
        return firstLineNumber;
    }

    public void setFlags(int flags) {
        this.flags = flags;
    }

    @Override
    public String toString() {
        return "ClassFileRecordDeclaration{" + internalTypeName + ", firstLineNumber=" + firstLineNumber + "}";
    }
}
