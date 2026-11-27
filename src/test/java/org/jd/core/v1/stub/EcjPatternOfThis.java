/*
 * Copyright (c) 2026 Nicolas Baumann (@nbauma109), with Claude Sonnet 5.5.
 * This project is distributed under the GPLv3 license.
 * This is a copyleft license that gives the user the right to use,
 * copy and modify the code freely for non-commercial purposes.
 */

package org.jd.core.v1.stub;

/**
 * 'Statement.enclosingSwitchExpression' of the Eclipse compiler: 'this' is tested by a pattern
 */
public class EcjPatternOfThis {
    static class Yield extends EcjPatternOfThis {
        boolean implicit;
    }

    EcjPatternOfThis parent;

    EcjPatternOfThis valueOnly(EcjPatternOfThis current) {
        boolean flag = this instanceof Yield yield1 && yield1.implicit;
        return flag ? current : null;
    }

    EcjPatternOfThis beforeALoop(EcjPatternOfThis current) {
        boolean flag = this instanceof Yield yield2 && yield2.implicit;
        do {
            if (flag) {
                return current;
            }
        } while ((current = current.parent) != null);
        return null;
    }

    /** an element of an array is kept in a temporary in a static method too */
    static EcjPatternOfThis first(EcjPatternOfThis[] values) {
        return values[0] instanceof Yield yield3 && yield3.implicit ? yield3 : null;
    }
}
