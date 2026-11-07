/*
 * Copyright (c) 2026 Nicolas Baumann (@nbauma109), with Claude Sonnet 5.5.
 * This project is distributed under the GPLv3 license.
 * This is a Copyleft license that gives the user the right to use,
 * copy and modify the code freely for non-commercial purposes.
 */

package org.jd.core.v1;

import org.jd.core.v1.loader.ClassPathLoader;
import org.jd.core.v1.printer.PlainTextPrinter;
import org.junit.Test;

/**
 * The exception which a generic call throws comes from the type arguments which the argument inherits: the handler is reachable, so the resource keeps the type of
 * its anonymous class, whose close throws nothing.
 */
public class ConcreteContainerThrowsTest extends AbstractJdTest {

    @Test
    public void testResourceOfAConcreteContainerKeepsItsAnonymousType() throws Exception {
        String source = decompileSuccess(new ClassPathLoader(), new PlainTextPrinter(), "org/jd/core/v1/stub/ConcreteContainerThrows");

        assertTrue(source.contains("try (var concrete = new AutoCloseable() {"));
    }
}
