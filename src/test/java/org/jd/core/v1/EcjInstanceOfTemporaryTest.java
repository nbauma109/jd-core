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

/** The instanceof patterns which the Eclipse compiler (ECJ) generates: see the stub EcjInstanceOfTemporary */
public class EcjInstanceOfTemporaryTest extends AbstractJdTest {

    @Test
    public void testPatternOfAFieldWhichIsKeptInAVariable() throws Exception {
        String printed;

        try (InputStream is = getClass().getResourceAsStream("/jar/ecj-instanceof-temporary-ecj-17.jar")) {
            printed = decompileSuccess(new ZipLoader(is), new NoLineNumberPrinter(), "org/jd/core/v1/stub/EcjInstanceOfTemporary");
        }

        String source = printed.replaceAll("\\s+", " ");

        // '(v = this.context) instanceof Method' is no valid test: the field is tested, and the variable which holds it does not exist
        assertFalse(printed, source.contains("context = this.context"));
        assertTrue(printed, source.contains("if (this.context instanceof Method method) {"));
        assertTrue(printed, source.contains("else if (this.context instanceof Type type) {"));
        assertTrue(printed, source.contains("if (shared instanceof Method method)"));
        assertTrue(printed, source.contains("if (provide() instanceof Type type)"));
        assertTrue(printed, source.contains("if (make() instanceof Method method1)"));
        // the result of a generic call has another type for the source: the test of the value stays, with its cast
        assertTrue(printed, source.contains("if ((object = this.supplier.get()) instanceof Type) { Type type1 = (Type)object;"));
        assertTrue(printed, source.contains("if (provideBase() instanceof Method method2)"));
        // the type of the element of an array is unknown for the detection of the unconditional tests: the test stays, with its cast
        assertTrue(printed, source.contains("if ((context = this.array[0]) instanceof Type) { Type type1 = (Type)context;"));
        // the variable which the code reads after the test is no temporary of the compiler: its assignment stays, between parentheses
        assertTrue(printed, source.contains("int parameter(Object value) { Object object; if ((object = value = this.context) instanceof Method) {"));
        assertTrue(printed, source.contains("return value.hashCode();"));
        assertTrue(printed, CompilerUtil.compile("17", new InMemoryJavaSourceFileObject("org/jd/core/v1/stub/EcjInstanceOfTemporary", printed)));
    }
}
