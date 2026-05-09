/*
 * Copyright (c) 2008, 2019 Emmanuel Dupuy.
 * This project is distributed under the GPLv3 license.
 * This is a Copyleft license that gives the user the right to use,
 * copy and modify the code freely for non-commercial purposes.
 */

package org.jd.core.v1;

import org.jd.core.v1.compiler.CompilerUtil;
import org.jd.core.v1.compiler.InMemoryJavaSourceFileObject;
import org.jd.core.v1.loader.ClassPathLoader;
import org.jd.core.v1.printer.PlainTextPrinter;
import org.jd.core.v1.stub.DoubleCastedFloat;
import org.junit.Test;

import java.util.Collections;

public class DoubleCastedFloatTest extends AbstractJdTest {

    @Test
    // Test for double casted float precision issue
    public void testDoubleCastedFloat() throws Exception {
        String internalClassName = DoubleCastedFloat.class.getName().replace('.', '/');
        String source = decompile(new ClassPathLoader(), new PlainTextPrinter(), internalClassName, Collections.emptyMap());
        assertEquals(-1, source.indexOf("// Byte code:"));

        // Check decompiled source code - should preserve float notation without explicit cast
        assertTrue("Line with 0.2F not found", source.contains("double x = 0.2F"));
        // Variable cast should be kept
        assertTrue("Line with (double)f not found", source.contains("double z = (double)f"));

        // Recompile decompiled source code and check errors
        assertTrue(CompilerUtil.compile("1.8", new InMemoryJavaSourceFileObject(internalClassName, source)));
    }
}
