/*
 * Copyright (c) 2026 Nicolas Baumann (@nbauma109), with Claude Sonnet 5.5.
 * This project is distributed under the GPLv3 license.
 * This is a Copyleft license that gives the user the right to use,
 * copy and modify the code freely for non-commercial purposes.
 */
package org.jd.core.v1.service.converter.classfiletojavasyntax.util;

import org.apache.bcel.classfile.ClassFormatException;
import org.apache.bcel.classfile.ClassParser;
import org.apache.bcel.classfile.Field;
import org.apache.bcel.classfile.JavaClass;
import org.jd.core.v1.api.loader.Loader;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** What the declaration of a class says, read from its class file: the answers are 'unknown' (false or null) when the class cannot be loaded */
public final class ClassMetadata {
    private static final String OBJECT = "java/lang/Object";
    private static final String RECORD = "java/lang/Record";

    private ClassMetadata() {}

    /** @return true if the class, one of its superclasses or one of its interfaces declares a field of this name */
    public static boolean declaresField(Loader loader, String internalName, String fieldName) {
        return declaresField(loader, internalName, fieldName, new HashSet<>());
    }

    private static boolean declaresField(Loader loader, String internalName, String fieldName, Set<String> visited) {
        JavaClass javaClass = internalName == null || OBJECT.equals(internalName) || !visited.add(internalName) ? null : parse(loader, internalName);

        if (javaClass == null) {
            return false;
        }
        for (Field field : javaClass.getFields()) {
            if (field.getName().equals(fieldName)) {
                return true;
            }
        }
        if (declaresField(loader, internalNameOf(javaClass.getSuperclassName()), fieldName, visited)) {
            return true;
        }
        for (String interfaceName : javaClass.getInterfaceNames()) {
            if (declaresField(loader, internalNameOf(interfaceName), fieldName, visited)) {
                return true;
            }
        }
        return false;
    }

    /** @return the names of the components of the record, in the order of its declaration; null if the class is not a record or is unknown */
    public static List<String> recordComponentNames(Loader loader, String internalName) {
        JavaClass javaClass = internalName == null ? null : parse(loader, internalName);

        if (javaClass == null || !RECORD.equals(internalNameOf(javaClass.getSuperclassName()))) {
            return null;
        }

        List<String> names = new ArrayList<>();

        for (Field field : javaClass.getFields()) {
            if (!field.isStatic()) {
                names.add(field.getName());
            }
        }
        return names;
    }

    private static String internalNameOf(String className) {
        return className == null ? null : className.replace('.', '/');
    }

    private static JavaClass parse(Loader loader, String internalName) {
        try {
            if (loader == null || !loader.canLoad(internalName)) {
                return null;
            }
            return new ClassParser(new ByteArrayInputStream(loader.load(internalName)), internalName).parse();
        } catch (IOException | ClassFormatException e) {
            return null;
        }
    }
}
