/*
 * Copyright (c) 2026 GPLv3.
 * This project is distributed under the GPLv3 license.
 * This is a Copyleft license that gives the user the right to use,
 * copy and modify the code freely for non-commercial purposes.
 */

package org.jd.core.v1;

import org.jd.core.v1.api.loader.Loader;
import org.jd.core.v1.loader.ZipLoader;
import org.jd.core.v1.printer.PlainTextPrinter;
import org.junit.Test;

import java.io.InputStream;
import java.util.Collections;

/** The accessors of javac 8 to the private static members of the outer class are replaced by the members themselves. */
public class AccessorStaticsTest extends AbstractJdTest {
    @Test
    public void test() throws Exception {
        try (InputStream is = this.getClass().getResourceAsStream("/jar/accessor-statics-javac-8.jar")) {
            Loader loader = new ZipLoader(is);
            String source = decompileSuccess(loader, new PlainTextPrinter(), "org/jd/core/v1/stub/AccessorStatics",
                    Collections.singletonMap("realignLineNumbers", Boolean.TRUE));

            assertFalse(source, source.contains("access$"));
            assertTrue(source, source.contains("AccessorStatics.counter++"));
            assertTrue(source, source.contains("AccessorStatics.total++"));
            assertTrue(source, source.contains("AccessorStatics.total += "));
            assertTrue(source, source.contains("AccessorStatics.twice("));
        }
    }
}
