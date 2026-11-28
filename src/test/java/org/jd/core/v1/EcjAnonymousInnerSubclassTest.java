/*
 * Copyright (c) 2026 Nicolas Baumann (@nbauma109), with Claude Sonnet 5.5.
 * This project is distributed under the GPLv3 license.
 * This is a copyleft license that gives the user the right to use,
 * copy and modify the code freely for non-commercial purposes.
 */

package org.jd.core.v1;

import org.jd.core.v1.compiler.CompilerUtil;
import org.jd.core.v1.compiler.InMemoryJavaSourceFileObject;
import org.jd.core.v1.loader.ZipLoader;
import org.jd.core.v1.printer.NoLineNumberPrinter;
import org.junit.Test;

import java.io.InputStream;

public class EcjAnonymousInnerSubclassTest extends AbstractJdTest {

    /** neither enclosing instance is an argument of the creation: the one of the superclass is implicit, or qualifies it */
    @Test
    public void testAnonymousSubclassesOfAnInnerClass() throws Exception {
        String printed;

        try (InputStream is = getClass().getResourceAsStream("/jar/ecj-anonymous-inner-subclass-ecj-17.jar")) {
            printed = decompileSuccess(new ZipLoader(is), new NoLineNumberPrinter(), "org/jd/core/v1/stub/EcjAnonymousInnerSubclass");
        }

        String source = printed.replaceAll("\\s+", " ");

        assertTrue(printed, source.contains("new Inner(added, new String[] { part })") || source.contains("new Inner(added, part)"));
        assertTrue(printed, source.contains("new Inner(value, new String[0])") || source.contains("new Inner(value)"));
        assertTrue(printed, source.contains("other.new Inner(value"));
        assertTrue(printed, CompilerUtil.compile("17", new InMemoryJavaSourceFileObject("org/jd/core/v1/stub/EcjAnonymousInnerSubclass", printed)));
    }
}
