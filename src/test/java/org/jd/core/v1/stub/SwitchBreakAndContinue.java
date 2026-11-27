/*
 * Copyright (c) 2026 Nicolas Baumann (@nbauma109), with Claude Sonnet 5.5.
 * This project is distributed under the GPLv3 license.
 * This is a copyleft license that gives the user the right to use,
 * copy and modify the code freely for non-commercial purposes.
 */

package org.jd.core.v1.stub;

/**
 * 'Server.escape' of Log4j: in a loop, some cases 'break' to the code which follows a 'switch' without 'default', others 'continue' the loop
 */
public class SwitchBreakAndContinue {
    public static String escape(String name) {
        StringBuilder sb = new StringBuilder(name.length() * 2);
        boolean needsQuotes = false;
        for (int i = 0; i < name.length(); i++) {
            char c = name.charAt(i);
            switch (c) {
                case '\\':
                case '*':
                    sb.append('\\');
                    needsQuotes = true;
                    break;
                case ',':
                case '=':
                    needsQuotes = true;
                    break;
                case '\r':
                    continue;
                case '\n':
                    sb.append("\\n");
                    needsQuotes = true;
                    continue;
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

    /** the last case is no 'default' */
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
            }
            sum++;
        }
        return sum;
    }
}
