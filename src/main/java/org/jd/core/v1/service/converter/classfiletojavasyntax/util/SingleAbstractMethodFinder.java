/*
 * Copyright (c) 2008, 2019 Emmanuel Dupuy.
 * This project is distributed under the GPLv3 license.
 * This is a Copyleft license that gives the user the right to use,
 * copy and modify the code freely for non-commercial purposes.
 */

package org.jd.core.v1.service.converter.classfiletojavasyntax.util;

import org.apache.bcel.Const;
import org.apache.bcel.classfile.ClassFormatException;
import org.apache.bcel.classfile.ClassParser;
import org.apache.bcel.classfile.JavaClass;
import org.jd.core.v1.api.loader.Loader;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
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
        Optional<Map<String, Member>> readMembers = membersOf(internalName, new HashMap<>());

        if (readMembers.isEmpty()) {
            return NONE;
        }

        Map<String, Member> members = readMembers.get();

        // A lambda is only created for a functional interface: the abstract methods which remain are override-equivalent once the type
        // arguments of the super interfaces are substituted (A<String, T> and B<T>: apply(Object) and apply(String)); they all are
        // the same method, which is seen with the most specific returned type
        Member found = null;

        for (Member member : members.values()) {
            if (member.isAbstract) {
                if (found != null && !(found.method.name().equals(member.method.name()) && arity(found.method.descriptor()) == arity(member.method.descriptor()))) {
                    return NONE;
                }
                found = found == null ? member : moreSpecific(found, member);
            }
        }
        return found == null ? NONE : found.method.toArray();
    }

    private static int arity(String descriptor) {
        int count = 0;
        int index = 1;

        while (descriptor.charAt(index) != ')') {
            while (descriptor.charAt(index) == '[') {
                index++;
            }
            index = descriptor.charAt(index) == 'L' ? descriptor.indexOf(';', index) + 1 : index + 1;
            count++;
        }
        return count;
    }

    /** The interface which declares a method, its name and descriptor */
    private record MethodRef(String declaringInterface, String name, String descriptor) {
        String[] toArray() {
            return new String[] {declaringInterface, name, descriptor};
        }
    }

    /** An inherited member: its declaration, and the interface which declares it with all its super interfaces */
    private record Member(MethodRef method, boolean isAbstract, boolean returnsTypeVariable, Set<String> ancestors) {
    }

    /** @return the members of an interface by signature, the inherited ones included (the most specific declaration wins), or empty if an interface could not be read */
    private Optional<Map<String, Member>> membersOf(String internalName, Map<String, Optional<Map<String, Member>>> known) {
        if (known.containsKey(internalName)) {
            return known.get(internalName);
        }
        Optional<Map<String, Member>> members = readMembers(internalName, known);

        known.put(internalName, members);
        return members;
    }

    private Optional<Map<String, Member>> readMembers(String internalName, Map<String, Optional<Map<String, Member>>> known) {
        try {
            if (!loader.canLoad(internalName)) {
                return Optional.empty();
            }
            JavaClass javaClass = new ClassParser(new ByteArrayInputStream(loader.load(internalName)), internalName).parse();
            if (!javaClass.isInterface()) {
                return Optional.empty();
            }

            Map<String, Member> members = new HashMap<>();
            Set<String> ancestors = new HashSet<>();

            ancestors.add(internalName);
            for (String superInterface : javaClass.getInterfaceNames()) {
                Optional<Map<String, Member>> inherited = membersOf(superInterface.replace('.', '/'), known);

                if (inherited.isEmpty()) {
                    return Optional.empty();
                }
                ancestors.addAll(ancestorsOf.get(superInterface.replace('.', '/')));
                inherited.get().forEach((signature, member) -> members.merge(signature, member, SingleAbstractMethodFinder::moreSpecific));
            }
            addDeclaredMembers(javaClass, internalName, members, ancestors);
            ancestorsOf.put(internalName, ancestors);
            return Optional.of(members);
        } catch (IOException | ClassFormatException e) {
            return Optional.empty();
        }
    }

    /**
     * The methods which are declared come first: the bridge methods (the compiler's copies of the declarations whose returned type
     * is covariant, or whose parameter types are generic) only take the place of the ones which are inherited
     */
    private static void addDeclaredMembers(JavaClass javaClass, String internalName, Map<String, Member> members, Set<String> ancestors) {
        Set<String> declared = new HashSet<>();

        for (boolean bridges : new boolean[] {false, true}) {
            for (org.apache.bcel.classfile.Method method : javaClass.getMethods()) {
                String declaration = method.getName() + method.getSignature();
                // The methods whose parameters are the same are override-equivalent, whatever their (covariant) returned type is
                String signature = declaration.substring(0, declaration.indexOf(')') + 1);

                if (isBridge(method) == bridges && !method.isStatic() && !method.isPrivate() && !OBJECT_METHODS.contains(declaration)
                        && (!bridges || !declared.contains(signature))) {
                    members.put(signature, new Member(new MethodRef(internalName, method.getName(), method.getSignature()), method.isAbstract(),
                            returnsTypeVariable(method), ancestors));
                    declared.add(signature);
                }
            }
        }
    }

    private static boolean isBridge(org.apache.bcel.classfile.Method method) {
        return (method.getAccessFlags() & Const.ACC_BRIDGE) != 0 || method.isSynthetic();
    }

    private static boolean returnsTypeVariable(org.apache.bcel.classfile.Method method) {
        String genericSignature = method.getGenericSignature();

        if (genericSignature == null) {
            return false;
        }
        int start = genericSignature.indexOf(')') + 1;
        int throwsIndex = genericSignature.indexOf('^', start);

        // (the generic exceptions which follow the returned type, '^TE;', are not part of it)
        return containsTypeVariable(genericSignature.substring(0, throwsIndex < 0 ? genericSignature.length() : throwsIndex), start);
    }

    /** @return true if a type variable (T...;) is used anywhere in the type signature which starts at the index (T[], List&lt;T&gt;, ...) */
    private static boolean containsTypeVariable(String signature, int start) {
        int index = start;

        while (index < signature.length()) {
            char c = signature.charAt(index);

            if (c == 'T') {
                return true;
            }
            if (c == 'L' || c == '.') {
                // a class name: it stops at the type arguments, at the end of the type, or at the name of an inner class
                index++;
                while (index < signature.length() && "<;.".indexOf(signature.charAt(index)) < 0) {
                    index++;
                }
            } else {
                index++;
            }
        }
        return false;
    }

    /** The declaration of a sub interface overrides the one of its super interface; unrelated ones are override-equivalent: the generic return type is the most specific */
    private static Member moreSpecific(Member first, Member second) {
        if (first.method.declaringInterface.equals(second.method.declaringInterface)) {
            return first;
        }
        if (first.ancestors.contains(second.method.declaringInterface)) {
            return first;
        }
        if (second.ancestors.contains(first.method.declaringInterface)) {
            return second;
        }
        return second.returnsTypeVariable && !first.returnsTypeVariable ? second : first;
    }
}
