/*
 * Copyright (c) 2026 Nicolas Baumann (@nbauma109), with Claude Sonnet 5.5.
 * This project is distributed under the GPLv3 license.
 * This is a copyleft license that gives the user the right to use,
 * copy and modify the code freely for non-commercial purposes.
 */

package org.jd.core.v1.stub;

/**
 * The Eclipse compiler switches on an enum, in a method of the enum itself, with a synthetic method '$SWITCH_TABLE$...' which maps the ordinals
 */
public enum EcjEnumSwitch {
    FIRST, SECOND, THIRD;

    int rank() {
        switch (this) {
            case FIRST:
                return 1;
            case THIRD:
                return 3;
            default:
                return 0;
        }
    }

    static int check(int slots, EcjEnumSwitch category) {
        if (slots != switch (category) {
            case FIRST -> 1;
            case SECOND -> 2;
            case THIRD -> 3;
        }) {
            throw new AssertionError("Unexpected size");
        }
        return slots;
    }

    static boolean same(String text, EcjEnumSwitch category) {
        return text.equals(switch (category) {
            case FIRST -> "a";
            default -> "b";
        });
    }

    static boolean larger(int slots, EcjEnumSwitch category) {
        return slots != switch (category) {
            case FIRST -> slots * 2;
            default -> slots;
        };
    }

    static int[] codes() {
        return new int[] { 1, 2 };
    }

    static int code(int index) {
        return switch (codes()[index]) {
            case 1 -> 10;
            default -> 20;
        };
    }

    static String name(EcjEnumSwitch step) {
        String name = "other";
        switch (step) {
            case SECOND:
                name = "second";
                break;
            case FIRST:
                name = "first";
                break;
            default:
                break;
        }
        return name;
    }
}
