/*
 * Copyright (c) 2008, 2019 Emmanuel Dupuy.
 * This project is distributed under the GPLv3 license.
 * This is a Copyleft license that gives the user the right to use,
 * copy and modify the code freely for non-commercial purposes.
 */

package org.jd.core.v1.service.converter.classfiletojavasyntax.util;

import org.apache.bcel.classfile.ClassFormatException;
import org.apache.bcel.classfile.ClassParser;
import org.apache.bcel.classfile.JavaClass;
import org.jd.core.v1.api.loader.Loader;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

/** Finds the (erased) parameter descriptors of the methods of a class, its superclasses and its interfaces, which have a given name. */
public class InheritedMethodFinder {
    private final Loader loader;

    public InheritedMethodFinder(Loader loader) {
        this.loader = loader;
    }

    /** @return the descriptors of the parameters, like '(Ljava/lang/Object;)', or null if a class could not be read */
    public Set<String> parameterDescriptors(String internalName, String methodName, String inheritingPackage) {
        Set<String> descriptors = new HashSet<>();

        return collect(internalName, methodName, inheritingPackage, descriptors, new HashSet<>()) ? descriptors : null;
    }

    static String packageOf(String internalName) {
        int index = internalName.lastIndexOf('/');

        return index < 0 ? "" : internalName.substring(0, index);
    }

    private boolean collect(String internalName, String methodName, String inheritingPackage, Set<String> descriptors, Set<String> visited) {
        if (!visited.add(internalName)) {
            return true;
        }
        try {
            if (!loader.canLoad(internalName)) {
                return false;
            }

            JavaClass javaClass = new ClassParser(new ByteArrayInputStream(loader.load(internalName)), internalName).parse();

            for (org.apache.bcel.classfile.Method method : javaClass.getMethods()) {
                // (a method which is not public nor protected is only inherited within its package)
                if (method.getName().equals(methodName) && !method.isPrivate()
                        && (method.isPublic() || method.isProtected() || packageOf(internalName).equals(inheritingPackage))) {
                    String signature = method.getSignature();

                    descriptors.add(signature.substring(0, signature.indexOf(')') + 1));
                }
            }
            if (javaClass.getSuperclassName() != null && !"java.lang.Object".equals(javaClass.getSuperclassName())
                    && !collect(javaClass.getSuperclassName().replace('.', '/'), methodName, inheritingPackage, descriptors, visited)) {
                return false;
            }
            for (String superInterface : javaClass.getInterfaceNames()) {
                if (!collect(superInterface.replace('.', '/'), methodName, inheritingPackage, descriptors, visited)) {
                    return false;
                }
            }
            return true;
        } catch (IOException | ClassFormatException e) {
            return false;
        }
    }
}
