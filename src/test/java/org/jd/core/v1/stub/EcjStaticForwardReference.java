/*
 * Copyright (c) 2026 Nicolas Baumann (@nbauma109), with Claude Sonnet 5.5.
 * This project is distributed under the GPLv3 license.
 * This is a copyleft license that gives the user the right to use,
 * copy and modify the code freely for non-commercial purposes.
 */

package org.jd.core.v1.stub;

import java.util.ArrayList;
import java.util.List;

/**
 * 'Scanner.Goal' of the Eclipse compiler: static fields which a static block computes, which the initializers of other static fields, assigned after the block, read
 */
public class EcjStaticForwardReference extends EcjStaticForwardReferenceBase {
    final int[] own;

    static int total = 0;
    static int[] rules;

    static EcjStaticForwardReference goal;

    static int[] follow = { 1, 2 };

    static int length = inherited.length;

    static {
        List<Integer> values = new ArrayList<>();

        for (int i = 0; i < 3; i++) {
            values.add(i);
            total += i;
        }
        rules = values.stream().mapToInt(Integer::intValue).toArray();

        goal = new EcjStaticForwardReference(rules, follow);
    }

    EcjStaticForwardReference(int[] own, int[] follow) {
        this.own = own.length > follow.length ? own : follow;
    }
}

class EcjStaticForwardReferenceBase {
    static int[] inherited = { 3 };
}
