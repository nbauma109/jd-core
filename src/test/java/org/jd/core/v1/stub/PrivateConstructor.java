/*
 * Copyright (c) 2026 Nicolas Baumann (@nbauma109), with Claude Sonnet 5.5.
 * This project is distributed under the GPLv3 license.
 * This is a copyleft license that gives the user the right to use,
 * copy and modify the code freely for non-commercial purposes.
 */

package org.jd.core.v1.stub;

/** A utility class: its explicit private constructor is not the default constructor of a public class. */
public final class PrivateConstructor {

    private PrivateConstructor() {
    }

    public static int answer() {
        return 42;
    }

    /** The default constructor of a private class is private: it is not printed. */
    private static class PrivateNested {
    }

    public static Object create() {
        return new PrivateNested();
    }
}
