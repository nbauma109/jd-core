/*
 * Copyright (c) 2025 GPLv3.
 * This project is distributed under the GPLv3 license.
 * This is a Copyleft license that gives the user the right to use,
 * copy and modify the code freely for non-commercial purposes.
 */

package org.jd.core.v1;

import org.jd.core.v1.api.loader.Loader;
import org.jd.core.v1.compiler.CompilerUtil;
import org.jd.core.v1.compiler.InMemoryJavaSourceFileObject;
import org.jd.core.v1.loader.ZipLoader;
import org.jd.core.v1.printer.PlainTextPrinter;
import org.junit.Test;

import java.io.InputStream;

public class SwitchExpressionTest extends AbstractJdTest {
    @Test
    public void test() throws Exception {
        String internalClassName = SwitchExpression.class.getName().replace('.', '/');
        try (InputStream is = this.getClass().getResourceAsStream("/jar/switch-expression-jdk17.0.6.jar")) {
            Loader loader = new ZipLoader(is);
            String source = decompileSuccess(loader, new PlainTextPrinter(), internalClassName);

            // Recompile decompiled source code and check errors
            assertTrue(CompilerUtil.compile("17", new InMemoryJavaSourceFileObject(internalClassName, source)));
        }
    }

    @Test
    public void testECJ() throws Exception {
        String internalClassName = SwitchExpression.class.getName().replace('.', '/');
        try (InputStream is = this.getClass().getResourceAsStream("/jar/switch-expression-ecj-17.jar")) {
            Loader loader = new ZipLoader(is);
            String source = decompileSuccess(loader, new PlainTextPrinter(), internalClassName);
            
            // Recompile decompiled source code and check errors
            assertTrue(CompilerUtil.compile("17", new InMemoryJavaSourceFileObject(internalClassName, source)));
        }
    }
    
}
