/*
 * Copyright (c) 2026 Nicolas Baumann (@nbauma109).
 * This project is distributed under the GPLv3 license.
 * This is a Copyleft license that gives the user the right to use,
 * copy and modify the code freely for non-commercial purposes.
 */

package org.jd.core.v1;

import org.jd.core.v1.api.loader.Loader;
import org.jd.core.v1.loader.ClassPathLoader;
import org.jd.core.v1.loader.ZipLoader;
import org.jd.core.v1.printer.PlainTextPrinter;
import org.junit.Test;

import java.io.InputStream;
import java.util.Collections;
import java.util.Map;

/**
 * An inner loop on an array, left with 'continue' of its outer loop on two paths and followed by a 'throw' (e.g. BCEL's
 * 'Pass3aVerifier.delayedPass2Checks'): neither path loses its exit, and the update of the outer loop is not lost.
 */
public class ContinueAfterArrayLoopTest extends AbstractJdTest {
    private static final String CLASS_NAME = "org/jd/core/v1/stub/ContinueAfterArrayLoop";
    private static final Map<String, Object> REALIGN = Collections.singletonMap("realignLineNumbers", Boolean.TRUE);

    private static int count(String source, String text) {
        return source.split(java.util.regex.Pattern.quote(text), -1).length - 1;
    }

    private static void assertBothPathsLeaveTheInnerLoop(String source) {
        assertTrue(source, source.contains("for (int number : numbers)"));
        // The path which reports the offset and the one which records it
        assertEquals(source, 2, count(source, "break;"));
        // The index of the outer loop is not left behind
        assertFalse(source, source.contains("; )"));
        assertFalse(source, source.contains("throw null"));
    }

    @Test
    public void testJavac8() throws Exception {
        try (InputStream is = this.getClass().getResourceAsStream("/jar/continue-after-array-loop-javac-8.jar")) {
            Loader loader = new ZipLoader(is);

            assertBothPathsLeaveTheInnerLoop(decompileSuccess(loader, new PlainTextPrinter(), CLASS_NAME, REALIGN));
        }
    }

    @Test
    public void testJavac() throws Exception {
        assertBothPathsLeaveTheInnerLoop(decompileSuccess(new ClassPathLoader(), new PlainTextPrinter(), CLASS_NAME, REALIGN));
    }

    @Test
    public void testEcj() throws Exception {
        try (InputStream is = this.getClass().getResourceAsStream("/jar/continue-after-array-loop-ecj-17.jar")) {
            Loader loader = new ZipLoader(is);

            assertBothPathsLeaveTheInnerLoop(decompileSuccess(loader, new PlainTextPrinter(), CLASS_NAME, REALIGN));
        }
    }
}
