/*
 * Copyright (c) 2026 Nicolas Baumann (@nbauma109), with Claude Sonnet 5.5.
 * This project is distributed under the GPLv3 license.
 * This is a Copyleft license that gives the user the right to use,
 * copy and modify the code freely for non-commercial purposes.
 */

package org.jd.core.v1;

import org.apache.bcel.classfile.ClassParser;
import org.jd.core.v1.model.classfile.ClassFile;
import org.jd.core.v1.service.converter.classfiletojavasyntax.util.EnclosingMethods;
import org.jd.core.v1.stub.InitializerLocals;
import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class EnclosingMethodsTest {
    private static ClassFile classFile(String name) throws IOException {
        String resource = name.replace('.', '/') + ".class";

        try (InputStream in = EnclosingMethodsTest.class.getClassLoader().getResourceAsStream(resource)) {
            return new ClassFile(new ClassParser(in, resource).parse());
        }
    }

    @Test
    public void testClassCreatedByALambdaOfTheStaticInitializer() throws IOException {
        ClassFile outer = classFile(InitializerLocals.class.getName());
        ClassFile local = classFile(InitializerLocals.class.getName() + "$1OfTheStaticInitializer");

        assertTrue(EnclosingMethods.isDeclaredInAnInitializer(local));
        assertTrue(EnclosingMethods.isCreatedByTheStaticInitializer(local, outer));
    }

    @Test
    public void testClassCreatedByTheInstanceInitializer() throws IOException {
        ClassFile outer = classFile(InitializerLocals.class.getName());
        ClassFile local = classFile(InitializerLocals.class.getName() + "$1OfTheInstanceInitializer");

        assertTrue(EnclosingMethods.isDeclaredInAnInitializer(local));
        assertFalse(EnclosingMethods.isCreatedByTheStaticInitializer(local, outer));
        assertFalse(EnclosingMethods.isDeclaredInStaticMethod(local, outer));
    }
}
