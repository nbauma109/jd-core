/*
 * Copyright (c) 2026 Nicolas Baumann (@nbauma109), with Claude Sonnet 5.5.
 * This project is distributed under the GPLv3 license.
 * This is a copyleft license that gives the user the right to use,
 * copy and modify the code freely for non-commercial purposes.
 */

package org.jd.core.v1;

/** The rules of a switch expression which are ternary operators, decompiled and recompiled by {@link SwitchExpressionTernaryRuleTest} */
public class SwitchExpressionTernaryRule {

    boolean primitive;

    char[] signature(int id, String name) {
        char[] argument = switch (id) {
            case 26, 27, 28 ->
                this.primitive
                ? name.toCharArray()
                : "Ljava/lang/Object;".toCharArray();
            default -> {
                if (id > 128 && name.length() > 3)
                    yield name.trim().toCharArray();
                else
                    yield name.isEmpty() || name.isBlank()
                        ? name.toCharArray()
                        : "Ljava/lang/Object;".toCharArray();
            }
        };
        return new String(argument).concat("I)I").toCharArray();
    }
}
