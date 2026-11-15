/*
 * Copyright (c) 2026 Nicolas Baumann (@nbauma109), with Claude Sonnet 5.5.
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

/** Patterns found by decompiling and recompiling Commons Text, which is compiled for Java 8 */
public class CommonsTextPatternsTest extends AbstractJdTest {

    @Test
    public void testMethodReferenceCompiledToLambdaCapturingItsReceiver() throws Exception {
        String internalClassName = "org/apache/commons/text/similarity/IntersectionSimilarity";
        String source = decompileSuccess(new ClassPathLoader(), new PlainTextPrinter(), internalClassName);

        // javac compiles 'bag::add' into a lambda which receives 'bag' in its parameter 'rec$' (and 'bag' was stored in a variable first)
        assertEquals(-1, source.indexOf("rec$"));
        assertEquals(-1, source.indexOf("x$0"));
        assertTrue(source.contains("objects.forEach(bag::add);"));
    }

    @Test
    public void testBoundMethodReferenceOfGenericMap() throws Exception {
        String internalClassName = "org/apache/commons/text/lookup/FunctionStringLookup";
        String source = decompileSuccess(new ClassPathLoader(), new PlainTextPrinter(), internalClassName);

        // The type variables of 'toMap(map)', the receiver of the method reference, are bound by its argument: no raw cast of 'map'
        assertEquals(-1, source.indexOf("(Map)map"));
        assertTrue(source.contains("toMap(map)::get"));
    }
}
