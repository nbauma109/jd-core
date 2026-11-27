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

public class ConcatenatedIntConstantTest extends AbstractJdTest {

    /** 'append(int)' appends a number, 'append(char)' a character: the constants of the concatenation stay what they were */
    @Test
    public void testConstantsKeepTheTypeWhichIsAppended() throws Exception {
        String printed;

        try (InputStream is = getClass().getResourceAsStream("/jar/concatenated-int-constant-javac-8.jar")) {
            printed = decompileSuccess(new ZipLoader(is), new NoLineNumberPrinter(), "org/jd/core/v1/stub/ConcatenatedIntConstant");
        }

        assertTrue(printed, printed.contains("name + \" tried \" + 10000 + \" times, \" + 65;"));
        assertTrue(printed, printed.contains("return 10000 + name;"));
        assertTrue(printed, printed.contains("return name + 'A' + '"));
        assertTrue(printed, CompilerUtil.compile("17", new InMemoryJavaSourceFileObject("org/jd/core/v1/stub/ConcatenatedIntConstant", printed)));
    }
}
