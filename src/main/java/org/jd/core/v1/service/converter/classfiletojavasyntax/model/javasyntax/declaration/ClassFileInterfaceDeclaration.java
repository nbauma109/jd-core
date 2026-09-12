/*
 * Copyright (c) 2008, 2019 Emmanuel Dupuy.
 * This project is distributed under the GPLv3 license.
 * This is a Copyleft license that gives the user the right to use,
 * copy and modify the code freely for non-commercial purposes.
 */

package org.jd.core.v1.service.converter.classfiletojavasyntax.model.javasyntax.declaration;

import org.jd.core.v1.model.javasyntax.declaration.InterfaceDeclaration;
import org.jd.core.v1.model.javasyntax.reference.BaseAnnotationReference;
import org.jd.core.v1.model.javasyntax.type.BaseType;
import org.jd.core.v1.model.javasyntax.type.BaseTypeParameter;

import java.util.Optional;

public class ClassFileInterfaceDeclaration extends InterfaceDeclaration implements ClassFileTypeDeclaration {
    private final int firstLineNumber;

    public ClassFileInterfaceDeclaration(BaseAnnotationReference annotationReferences, int flags, String internalName, String name, BaseTypeParameter typeParameters, BaseType interfaces, ClassFileBodyDeclaration bodyDeclaration) {
        this(annotationReferences, flags, internalName, name, typeParameters, interfaces, null, bodyDeclaration);
    }

    public ClassFileInterfaceDeclaration(BaseAnnotationReference annotationReferences, int flags, String internalName, String name, BaseTypeParameter typeParameters, BaseType interfaces, BaseType permittedSubclasses, ClassFileBodyDeclaration bodyDeclaration) {
        super(annotationReferences, flags, internalName, name, typeParameters, interfaces, permittedSubclasses, bodyDeclaration);
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
        return "ClassFileInterfaceDeclaration{" + internalTypeName + ", firstLineNumber=" + firstLineNumber + "}";
    }
}
