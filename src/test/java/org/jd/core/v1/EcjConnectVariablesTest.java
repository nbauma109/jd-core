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

public class EcjConnectVariablesTest extends AbstractJdTest {

    /** a case which leaves the labeled block of a switch does not move the code which follows the switch into the last case */
    @Test
    public void testSwitchInALabeledBlock() throws Exception {
        String printed;

        try (InputStream is = getClass().getResourceAsStream("/jar/ecj-connect-variables-ecj-17.jar")) {
            printed = decompileSuccess(new ZipLoader(is), new PlainTextPrinter(), "org/jd/core/v1/stub/EcjConnectVariables");
        }

        assertTrue(printed, printed.contains("break label"));
        assertFalse(printed, printed.contains("} else if (!superType.endsWith"));
        assertTrue(printed, CompilerUtil.compile("17", new InMemoryJavaSourceFileObject("org/jd/core/v1/stub/EcjConnectVariables", printed)));
    }
}

