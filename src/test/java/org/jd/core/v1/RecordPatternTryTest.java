/*
 * Copyright (c) 2026 Nicolas Baumann (@nbauma109), with Claude Sonnet 5.5.
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

/**
 * javac protects the invocation of each accessor of a record pattern by its own entry of the exception table: the 'try' is turned back to the pattern. A
 * hand-written 'try' of the same shape which also protects the body (the byte code has one entry which covers it) is kept, as the exceptions of the body would
 * not be wrapped in a 'MatchException' by the pattern.
 */
public class RecordPatternTryTest extends AbstractJdTest {

    @Test
    public void testLoweredFormIsTheRecordPatternAndTheHandWrittenFormIsKept() throws Exception {
        try (InputStream is = this.getClass().getResourceAsStream("/jar/RecordPatternTry-JAVAC-21.jar")) {
            Loader loader = new ZipLoader(is);
            String source = decompile(loader, new PlainTextPrinter(), "org/jd/core/v1/stub/RecordPatternTry", java.util.Collections.emptyMap());
            String lowered = source.substring(source.indexOf("void lowered("), source.indexOf("void handWritten("));
            String handWritten = source.substring(source.indexOf("void handWritten("));

            assertTrue(lowered.contains("o instanceof Box(int value)"));
            assertFalse(lowered.contains("MatchException"));
            assertFalse(handWritten.contains("instanceof Box("));
            assertTrue(handWritten.contains("catch (Throwable e)"));
            assertTrue(handWritten.contains("throw new MatchException(e.toString(), e);"));
        }
    }
}
