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
 * A line which cannot be aligned shifts every following line: the whole end of the class was one line late.
 */
public class RealignmentCascadesTest extends AbstractJdTest {
    private static final Map<String, Object> REALIGN = Collections.singletonMap("realignLineNumbers", Boolean.TRUE);
    private static final Pattern NUMBERED_LINE = Pattern.compile("(?m)^/\\*\\s*(\\d+):\\s*(\\d+) \\*/(.*)$");

    private static void assertAligned(String source) {
        Matcher matcher = NUMBERED_LINE.matcher(source);
        int numbered = 0;

        while (matcher.find()) {
            int original = Integer.parseInt(matcher.group(2));

            if (original != 0) {
                numbered++;
                assertEquals("Line " + matcher.group(1) + " is " + original + " in:\n" + source, original, Integer.parseInt(matcher.group(1)));
            }
        }
        assertTrue(source, numbered >= 2);
    }

    @Test
    public void testEnumWithoutConstantsJavac() throws Exception {
        assertAligned(decompileSuccess(new ClassPathLoader(), new PlainTextPrinter(), "org/jd/core/v1/stub/RealignmentCascades$Utils", REALIGN));
    }

    @Test
    public void testTernaryOnAnonymousClassJavac() throws Exception {
        assertAligned(decompileSuccess(new ClassPathLoader(), new PlainTextPrinter(), "org/jd/core/v1/stub/RealignmentCascades", REALIGN));
    }

    @Test
    public void testEcj() throws Exception {
        try (InputStream is = this.getClass().getResourceAsStream("/jar/realignment-cascades-ecj-17.jar")) {
            Loader loader = new ZipLoader(is);

            assertAligned(decompileSuccess(loader, new PlainTextPrinter(), "org/jd/core/v1/stub/RealignmentCascades", REALIGN));
        }
        try (InputStream is = this.getClass().getResourceAsStream("/jar/realignment-cascades-ecj-17.jar")) {
            Loader loader = new ZipLoader(is);

            assertAligned(decompileSuccess(loader, new PlainTextPrinter(), "org/jd/core/v1/stub/RealignmentCascades$Utils", REALIGN));
        }
    }
}
