/*
 * Copyright (c) 2008, 2026 Emmanuel Dupuy and others.
 * This project is distributed under the GPLv3 license.
 * This is a Copyleft license that gives the user the right to use,
 * copy and modify the code freely for non-commercial purposes.
 */

package org.jd.core.v1;

import org.jd.core.v1.compiler.CompilerUtil;
import org.jd.core.v1.compiler.InMemoryJavaSourceFileObject;
import org.jd.core.v1.loader.ClassPathLoader;
import org.jd.core.v1.printer.PlainTextPrinter;
import org.junit.Test;

public class CommonsLangTest extends AbstractJdTest {
    @Test
    public void testRandomUtils() throws Exception {
        String internalClassName = "org/apache/commons/lang3/RandomUtils";
        String source = decompileSuccess(new ClassPathLoader(), new PlainTextPrinter(), internalClassName);

        // Recompile decompiled source code and check errors
        assertTrue(CompilerUtil.compile("1.8", new InMemoryJavaSourceFileObject(internalClassName, source)));
    }

    @Test
    public void testRandomStringUtils() throws Exception {
        String internalClassName = "org/apache/commons/lang3/RandomStringUtils";
        String source = decompileSuccess(new ClassPathLoader(), new PlainTextPrinter(), internalClassName);

        // Recompile decompiled source code and check errors
        assertTrue(CompilerUtil.compile("1.8", new InMemoryJavaSourceFileObject(internalClassName, source)));
    }

    @Test
    public void testEnumUtils() throws Exception {
        String internalClassName = "org/apache/commons/lang3/EnumUtils";
        String source = decompileSuccess(new ClassPathLoader(), new PlainTextPrinter(), internalClassName);

        // Recompile decompiled source code and check errors
        assertTrue(CompilerUtil.compile("1.8", new InMemoryJavaSourceFileObject(internalClassName, source)));
    }

    @Test
    public void testThreadUtils() throws Exception {
        String internalClassName = "org/apache/commons/lang3/ThreadUtils";
        String source = decompileSuccess(new ClassPathLoader(), new PlainTextPrinter(), internalClassName);

        // Recompile decompiled source code and check errors
        assertTrue(CompilerUtil.compile("1.8", new InMemoryJavaSourceFileObject(internalClassName, source)));
    }

    @Test
    public void testLockingVisitors() throws Exception {
        String internalClassName = "org/apache/commons/lang3/concurrent/locks/LockingVisitors";
        String source = decompileSuccess(new ClassPathLoader(), new PlainTextPrinter(), internalClassName);

        // Recompile decompiled source code and check errors
        assertTrue(CompilerUtil.compile("1.8", new InMemoryJavaSourceFileObject(internalClassName, source)));
    }

    @Test
    public void testLangCollectors() throws Exception {
        String internalClassName = "org/apache/commons/lang3/stream/LangCollectors";
        String source = decompileSuccess(new ClassPathLoader(), new PlainTextPrinter(), internalClassName);

        // Recompile decompiled source code and check errors
        assertTrue(CompilerUtil.compile("1.8", new InMemoryJavaSourceFileObject(internalClassName, source)));
    }

    @Test
    public void testDurationUtils() throws Exception {
        String internalClassName = "org/apache/commons/lang3/time/DurationUtils";
        String source = decompileSuccess(new ClassPathLoader(), new PlainTextPrinter(), internalClassName);

        // Recompile decompiled source code and check errors
        assertTrue(CompilerUtil.compile("1.8", new InMemoryJavaSourceFileObject(internalClassName, source)));
    }

    @Test
    public void testTimeZones() throws Exception {
        String internalClassName = "org/apache/commons/lang3/time/TimeZones";
        String source = decompileSuccess(new ClassPathLoader(), new PlainTextPrinter(), internalClassName);

        // Recompile decompiled source code and check errors
        assertTrue(CompilerUtil.compile("1.8", new InMemoryJavaSourceFileObject(internalClassName, source)));
    }

    @Override
    protected void printSource(String source) {
        if (source.contains("class RandomUtils")) {
            super.printSource(source);
        }
    }
}
