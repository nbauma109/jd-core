/*
 * Copyright (c) 2026 Nicolas Baumann (@nbauma109), with Claude Sonnet 5.5.
 * This project is distributed under the GPLv3 license.
 * This is a Copyleft license that gives the user the right to use,
 * copy and modify the code freely for non-commercial purposes.
 */

package org.jd.core.v1;

import org.jd.core.v1.api.loader.Loader;
import org.jd.core.v1.compiler.CompilerUtil;
import org.jd.core.v1.compiler.InMemoryJavaSourceFileObject;
import org.jd.core.v1.loader.ClassPathLoader;
import org.jd.core.v1.printer.PlainTextPrinter;
import org.jd.core.v1.stub.EnumConstantBodyStaticCall;
import org.junit.Test;

public class EnumConstantBodyStaticCallTest extends AbstractJdTest {
    @Test
    public void test() throws Exception {
        String internalClassName = EnumConstantBodyStaticCall.class.getName().replace('.', '/');
        Loader loader = new ClassPathLoader();
        String source = decompileSuccess(loader, new PlainTextPrinter(), internalClassName);

        // The anonymous constant body has no name to print: the inherited static calls must stay unqualified
        assertEquals(-1, source.indexOf("null."));
        assertTrue(source.contains("upper(join("));

        // Recompile decompiled source code and check errors
        assertTrue(CompilerUtil.compile("1.8", new InMemoryJavaSourceFileObject(internalClassName, source)));
    }
}
