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

public class EcjTernaryCastTest extends AbstractJdTest {

    /** the test and the cast of an element of an array, in a ternary, are the pattern of the test: the type of the elements is the one of the array */
    @Test
    public void testTestAndCastInATernary() throws Exception {
        String printed;

        try (InputStream is = getClass().getResourceAsStream("/jar/ecj-ternary-cast-ecj-17.jar")) {
            printed = decompileSuccess(new ZipLoader(is), new NoLineNumberPrinter(), "org/jd/core/v1/stub/EcjTernaryCast");
        }

        String source = printed.replaceAll("\\s+", " ");

        assertTrue(printed, source.contains("(arguments[i] instanceof Wrapper wrapper) ? wrapper.wrapped : arguments[i]"));
        assertTrue(printed, CompilerUtil.compile("17", new InMemoryJavaSourceFileObject("org/jd/core/v1/stub/EcjTernaryCast", printed)));
    }

    /** the elements of a field are typed by the field, the ones of a list are not: the test of the elements of a list stays what it was */
    @Test
    public void testElementsOfAFieldAndOfAList() throws Exception {
        String printed;

        try (InputStream is = getClass().getResourceAsStream("/jar/ecj-ternary-cast-other-ecj-17.jar")) {
            printed = decompileSuccess(new ZipLoader(is), new NoLineNumberPrinter(), "org/jd/core/v1/stub/EcjTernaryCastOther");
        }

        String source = printed.replaceAll("\\s+", " ");

        assertTrue(printed, source.contains("this.items[i] instanceof Wrapper wrapper"));
        // the array which an anonymous class captures: its bounds are unknown, the test stays
        assertFalse(printed, source.contains("instanceof Runnable runnable"));
    }
}
