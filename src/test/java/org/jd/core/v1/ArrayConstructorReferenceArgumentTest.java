/*
 * Copyright (c) 2026 Nicolas Baumann (@nbauma109), with Claude Sonnet 5.5.
 * This project is distributed under the GPLv3 license.
 * This is a copyleft license that gives the user the right to use,
 * copy and modify the code freely for non-commercial purposes.
 */

package org.jd.core.v1;

import org.jd.core.v1.compiler.CompilerUtil;
import org.jd.core.v1.compiler.InMemoryJavaSourceFileObject;
import org.jd.core.v1.loader.ClassPathLoader;
import org.jd.core.v1.printer.NoLineNumberPrinter;
import org.junit.Test;

public class ArrayConstructorReferenceArgumentTest extends AbstractJdTest {

    /** the reference takes its type arguments from the parameter it is passed to: a raw 'Function' cast gives the constructor an 'Object' size */
    @Test
    public void testNoRawCastOfTheReference() throws Exception {
        String internalClassName = ArrayConstructorReferenceArgument.class.getName().replace('.', '/');
        String printed = decompileSuccess(new ClassPathLoader(), new NoLineNumberPrinter(), internalClassName);
        String source = printed.replaceAll("\\s+", " ");

        assertFalse(printed, source.contains("(Function)"));
        assertTrue(printed, source.contains("Reference[]::new"));
        assertTrue(printed, source.contains("Binding[]::new"));
        assertTrue(printed, source.contains("String[]::new"));
        assertTrue(printed, CompilerUtil.compile("17", new InMemoryJavaSourceFileObject(internalClassName, printed)));
    }
}
