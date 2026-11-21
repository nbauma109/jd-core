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

public class EcjLoopExitFromSwitchTest extends AbstractJdTest {

    /** the exit of a loop from a switch is the break of the loop which contains it, not of a loop which comes after it in the code */
    @Test
    public void testLoopExitFromASwitch() throws Exception {
        String printed;

        try (InputStream is = getClass().getResourceAsStream("/jar/ecj-loop-exit-from-switch-ecj-17.jar")) {
            printed = decompileSuccess(new ZipLoader(is), new NoLineNumberPrinter(), "org/jd/core/v1/stub/EcjLoopExitFromSwitch");
        }

        String source = printed.replaceAll("\\s+", " ");

        int loop = source.indexOf(": while (this.index < tokens.length)");

        assertTrue(printed, loop > 0);

        String label = source.substring(source.lastIndexOf(' ', loop - 1) + 1, loop);

        assertTrue(printed, source.contains("break " + label + ";"));
        assertTrue(printed, CompilerUtil.compile("17", new InMemoryJavaSourceFileObject("org/jd/core/v1/stub/EcjLoopExitFromSwitch", printed)));
    }
}

