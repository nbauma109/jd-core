/*
 * Copyright (c) 2026 Nicolas Baumann (@nbauma109), with Claude Sonnet 5.5.
 * This project is distributed under the GPLv3 license.
 * This is a copyleft license that gives the user the right to use,
 * copy and modify the code freely for non-commercial purposes.
 */

package org.jd.core.v1.stub;

/**
 * A pattern which tests a parameter, compiled without debug information: its type is the one of the signature of the method
 */
public class EcjPatternNoDebug {
    int length(Object value) {
        return value instanceof String text ? text.length() : 0;
    }
}
