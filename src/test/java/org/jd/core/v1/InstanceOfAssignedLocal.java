/*
 * Copyright (c) 2026 Nicolas Baumann (@nbauma109), with Claude Sonnet 5.5.
 * This project is distributed under the GPLv3 license.
 * This is a copyleft license that gives the user the right to use,
 * copy and modify the code freely for non-commercial purposes.
 */

package org.jd.core.v1;

/** A variable which a test assigns and which the code uses before or after it, or whose type is the one of the tested value: {@link InstanceOfAssignedLocalTest} */
public class InstanceOfAssignedLocal {
    String text = "t";
    Object field = "f";

    void consume(Object o) {
        // (only used by the test)
    }

    int earlier() {
        Object x = new Object();
        consume(x);
        if ((x = this.field) instanceof String) {
            String s = (String) x;
            consume(s);
        }
        return 0;
    }

    void unconditional() {
        Object x;
        if ((x = this.text) instanceof String) {
            String s = (String) x;
            consume(s);
        }
    }

    int after() {
        Object x;
        if ((x = this.field) instanceof String) {
            String s = (String) x;
            consume(s);
        }
        return x.hashCode();
    }

    java.util.List<String> strings = java.util.List.of("a");

    void generic() {
        Object x;
        if ((x = this.strings.get(0)) instanceof String) {
            String s = (String) x;
            consume(s);
        }
    }

    void constructor() {
        Object x;
        if ((x = new Object()) instanceof String) {
            String s = (String) x;
            consume(s);
        }
    }

    Object conditional(boolean flag) {
        Object x;
        if ((x = this.field) instanceof String) {
            String s = (String) x;
            consume(s);
        }
        if (flag) {
            x = new Object();
        }
        return x;
    }
}
