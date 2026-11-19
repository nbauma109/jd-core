/*
 * Copyright (c) 2026 Nicolas Baumann (@nbauma109), with Claude Sonnet 5.5.
 * This project is distributed under the GPLv3 license.
 * This is a copyleft license that gives the user the right to use,
 * copy and modify the code freely for non-commercial purposes.
 */

package org.jd.core.v1;

import java.io.IOException;
import java.io.InputStream;

/** An ordinary try statement (no try-with-resources) which ends with the close of a stream and whose handler adds a suppressed exception */
public class TryWithTheCloseOfAnOrdinaryHandler {

    void run(InputStream stream, Exception context) throws IOException {
        try {
            stream.read();
            stream.close();
        } catch (Throwable t) {
            t.addSuppressed(context);
            throw new IllegalStateException(t);
        }
    }
}
