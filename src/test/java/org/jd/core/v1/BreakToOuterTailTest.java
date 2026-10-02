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
 * A loop which is left by a 'break' of its enclosing loop, and by a 'break' to the statements which follow it in this enclosing loop:
 * these statements are after the loop, they are not copied in the loop with a 'continue' which would be the one of the loop.
 */
public class BreakToOuterTailTest extends AbstractJdTest {
    private static final String CLASS_NAME = "org/jd/core/v1/stub/BreakToOuterTail";
    private static final Map<String, Object> REALIGN = Collections.singletonMap("realignLineNumbers", Boolean.TRUE);
    private static final Pattern NUMBERED_LINE = Pattern.compile("(?m)^/\\*\\s*(\\d+):\\s*(\\d+) \\*/(.*)$");

    private static void assertTheTailIsAfterTheInnerLoop(String source) {
        assertTrue(source, source.contains("break label"));
        assertFalse(source, source.contains("continue"));
        // 'anchor = sOff++;' follows the closing brace of the inner loop
        assertTrue(source, source.matches("(?s).*anchor\\+\\+;\\s*/\\*[^*]*\\*/\\s*\\}\\s*/\\*[^*]*\\*/\\s*anchor = sOff\\+\\+;.*"));

        Matcher matcher = NUMBERED_LINE.matcher(source);
        int numbered = 0;

        while (matcher.find()) {
            int original = Integer.parseInt(matcher.group(2));

            if (original != 0) {
                numbered++;
                assertEquals("Line " + matcher.group(1) + " is " + original + " in:\n" + source, original, Integer.parseInt(matcher.group(1)));
            }
        }
        assertTrue(source, numbered >= 10);
    }

    @Test
    public void testJavac() throws Exception {
        assertTheTailIsAfterTheInnerLoop(decompileSuccess(new ClassPathLoader(), new PlainTextPrinter(), CLASS_NAME, REALIGN));
    }

    @Test
    public void testEcj() throws Exception {
        try (InputStream is = this.getClass().getResourceAsStream("/jar/break-to-outer-tail-ecj-17.jar")) {
            Loader loader = new ZipLoader(is);

            assertTheTailIsAfterTheInnerLoop(decompileSuccess(loader, new PlainTextPrinter(), CLASS_NAME, REALIGN));
        }
    }
}
