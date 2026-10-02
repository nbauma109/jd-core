/*
 * Copyright (c) 2008, 2019 Emmanuel Dupuy.
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
 * An inner loop left with 'continue outer' stays a loop, and the statements which follow it stay in the outer loop.
 */
public class LabeledContinueLoopsTest extends AbstractJdTest {
    private static final String CLASS_NAME = "org/jd/core/v1/stub/LabeledContinueLoops";
    private static final Map<String, Object> REALIGN = Collections.singletonMap("realignLineNumbers", Boolean.TRUE);
    private static final Pattern NUMBERED_LINE = Pattern.compile("(?m)^/\\*\\s*(\\d+):\\s*(\\d+) \\*/(.*)$");

    private static void assertInnerLoopIsKept(String source) {
        assertTrue(source, source.contains("for (int b = 0; b < count; b++) {"));
        assertTrue(source, source.contains("continue label"));
        assertFalse(source, source.contains("while (true)"));
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
        assertTrue(source, numbered >= 6);
    }

    private static void assertUpdateIsInTheHeader(String source) {
        assertTrue(source, source.contains("i < array.length - target.length + 1; i++) {"));
        assertFalse(source, source.contains("i++; continue"));
        assertTrue(source, source.contains("for (int j = 0; j < target.length; j++) {"));
        assertTrue(source, source.contains("continue label"));
    }

    @Test
    public void testJavac() throws Exception {
        String source = decompileSuccess(new ClassPathLoader(), new PlainTextPrinter(), CLASS_NAME, REALIGN);

        assertInnerLoopIsKept(source);
        assertUpdateIsInTheHeader(source);
    }

    @Test
    public void testEcj() throws Exception {
        try (InputStream is = this.getClass().getResourceAsStream("/jar/labeled-continue-loops-ecj-17.jar")) {
            Loader loader = new ZipLoader(is);

            String source = decompileSuccess(loader, new PlainTextPrinter(), CLASS_NAME, REALIGN);

            assertInnerLoopIsKept(source);
            assertUpdateIsInTheHeader(source);
        }
    }
}
