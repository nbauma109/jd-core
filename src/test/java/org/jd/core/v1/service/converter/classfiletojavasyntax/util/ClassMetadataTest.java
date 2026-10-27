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
}
