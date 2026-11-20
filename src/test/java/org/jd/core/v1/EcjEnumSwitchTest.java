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
import org.jd.core.v1.printer.PlainTextPrinter;
import org.junit.Test;

import java.io.InputStream;

public class EcjEnumSwitchTest extends AbstractJdTest {

    /** the ordinals of the map of the Eclipse compiler are the constants of the enum in a switch expression too, and its constants are ints */
    @Test
    public void testSwitchExpressionOnAnEnum() throws Exception {
        String printed;

        try (InputStream is = getClass().getResourceAsStream("/jar/ecj-enum-switch-ecj-17.jar")) {
            printed = decompileSuccess(new ZipLoader(is), new PlainTextPrinter(), "org/jd/core/v1/stub/EcjEnumSwitch");
        }

        assertTrue(printed, printed.contains("case SECOND -> 2;"));
        assertFalse(printed, printed.contains("-> true"));
        assertFalse(printed, printed.contains("$SWITCH_TABLE"));
        assertTrue(printed, CompilerUtil.compile("17", new InMemoryJavaSourceFileObject("org/jd/core/v1/stub/EcjEnumSwitch", printed)));
    }
}
