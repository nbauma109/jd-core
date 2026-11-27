/*
 * Copyright (c) 2026 Nicolas Baumann (@nbauma109), with Claude Sonnet 5.5.
 * This project is distributed under the GPLv3 license.
 * This is a copyleft license that gives the user the right to use,
 * copy and modify the code freely for non-commercial purposes.
 */

package org.jd.core.v1;

import org.jd.core.v1.compiler.CompilerUtil;
import org.jd.core.v1.compiler.InMemoryJavaSourceFileObject;
import org.jd.core.v1.loader.ZipLoader;
import org.jd.core.v1.printer.NoLineNumberPrinter;
import org.junit.Test;

import java.io.InputStream;

public class SwitchBeforeATryTest extends AbstractJdTest {

    /** a 'switch' statement whose cases return is no 'switch' expression, whatever its 'default' goes on with */
    @Test
    public void testSwitchBeforeATryOrALoop() throws Exception {
        String printed;

        try (InputStream is = getClass().getResourceAsStream("/jar/switch-before-a-try-javac-8.jar")) {
            printed = decompileSuccess(new ZipLoader(is), new NoLineNumberPrinter(), "org/jd/core/v1/stub/SwitchBeforeATry");
        }

        String source = printed.replaceAll("\\s+", " ");

        assertTrue(printed, source.contains("switch (name) { case \"NoOp\": return \"none\"; case \"WebApp\": return \"web\";"));
        assertTrue(printed, source.contains("switch (value) { case 1: return \"one\"; case 2: return \"two\";"));
        assertTrue(printed, source.contains("switch (value) { case 1: return -1; case 2: return -2;"));
        assertTrue(printed, CompilerUtil.compile("8", new InMemoryJavaSourceFileObject("org/jd/core/v1/stub/SwitchBeforeATry", printed)));
    }
}
