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

public class VarArgsElementsOverloadTest extends AbstractJdTest {

    /** without the array, the call selects the overload which takes the elements one by one */
    @Test
    public void testArrayWhoseElementsSelectAnOverload() throws Exception {
        String printed;

        try (InputStream is = getClass().getResourceAsStream("/jar/varargs-elements-overload-javac-11.jar")) {
            printed = decompileSuccess(new ZipLoader(is), new NoLineNumberPrinter(), "org/jd/core/v1/stub/VarArgsElementsOverload");
        }

        String source = printed.replaceAll("\\s+", " ");

        assertTrue(printed, source.contains("return filter(message, new Object[] { first });"));
        assertTrue(printed, source.contains("return filter(message, new Object[] { first, second });"));
        assertTrue(printed, source.contains("return filter(message, first, second, third);"));
        assertTrue(printed, source.contains("return filter(message, new Object[] { value });"));
        assertTrue(printed, source.contains("of(\"%s %s\", second)"));
        assertTrue(printed, source.contains("join(\"-\", new Object[] { null })"));
        assertTrue(printed, source.contains("join(\"-\", new Object[] { count })"));
        assertTrue(printed, source.contains("join(\"-\", text)"));
        assertTrue(printed, source.contains("number(new Object[] { value });"));
        assertTrue(printed, source.contains("number(new Object[] { letter });"));
        assertTrue(printed, source.contains("serial(new Object[] { value });"));
        assertTrue(printed, source.contains("names(new Object[] { strings });"));
        assertTrue(printed, source.contains("names(ints);"));
        assertTrue(printed, source.contains("text(ints);"));
        assertTrue(printed, source.contains("text(new Object[] { string });"));
        assertTrue(printed, source.contains("text(integer);"));
        assertTrue(printed, source.contains("number(string);"));
        assertTrue(printed, source.contains("names(object);"));
        assertTrue(printed, source.contains("lazy(\"-\", text::trim, text::strip, text::intern)"));
        assertTrue(printed, source.contains("sum(new int[] { value });"));
        assertTrue(printed, source.contains("listed(value);"));
        assertTrue(printed, source.contains("serialized(new int[] { value });"));
        assertTrue(printed, source.contains("text(value);"));
        assertTrue(printed, CompilerUtil.compile("11", new InMemoryJavaSourceFileObject("org/jd/core/v1/stub/VarArgsElementsOverload", printed)));
    }
}
