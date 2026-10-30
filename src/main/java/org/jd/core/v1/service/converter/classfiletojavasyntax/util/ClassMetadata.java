/*
 * Copyright (c) 2026 Nicolas Baumann (@nbauma109), with Claude Sonnet 5.5.
 * This project is distributed under the GPLv3 license.
 * This is a Copyleft license that gives the user the right to use,
 * copy and modify the code freely for non-commercial purposes.
 */
package org.jd.core.v1.service.converter.classfiletojavasyntax.util;

import org.apache.bcel.Const;
import org.apache.bcel.classfile.ClassFormatException;
import org.apache.bcel.classfile.ClassParser;
import org.apache.bcel.classfile.Field;
import org.apache.bcel.classfile.InnerClass;
import org.apache.bcel.classfile.InnerClasses;
import org.apache.bcel.classfile.Attribute;
import org.apache.bcel.classfile.JavaClass;
import org.apache.bcel.classfile.Signature;
import org.apache.bcel.classfile.Method;
import org.jd.core.v1.api.loader.Loader;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/** What the declaration of a class says, read from its class file: the answers are 'unknown' (false or null) when the class cannot be loaded */
public final class ClassMetadata {
    /** The public methods of 'Object' which an interface can redeclare without making them abstract methods of its own: 'equals(Other)' is another method */
    private static final Set<String> OBJECT_METHODS = Set.of("equals(Ljava/lang/Object;)Z", "hashCode()I", "toString()Ljava/lang/String;");

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

    /** @return true if the class is the ancestor, or extends or implements it (known by the classes which can be read) */
    public static boolean isSubtype(Loader loader, String internalName, String ancestor) {
        return isSubtype(loader, internalName, ancestor, new HashSet<>());
    }

    private static boolean isSubtype(Loader loader, String internalName, String ancestor, Set<String> visited) {
        if (internalName == null || !visited.add(internalName)) {
            return false;
        }
        if (internalName.equals(ancestor)) {
            return true;
        }

        JavaClass javaClass = parse(loader, internalName);

        if (javaClass == null) {
            return false;
        }
        if (isSubtype(loader, internalNameOf(javaClass.getSuperclassName()), ancestor, visited)) {
            return true;
        }
        for (String interfaceName : javaClass.getInterfaceNames()) {
            if (isSubtype(loader, internalNameOf(interfaceName), ancestor, visited)) {
                return true;
            }
        }
        return false;
    }

    /** @return true if a super class declares a private method of this name and descriptor: one of the same signature in a subclass hides it, it does not override it */
    public static boolean declaresPrivateMethod(Loader loader, String internalName, String name, String descriptor) {
        Set<String> visited = new HashSet<>();
        String current = internalName;

        while (current != null && visited.add(current)) {
            JavaClass javaClass = parse(loader, current);

            if (javaClass == null) {
                return false;
            }
            for (Method method : javaClass.getMethods()) {
                if (method.isPrivate() && method.getName().equals(name) && method.getSignature().equals(descriptor)) {
                    return true;
                }
            }
            current = internalNameOf(javaClass.getSuperclassName());
        }
        return false;
    }

    /** @return the names of the exceptions which the method of this name and descriptor declares in the class, a super class or an interface; empty if none or if it is unknown */
    public static Set<String> declaredExceptions(Loader loader, String internalName, String name, String descriptor) {
        return declaredExceptions(loader, internalName, name, descriptor, true, new HashSet<>());
    }

    /** @param includePrivate true for the class which is asked: it can call its own private methods, the ones of its super classes are not inherited */
    private static Set<String> declaredExceptions(Loader loader, String internalName, String name, String descriptor, boolean includePrivate, Set<String> visited) {
        JavaClass javaClass = internalName == null || !visited.add(internalName) ? null : parse(loader, internalName);

        if (javaClass == null) {
            return Collections.emptySet();
        }
        for (Method method : javaClass.getMethods()) {
            if ((includePrivate || !method.isPrivate()) && method.getName().equals(name) && method.getSignature().equals(descriptor)) {
                return exceptionNames(method);
            }
        }

        List<String> parents = new ArrayList<>();

        parents.add(internalNameOf(javaClass.getSuperclassName()));
        for (String interfaceName : javaClass.getInterfaceNames()) {
            parents.add(internalNameOf(interfaceName));
        }
        for (String parent : parents) {
            Set<String> exceptions = declaredExceptions(loader, parent, name, descriptor, false, visited);

            if (!exceptions.isEmpty()) {
                return exceptions;
            }
        }
        return Collections.emptySet();
    }

    /** @return the generic signature of the method of this name and descriptor which the class, a super class or an interface declares; null if it has none or is unknown */
    public static String declaredGenericSignature(Loader loader, String internalName, String name, String descriptor) {
        return declaredGenericSignature(loader, internalName, name, descriptor, true, new HashSet<>());
    }

    /** @param includePrivate true for the class of the invocation, which can call its own private methods: the ones of its super classes are not inherited */
    private static String declaredGenericSignature(Loader loader, String internalName, String name, String descriptor, boolean includePrivate, Set<String> visited) {
        JavaClass javaClass = internalName == null || !visited.add(internalName) ? null : parse(loader, internalName);

        if (javaClass == null) {
            return null;
        }
        for (Method method : javaClass.getMethods()) {
            if ((includePrivate || !method.isPrivate()) && method.getName().equals(name) && method.getSignature().equals(descriptor)) {
                return genericSignature(method);
            }
        }

        List<String> parents = new ArrayList<>();

        parents.add(internalNameOf(javaClass.getSuperclassName()));
        for (String interfaceName : javaClass.getInterfaceNames()) {
            parents.add(internalNameOf(interfaceName));
        }
        return parents.stream().map(parent -> declaredGenericSignature(loader, parent, name, descriptor, false, visited)).filter(Objects::nonNull).findFirst().orElse(null);
    }

    /** @return the generic signature of the method, null if its types have no type argument */
    public static String genericSignature(Method method) {
        for (Attribute attribute : method.getAttributes()) {
            if (attribute instanceof Signature signature) {
                return signature.getSignature();
            }
        }
        return null;
    }

    /** @return the names of the exceptions which the method declares */
    public static Set<String> exceptionNames(Method method) {
        Set<String> names = new HashSet<>();

        if (method.getExceptionTable() != null) {
            for (String exception : method.getExceptionTable().getExceptionNames()) {
                names.add(internalNameOf(exception));
            }
        }
        return names;
    }

    /**
     * @return true if the class is the ancestor, or extends or implements it with its own type parameters as type arguments, in the same order:
     * 'ArrayList&lt;E&gt;' is a 'Collection&lt;E&gt;', 'interface B&lt;T&gt; extends A&lt;String&gt;' is no 'A&lt;T&gt;' (known by the classes which can be read)
     */
    public static boolean isSubtypeWithSameTypeArguments(Loader loader, String internalName, String ancestor) {
        return isSubtypeWithSameTypeArguments(loader, internalName, ancestor, new HashSet<>());
    }

    private static boolean isSubtypeWithSameTypeArguments(Loader loader, String internalName, String ancestor, Set<String> visited) {
        if (internalName == null || !visited.add(internalName)) {
            return false;
        }
        if (internalName.equals(ancestor)) {
            return true;
        }

        JavaClass javaClass = parse(loader, internalName);
        String signature = javaClass == null ? null : classSignature(javaClass);

        if (signature == null) {
            return false;
        }

        List<String> parameters = new ArrayList<>();
        int index = formalParameters(signature, parameters);
        StringBuilder own = new StringBuilder("<");

        parameters.forEach(parameter -> own.append('T').append(parameter).append(';'));
        own.append('>');

        while (index < signature.length()) {
            int end = endOfType(signature, index);
            String type = signature.substring(index, end);
            int arguments = type.indexOf('<');

            if (arguments >= 0 && type.substring(arguments, type.lastIndexOf('>') + 1).equals(own.toString())
                    && isSubtypeWithSameTypeArguments(loader, type.substring(1, arguments), ancestor, visited)) {
                return true;
            }
            index = end;
        }
        return false;
    }

    /**
     * @return the signatures of the parameters of the abstract method of a functional interface (generic ones when the method has a signature); empty if the
     * interface is unknown or has no such method
     */
    public static List<String> functionalMethodParameters(Loader loader, String internalName) {
        JavaClass javaClass = internalName == null ? null : parse(loader, internalName);

        if (javaClass == null || !javaClass.isInterface()) {
            return Collections.emptyList();
        }
        for (Method method : javaClass.getMethods()) {
            if (method.isAbstract() && !method.isStatic() && !OBJECT_METHODS.contains(method.getName() + method.getSignature())) {
                return parameterSignatures(method);
            }
        }
        return inheritedFunctionalMethodParameters(loader, javaClass);
    }

    /** 'interface Visitor&lt;T&gt; extends ToIntFunction&lt;Other&gt;' has the method of its super interface, where its type arguments replace the type parameters */
    private static List<String> inheritedFunctionalMethodParameters(Loader loader, JavaClass javaClass) {
        String signature = classSignature(javaClass);

        if (signature == null) {
            return Collections.emptyList();
        }

        // The first super type is 'java/lang/Object' for an interface
        int index = endOfType(signature, formalParameters(signature, new ArrayList<>()));

        while (index < signature.length()) {
            int end = endOfType(signature, index);
            List<String> parameters = substitutedParameters(loader, signature.substring(index, end));

            if (!parameters.isEmpty()) {
                return parameters;
            }
            index = end;
        }
        return Collections.emptyList();
    }

    /** @return the parameters of the functional method of a super interface, as its type arguments give them; empty if it has none */
    private static List<String> substitutedParameters(Loader loader, String superType) {
        int generic = superType.indexOf('<');
        String superName = superType.substring(1, generic < 0 ? superType.length() - 1 : generic);
        List<String> typeParameters = typeParameterNames(loader, superName);
        List<String> typeArguments = typeArgumentsOf(superType);
        List<String> substituted = new ArrayList<>();

        for (String parameter : functionalMethodParameters(loader, superName)) {
            int k = typeParameters.indexOf(parameter.length() > 2 ? parameter.substring(1, parameter.length() - 1) : "");

            substituted.add(parameter.startsWith("T") && k >= 0 && k < typeArguments.size() ? typeArguments.get(k) : parameter);
        }
        return substituted;
    }

    /** @return false if the class is known and its 'InnerClasses' entry gives it a name: 'Named.$1' is not anonymous; true when it is anonymous or unknown */
    public static boolean isAnonymousOrUnknown(Loader loader, String internalName) {
        JavaClass javaClass = parse(loader, internalName);

        if (javaClass == null) {
            return true;
        }
        for (Attribute attribute : javaClass.getAttributes()) {
            if (attribute instanceof InnerClasses innerClasses) {
                for (InnerClass inner : innerClasses.getInnerClasses()) {
                    if (internalName.equals(javaClass.getConstantPool().getConstantString(inner.getInnerClassIndex(), Const.CONSTANT_Class))) {
                        return inner.getInnerNameIndex() == 0;
                    }
                }
            }
        }
        return true;
    }

    /** @return the type arguments of a generic type signature: 'Ljava/util/Map&lt;TK;+TV;&gt;;' gives 'TK;', '+TV;' */
    private static List<String> typeArgumentsOf(String type) {
        List<String> arguments = new ArrayList<>();
        int index = type.indexOf('<') + 1;

        while (index > 0 && index < type.length() && type.charAt(index) != '>') {
            int start = index;

            if (type.charAt(index) == '*') {
                index++;
            } else {
                index = endOfType(type, type.charAt(index) == '+' || type.charAt(index) == '-' ? index + 1 : index);
            }
            arguments.add(type.substring(start, index));
        }
        return arguments;
    }

    private static List<String> parameterSignatures(Method method) {
        List<String> parameters = new ArrayList<>();
        String generic = genericSignature(method);

        if (generic == null) {
            for (org.apache.bcel.generic.Type type : org.apache.bcel.generic.Type.getArgumentTypes(method.getSignature())) {
                parameters.add(type.getSignature());
            }
            return parameters;
        }

        int index = generic.indexOf('(') + 1;

        while (index < generic.indexOf(')')) {
            int end = endOfType(generic, index);

            parameters.add(generic.substring(index, end));
            index = end;
        }
        return parameters;
    }

    /** @return the names of the type parameters of the class, in the order of its declaration: 'Pair&lt;K, V&gt;' gives K, V; none if it has none or is unknown */
    public static List<String> typeParameterNames(Loader loader, String internalName) {
        JavaClass javaClass = internalName == null ? null : parse(loader, internalName);
        String signature = javaClass == null ? null : classSignature(javaClass);
        List<String> names = new ArrayList<>();

        if (signature != null) {
            formalParameters(signature, names);
        }
        return names;
    }

    private static String classSignature(JavaClass javaClass) {
        for (Attribute attribute : javaClass.getAttributes()) {
            if (attribute instanceof Signature signature) {
                return signature.getSignature();
            }
        }
        return null;
    }

    /** Reads the formal type parameters of a class signature, '&lt;E:Ljava/lang/Object;&gt;', and @return the index of its first super type */
    private static int formalParameters(String signature, List<String> parameters) {
        if (signature.charAt(0) != '<') {
            return 0;
        }

        int index = 1;

        while (signature.charAt(index) != '>') {
            int colon = signature.indexOf(':', index);

            parameters.add(signature.substring(index, colon));
            index = colon;
            while (signature.charAt(index) == ':') {
                index++;
                if (signature.charAt(index) != ':') {
                    index = endOfType(signature, index);
                }
            }
        }
        return index + 1;
    }

    /** @return the index which follows the type which starts at the index: a class with type arguments, a type variable or an array */
    private static int endOfType(String signature, int start) {
        int index = start;
        int depth = 0;

        while (signature.charAt(index) == '[') {
            index++;
        }
        if (signature.charAt(index) != 'L' && signature.charAt(index) != 'T') {
            // A primitive type is one character: 'I' in '(ITT;I)I'
            return index + 1;
        }
        do {
            char c = signature.charAt(index++);

            if (c == '<') {
                depth++;
            } else if (c == '>') {
                depth--;
            }
            if (c == ';' && depth == 0) {
                return index;
            }
        } while (index < signature.length());
        return index;
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
