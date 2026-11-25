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

public class EcjStaticForwardReferenceTest extends AbstractJdTest {

    /** a field which a static block computes is printed after the fields which have an initializer: its simple name would be an illegal forward reference */
    @Test
    public void testInitializerReadsAFieldOfTheStaticBlock() throws Exception {
        String printed;

        try (InputStream is = getClass().getResourceAsStream("/jar/ecj-static-forward-reference-ecj-17.jar")) {
            printed = decompileSuccess(new ZipLoader(is), new NoLineNumberPrinter(), "org/jd/core/v1/stub/EcjStaticForwardReference");
        }

        assertTrue(printed, CompilerUtil.compile("17", new InMemoryJavaSourceFileObject("org/jd/core/v1/stub/EcjStaticForwardReference", printed)));
    }
}
