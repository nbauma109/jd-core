/*
 * Copyright (c) 2008, 2019 Emmanuel Dupuy.
 * This project is distributed under the GPLv3 license.
 * This is a Copyleft license that gives the user the right to use,
 * copy and modify the code freely for non-commercial purposes.
 */

package org.jd.core.v1;

import org.jd.core.v1.loader.ZipLoader;
import org.jd.core.v1.printer.PlainTextPrinter;
import org.junit.Test;

import java.io.InputStream;
import java.util.Collections;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * ECJ compiles the 'switch' on a string as a single 'switch' on the hash code, which jumps to the code of each string.
 */
public class EcjStringSwitchTest extends AbstractJdTest {
    private static final Map<String, Object> REALIGN = Collections.singletonMap("realignLineNumbers", Boolean.TRUE);
    private static final Pattern NUMBERED_LINE = Pattern.compile("(?m)^/\\*\\s*(\\d+):\\s*(\\d+) \\*/(.*)$");

    /** The line number comment which starts each line of the printed source */
    private static final String BETWEEN_LABELS = "\\s*/\\*[^*]*\\*/\\s*";

    private String decompile() throws Exception {
        try (InputStream is = this.getClass().getResourceAsStream("/jar/ecj-string-switch-ecj-17.jar")) {
            return decompileSuccess(new ZipLoader(is), new PlainTextPrinter(), "org/jd/core/v1/stub/EcjStringSwitch", REALIGN);
        }
    }

    @Test
    public void testStringsAreTheLabelsOfTheSwitch() throws Exception {
        String source = decompile();

        assertTrue(source, source.contains("switch (name) {"));
        assertTrue(source, source.contains("case \"8.0\":"));
        assertTrue(source, source.contains("case \"10.0\":"));
        assertTrue(source, source.contains("switch (key) {"));
        assertFalse(source, source.contains("hashCode"));
        assertFalse(source, source.contains("throw null"));
        assertFalse(source, source.contains(".equals("));
    }

    @Test
    public void testStringsWithTheSameCodeAndTheSameHashCode() throws Exception {
        String source = decompile();

        // 'not' and 'nor' share their code, 'Aa' and 'BB' have the same hash code
        assertTrue(source, source.matches("(?s).*case \"(nor|not)\":" + BETWEEN_LABELS + "case \"(nor|not)\":.*"));
        assertTrue(source, source.matches("(?s).*case \"(Aa|BB)\":" + BETWEEN_LABELS + "case \"(Aa|BB)\":.*"));
    }

    @Test
    public void testLineNumbersAreAligned() throws Exception {
        String source = decompile();
        Matcher matcher = NUMBERED_LINE.matcher(source);
        int numbered = 0;

        while (matcher.find()) {
            int original = Integer.parseInt(matcher.group(2));

            if (original != 0) {
                numbered++;
                assertEquals("Line " + matcher.group(1) + " is " + original + " in:\n" + source, original, Integer.parseInt(matcher.group(1)));
            }
        }
        assertTrue(source, numbered >= 8);
    }
}
