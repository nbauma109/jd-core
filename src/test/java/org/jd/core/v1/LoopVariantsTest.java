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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * The update of a loop which is only reached by 'continue' is the update of its 'for', whatever the nested loops are.
 */
public class LoopVariantsTest extends AbstractJdTest {
    private static final String CLASS_NAME = "org/jd/core/v1/stub/LoopVariants";
    private static final Map<String, Object> REALIGN = Collections.singletonMap("realignLineNumbers", Boolean.TRUE);
    private static final Pattern NUMBERED_LINE = Pattern.compile("(?m)^/\\*\\s*(\\d+):\\s*(\\d+) \\*/(.*)$");

    private static String method(String source, String name) {
        int start = source.indexOf(" " + name + "(");

        return source.substring(start, source.indexOf("  public ", start + 1) > 0 ? source.indexOf("  public ", start + 1) : source.length());
    }

    private static void assertLoops(String source) {
        assertTrue(source, method(source, "skipPairs").contains("j += 2) {"));
        assertTrue(source, method(source, "lastNonZero").contains("i--) {"));
        assertFalse(source, method(source, "lastNonZero").contains("i--; continue"));

        String find = method(source, "find");

        assertTrue(source, find.contains("i++) {"));
        assertTrue(source, find.contains("continue label"));
        assertFalse(source, find.contains("i++; continue"));

        // The updates are not the same one: they stay where they are
        assertTrue(source, method(source, "differentUpdates").contains("i++;"));
        assertTrue(source, method(source, "nestedContinue").contains("continue"));
        assertFalse(source, source.contains("throw null"));

        Matcher matcher = NUMBERED_LINE.matcher(source);

        while (matcher.find()) {
            int original = Integer.parseInt(matcher.group(2));

            if (original != 0) {
                assertEquals("Line " + matcher.group(1) + " is " + original + " in:\n" + source, original, Integer.parseInt(matcher.group(1)));
            }
        }
    }

    @Test
    public void testJavac() throws Exception {
        assertLoops(decompileSuccess(new ClassPathLoader(), new PlainTextPrinter(), CLASS_NAME, REALIGN));
    }

    @Test
    public void testEcj() throws Exception {
        try (InputStream is = this.getClass().getResourceAsStream("/jar/loop-variants-ecj-17.jar")) {
            Loader loader = new ZipLoader(is);

            assertLoops(decompileSuccess(loader, new PlainTextPrinter(), CLASS_NAME, REALIGN));
        }
    }
}
