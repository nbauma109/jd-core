/*
 * Copyright (c) 2026 GPLv3.
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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * An inner loop which is left with 'continue' of the main loop stays a loop, even if the main loop is a 'do ... while' and the
 * inner loop is followed by a 'return'.
 */
public class ContinueMainLoopTest extends AbstractJdTest {
    private static final String CLASS_NAME = "org/jd/core/v1/stub/ContinueMainLoop";
    private static final Map<String, Object> REALIGN = Collections.singletonMap("realignLineNumbers", Boolean.TRUE);
    private static final Pattern NUMBERED_LINE = Pattern.compile("(?m)^/\\*\\s*(\\d+):\\s*(\\d+) \\*/(.*)$");

    private static void assertInnerLoopIsKept(String source) {
        String method = source.substring(source.indexOf("containsInDoWhile("));

        assertTrue(method, method.contains("label"));
        assertTrue(method, method.contains(": do {"));
        assertTrue(method, method.contains("for (ptr += 4; ptr < branchEnd; ptr += 2) {"));
        assertTrue(method, method.contains("continue label"));
        assertFalse(method, method.contains("while (true)"));
        assertFalse(source, source.contains("throw null"));

        Matcher matcher = NUMBERED_LINE.matcher(source);
        int numbered = 0;

        while (matcher.find()) {
            int original = Integer.parseInt(matcher.group(2));

            if (original != 0) {
                numbered++;
                assertEquals("Line " + matcher.group(1) + " is " + original + " in:\n" + source, original, Integer.parseInt(matcher.group(1)));
            }
        }
        assertTrue(source, numbered >= 15);
    }

    @Test
    public void testJavac() throws Exception {
        assertInnerLoopIsKept(decompileSuccess(new ClassPathLoader(), new PlainTextPrinter(), CLASS_NAME, REALIGN));
    }

    @Test
    public void testEcj() throws Exception {
        try (InputStream is = this.getClass().getResourceAsStream("/jar/continue-main-loop-ecj-17.jar")) {
            Loader loader = new ZipLoader(is);

            assertInnerLoopIsKept(decompileSuccess(loader, new PlainTextPrinter(), CLASS_NAME, REALIGN));
        }
    }
}
