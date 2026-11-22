/*
 * Copyright (c) 2026 Nicolas Baumann (@nbauma109), with Claude Sonnet 5.5.
 * This project is distributed under the GPLv3 license.
 * This is a copyleft license that gives the user the right to use,
 * copy and modify the code freely for non-commercial purposes.
 */

package org.jd.core.v1;

import org.jd.core.v1.api.loader.Loader;
import org.jd.core.v1.compiler.CompilerUtil;
import org.jd.core.v1.compiler.InMemoryJavaSourceFileObject;
import org.jd.core.v1.loader.ClassPathLoader;
import org.jd.core.v1.printer.PlainTextPrinter;
import org.jd.core.v1.stub.DefaultConstructor;
import org.jd.core.v1.stub.PrivateConstructor;
import org.junit.Test;

public class PrivateConstructorTest extends AbstractJdTest {

    /** An explicit private constructor of a public class is not a default constructor: it must be kept. */
    @Test
    public void testPrivateConstructorIsKept() throws Exception {
        String internalClassName = PrivateConstructor.class.getName().replace('.', '/');
        Loader loader = new ClassPathLoader();
        String source = decompileSuccess(loader, new PlainTextPrinter(), internalClassName).replaceAll("/\\*[^*]*\\*/", "");

        assertTrue(source, source.contains("private PrivateConstructor()"));
        // the default constructor of a private nested class is private too: it is still not printed
        assertFalse(source, source.contains("PrivateNested() {"));
        assertTrue(source, CompilerUtil.compile("1.8", new InMemoryJavaSourceFileObject(internalClassName, source)));
    }

    /** The default constructor is still removed. */
    @Test
    public void testDefaultConstructorIsRemoved() throws Exception {
        String internalClassName = DefaultConstructor.class.getName().replace('.', '/');
        Loader loader = new ClassPathLoader();
        String source = decompileSuccess(loader, new PlainTextPrinter(), internalClassName).replaceAll("/\\*[^*]*\\*/", "");

        assertFalse(source, source.contains("DefaultConstructor()"));
    }
}
