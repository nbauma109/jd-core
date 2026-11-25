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

public class EcjSwitchInTryTest extends AbstractJdTest {

    /** the Eclipse compiler puts the copy of the 'finally' statements which the normal completion runs after the handler: the end of the 'try' block is none of it */
    @Test
    public void testNormalCompletionRunsTheCopyAfterTheHandler() throws Exception {
        String printed;

        try (InputStream is = getClass().getResourceAsStream("/jar/ecj-switch-in-try-ecj-17.jar")) {
            printed = decompileSuccess(new ZipLoader(is), new NoLineNumberPrinter(), "org/jd/core/v1/stub/EcjSwitchInTry");
        }

        String source = printed.replaceAll("\\s+", " ");

        assertTrue(printed, source.contains("try { switch (state) { case 0: result = 1; break; case 1: result = 2; break; } } finally { this.closed++; } return result;"));
        assertTrue(printed, source.contains("throw new AssertionError(previous);"));
        assertTrue(printed, source.contains("try { if (state == 0) { result = 1; } else result = 2; } finally { this.closed++; } return result;"));
        // the 'catch' blocks end with a copy of their own
        assertTrue(printed, source.contains("return -1; result = Integer.parseInt(text); } catch (NumberFormatException numberFormatException) { result = -2; } finally { this.closed++; } return result;"));
        assertTrue(printed, source.contains("result = -3; } finally { this.closed++; } return result;"));
        assertTrue(printed, source.contains("result = state; } finally { this.closed++; } result++; } finally { this.closed += 2; } return result;"));
        assertTrue(printed, CompilerUtil.compile("17", new InMemoryJavaSourceFileObject("org/jd/core/v1/stub/EcjSwitchInTry", printed)));
    }
}
