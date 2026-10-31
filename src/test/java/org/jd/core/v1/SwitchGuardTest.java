/*
 * Copyright (c) 2026 Nicolas Baumann (@nbauma109), with Claude Sonnet 5.5.
 * This project is distributed under the GPLv3 license.
 * This is a Copyleft license that gives the user the right to use,
 * copy and modify the code freely for non-commercial purposes.
 */

package org.jd.core.v1;

import org.jd.core.v1.api.loader.Loader;
import org.jd.core.v1.loader.ZipLoader;
import org.jd.core.v1.printer.PlainTextPrinter;
import org.junit.Test;

import java.io.InputStream;

/**
 * A guard ('case String s when s.isEmpty()') sets the restart index and jumps back to the call which selects the case: the method is not
 * decompiled to a switch which would select another case, while the switch without a guard is.
 */
public class SwitchGuardTest extends AbstractJdTest {

    @Test
    public void testJAVAC21() throws Exception {
        try (InputStream is = this.getClass().getResourceAsStream("/jar/SwitchGuard-JAVAC-21.jar")) {
            Loader loader = new ZipLoader(is);
            String source = decompile(loader, new PlainTextPrinter(), "org/jd/core/v1/stub/SwitchGuard", java.util.Collections.emptyMap());

            assertTrue(source.contains("Decompilation failed"));
            assertTrue(source.contains("case String s:"));
            assertFalse(source.contains("when"));
        }
    }

    /** A loop which ends before the switch can store in the slot which the switch reuses for its restart index, and jump back: it is no guard */
    @Test
    public void testLoopBeforeTheSwitchIsNoGuard() throws Exception {
        try (InputStream is = this.getClass().getResourceAsStream("/jar/SwitchAfterLoop-JAVAC-21.jar")) {
            Loader loader = new ZipLoader(is);
            String source = decompile(loader, new PlainTextPrinter(), "org/jd/core/v1/stub/SwitchAfterLoop", java.util.Collections.emptyMap());

            assertFalse(source.contains("Decompilation failed"));
            assertTrue(source.contains("switch (o)"));
            assertTrue(source.contains("case String s:"));
        }
    }

    /** A loop which encloses the switch can end by a store of the slot which the switch reuses for its restart index, and jump back: it is no guard */
    @Test
    public void testLoopAroundTheSwitchIsNoGuard() throws Exception {
        try (InputStream is = this.getClass().getResourceAsStream("/jar/SwitchInLoop-JAVAC-21.jar")) {
            Loader loader = new ZipLoader(is);
            String source = decompile(loader, new PlainTextPrinter(), "org/jd/core/v1/stub/SwitchInLoop", java.util.Collections.emptyMap());

            assertFalse(source.contains("Decompilation failed"));
            assertTrue(source.contains("case String s:"));
        }
    }
}
