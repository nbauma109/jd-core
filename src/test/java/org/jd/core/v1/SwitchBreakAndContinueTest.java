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

public class SwitchBreakAndContinueTest extends AbstractJdTest {

    /** the code which follows the 'switch' is no 'default' of it: the cases which continue the loop skip it */
    @Test
    public void testCasesWhichBreakAndCasesWhichContinue() throws Exception {
        String printed;

        try (InputStream is = getClass().getResourceAsStream("/jar/switch-break-and-continue-javac-17.jar")) {
            printed = decompileSuccess(new ZipLoader(is), new NoLineNumberPrinter(), "org/jd/core/v1/stub/SwitchBreakAndContinue");
        }

        String source = printed.replaceAll("\\s+", " ");

        assertTrue(printed, source.contains("case '\\r': continue;"));
        assertTrue(printed, source.contains("needsQuotes = true; continue; } sb.append(c);"));
        assertTrue(printed, source.contains("} return sum; } return -sum;"));
        assertTrue(printed, source.contains("sum += 4; continue; } throw new IllegalStateException("));
        assertTrue(printed, source.contains("index < values.length; index++)"));
        assertTrue(printed, CompilerUtil.compile("17", new InMemoryJavaSourceFileObject("org/jd/core/v1/stub/SwitchBreakAndContinue", printed)));
    }
}
