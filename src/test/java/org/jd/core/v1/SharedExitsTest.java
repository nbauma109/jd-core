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
 * The exit of a loop is not copied for each 'break' which leads to it, even if some of them are after the 'switch' of the loop.
 */
public class SharedExitsTest extends AbstractJdTest {
    private static final Map<String, Object> REALIGN = Collections.singletonMap("realignLineNumbers", Boolean.TRUE);
    private static final Pattern NUMBERED_LINE = Pattern.compile("(?m)^/\\*\\s*(\\d+):\\s*(\\d+) \\*/(.*)$");

    @Test
    public void testTheExitOfALoopIsNotCopiedForTheBreaksAfterItsSwitch() throws Exception {
        String source = decompileSuccess(new ClassPathLoader(), new PlainTextPrinter(), "org/jd/core/v1/stub/SharedExits", REALIGN);
        String method = source.substring(source.indexOf("void flags("));

        assertEquals(method, 1, method.split("throw new IllegalArgumentException", -1).length - 1);

        Matcher matcher = NUMBERED_LINE.matcher(method);
        int numbered = 0;

        while (matcher.find()) {
            int original = Integer.parseInt(matcher.group(2));

            if (original != 0) {
                numbered++;
                assertEquals("Line " + matcher.group(1) + " is " + original + " in:\n" + method, original, Integer.parseInt(matcher.group(1)));
            }
        }
        assertTrue(method, numbered >= 10);
    }

    private String decompileWithEcj() throws Exception {
        try (InputStream is = this.getClass().getResourceAsStream("/jar/shared-exits-ecj-17.jar")) {
            Loader loader = new ZipLoader(is);

            return decompileSuccess(loader, new PlainTextPrinter(), "org/jd/core/v1/stub/SharedExits", REALIGN);
        }
    }

    @Test
    public void testTestsOfOneStatementAreOneReturn() throws Exception {
        String source = decompileWithEcj();
        String method = source.substring(source.indexOf("boolean sameKind("), source.indexOf("boolean separateLines("));

        // ECJ compiles the returned expression into jumps to a few returns of 'true' and 'false'
        assertTrue(method, method.contains("return a.length() == b.length() &&"));
        assertTrue(method, method.contains("a.hashCode() == b.hashCode();"));
        assertFalse(method, method.contains("return true"));
        assertFalse(method, method.contains("return false"));
    }

    @Test
    public void testStatementsOnTheirOwnLinesAreNotRewrittenAsOneReturn() throws Exception {
        for (String source : new String[] {decompileWithEcj(), decompileSuccess(new ClassPathLoader(), new PlainTextPrinter(), "org/jd/core/v1/stub/SharedExits", REALIGN)}) {
            String method = source.substring(source.indexOf("boolean separateLines("), source.indexOf("int parse("));

            assertTrue(method, method.contains("return true;"));
            assertTrue(method, method.contains("return false;"));
        }
    }
}
