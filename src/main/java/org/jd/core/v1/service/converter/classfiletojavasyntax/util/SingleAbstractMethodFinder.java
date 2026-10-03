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
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/** Finds the only abstract method of a functional interface, the inherited ones included. */
public class SingleAbstractMethodFinder {
    private static final String[] NONE = new String[0];

    private final Loader loader;
    private final Map<String, String[]> singleAbstractMethods = new HashMap<>();

    public SingleAbstractMethodFinder(Loader loader) {
        this.loader = loader;
    }

    /** @return the declaring interface, name and descriptor of the only abstract method of an interface, or an empty array */
    public String[] find(String internalName) {
        return singleAbstractMethods.computeIfAbsent(internalName, this::findUncached);
    }

    private String[] findUncached(String internalName) {
        Map<String, String[]> abstractMethods = new HashMap<>();
        Set<String> overridden = new HashSet<>();

        if (!collectAbstractMethods(internalName, abstractMethods, overridden, new HashSet<>())) {
            return NONE;
        }
        // A default method may be met after the abstract method it overrides
        abstractMethods.keySet().removeAll(overridden);
        return abstractMethods.size() == 1 ? abstractMethods.values().iterator().next() : NONE;
    }

    /** @return false if an interface could not be read */
    private boolean collectAbstractMethods(String internalName, Map<String, String[]> abstractMethods, Set<String> overridden, Set<String> visited) {
        if (!visited.add(internalName)) {
            return true;
        }
        try {
            if (!loader.canLoad(internalName)) {
                return false;
            }
            JavaClass javaClass = new ClassParser(new ByteArrayInputStream(loader.load(internalName)), internalName).parse();
            if (!javaClass.isInterface()) {
                return false;
            }
            for (org.apache.bcel.classfile.Method method : javaClass.getMethods()) {
                String signature = method.getName() + method.getSignature();
                if (method.isStatic() || method.isPrivate()) {
                    continue;
                }
                if (!method.isAbstract()) {
                    overridden.add(signature);
                } else if (!"equals(Ljava/lang/Object;)Z".equals(signature) && !"hashCode()I".equals(signature)
                        && !"toString()Ljava/lang/String;".equals(signature)) {
                    abstractMethods.putIfAbsent(signature, new String[] {internalName, method.getName(), method.getSignature()});
                }
            }
            for (String superInterface : javaClass.getInterfaceNames()) {
                if (!collectAbstractMethods(superInterface.replace('.', '/'), abstractMethods, overridden, visited)) {
                    return false;
                }
            }
            return true;
        } catch (IOException | ClassFormatException e) {
            return false;
        }
    }
}
