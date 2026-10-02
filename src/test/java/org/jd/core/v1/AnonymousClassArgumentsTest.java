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
 * javac records no line number for the arguments which follow the anonymous class of a call: they inherit the line of the call, which is
 * before the body of the class, so they must not carry it. ECJ records the lines of these arguments, they are kept.
 */
public class AnonymousClassArgumentsTest extends AbstractJdTest {
    private static final String CLASS_NAME = "org/jd/core/v1/stub/AnonymousClassArguments";
    private static final Map<String, Object> REALIGN = Collections.singletonMap("realignLineNumbers", Boolean.TRUE);
    private static final Pattern NUMBERED_LINE = Pattern.compile("(?m)^/\\*\\s*(\\d+):\\s*(\\d+) \\*/(.*)$");

    /** @return the line of the source of the line which contains the text, "0" if it is unknown, and the line of the decompiled source in [1] */
    private static String[] linesOf(String source, String text) {
        Matcher matcher = NUMBERED_LINE.matcher(source);

        while (matcher.find()) {
            if (matcher.group(3).contains(text)) {
                return new String[] {matcher.group(2), matcher.group(1)};
            }
        }
        throw new AssertionError(text + " is not in:\n" + source);
    }

    @Test
    public void testJavacDoesNotKnowTheLineOfTheArgumentsAfterTheBody() throws Exception {
        String source = decompileSuccess(new ClassPathLoader(), new PlainTextPrinter(), CLASS_NAME, REALIGN);

        assertEquals(source, "0", linesOf(source, "null, \"last\")")[0]);
    }

    @Test
    public void testEcjKnowsTheLinesOfTheArgumentsAfterTheBody() throws Exception {
        try (InputStream is = this.getClass().getResourceAsStream("/jar/anonymous-class-arguments-ecj-17.jar")) {
            Loader loader = new ZipLoader(is);
            String source = decompileSuccess(loader, new PlainTextPrinter(), CLASS_NAME, REALIGN);

            String[] lines = linesOf(source, "null,");

            assertFalse(source, "0".equals(lines[0]));
            assertEquals(source, lines[1], lines[0]);

            lines = linesOf(source, "\"last\")");
            assertFalse(source, "0".equals(lines[0]));
            assertEquals(source, lines[1], lines[0]);
        }
    }
}
