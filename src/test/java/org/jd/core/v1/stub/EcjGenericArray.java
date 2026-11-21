/*
 * Copyright (c) 2026 Nicolas Baumann (@nbauma109), with Claude Sonnet 5.5.
 * This project is distributed under the GPLv3 license.
 * This is a copyleft license that gives the user the right to use,
 * copy and modify the code freely for non-commercial purposes.
 */

package org.jd.core.v1.stub;

import java.util.Arrays;
import java.util.function.Function;

/**
 * 'Scope.filterValidTypes' of the Eclipse compiler: the array of a type variable is typed by the signature, not by its erasure
 */
public class EcjGenericArray {
    static <T extends CharSequence> T[] filterValid(long first, T[] all, double second, Function<Integer, T[]> ctor) {
        T[] valid = ctor.apply(all.length + (int) (first + second));
        int count = 0;
        for (T type : all) {
            if (!type.isEmpty()) {
                valid[count++] = type;
            }
        }
        if (count == all.length) {
            return all;
        }
        return Arrays.copyOf(valid, count);
    }
}
