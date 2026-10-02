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
 * The parameters of a lambda which initializes a field are not local variables of the constructor: the initializer is moved back to the
 * declaration of the field.
 */
public class LambdaFieldInitializersTest extends AbstractJdTest {
    private static final String CLASS_NAME = "org/jd/core/v1/stub/LambdaFieldInitializers";
    private static final Map<String, Object> REALIGN = Collections.singletonMap("realignLineNumbers", Boolean.TRUE);
    private static final Pattern NUMBERED_LINE = Pattern.compile("(?m)^/\\*\\s*(\\d+):\\s*(\\d+) \\*/(.*)$");

    private static void assertInitializersAreOnTheFields(String source) {
        assertTrue(source, source.contains("adder = (a, b) -> a + b;"));
        assertTrue(source, source.contains("echo = text -> text;"));
        assertFalse(source, source.contains("this.adder ="));
        assertFalse(source, source.contains("this.echo ="));

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
        assertInitializersAreOnTheFields(decompileSuccess(new ClassPathLoader(), new PlainTextPrinter(), CLASS_NAME, REALIGN));
    }

    @Test
    public void testEcj() throws Exception {
        try (InputStream is = this.getClass().getResourceAsStream("/jar/lambda-field-initializers-ecj-17.jar")) {
            Loader loader = new ZipLoader(is);

            assertInitializersAreOnTheFields(decompileSuccess(loader, new PlainTextPrinter(), CLASS_NAME, REALIGN));
        }
    }
}
