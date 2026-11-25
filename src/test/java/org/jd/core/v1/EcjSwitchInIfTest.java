/*
 * Copyright (c) 2026 Nicolas Baumann (@nbauma109), with Claude Sonnet 5.5.
 * This project is distributed under the GPLv3 license.
 * This is a copyleft license that gives the user the right to use,
 * copy and modify the code freely for non-commercial purposes.
 */

package org.jd.core.v1;

import org.jd.core.v1.compiler.CompilerUtil;
import org.jd.core.v1.compiler.InMemoryJavaSourceFileObject;
import org.jd.core.v1.loader.ClassPathLoader;
import org.jd.core.v1.loader.ZipLoader;
import org.jd.core.v1.printer.NoLineNumberPrinter;
import org.junit.Test;

import java.io.InputStream;

public class EcjSwitchInIfTest extends AbstractJdTest {

    /** 'if (c) { switch (x) { case 1: continue; default: } } rest': the empty default case is the end of the switch, not a case which holds the code which follows */
    @Test
    public void testCodeAfterASwitchWhichIsInAnIf() throws Exception {
        String printed;

        try (InputStream is = getClass().getResourceAsStream("/jar/ecj-switch-in-if-ecj-17.jar")) {
            printed = decompileSuccess(new ZipLoader(is), new NoLineNumberPrinter(), "org/jd/core/v1/stub/EcjSwitchInIf");
        }

        String source = printed.replaceAll("\\s+", " ");

        // the code which follows the 'if' is not in it: it is reached when the dimensions are none
        assertTrue(printed, source.contains("default: current = current.bound; break; } } Binding first = current.bound;"));
        assertTrue(printed, source.contains("continue; } count += 2;"));
        assertTrue(printed, source.contains("for (int i = 0; i < max; i++)"));
        // the switch of the else branch, whose last case is empty, is not the one of the code which follows the branch
        assertFalse(printed, source.contains("throw null"));
        assertTrue(printed, source.contains("case 1: msecs += 100 * text.charAt(start);"));
        assertTrue(printed, CompilerUtil.compile("17", new InMemoryJavaSourceFileObject("org/jd/core/v1/stub/EcjSwitchInIf", printed)));
    }

    /** 'AbstractService.stopAsync' of Guava: the switch of a try, in an if whose test jumps over it, is not the one which the test of the 'if' ends */
    @Test
    public void testSwitchInATryInAnIf() throws Exception {
        String source = decompileSuccess(new ClassPathLoader(), new NoLineNumberPrinter(), "com/google/common/util/concurrent/AbstractService");

        assertTrue(source, source.contains("doCancelStart();"));
        assertTrue(source, source.contains("isStoppable is incorrectly implemented"));
    }
}
