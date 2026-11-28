/*
 * Copyright (c) 2026 Nicolas Baumann (@nbauma109), with Claude Sonnet 5.5.
 * This project is distributed under the GPLv3 license.
 * This is a copyleft license that gives the user the right to use,
 * copy and modify the code freely for non-commercial purposes.
 */

package org.jd.core.v1.stub;

/**
 * 'Server.escape' of Log4j: in a loop, some cases 'break' to the code which follows a 'switch' whose 'default' only leaves it, others 'continue' the loop
 */
public class SwitchBreakAndContinue {
    public static String escape(String name) {
        StringBuilder sb = new StringBuilder(name.length() * 2);
        boolean needsQuotes = false;
        for (int i = 0; i < name.length(); i++) {
            char c = name.charAt(i);
            switch (c) {
                case '\\', '*':
                    sb.append('\\');
                    needsQuotes = true;
                    break;
                case ',', '=':
                    needsQuotes = true;
                    break;
                case '\r':
                    continue;
                case '\n':
                    sb.append("\\n");
                    needsQuotes = true;
                    continue;
                default:
                    break;
            }
            sb.append(c);
        }
        if (needsQuotes) {
            sb.insert(0, '"');
        }
        return sb.toString();
    }

    /** a 'default' which no case jumps to stays a 'default' */
    public static int withDefault(int[] values) {
        int sum = 0;
        for (int value : values) {
            switch (value) {
                case 1:
                    continue;
                case 2:
                    sum += 2;
                    break;
                default:
                    sum += value;
                    break;
            }
            sum++;
        }
        return sum;
    }

    /** the last case is no 'default', which only leaves the switch */
    public static int withoutLastDefault(int[] values) {
        int sum = 0;
        for (int value : values) {
            switch (value) {
                case 1:
                    continue;
                case 2:
                    sum += 2;
                    break;
                case 3:
                    sum += 3;
                    break;
                default:
                    break;
            }
            sum++;
        }
        return sum;
    }

    /** the code which follows the switch leaves a 'while' loop */
    public static int whileReturns(int[] values) {
        int sum = 0;
        int i = 0;
        while (i < values.length) {
            switch (values[i++]) {
                case 1:
                    continue;
                case 2:
                    sum += 2;
                    break;
                case 3:
                    sum += 3;
                    break;
                default:
                    break;
            }
            return sum;
        }
        return -sum;
    }
    public static int whileThrows(int[] values) {
        int sum = 0;
        int i = 0;
        while (i < values.length) {
            int value = values[i++];
            switch (value) {
                case 1:
                    continue;
                case 2:
                    sum += 2;
                    break;
                case 4:
                    sum += 4;
                    continue;
                default:
                    break;
            }
            throw new IllegalStateException("" + sum);
        }
        return -sum;
    }

    /**
     * Only a 'continue' reaches the update of this loop, as the code which follows the switch returns: the loop keeps its update. The cases which
     * break still fall into the next one, which this fixture does not claim to be right.
     */
    public static int onlyContinueReachesTheUpdate(int[] values) {
        int sum = 0;
        for (int index = 0; index < values.length; index++) {
            switch (values[index]) {
                case 1:
                    continue;
                case 2:
                    sum += 2;
                    break;
                case 3:
                    sum += 3;
                    break;
                default:
                    break;
            }
            return sum;
        }
        return -sum;
    }

    /** a case falls into 'default' only if a test says so, and another one breaks to the code which follows: 'default' stays a case */
    public static int conditionalFallThrough(int[] values) {
        int sum = 0;
        for (int value : values) {
            switch (value) {
                case 1:
                    break;
                case 2:
                    if (sum > 4) {
                        continue;
                    }
                default:
                    sum += 2;
                    break;
            }
            sum += 3;
        }
        return sum;
    }
}
