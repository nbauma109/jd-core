/*
 * Copyright (c) 2026 GPLv3.
 * This project is distributed under the GPLv3 license.
 * This is a Copyleft license that gives the user the right to use,
 * copy and modify the code freely for non-commercial purposes.
 */

package org.jd.core.v1;

import org.jd.core.v1.loader.ClassPathLoader;
import org.jd.core.v1.printer.PlainTextPrinter;
import org.junit.Test;

import java.util.Collections;
import java.util.Map;

public class FieldInitializersAndLocalsTest extends AbstractJdTest {
    @Test
    public void testInitializersAreMovedBackToTheFields() throws Exception {
        Map<String, Object> configuration = Collections.singletonMap("realignLineNumbers", Boolean.TRUE);
        String source = decompileSuccess(new ClassPathLoader(), new PlainTextPrinter(), "org/jd/core/v1/stub/FieldInitializersAndLocals", configuration);

        assertTrue(source, source.contains("private boolean debug = false;"));
        assertTrue(source, source.contains("byName = new HashMap<>();"));
        assertTrue(source, source.contains("byType = new HashMap<>();"));
        assertFalse(source, source.contains("this.debug = false;"));
        assertFalse(source, source.contains("this.byName = "));
    }
}
