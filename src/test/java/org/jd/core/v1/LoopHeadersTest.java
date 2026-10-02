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
 * The initialization, the condition and the update of a 'for' stay in its header, whatever the body is.
 */
public class LoopHeadersTest extends AbstractJdTest {
    private static final String CLASS_NAME = "org/jd/core/v1/stub/LoopHeaders";
    private static final Map<String, Object> REALIGN = Collections.singletonMap("realignLineNumbers", Boolean.TRUE);
    private static final Pattern NUMBERED_LINE = Pattern.compile("(?m)^/\\*\\s*(\\d+):\\s*(\\d+) \\*/(.*)$");

    private static void assertHeaders(String source) {
        // The body ends with a 'break': the update is only reached by 'continue'
        assertTrue(source, source.contains("for (; j >= 0; j--) {"));
        assertFalse(source, source.contains("j--; continue"));
        // The condition contains a ternary operator
        assertTrue(source, source.contains("for (int i = 0; i < ((extra == null) ? 0 : extra.length); i++) {"));
        assertTrue(source, source.contains("int value;"));
        assertFalse(source, source.contains("while (true)"));
        // The first statement of the body is on the line of the header
        assertTrue(source, source.contains("for (int i = 0; i < parameters; i++) {"));
        // A statement of the body, on its own line, is not the update of a 'for'
        assertTrue(source, source.contains("while (position + 1 < pattern.length()) {"));
        assertFalse(source, source.contains("position++)"));

        Matcher matcher = NUMBERED_LINE.matcher(source);
        int numbered = 0;

        while (matcher.find()) {
            int original = Integer.parseInt(matcher.group(2));

            if (original != 0) {
                numbered++;
                assertEquals("Line " + matcher.group(1) + " is " + original + " in:\n" + source, original, Integer.parseInt(matcher.group(1)));
            }
        }
        assertTrue(source, numbered >= 20);
    }

    @Test
    public void testJavac() throws Exception {
        assertHeaders(decompileSuccess(new ClassPathLoader(), new PlainTextPrinter(), CLASS_NAME, REALIGN));
    }

    @Test
    public void testEcj() throws Exception {
        try (InputStream is = this.getClass().getResourceAsStream("/jar/loop-headers-ecj-17.jar")) {
            Loader loader = new ZipLoader(is);

            assertHeaders(decompileSuccess(loader, new PlainTextPrinter(), CLASS_NAME, REALIGN));
        }
    }
}
