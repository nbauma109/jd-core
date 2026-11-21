/*
 * Copyright (c) 2026 Nicolas Baumann (@nbauma109), with Claude Sonnet 5.5.
 * This project is distributed under the GPLv3 license.
 * This is a copyleft license that gives the user the right to use,
 * copy and modify the code freely for non-commercial purposes.
 */

package org.jd.core.v1.stub;

import java.util.concurrent.TimeUnit;

/**
 * Javac switches on an enum of another class with the map of the ordinals '$SwitchMap$', which is in a synthetic class
 */
public class JavacEnumSwitch {
    static int check(int slots, TimeUnit unit) {
        if (slots != switch (unit) {
            case SECONDS -> 1;
            case MINUTES -> 2;
            default -> 3;
        }) {
            throw new AssertionError("Unexpected size");
        }
        return slots;
    }

    static final int[] CODES = { 1, 2 };

    static int code(int index) {
        return switch (CODES[index]) {
            case 1 -> 10;
            default -> 20;
        };
    }
}
