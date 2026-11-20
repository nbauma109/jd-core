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
import org.jd.core.v1.printer.PlainTextPrinter;
import org.junit.Test;

import java.io.InputStream;

public class EcjBoundsLoopTest extends AbstractJdTest {

    /** a jump to the end of a labeled block inside a loop is a 'break' of the block, not a 'continue' of the loop */
    @Test
    public void testLabeledBlockInALoop() throws Exception {
        String printed;

        try (InputStream is = getClass().getResourceAsStream("/jar/ecj-bounds-loop-ecj-17.jar")) {
            printed = decompileSuccess(new ZipLoader(is), new PlainTextPrinter(), "org/jd/core/v1/stub/EcjBoundsLoop");
        }

        assertTrue(printed, printed.contains("break labelMerge"));
        assertTrue(printed, CompilerUtil.compile("17", new InMemoryJavaSourceFileObject("org/jd/core/v1/stub/EcjBoundsLoop", printed)));
    }
}
