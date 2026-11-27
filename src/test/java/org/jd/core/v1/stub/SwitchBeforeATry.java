/*
 * Copyright (c) 2026 Nicolas Baumann (@nbauma109), with Claude Sonnet 5.5.
 * This project is distributed under the GPLv3 license.
 * This is a copyleft license that gives the user the right to use,
 * copy and modify the code freely for non-commercial purposes.
 */

package org.jd.core.v1.stub;

/**
 * 'Log4jProvider.createThreadContextMap' of Log4j: the cases of a 'switch' return, and its 'default' goes on with a 'try' or a loop
 */
public class SwitchBeforeATry {
    static String load(String name) throws ReflectiveOperationException {
        return Class.forName(name).getName();
    }

    static String ofString(String name) {
        switch (name) {
            case "NoOp":
                return "none";
            case "WebApp":
                return "web";
            default:
                try {
                    return load(name);
                } catch (ReflectiveOperationException e) {
                    System.out.println("catch");
                }
        }
        return "fallback";
    }

    static String ofInt(int value) {
        switch (value) {
            case 1:
                return "one";
            case 2:
                return "two";
            default:
                try {
                    return load("x");
                } catch (ReflectiveOperationException e) {
                    System.out.println("catch");
                }
        }
        return "fallback";
    }

    static int beforeALoop(int value, int[] values) {
        switch (value) {
            case 1:
                return -1;
            case 2:
                return -2;
            default:
                for (int i = 0; i < values.length; i++) {
                    value += values[i];
                }
        }
        return value;
    }
}
