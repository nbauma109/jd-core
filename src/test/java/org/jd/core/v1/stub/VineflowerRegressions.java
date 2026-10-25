/*
 * Copyright (c) 2026 Nicolas Baumann (@nbauma109), with Claude Sonnet 5.5.
 * This project is distributed under the GPLv3 license.
 * This is a Copyleft license that gives the user the right to use,
 * copy and modify the code freely for non-commercial purposes.
 */

package org.jd.core.v1.stub;

public class VineflowerRegressions {

    public double widenedFloat() {
        return 0.2F;
    }

    public String switchExpression(int i) {
        return switch (i) {
            case 1 -> "1";
            case 2 -> "2";
            default -> "Unknown";
        };
    }

    @SuppressWarnings("java:S9435") // the negated condition is the pattern under test
    public boolean negatedAnd(double n) {
        return !((n < 1.0 - n) && n > 3) ? n < 5.0 : n > 2;
    }

    public boolean or(double n) {
        return ((n < 1.0 - n) || n > 3) ? n < 5.0 : n > 2;
    }

    public boolean and(double n) {
        return ((n < 1.0 - n) && n > 3) ? n < 5.0 : n > 2;
    }
}
