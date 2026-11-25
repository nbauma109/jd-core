/*
 * Copyright (c) 2026 Nicolas Baumann (@nbauma109), with Claude Sonnet 5.5.
 * This project is distributed under the GPLv3 license.
 * This is a copyleft license that gives the user the right to use,
 * copy and modify the code freely for non-commercial purposes.
 */

package org.jd.core.v1.stub;

/**
 * A switch which is in a try which has a finally block, compiled by the Eclipse compiler
 */
public class EcjSwitchInTry {
    int closed;

    int withDefault(int state) {
        int result = 0;

        try {
            switch (state) {
                case 0:
                    result = 1;
                    break;
                case 1:
                    result = 2;
                    break;
                default:
            }
        } finally {
            closed++;
        }
        return result;
    }

    int withThrow(int state) {
        int result = 0;

        try {
            int previous = state;

            switch (previous) {
                case 0:
                    result = 1;
                    break;
                case 1:
                    result = 2;
                    break;
                case 2:
                case 3:
                    throw new AssertionError(previous);
            }
        } finally {
            closed++;
        }
        return result;
    }

    int withIf(int state) {
        int result = 0;

        try {
            if (state == 0) {
                result = 1;
            } else {
                result = 2;
            }
        } finally {
            closed++;
        }
        return result;
    }

    void switchOnly(int state) {
        try {
            switch (state) {
                case 0:
                    closed = 1;
                    break;
                case 1:
                    closed = 2;
                    break;
                default:
                    closed = 3;
            }
        } finally {
            closed++;
        }
    }

    int withCatchAndReturn(String text) {
        int result = 0;

        try {
            if (text.isEmpty()) {
                return -1;
            }
            result = Integer.parseInt(text);
        } catch (NumberFormatException e) {
            result = -2;
        } finally {
            closed++;
        }
        return result;
    }

    int nested(int state) {
        int result = 0;

        try {
            try {
                result = state;
            } finally {
                closed++;
            }
            result++;
        } finally {
            closed += 2;
        }
        return result;
    }

    int severalCatches(String text) {
        int result = 0;

        try {
            result = Integer.parseInt(text);
        } catch (NumberFormatException e) {
            result = -2;
        } catch (NullPointerException e) {
            result = -3;
        } finally {
            closed++;
        }
        return result;
    }
}
