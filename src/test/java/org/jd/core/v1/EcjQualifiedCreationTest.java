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

public class EcjQualifiedCreationTest extends AbstractJdTest {

    /** the 'getClass()' by which the Eclipse compiler checks that an outer instance is not null is no statement: the creation is qualified by the instance */
    @Test
    public void testCreationQualifiedByAnInstanceOfATopLevelClass() throws Exception {
        String printed;

        try (InputStream is = getClass().getResourceAsStream("/jar/ecj-qualified-creation-ecj-17.jar")) {
            printed = decompileSuccess(new ZipLoader(is), new NoLineNumberPrinter(), "org/jd/core/v1/stub/EcjQualifiedCreation");
        }

        String source = printed.replaceAll("\\s+", " ");

        assertTrue(printed, source.contains("return OUTER.new Walker(source);"));
        assertTrue(printed, source.contains("return walker.new Step();"));
        // the checks which the source makes stay
        assertTrue(printed, source.contains("value.getClass(); return outer.new Walker(() -> {"));
        assertTrue(printed, source.split("value.getClass\\(\\)", -1).length == 3);
        assertTrue(printed, source.contains("outer.getClass(); return"));
        assertTrue(printed, source.contains("outer.new Walker(source)).source.length();") || source.contains("outer.new Walker(source).source.length();"));
        assertTrue(printed, CompilerUtil.compile("17", new InMemoryJavaSourceFileObject("org/jd/core/v1/stub/EcjQualifiedCreation", printed)));
    }
}
