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

public class EcjSwitchLocalsTest extends AbstractJdTest {

    /** the variables with the same name, declared in the cases of different switches, are declared where they are first assigned */
    @Test
    public void testSameNameInDifferentSwitches() throws Exception {
        String printed;

        try (InputStream is = getClass().getResourceAsStream("/jar/ecj-switch-locals-ecj-17.jar")) {
            printed = decompileSuccess(new ZipLoader(is), new NoLineNumberPrinter(), "org/jd/core/v1/stub/EcjSwitchLocals");
        }

        String source = printed.replaceAll("\\s+", " ");

        assertFalse(printed, source.contains("glb = null"));
        assertEquals(printed, 3, source.split("String\\[\\] glb|glb\\[\\]", -1).length - 1);
        assertTrue(printed, CompilerUtil.compile("17", new InMemoryJavaSourceFileObject("org/jd/core/v1/stub/EcjSwitchLocals", printed)));
    }
}
