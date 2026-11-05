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
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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

        declarations.forEach(declaration -> exceptions.addAll(substitutedExceptions(loader, internalName, declaration, List.of(), Map.of())));
        exceptions.removeIf(name -> !allowedByAll(loader, internalName, name, declarations, List.of(), Map.of()));
        return exceptions;
    }

    /**
     * @return the names of the exceptions which the declaration throws for this interface: 'Both extends A&lt;SQLException&gt;' gives 'SQLException' for 'A.run() throws E', its
     * erased bound if unknown; a type variable of the target which has a bound is kept as its token 'TE;'
     */
    private static Set<String> substitutedExceptions(Loader loader, String internalName, Map.Entry<String, Method> declaration, List<String> targetArguments,
            Map<String, String> variableBounds) {
        Set<String> names = new LinkedHashSet<>();

        for (String token : thrownBy(loader, internalName, declaration, targetArguments)) {
            if (token.startsWith("L")) {
                names.add(token.substring(1, token.length() - 1));
            } else if (isVariable(token, variableBounds)) {
                names.add(token);
            } else {
                names.addAll(exceptionNames(declaration.getValue()));
            }
        }
        return names;
    }

    private static boolean isVariable(String token, Map<String, String> variableBounds) {
        return token.startsWith("T") && token.endsWith(";") && variableBounds.containsKey(token.substring(1, token.length() - 1));
    }

    /** @return true if the variable is the other one or is bounded by it through a chain of variables ('E extends X'): 'throws X' allows an 'E' */
    private static boolean isBoundedBy(String variable, String other, Map<String, String> variableBounds) {
        Set<String> visited = new HashSet<>();
        String current = variable;

        while (current != null && visited.add(current)) {
            if (current.equals(other)) {
                return true;
            }

            String bound = variableBounds.get(current.substring(1, current.length() - 1));

            current = bound != null && bound.startsWith("T") && bound.endsWith(";") ? bound : null;
        }
        return false;
    }

    /** @return the internal name of the class which bounds the variable, found through the chain of variables which bound each other */
    private static String classBound(String variable, Map<String, String> variableBounds) {
        Set<String> visited = new HashSet<>();
        String bound = variableBounds.get(variable.substring(1, variable.length() - 1));

        while (bound != null && bound.startsWith("T") && bound.endsWith(";") && visited.add(bound)) {
            bound = variableBounds.get(bound.substring(1, bound.length() - 1));
        }
        return bound == null || bound.startsWith("T") && bound.endsWith(";") ? "java/lang/Exception" : bound;
    }

    /**
     * @return true if each of the declarations allows the exception: a class is allowed by a super type of it, a type variable of the target by itself or by a super type of its
     * bound; a class is not allowed by a type variable, 'throws E' does not allow every 'IOException'
     */
    private static boolean allowedByAll(Loader loader, String internalName, String exception, List<Map.Entry<String, Method>> declarations, List<String> targetArguments,
            Map<String, String> variableBounds) {
        boolean variable = isVariable(exception, variableBounds);
        String bound = variable ? classBound(exception, variableBounds) : exception;

        return declarations.stream().allMatch(declaration -> {
            Set<String> allowed = substitutedExceptions(loader, internalName, declaration, targetArguments, variableBounds);

            return allowed.stream().anyMatch(candidate -> isVariable(candidate, variableBounds)
                    ? variable && isBoundedBy(exception, candidate, variableBounds) : isSubtype(loader, bound, candidate));
        });
    }

    /**
     * @return the thrown types of the abstract method of a functional interface as its signature gives them, a type variable included ('^TE;'): 'Task&lt;E extends Exception&gt;'
     * has 'TE;' where 'functionalMethodExceptions' gives its erased bound. Where several parents declare the method, only what all of them allow is thrown.
     */
    public static List<String> functionalMethodThrown(Loader loader, String internalName) {
        return functionalMethodThrown(loader, internalName, List.of(), Map.of());
    }

    public static List<String> functionalMethodThrown(Loader loader, String internalName, List<String> targetArguments) {
        return functionalMethodThrown(loader, internalName, targetArguments, Map.of());
    }

    /**
     * @param targetArguments the type arguments of the target for the type parameters of the interface, as signature tokens ('Ljava/sql/SQLException;'): the inherited contracts are
     * intersected once they are substituted, 'Both&lt;SQLException&gt; extends A&lt;E&gt;, B' throws what 'A&lt;SQLException&gt;' and 'B' both allow
     * @param variableBounds the first bound of the type variables of the enclosing declaration which the target arguments are, by name: 'Both&lt;E&gt;' is given the token 'TE;'
     */
    public static List<String> functionalMethodThrown(Loader loader, String internalName, List<String> targetArguments, Map<String, String> variableBounds) {
        return thrownByAll(loader, internalName, functionalMethodDeclarations(loader, internalName), targetArguments, variableBounds);
    }

    /**
     * @return the thrown types which all the inherited declarations of 'close()' allow when the interface inherits several of them ('C extends A, B' where 'A.close' throws
     * 'IOException' and 'B.close' 'SQLException' throws neither), once the type arguments of the resource are substituted ('C&lt;SQLException&gt;' for
     * 'A&lt;E&gt;.close() throws E'); empty if it inherits one at most
     */
    public static Optional<List<String>> inheritedCloseThrown(Loader loader, String internalName, List<String> targetArguments, Map<String, String> variableBounds) {
        List<Map.Entry<String, Method>> declarations = new ArrayList<>();

        for (Map.Entry<String, Method> declaration : abstractDeclarations(loader, internalName, new HashSet<>())) {
            if ("close".equals(declaration.getValue().getName()) && "()".equals(parameters(loader, internalName, declaration))
                    && declarations.stream().noneMatch(known -> known.getKey().equals(declaration.getKey()))) {
                declarations.add(declaration);
            }
        }
        return declarations.size() > 1 && !declarations.get(0).getKey().equals(internalName)
                ? Optional.of(thrownByAll(loader, internalName, declarations, targetArguments, variableBounds)) : Optional.empty();
    }

    private static List<String> thrownByAll(Loader loader, String internalName, List<Map.Entry<String, Method>> declarations, List<String> targetArguments,
            Map<String, String> variableBounds) {
        List<String> thrown = new ArrayList<>();

        for (Map.Entry<String, Method> declaration : declarations) {
            for (String token : thrownBy(loader, internalName, declaration, targetArguments)) {
                if (!thrown.contains(token) && (declarations.size() == 1 || allowedToken(loader, token, declarations, internalName, targetArguments, variableBounds))) {
                    thrown.add(token);
                }
            }
        }
        return thrown;
    }

    /** A class is allowed by the declarations which throw a super type of it; a type variable of the interface only by the ones which throw the same one */
    private static boolean allowedToken(Loader loader, String token, List<Map.Entry<String, Method>> declarations, String internalName, List<String> targetArguments,
            Map<String, String> variableBounds) {
        if (token.startsWith("T") && !isVariable(token, variableBounds)) {
            return declarations.stream().allMatch(declaration -> thrownBy(loader, internalName, declaration, targetArguments).contains(token));
        }
        return allowedByAll(loader, internalName, isVariable(token, variableBounds) ? token : token.substring(1, token.length() - 1), declarations, targetArguments, variableBounds);
    }

    private static List<String> thrownBy(Loader loader, String internalName, Map.Entry<String, Method> owned, List<String> targetArguments) {
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
        return substitute(thrown, typeParameterNames(loader, internalName), targetArguments);
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
                boolean same = other.getValue().getName().equals(first.getValue().getName())
                        && parameters(loader, internalName, other).equals(parameters(loader, internalName, first));

                if (same && declarations.stream().noneMatch(known -> known.getKey().equals(other.getKey()))) {
                    declarations.add(other);
                }
            }
        }
        return declarations;
    }

    /**
     * @return the parameters of the method in the terms of the interface, without type arguments and return type: the methods which differ by their covariant return types or
     * by the type arguments which their generic parent gives them ('Broad&lt;T&gt;.run(T)' is 'run(String)' for 'Both extends Broad&lt;String&gt;') override each other
     */
    private static String parameters(Loader loader, String internalName, Map.Entry<String, Method> declaration) {
        Method method = declaration.getValue();
        String signature = genericSignature(method) != null ? genericSignature(method) : method.getSignature();

        // The variables which the method declares hide those of its interface: they are erased before the arguments of the interface are substituted
        signature = eraseMethodTypeVariables(signature);
        if (!declaration.getKey().equals(internalName)) {
            List<String> arguments = ancestorTypeArguments(loader, internalName, declaration.getKey()).orElse(List.of());

            signature = substituteTypeVariables(signature, typeParameterNames(loader, declaration.getKey()), arguments);
        }

        String parameters = signature.substring(0, signature.indexOf(')') + 1);
        String erased;

        do {
            erased = parameters;
            parameters = parameters.replaceAll("<[^<>]*>", "");
        } while (!parameters.equals(erased));
        return parameters;
    }

    /**
     * @return the signature where the type variables which the method declares ('&lt;T:Ljava/lang/Object;&gt;(TT;)V') are replaced by the erasure of their first bound: '&lt;T&gt; void run(T)' and
     * 'void run(Object)' have the same erasure, so they override each other in a sub interface
     */
    private static String eraseMethodTypeVariables(String signature) {
        if (!signature.startsWith("<")) {
            return signature;
        }

        int end = endOfFormalTypeParameters(signature);
        Map<String, String> bounds = firstBounds(signature.substring(1, end - 1));
        String rest = signature.substring(end);

        for (Map.Entry<String, String> entry : bounds.entrySet()) {
            String bound = entry.getValue();

            for (int i = 0; i < bounds.size() && bound.startsWith("T") && bounds.containsKey(bound.substring(1, bound.length() - 1)); i++) {
                bound = bounds.get(bound.substring(1, bound.length() - 1));
            }
            if (bound.startsWith("T")) {
                bound = "Ljava/lang/Object;";
            }
            rest = rest.replaceAll("(?<=[(;\\[<>*+\\-])T" + Pattern.quote(entry.getKey()) + ";", Matcher.quoteReplacement(bound));
        }
        return rest;
    }

    /** @return the index which follows the '&gt;' that closes the formal type parameters which start the signature */
    private static int endOfFormalTypeParameters(String signature) {
        int depth = 0;
        int end = 0;

        do {
            char c = signature.charAt(end);

            if (c == '<') {
                depth++;
            } else if (c == '>') {
                depth--;
            }
            end++;
        } while (depth > 0 && end < signature.length());
        return end;
    }

    /** @return the index which follows the bound which starts at the index, the index itself when the bound is empty ('T::Ljava/lang/Comparable;' has no class bound) */
    private static int endOfBound(String formals, int index) {
        return index < formals.length() && formals.charAt(index) != ':' ? endOfType(formals, index) : index;
    }

    /** @return the first bound of each formal type parameter ('T:Ljava/lang/Object;U::Ljava/lang/Comparable;'), 'Object' when it has none */
    private static Map<String, String> firstBounds(String formals) {
        Map<String, String> bounds = new LinkedHashMap<>();
        int index = 0;

        while (index < formals.length()) {
            int colon = formals.indexOf(':', index);
            String name = formals.substring(index, colon);
            String bound = null;

            index = colon;
            while (index < formals.length() && formals.charAt(index) == ':') {
                index++;

                int next = endOfBound(formals, index);

                if (bound == null && next > index) {
                    bound = formals.substring(index, next);
                }
                index = next;
            }
            bounds.put(name, bound == null ? "Ljava/lang/Object;" : bound);
        }
        return bounds;
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
        List<Map.Entry<String, Method>> defaults = defaultDeclarations(loader, internalName, new HashSet<>());

        // A default method of an interface implements the abstract methods of its ancestors, whichever branch of the hierarchy they were reached by
        methods.removeIf(declared -> defaults.stream().anyMatch(implemented -> !implemented.getKey().equals(declared.getKey())
                && isSubtype(loader, implemented.getKey(), declared.getKey()) && implemented.getValue().getName().equals(declared.getValue().getName())
                && parameters(loader, internalName, declared).equals(parameters(loader, internalName, implemented))));
        return methods;
    }

    /** @return the default methods of the interface and of all its ancestors, with the name of the interface which declares each */
    private static List<Map.Entry<String, Method>> defaultDeclarations(Loader loader, String internalName, Set<String> visited) {
        JavaClass javaClass = internalName == null || !visited.add(internalName) ? null : parse(loader, internalName);
        List<Map.Entry<String, Method>> defaults = new ArrayList<>();

        if (javaClass == null || !javaClass.isInterface()) {
            return defaults;
        }
        for (Method method : javaClass.getMethods()) {
            if (!method.isAbstract() && !method.isStatic() && !method.isPrivate()) {
                defaults.add(Map.entry(internalName, method));
            }
        }
        for (String interfaceName : javaClass.getInterfaceNames()) {
            defaults.addAll(defaultDeclarations(loader, internalNameOf(interfaceName), visited));
        }
        return defaults;
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
