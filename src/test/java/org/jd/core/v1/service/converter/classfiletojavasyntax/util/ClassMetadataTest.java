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
