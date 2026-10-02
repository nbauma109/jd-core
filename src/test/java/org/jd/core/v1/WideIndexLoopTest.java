/*
 * Copyright (c) 2026 Nicolas Baumann (@nbauma109).
 * This project is distributed under the GPLv3 license.
 * This is a Copyleft license that gives the user the right to use,
 * copy and modify the code freely for non-commercial purposes.
 */

package org.jd.core.v1;

import org.jd.core.v1.loader.ClassPathLoader;
import org.jd.core.v1.printer.PlainTextPrinter;
import org.junit.Test;

import java.util.Collections;

/** The update of an index above the local slot 255 is a 'wide iinc': the loop is still an enhanced 'for'. */
public class WideIndexLoopTest extends AbstractJdTest {
    @Test
    public void test() throws Exception {
        String source = decompileSuccess(new ClassPathLoader(), new PlainTextPrinter(), "org/jd/core/v1/stub/WideIndexLoop",
                Collections.singletonMap("realignLineNumbers", Boolean.TRUE));

        assertTrue(source, source.contains(" : a) {"));
        assertTrue(source, source.contains(" : b) {"));
        assertFalse(source, source.contains("throw null"));
    }
}
