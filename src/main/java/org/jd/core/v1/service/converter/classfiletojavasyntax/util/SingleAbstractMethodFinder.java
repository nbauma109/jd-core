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
    private static final Set<String> OBJECT_METHODS = Set.of("equals(Ljava/lang/Object;)Z", "hashCode()I", "toString()Ljava/lang/String;");

    private final Loader loader;
    private final Map<String, String[]> singleAbstractMethods = new HashMap<>();
    private final Map<String, Set<String>> ancestorsOf = new HashMap<>();

    public SingleAbstractMethodFinder(Loader loader) {
        this.loader = loader;
    }

    /** @return the declaring interface, name and descriptor of the only abstract method of an interface, or an empty array */
    public String[] find(String internalName) {
        return singleAbstractMethods.computeIfAbsent(internalName, this::findUncached);
    }

    private String[] findUncached(String internalName) {
        Map<String, Member> members = membersOf(internalName, new HashMap<>());

        if (members == null) {
            return NONE;
        }

        String[] found = NONE;

        for (Member member : members.values()) {
            if (member.isAbstract) {
                if (found != NONE) {
                    return NONE;
                }
                found = member.method;
            }
        }
        return found;
    }

    /** An inherited member: its declaration, and the interface which declares it with all its super interfaces */
    private record Member(String[] method, boolean isAbstract, boolean returnsTypeVariable, Set<String> ancestors) {
    }

    /** @return the members of an interface by signature, the inherited ones included (the most specific declaration wins), or null if an interface could not be read */
    private Map<String, Member> membersOf(String internalName, Map<String, Map<String, Member>> known) {
        if (known.containsKey(internalName)) {
            return known.get(internalName);
        }
        Map<String, Member> members = readMembers(internalName, known);

        known.put(internalName, members);
        return members;
    }

    private Map<String, Member> readMembers(String internalName, Map<String, Map<String, Member>> known) {
        try {
            if (!loader.canLoad(internalName)) {
                return null;
            }
            JavaClass javaClass = new ClassParser(new ByteArrayInputStream(loader.load(internalName)), internalName).parse();
            if (!javaClass.isInterface()) {
                return null;
            }

            Map<String, Member> members = new HashMap<>();
            Set<String> ancestors = new HashSet<>();

            ancestors.add(internalName);
            for (String superInterface : javaClass.getInterfaceNames()) {
                Map<String, Member> inherited = membersOf(superInterface.replace('.', '/'), known);

                if (inherited == null) {
                    return null;
                }
                ancestors.addAll(ancestorsOf.get(superInterface.replace('.', '/')));
                inherited.forEach((signature, member) -> members.merge(signature, member, SingleAbstractMethodFinder::moreSpecific));
            }
            for (org.apache.bcel.classfile.Method method : javaClass.getMethods()) {
                String signature = method.getName() + method.getSignature();

                if (!method.isStatic() && !method.isPrivate() && !OBJECT_METHODS.contains(signature)) {
                    members.put(signature, new Member(new String[] {internalName, method.getName(), method.getSignature()}, method.isAbstract(),
                            returnsTypeVariable(method), ancestors));
                }
            }
            ancestorsOf.put(internalName, ancestors);
            return members;
        } catch (IOException | ClassFormatException e) {
            return null;
        }
    }

    private static boolean returnsTypeVariable(org.apache.bcel.classfile.Method method) {
        String genericSignature = method.getGenericSignature();

        return genericSignature != null && genericSignature.substring(genericSignature.indexOf(')') + 1).startsWith("T");
    }

    /** The declaration of a sub interface overrides the one of its super interface; unrelated ones are override-equivalent: the generic return type is the most specific */
    private static Member moreSpecific(Member first, Member second) {
        if (first.method[0].equals(second.method[0])) {
            return first;
        }
        if (first.ancestors.contains(second.method[0])) {
            return first;
        }
        if (second.ancestors.contains(first.method[0])) {
            return second;
        }
        return second.returnsTypeVariable && !first.returnsTypeVariable ? second : first;
    }
}
