/*
 * Copyright (c) 2026 Nicolas Baumann (@nbauma109), with Claude Sonnet 5.5.
 * This project is distributed under the GPLv3 license.
 * This is a Copyleft license that gives the user the right to use,
 * copy and modify the code freely for non-commercial purposes.
 */

package org.jd.core.v1.service.converter.classfiletojavasyntax.util;

import org.jd.core.v1.api.loader.Loader;
import org.jd.core.v1.loader.ClassPathLoader;
import org.junit.Test;

import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class ClassMetadataTest {
    private static final Loader LOADER = new ClassPathLoader();

    /** A record to read: its components, a static field which is none, and a method which is no accessor */
    record Pair(int first, String second) {
        static final int COUNT = 2;

        int sum() {
            return first;
        }
    }

    interface Reads {
        int read() throws java.io.IOException;
    }

    static class Plain {
        public int read() {
            return 1;
        }
    }

    static class PlainReader extends Plain implements Reads {
    }

    interface ArrayFunction<E> {
        int apply(E[] values);
    }

    interface Defaulted<E> extends java.util.function.Consumer<String>, java.util.function.ToIntFunction<E> {
        @Override
        default void accept(String text) {
            // implemented by default
        }
    }

    interface Overloading extends java.util.function.ToIntFunction<String> {
        default int applyAsInt(Integer number) {
            return 0;
        }
    }

    interface Reversed<K, V> extends java.util.Map<V, K> {
    }

    interface Task<E extends Exception> {
        void run() throws E;
    }

    interface GenericParameterRunner<T> {
        void run(T value) throws Exception;
    }

    interface StringRunner {
        void run(String value) throws java.io.IOException;
    }

    interface SubstitutedRunner extends GenericParameterRunner<String>, StringRunner {
    }

    interface GenericRunner<E extends Exception> {
        void run() throws E;
    }

    interface InputRunner {
        void run() throws java.io.IOException;
    }

    interface TargetRunner<E extends Exception> extends GenericRunner<E>, InputRunner {
    }

    interface InputTask extends Task<java.io.IOException> {
    }

    static class Opening {
        Opening() throws java.io.IOException {
            // opens
        }
    }

    private static final String PREFIX = "org/jd/core/v1/service/converter/classfiletojavasyntax/util/ClassMetadataTest$";

    @Test
    public void testDeclaringClassOfAnInheritedMethod() {
        assertEquals("java/util/ArrayList", ClassMetadata.declaringClass(LOADER, "java/util/ArrayList", "size", "()I").orElse(null));
        assertEquals("java/util/Collection", ClassMetadata.declaringClass(LOADER, "java/util/ArrayList", "stream", "()Ljava/util/stream/Stream;").orElse(null));
        assertFalse(ClassMetadata.declaringClass(LOADER, "java/util/ArrayList", "noSuchMethod", "()V").isPresent());
        assertFalse(ClassMetadata.declaringClass(LOADER, null, "size", "()I").isPresent());
    }

    @Test
    public void testTypeArgumentsOfAnAncestor() {
        assertEquals(List.of("TE;"), ClassMetadata.ancestorTypeArguments(LOADER, "java/util/ArrayList", "java/util/List").orElse(null));
        assertEquals(List.of("TV;", "TK;"), ClassMetadata.ancestorTypeArguments(LOADER, PREFIX + "Reversed", "java/util/Map").orElse(null));
        assertEquals(List.of("Ljava/io/IOException;"), ClassMetadata.ancestorTypeArguments(LOADER, PREFIX + "InputTask", PREFIX + "Task").orElse(null));
        assertFalse(ClassMetadata.ancestorTypeArguments(LOADER, "java/util/ArrayList", "java/lang/String").isPresent());
        assertFalse(ClassMetadata.ancestorTypeArguments(LOADER, "no/such/Class", "java/util/List").isPresent());
    }

    @Test
    public void testCommonSuperclass() {
        assertEquals("java/io/IOException", ClassMetadata.commonSuperclass(LOADER, "java/io/FileNotFoundException", "java/io/EOFException"));
        assertEquals("java/lang/String", ClassMetadata.commonSuperclass(LOADER, "java/lang/String", "java/lang/String"));
        assertEquals("java/io/IOException", ClassMetadata.commonSuperclass(LOADER, "java/io/IOException", "java/io/EOFException"));
        assertEquals(null, ClassMetadata.commonSuperclass(LOADER, "no/such/Class", "java/io/EOFException"));
    }

    @Test
    public void testInheritedFunctionalMethodsWhichParameterTypesAreSubstitutedAreIntersected() {
        assertEquals(List.of("Ljava/io/IOException;"), ClassMetadata.functionalMethodThrown(LOADER, PREFIX + "SubstitutedRunner"));
        assertEquals(java.util.Set.of("java/io/IOException"), ClassMetadata.functionalMethodExceptions(LOADER, PREFIX + "SubstitutedRunner"));
    }

    @Test
    public void testInheritedContractsAreIntersectedOnceTheTargetArgumentsAreSubstituted() {
        assertTrue(ClassMetadata.functionalMethodThrown(LOADER, PREFIX + "TargetRunner", List.of("Ljava/sql/SQLException;")).isEmpty());
        assertEquals(List.of("Ljava/io/IOException;"), ClassMetadata.functionalMethodThrown(LOADER, PREFIX + "TargetRunner", List.of("Ljava/io/IOException;")));
    }

    @Test
    public void testExceptionsOfAFunctionalMethod() {
        assertEquals(java.util.Set.of("java/lang/Exception"), ClassMetadata.functionalMethodExceptions(LOADER, "java/util/concurrent/Callable"));
        assertEquals(List.of("Ljava/lang/Exception;"), ClassMetadata.functionalMethodThrown(LOADER, "java/util/concurrent/Callable"));
        assertEquals(List.of("TE;"), ClassMetadata.functionalMethodThrown(LOADER, PREFIX + "Task"));
        assertEquals(List.of("Ljava/io/IOException;"), ClassMetadata.functionalMethodThrown(LOADER, PREFIX + "InputTask"));
        assertEquals(java.util.Set.of("java/io/IOException"), ClassMetadata.functionalMethodExceptions(LOADER, PREFIX + "InputTask"));
        assertTrue(ClassMetadata.functionalMethodThrown(LOADER, "java/lang/String").isEmpty());
        assertTrue(ClassMetadata.functionalMethodExceptions(LOADER, null).isEmpty());
    }

    @Test
    public void testSubstitutionOfTypeVariables() {
        List<String> parameters = List.of("E");
        List<String> arguments = List.of("Ljava/io/IOException;");

        assertEquals("Ljava/util/List<Ljava/io/IOException;>;", ClassMetadata.substituteTypeVariables("Ljava/util/List<TE;>;", parameters, arguments));
        assertEquals("[Ljava/io/IOException;", ClassMetadata.substituteTypeVariables("[TE;", parameters, arguments));
        assertEquals("Ljava/util/TE;", ClassMetadata.substituteTypeVariables("Ljava/util/TE;", parameters, arguments));
        assertEquals("TF;", ClassMetadata.substituteTypeVariables("TF;", parameters, arguments));
        assertEquals("TE;", ClassMetadata.substituteTypeVariables("TE;", parameters, List.of()));
    }

    @Test
    public void testKnownClassesAndConstructorExceptions() {
        assertTrue(ClassMetadata.isKnown(LOADER, "java/lang/String"));
        assertFalse(ClassMetadata.isKnown(null, "java/lang/String"));
        assertTrue(ClassMetadata.isKnown(new org.jd.core.v1.api.loader.Loader() {
            @Override
            public boolean canLoad(String internalName) {
                return false;
            }

            @Override
            public byte[] load(String internalName) {
                return new byte[0];
            }
        }, "java/lang/String"));
        assertFalse(ClassMetadata.isKnown(null, "no/such/Class"));
        assertFalse(ClassMetadata.isKnown(LOADER, null));
        assertEquals(java.util.Set.of("java/io/IOException"), ClassMetadata.declaredConstructorExceptions(LOADER, PREFIX + "Opening"));
        assertTrue(ClassMetadata.declaredConstructorExceptions(LOADER, "no/such/Class").isEmpty());
    }

    @Test
    public void testTheMethodOfTheSuperclassIsTheOneWhichIsCalledEvenWithoutExceptions() {
        String name = "org/jd/core/v1/service/converter/classfiletojavasyntax/util/ClassMetadataTest$PlainReader";

        assertTrue(ClassMetadata.declaredExceptions(LOADER, name, "read", "()I").isEmpty());
        assertEquals(null, ClassMetadata.declaredGenericSignature(LOADER, name, "read", "()I"));
    }

    @Test
    public void testFunctionalMethodWithArraysDefaultsAndOverloads() {
        String prefix = "org/jd/core/v1/service/converter/classfiletojavasyntax/util/ClassMetadataTest$";

        assertEquals(List.of("[TE;"), ClassMetadata.functionalMethodParameters(LOADER, prefix + "ArrayFunction"));
        assertEquals(List.of("TE;"), ClassMetadata.functionalMethodParameters(LOADER, prefix + "Defaulted"));
        assertEquals(List.of("Ljava/lang/String;"), ClassMetadata.functionalMethodParameters(LOADER, prefix + "Overloading"));
        assertTrue(ClassMetadata.isAnonymousOrUnknown(LOADER, "no/such/Class$1"));
        assertFalse(ClassMetadata.isAnonymousOrUnknown(LOADER, prefix + "PlainReader"));
    }

    @Test
    public void testFieldOfTheClassOfASuperclassOrOfAnInterface() {
        assertTrue(ClassMetadata.declaresField(LOADER, "java/util/ArrayList", "size"));
        assertTrue(ClassMetadata.declaresField(LOADER, "java/util/ArrayList", "modCount"));
        assertTrue(ClassMetadata.declaresField(LOADER, "java/io/ObjectOutputStream", "STREAM_MAGIC"));
    }

    @Test
    public void testFieldWhichIsNotDeclared() {
        assertFalse(ClassMetadata.declaresField(LOADER, "java/util/ArrayList", "noSuchField"));
        assertFalse(ClassMetadata.declaresField(LOADER, "java/lang/Object", "size"));
        assertFalse(ClassMetadata.declaresField(LOADER, "no/such/Class", "size"));
        assertFalse(ClassMetadata.declaresField(LOADER, null, "size"));
        assertFalse(ClassMetadata.declaresField(null, "java/util/ArrayList", "size"));
    }

    @Test
    public void testMethodOfTheClassOfASuperclassOrOfAnInterface() {
        assertTrue(ClassMetadata.declaresMethod(LOADER, "java/util/ArrayList", "size", "()I"));
        assertTrue(ClassMetadata.declaresMethod(LOADER, "java/util/ArrayList", "hashCode", "()I"));
        assertTrue(ClassMetadata.declaresMethod(LOADER, "java/util/ArrayList", "stream", "()Ljava/util/stream/Stream;"));
    }

    @Test
    public void testMethodWhichIsNotDeclared() {
        assertFalse(ClassMetadata.declaresMethod(LOADER, "java/util/ArrayList", "size", "(I)I"));
        assertFalse(ClassMetadata.declaresMethod(LOADER, "java/util/ArrayList", "noSuchMethod", "()V"));
        assertFalse(ClassMetadata.declaresMethod(LOADER, "no/such/Class", "size", "()I"));
        assertFalse(ClassMetadata.declaresMethod(LOADER, null, "size", "()I"));
    }

    /** A class with a private method: the method of a subclass does not override it */
    static class Hidden {
        @SuppressWarnings("unused")
        private int secret() {
            return 1;
        }

        int visible() {
            return 2;
        }
    }

    static class Subclass extends Hidden {
    }

    @Test
    public void testPrivateMethodIsNotInherited() {
        String subclass = Subclass.class.getName().replace('.', '/');

        assertFalse(ClassMetadata.declaresMethod(LOADER, subclass, "secret", "()I"));
        assertTrue(ClassMetadata.declaresMethod(LOADER, subclass, "visible", "()I"));
    }

    @Test
    public void testLoadable() {
        assertTrue(ClassMetadata.isLoadable(LOADER, "java/util/ArrayList"));
        assertFalse(ClassMetadata.isLoadable(LOADER, "no/such/Class"));
        assertFalse(ClassMetadata.isLoadable(LOADER, null));
        assertFalse(ClassMetadata.isLoadable(null, "java/util/ArrayList"));
    }

    @Test
    public void testComponentsOfARecord() {
        assertEquals(List.of(new ClassMetadata.Component("first", "I"), new ClassMetadata.Component("second", "Ljava/lang/String;")),
                ClassMetadata.recordComponents(LOADER, Pair.class.getName().replace('.', '/')));
    }

    @Test
    public void testTypeWhichIsNoRecord() {
        assertEquals(Collections.emptyList(), ClassMetadata.recordComponents(LOADER, "java/util/ArrayList"));
        assertEquals(Collections.emptyList(), ClassMetadata.recordComponents(LOADER, "no/such/Class"));
        assertEquals(Collections.emptyList(), ClassMetadata.recordComponents(LOADER, null));
        assertEquals(Collections.emptyList(), ClassMetadata.recordComponents(null, "java/util/ArrayList"));
    }

    @Test
    public void testPublicMethodOfASuperclassOrOfAnInterface() {
        assertTrue(ClassMetadata.declaresPublicMethod(LOADER, "java/util/ArrayList", "toString", "()Ljava/lang/String;"));
        assertTrue(ClassMetadata.declaresPublicMethod(LOADER, "java/util/ArrayList", "size", "()I"));
        // clone is protected in Object, finalize too
        assertFalse(ClassMetadata.declaresPublicMethod(LOADER, "java/lang/Object", "finalize", "()V"));
        assertTrue(ClassMetadata.declaresMethod(LOADER, "java/lang/Object", "finalize", "()V"));
        assertFalse(ClassMetadata.declaresPublicMethod(LOADER, "no/such/Class", "toString", "()Ljava/lang/String;"));
    }

    @Test
    public void testSubtypeWithTheTypeArgumentsOfTheSubtype() {
        assertTrue(ClassMetadata.isSubtypeWithSameTypeArguments(LOADER, "java/util/List", "java/util/List"));
        assertTrue(ClassMetadata.isSubtypeWithSameTypeArguments(LOADER, "java/util/List", "java/util/Collection"));
        assertTrue(ClassMetadata.isSubtypeWithSameTypeArguments(LOADER, "java/util/ArrayList", "java/util/Collection"));
        assertTrue(ClassMetadata.isSubtypeWithSameTypeArguments(LOADER, "java/util/ArrayList", "java/lang/Iterable"));
        // 'Properties' is a 'Hashtable&lt;Object, Object&gt;': it has no type argument of its own
        assertFalse(ClassMetadata.isSubtypeWithSameTypeArguments(LOADER, "java/util/Properties", "java/util/Map"));
        assertFalse(ClassMetadata.isSubtypeWithSameTypeArguments(LOADER, "java/util/List", "java/util/Map"));
        assertFalse(ClassMetadata.isSubtypeWithSameTypeArguments(LOADER, "java/lang/String", "java/lang/Comparable"));
        assertFalse(ClassMetadata.isSubtypeWithSameTypeArguments(LOADER, "no/such/Class", "java/util/Collection"));
        assertFalse(ClassMetadata.isSubtypeWithSameTypeArguments(LOADER, null, "java/util/Collection"));
    }

    @Test
    public void testDeclaredExceptions() {
        // declared by the class itself, by a super class, by an interface which an interface extends
        assertEquals(Collections.singleton("java/io/IOException"), ClassMetadata.declaredExceptions(LOADER, "java/io/BufferedInputStream", "read", "()I"));
        assertEquals(Collections.singleton("java/io/IOException"), ClassMetadata.declaredExceptions(LOADER, "java/io/DataInputStream", "close", "()V"));
        assertEquals(Collections.singleton("java/io/IOException"), ClassMetadata.declaredExceptions(LOADER, "java/nio/channels/SeekableByteChannel", "close", "()V"));
        // declares none, is unknown or has no such method
        assertTrue(ClassMetadata.declaredExceptions(LOADER, "java/util/ArrayList", "size", "()I").isEmpty());
        assertTrue(ClassMetadata.declaredExceptions(LOADER, "java/util/ArrayList", "noSuchMethod", "()V").isEmpty());
        assertTrue(ClassMetadata.declaredExceptions(LOADER, "no/such/Class", "close", "()V").isEmpty());
        // a class without type parameters has no signature
        assertFalse(ClassMetadata.isSubtypeWithSameTypeArguments(LOADER, "java/lang/Thread", "java/lang/Runnable"));
    }

    /** A private generic method of the class itself, which a subclass does not inherit */
    static class WithPrivate {
        @SuppressWarnings("unused")
        private static <T> T identity(T value) {
            return value;
        }
    }

    static class InheritingFromPrivate extends WithPrivate {
    }

    @Test
    public void testDeclaredGenericSignatureOfAPrivateMethod() {
        String owner = "org/jd/core/v1/service/converter/classfiletojavasyntax/util/ClassMetadataTest$WithPrivate";
        String subclass = "org/jd/core/v1/service/converter/classfiletojavasyntax/util/ClassMetadataTest$InheritingFromPrivate";

        assertEquals("<T:Ljava/lang/Object;>(TT;)TT;", ClassMetadata.declaredGenericSignature(LOADER, owner, "identity", "(Ljava/lang/Object;)Ljava/lang/Object;"));
        assertEquals(null, ClassMetadata.declaredGenericSignature(LOADER, subclass, "identity", "(Ljava/lang/Object;)Ljava/lang/Object;"));
        assertEquals(null, ClassMetadata.declaredGenericSignature(LOADER, "no/such/Class", "identity", "(Ljava/lang/Object;)Ljava/lang/Object;"));
    }

    @Test
    public void testClassWhichCannotBeParsedIsUnknown() {
        Loader garbage = new Loader() {
            @Override
            public boolean canLoad(String internalName) {
                return true;
            }

            @Override
            public byte[] load(String internalName) {
                return new byte[] { 1, 2, 3 };
            }
        };

        assertFalse(ClassMetadata.isLoadable(garbage, "x/Garbage"));
        assertFalse(ClassMetadata.isSubtypeWithSameTypeArguments(garbage, "x/Garbage", "java/util/Collection"));
        assertTrue(ClassMetadata.declaredExceptions(garbage, "x/Garbage", "close", "()V").isEmpty());
    }

    @Test
    public void testClassWithoutSuperClass() {
        // Object has no super class: the walk through the super types ends there
        assertFalse(ClassMetadata.isSubtypeWithSameTypeArguments(LOADER, "java/lang/Object", "x/Unknown"));
        assertTrue(ClassMetadata.declaredExceptions(LOADER, "java/lang/Object", "noSuchMethod", "()V").isEmpty());
        assertEquals(null, ClassMetadata.declaredGenericSignature(LOADER, "java/lang/Object", "noSuchMethod", "()V"));
        assertFalse(ClassMetadata.declaresPrivateMethod(LOADER, "java/lang/Object", "noSuchMethod", "()V"));
    }

    /** A private method which declares an exception, and a class which inherits it without being able to call it */
    static class DeclaringPrivately {
        @SuppressWarnings({ "unused", "java:S1130" }) // the declared exception is what the test reads
        private void io() throws java.io.IOException {
            // declares an exception
        }
    }

    static class InheritingPrivately extends DeclaringPrivately {
    }

    @Test
    public void testDeclaredExceptionsOfAPrivateMethod() {
        String owner = "org/jd/core/v1/service/converter/classfiletojavasyntax/util/ClassMetadataTest$DeclaringPrivately";
        String subclass = "org/jd/core/v1/service/converter/classfiletojavasyntax/util/ClassMetadataTest$InheritingPrivately";

        assertEquals(Collections.singleton("java/io/IOException"), ClassMetadata.declaredExceptions(LOADER, owner, "io", "()V"));
        assertTrue(ClassMetadata.declaredExceptions(LOADER, subclass, "io", "()V").isEmpty());
    }
}
