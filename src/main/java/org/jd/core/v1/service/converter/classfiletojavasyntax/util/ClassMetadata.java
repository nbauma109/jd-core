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
import org.apache.bcel.classfile.Method;
import org.jd.core.v1.api.loader.Loader;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
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

    /** @return true if the class can be read */
    public static boolean isLoadable(Loader loader, String internalName) {
        return internalName != null && parse(loader, internalName) != null;
    }

    /** @return true if the class, one of its superclasses or one of its interfaces declares a method of this name and descriptor */
    public static boolean declaresMethod(Loader loader, String internalName, String name, String descriptor) {
        return declaresMethod(loader, internalName, name, descriptor, false, new HashSet<>());
    }

    /** @return true if the class, one of its superclasses or one of its interfaces declares a public method of this name and descriptor */
    public static boolean declaresPublicMethod(Loader loader, String internalName, String name, String descriptor) {
        return declaresMethod(loader, internalName, name, descriptor, true, new HashSet<>());
    }

    private static boolean declaresMethod(Loader loader, String internalName, String name, String descriptor, boolean publicOnly, Set<String> visited) {
        JavaClass javaClass = internalName == null || !visited.add(internalName) ? null : parse(loader, internalName);

        if (javaClass == null) {
            return false;
        }
        for (Method method : javaClass.getMethods()) {
            // A private method is not inherited: the one of a subclass does not override it
            if (!method.isPrivate() && (!publicOnly || method.isPublic()) && method.getName().equals(name) && method.getSignature().equals(descriptor)) {
                return true;
            }
        }
        if (declaresMethod(loader, internalNameOf(javaClass.getSuperclassName()), name, descriptor, publicOnly, visited)) {
            return true;
        }
        for (String interfaceName : javaClass.getInterfaceNames()) {
            if (declaresMethod(loader, internalNameOf(interfaceName), name, descriptor, publicOnly, visited)) {
                return true;
            }
        }
        return false;
    }

    /** A component of a record: the name of its accessor and the descriptor of its type */
    public record Component(String name, String descriptor) {}

    /** @return the components of the record, in the order of its declaration; none if the class is not a record or is unknown */
    public static List<Component> recordComponents(Loader loader, String internalName) {
        JavaClass javaClass = internalName == null ? null : parse(loader, internalName);

        if (javaClass == null || !RECORD.equals(internalNameOf(javaClass.getSuperclassName()))) {
            return Collections.emptyList();
        }

        List<Component> components = new ArrayList<>();

        for (Field field : javaClass.getFields()) {
            if (!field.isStatic()) {
                components.add(new Component(field.getName(), field.getSignature()));
            }
        }
        return components;
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
