/*
 * Copyright (c) 2026 Nicolas Baumann (@nbauma109), with Claude Sonnet 5.5.
 * This project is distributed under the GPLv3 license.
 * This is a copyleft license that gives the user the right to use,
 * copy and modify the code freely for non-commercial purposes.
 */

package org.jd.core.v1.stub;

import java.util.List;
import java.util.function.Supplier;

/**
 * 'SwitchExpression.resolveType' of the Eclipse compiler: a rule of a switch expression yields from inside a loop
 */
public class EcjYieldInLoop {
    static int pick(int id, List<Integer> items, int base) {
        return switch (id) {
            case 1, 2 -> base;
            case 3 -> {
                if (base > 0) {
                    if (id == 3 && items.isEmpty()) {
                        yield 7;
                    }
                    for (Integer item : items) {
                        if (item > base) {
                            yield 8;
                        }
                    }
                    Supplier<Integer> supplier = () -> {
                        return base + 1;
                    };
                    Object marker = new Object() {
                        @Override
                        public String toString() {
                            return "marker";
                        }
                    };
                    yield supplier.get() + marker.toString().length();
                }
                yield 0;
            }
            default -> throw new IllegalStateException();
        };
    }

    static String name(int id, List<String> items) {
        return switch (id) {
            case 1 -> {
                for (String item : items) {
                    if (item.isEmpty()) {
                        yield "empty";
                    }
                }
                yield "none";
            }
            default -> "other";
        };
    }
}
