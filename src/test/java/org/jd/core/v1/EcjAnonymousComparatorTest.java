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

public class EcjAnonymousComparatorTest extends AbstractJdTest {

    /** the methods which a bridge method stands for override it for the type arguments which the class was made with, not for those a diamond may infer */
    @Test
    public void testAnonymousComparatorOfASupertype() throws Exception {
        String printed;

        try (InputStream is = getClass().getResourceAsStream("/jar/ecj-anonymous-comparator-ecj-17.jar")) {
            printed = decompileSuccess(new ZipLoader(is), new NoLineNumberPrinter(), "org/jd/core/v1/stub/EcjAnonymousComparator");
        }

        assertTrue(printed, printed.contains("new Comparator<Node>()") || printed.contains("new Comparator<EcjAnonymousComparator.Node>()"));
        assertFalse(printed, printed.contains("new Comparator<>()"));
        assertTrue(printed, CompilerUtil.compile("17", new InMemoryJavaSourceFileObject("org/jd/core/v1/stub/EcjAnonymousComparator", printed)));
    }
}
