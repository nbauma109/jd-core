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
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Map;
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
        Method method = resolve(loader, internalName, name, descriptor, true, new HashSet<>());

        return method == null ? Collections.emptySet() : exceptionNames(method);
    }

    /** @return the internal name of the class or interface which declares the method which is called on the class; null if none does or it is unknown */
    public static Optional<String> declaringClass(Loader loader, String internalName, String name, String descriptor) {
        return declaringClass(loader, internalName, name, descriptor, true, new HashSet<>());
    }

    private static Optional<String> declaringClass(Loader loader, String internalName, String name, String descriptor, boolean includePrivate, Set<String> visited) {
        JavaClass javaClass = internalName == null || !visited.add(internalName) ? null : parse(loader, internalName);

        if (javaClass == null) {
            return Optional.empty();
        }
        for (Method method : javaClass.getMethods()) {
            if ((includePrivate || !method.isPrivate()) && method.getName().equals(name) && method.getSignature().equals(descriptor)) {
                return Optional.of(internalName);
            }
        }
        List<String> parents = new ArrayList<>();

        parents.add(internalNameOf(javaClass.getSuperclassName()));
        for (String interfaceName : javaClass.getInterfaceNames()) {
            parents.add(internalNameOf(interfaceName));
        }
        for (String parent : parents) {
            Optional<String> declaring = declaringClass(loader, parent, name, descriptor, false, visited);

            if (declaring.isPresent()) {
                return declaring;
            }
        }
        return Optional.empty();
    }

    /**
     * @return the type arguments of the ancestor in the terms of the type parameters of the class: 'Reversed&lt;K, V&gt; extends Map&lt;V, K&gt;' gives 'TV;', 'TK;' for 'Map'; null
     * if the class has no such ancestor or is unknown
     */
    public static Optional<List<String>> ancestorTypeArguments(Loader loader, String internalName, String ancestor) {
        return ancestorTypeArguments(loader, internalName, ancestor, new HashSet<>());
    }

    private static Optional<List<String>> ancestorTypeArguments(Loader loader, String internalName, String ancestor, Set<String> visited) {
        JavaClass javaClass = internalName == null || !visited.add(internalName) ? null : parse(loader, internalName);

        if (javaClass == null) {
            return Optional.empty();
        }
        for (String superType : superTypes(javaClass)) {
            int generic = superType.indexOf('<');
            String superName = superType.substring(1, generic < 0 ? superType.length() - 1 : generic);
            List<String> arguments = typeArgumentsOf(superType);

            if (superName.equals(ancestor)) {
                return Optional.of(arguments);
            }

            Optional<List<String>> upper = ancestorTypeArguments(loader, superName, ancestor, visited);

            if (upper.isPresent()) {
                return Optional.of(substitute(upper.get(), typeParameterNames(loader, superName), arguments));
            }
        }
        return Optional.empty();
    }

    /**
     * @return the names of the exceptions which the abstract method of a functional interface declares ('Callable.call' throws 'Exception'); empty if unknown. Where several
     * parents declare it, only what all of them allow is thrown.
     */
    public static Set<String> functionalMethodExceptions(Loader loader, String internalName) {
        List<Map.Entry<String, Method>> declarations = functionalMethodDeclarations(loader, internalName);
        Set<String> exceptions = new LinkedHashSet<>();

        declarations.forEach(declaration -> exceptions.addAll(substitutedExceptions(loader, internalName, declaration)));
        exceptions.removeIf(name -> !allowedByAll(loader, internalName, name, declarations));
        return exceptions;
    }

    /** @return the names of the exceptions which the declaration throws for this interface: 'Both extends A&lt;SQLException&gt;' gives 'SQLException' for 'A.run() throws E', its erased bound if unknown */
    private static Set<String> substitutedExceptions(Loader loader, String internalName, Map.Entry<String, Method> declaration) {
        Set<String> names = new LinkedHashSet<>();

        for (String token : thrownBy(loader, internalName, declaration)) {
            if (token.startsWith("L")) {
                names.add(token.substring(1, token.length() - 1));
            } else {
                names.addAll(exceptionNames(declaration.getValue()));
            }
        }
        return names;
    }

    /** @return true if the exception is a sub type of one of the exceptions which each of the declarations throws */
    private static boolean allowedByAll(Loader loader, String internalName, String exception, List<Map.Entry<String, Method>> declarations) {
        return declarations.stream().allMatch(declaration -> substitutedExceptions(loader, internalName, declaration).stream().anyMatch(allowed -> isSubtype(loader, exception, allowed)));
    }

    /**
     * @return the thrown types of the abstract method of a functional interface as its signature gives them, a type variable included ('^TE;'): 'Task&lt;E extends Exception&gt;'
     * has 'TE;' where 'functionalMethodExceptions' gives its erased bound. Where several parents declare the method, only what all of them allow is thrown.
     */
    public static List<String> functionalMethodThrown(Loader loader, String internalName) {
        List<Map.Entry<String, Method>> declarations = functionalMethodDeclarations(loader, internalName);
        List<String> thrown = new ArrayList<>();

        for (Map.Entry<String, Method> declaration : declarations) {
            for (String token : thrownBy(loader, internalName, declaration)) {
                if (!thrown.contains(token) && (declarations.size() == 1 || allowedToken(loader, token, declarations, internalName))) {
                    thrown.add(token);
                }
            }
        }
        return thrown;
    }

    /** A class is allowed by the declarations which throw a super type of it; a type variable only by the ones which throw the same one */
    private static boolean allowedToken(Loader loader, String token, List<Map.Entry<String, Method>> declarations, String internalName) {
        if (token.startsWith("T")) {
            return declarations.stream().allMatch(declaration -> thrownBy(loader, internalName, declaration).contains(token));
        }
        return allowedByAll(loader, internalName, token.substring(1, token.length() - 1), declarations);
    }

    private static List<String> thrownBy(Loader loader, String internalName, Map.Entry<String, Method> owned) {
        List<String> thrown = new ArrayList<>();
        Method method = owned.getValue();
        String signature = genericSignature(method);

        if (signature != null && signature.indexOf('^') >= 0) {
            List<String> tokens = Arrays.asList(signature.substring(signature.indexOf('^') + 1).split("\\^"));

            if (owned.getKey().equals(internalName)) {
                thrown.addAll(tokens);
            } else {
                // 'IOTask extends Task<IOException>': the 'E' which 'Task.run' throws is the 'IOException' of the target
                List<String> ancestor = ancestorTypeArguments(loader, internalName, owned.getKey()).orElse(List.of());

                thrown.addAll(substitute(tokens, typeParameterNames(loader, owned.getKey()), ancestor));
            }
        } else {
            exceptionNames(method).forEach(name -> thrown.add("L" + name + ";"));
        }
        return thrown;
    }

    /** @return the declarations of the abstract method which the interface inherits from its parents (the one it declares itself if it does); empty if there is none */
    private static List<Map.Entry<String, Method>> functionalMethodDeclarations(Loader loader, String internalName) {
        List<Map.Entry<String, Method>> abstractMethods = abstractDeclarations(loader, internalName, new HashSet<>());
        List<Map.Entry<String, Method>> declarations = new ArrayList<>();

        if (abstractMethods.isEmpty()) {
            return declarations;
        }

        Map.Entry<String, Method> first = abstractMethods.get(0);

        declarations.add(first);
        if (!first.getKey().equals(internalName)) {
            for (Map.Entry<String, Method> other : abstractMethods) {
                boolean same = other.getValue().getName().equals(first.getValue().getName()) && parameters(other.getValue()).equals(parameters(first.getValue()));

                if (same && declarations.stream().noneMatch(known -> known.getKey().equals(other.getKey()))) {
                    declarations.add(other);
                }
            }
        }
        return declarations;
    }

    /** @return the parameters of the descriptor: the methods which differ by their covariant return types override each other */
    private static String parameters(Method method) {
        String descriptor = method.getSignature();

        return descriptor.substring(0, descriptor.indexOf(')') + 1);
    }

    /** @return the abstract method of the functional interface with the name of the interface which declares it; null if there is none */
    private static Map.Entry<String, Method> functionalMethodWithOwner(Loader loader, String internalName) {
        List<Map.Entry<String, Method>> abstractMethods = abstractDeclarations(loader, internalName, new HashSet<>());

        return abstractMethods.isEmpty() ? null : abstractMethods.get(0);
    }

    /** @return the abstract methods of the interface with the name of the interface which declares each: its own, then the inherited ones which no default method of it implements */
    private static List<Map.Entry<String, Method>> abstractDeclarations(Loader loader, String internalName, Set<String> visited) {
        JavaClass javaClass = internalName == null || !visited.add(internalName) ? null : parse(loader, internalName);
        List<Map.Entry<String, Method>> methods = new ArrayList<>();

        if (javaClass == null || !javaClass.isInterface()) {
            return methods;
        }
        for (Method method : javaClass.getMethods()) {
            if (method.isAbstract() && !method.isStatic() && !OBJECT_METHODS.contains(method.getName() + method.getSignature())) {
                methods.add(Map.entry(internalName, method));
            }
        }
        for (String interfaceName : javaClass.getInterfaceNames()) {
            methods.addAll(abstractDeclarations(loader, internalNameOf(interfaceName), visited));
        }
        for (Method method : javaClass.getMethods()) {
            if (!method.isAbstract() && !method.isStatic() && !method.isPrivate()) {
                methods.removeIf(declared -> !declared.getKey().equals(internalName) && declared.getValue().getName().equals(method.getName())
                        && parameters(declared.getValue()).equals(parameters(method)));
            }
        }
        return methods;
    }

    /** @return the first class which is a super class of the first one (or the class itself) and a super type of the second; null if one of them is unknown */
    public static String commonSuperclass(Loader loader, String first, String second) {
        String current = first;

        while (current != null && !isSubtype(loader, second, current)) {
            JavaClass javaClass = parse(loader, current);

            current = javaClass == null ? null : internalNameOf(javaClass.getSuperclassName());
        }
        return current;
    }

    /** @return the exceptions which the constructors of the class itself declare: the one of an anonymous class declares the ones of the constructor of its super class which it calls */
    public static Set<String> declaredConstructorExceptions(Loader loader, String internalName) {
        JavaClass javaClass = internalName == null ? null : parse(loader, internalName);
        Set<String> exceptions = new HashSet<>();

        if (javaClass != null) {
            for (Method method : javaClass.getMethods()) {
                if ("<init>".equals(method.getName())) {
                    exceptions.addAll(exceptionNames(method));
                }
            }
        }
        return exceptions;
    }

    /** @return the generic signature of the method of this name and descriptor which the class, a super class or an interface declares; null if it has none or is unknown */
    public static String declaredGenericSignature(Loader loader, String internalName, String name, String descriptor) {
        Method method = resolve(loader, internalName, name, descriptor, true, new HashSet<>());

        return method == null ? null : genericSignature(method);
    }

    /**
     * @return the first method of this name and descriptor which the class, then its super class, then its interfaces declare, even when it declares no exception and has no
     * generic signature: it is the one which is called. Null if none is found.
     * @param includePrivate true for the class which is asked: it can call its own private methods, the ones of its super classes are not inherited
     */
    private static Method resolve(Loader loader, String internalName, String name, String descriptor, boolean includePrivate, Set<String> visited) {
        JavaClass javaClass = internalName == null || !visited.add(internalName) ? null : parse(loader, internalName);

        if (javaClass == null) {
            return null;
        }
        for (Method method : javaClass.getMethods()) {
            if ((includePrivate || !method.isPrivate()) && method.getName().equals(name) && method.getSignature().equals(descriptor)) {
                return method;
            }
        }

        List<String> parents = new ArrayList<>();

        parents.add(internalNameOf(javaClass.getSuperclassName()));
        for (String interfaceName : javaClass.getInterfaceNames()) {
            parents.add(internalNameOf(interfaceName));
        }
        for (String parent : parents) {
            Method method = resolve(loader, parent, name, descriptor, false, visited);

            if (method != null) {
                return method;
            }
        }
        return null;
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
        List<Map.Entry<String, List<String>>> methods = abstractMethods(loader, internalName);

        return methods.isEmpty() ? Collections.emptyList() : methods.get(0).getValue();
    }

    /**
     * @return the abstract methods of an interface, its own and the ones it inherits, as 'name' and the parameters in the terms of the interface: the type
     * arguments of the super interfaces replace their type parameters, and a default method of the interface which has the same parameters implements the method
     */
    private static List<Map.Entry<String, List<String>>> abstractMethods(Loader loader, String internalName) {
        JavaClass javaClass = internalName == null ? null : parse(loader, internalName);
        List<Map.Entry<String, List<String>>> methods = new ArrayList<>();

        if (javaClass == null || !javaClass.isInterface()) {
            return methods;
        }
        for (Method method : javaClass.getMethods()) {
            if (method.isAbstract() && !method.isStatic() && !OBJECT_METHODS.contains(method.getName() + method.getSignature())) {
                methods.add(Map.entry(method.getName(), parameterSignatures(method)));
            }
        }
        for (String superType : superInterfaces(javaClass)) {
            methods.addAll(inheritedAbstractMethods(loader, superType));
        }
        for (Method method : javaClass.getMethods()) {
            if (!method.isAbstract() && !method.isStatic() && !method.isPrivate()) {
                Map.Entry<String, List<String>> implemented = Map.entry(method.getName(), parameterSignatures(method));

                methods.removeIf(implemented::equals);
            }
        }
        return methods;
    }

    /** @return the abstract methods of a super interface, where its type arguments replace the type parameters of the interface */
    private static List<Map.Entry<String, List<String>>> inheritedAbstractMethods(Loader loader, String superType) {
        int generic = superType.indexOf('<');
        String superName = superType.substring(1, generic < 0 ? superType.length() - 1 : generic);
        List<String> typeParameters = typeParameterNames(loader, superName);
        List<String> typeArguments = typeArgumentsOf(superType);
        List<Map.Entry<String, List<String>>> inherited = new ArrayList<>();

        for (Map.Entry<String, List<String>> method : abstractMethods(loader, superName)) {
            inherited.add(Map.entry(method.getKey(), substitute(method.getValue(), typeParameters, typeArguments)));
        }
        return inherited;
    }

    /** @return the signatures of the super interfaces of an interface: 'Ljava/util/function/ToIntFunction&lt;LOther;&gt;;', or the descriptors of the raw ones when there is no signature */
    private static List<String> superInterfaces(JavaClass javaClass) {
        List<String> superTypes = superTypes(javaClass);

        // The first super type is 'java/lang/Object' for an interface
        return superTypes.subList(1, superTypes.size());
    }

    /** @return the signatures of the super class then of the interfaces, or the descriptors of the raw ones when there is no signature */
    private static List<String> superTypes(JavaClass javaClass) {
        String signature = classSignature(javaClass);
        List<String> superTypes = new ArrayList<>();

        if (signature == null) {
            superTypes.add("L" + javaClass.getSuperclassName().replace('.', '/') + ";");
            for (String interfaceName : javaClass.getInterfaceNames()) {
                superTypes.add("L" + interfaceName.replace('.', '/') + ";");
            }
            return superTypes;
        }

        int index = formalParameters(signature, new ArrayList<>());

        while (index < signature.length()) {
            int end = endOfType(signature, index);

            superTypes.add(signature.substring(index, end));
            index = end;
        }
        return superTypes;
    }

    /** @return the parameters where the type arguments replace the type parameters, wherever they occur: '[TE;' becomes '[TT;', 'Ljava/util/List&lt;TE;&gt;;' 'Ljava/util/List&lt;TT;&gt;;' */
    private static List<String> substitute(List<String> parameters, List<String> typeParameters, List<String> typeArguments) {
        List<String> substituted = new ArrayList<>();

        for (String parameter : parameters) {
            substituted.add(substituteTypeVariables(parameter, typeParameters, typeArguments));
        }
        return substituted;
    }

    /** @return the signature where each type variable of the type parameters is replaced by the type argument at its index, in one pass: the arguments are not replaced again */
    public static String substituteTypeVariables(String signature, List<String> typeParameters, List<String> typeArguments) {
        StringBuilder result = new StringBuilder();
        int index = 0;

        while (index < signature.length()) {
            char c = signature.charAt(index);
            int end = signature.indexOf(';', index);
            boolean variableStart = c == 'T' && end > index && (index == 0 || "<;[+-(*)".indexOf(signature.charAt(index - 1)) >= 0);
            int k = variableStart ? typeParameters.indexOf(signature.substring(index + 1, end)) : -1;

            if (k >= 0 && k < typeArguments.size()) {
                result.append(typeArguments.get(k));
                index = end + 1;
            } else {
                result.append(c);
                index++;
            }
        }
        return result.toString();
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

    public static List<String> parameterSignatures(Method method) {
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
            byte[] bytes = bytesOf(loader, internalName);

            return bytes.length == 0 ? null : new ClassParser(new ByteArrayInputStream(bytes), internalName).parse();
        } catch (IOException | ClassFormatException e) {
            return null;
        }
    }

    /** @return the class file of the loader, or of the runtime for the classes of the platform which an archive loader does not hold; empty if neither has it */
    private static byte[] bytesOf(Loader loader, String internalName) throws IOException {
        if (internalName == null || loader == null) {
            return new byte[0];
        }
        if (loader.canLoad(internalName)) {
            return loader.load(internalName);
        }
        if (internalName.startsWith("java/") || internalName.startsWith("javax/")) {
            try (InputStream stream = ClassLoader.getPlatformClassLoader().getResourceAsStream(internalName + ".class")) {
                return stream == null ? new byte[0] : stream.readAllBytes();
            }
        }
        return new byte[0];
    }

    /** @return true if the hierarchy of the class can be read: its class file is held by the loader, or it is a class of the platform */
    public static boolean isKnown(Loader loader, String internalName) {
        return parse(loader, internalName) != null;
    }
}
