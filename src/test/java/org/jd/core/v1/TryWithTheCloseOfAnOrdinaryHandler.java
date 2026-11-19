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

    void rethrow(InputStream stream, Exception context) throws Throwable {
        try {
            stream.read();
            stream.close();
        } catch (Throwable t) {
            t.addSuppressed(context);
            throw t;
        }
    }

    void suppressInTheContext(InputStream stream, Throwable context) throws Throwable {
        try {
            stream.read();
            stream.close();
        } catch (Throwable t) {
            context.addSuppressed(t);
            throw t;
        }
    }

    void openInTheTry(Exception context) throws Throwable {
        try {
            InputStream opened = open();
            opened.read();
            opened.close();
        } catch (Throwable t) {
            t.addSuppressed(context);
            throw t;
        }
    }

    private static InputStream open() {
        return System.in;
    }

    private int count;

    void endsWithACallWhichIsNoClose(InputStream stream, Exception context) throws Throwable {
        try {
            stream.read();
        } catch (Throwable t) {
            t.addSuppressed(context);
            throw t;
        }
    }

    void endsWithAnAssignment(InputStream stream, Exception context) throws Throwable {
        try {
            stream.read();
            this.count = 1;
        } catch (Throwable t) {
            t.addSuppressed(context);
            throw t;
        }
    }
}
