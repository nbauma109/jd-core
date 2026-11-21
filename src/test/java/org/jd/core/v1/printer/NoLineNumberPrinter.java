/*
 * Copyright (c) 2026 Nicolas Baumann (@nbauma109), with Claude Sonnet 5.5.
 * This project is distributed under the GPLv3 license.
 * This is a copyleft license that gives the user the right to use,
 * copy and modify the code freely for non-commercial purposes.
 */

package org.jd.core.v1.printer;

/** A plain text printer which prints no line number comment: the tests which look for code in the decompiled source do not have to strip them */
public class NoLineNumberPrinter extends PlainTextPrinter {
    @Override
    protected void printLineNumber(int lineNumber) {
        // the line numbers are not printed
    }
}
