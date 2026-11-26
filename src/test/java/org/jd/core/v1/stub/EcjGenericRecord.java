/*
 * Copyright (c) 2026 Nicolas Baumann (@nbauma109), with Claude Sonnet 5.5.
 * This project is distributed under the GPLv3 license.
 * This is a copyleft license that gives the user the right to use,
 * copy and modify the code freely for non-commercial purposes.
 */

package org.jd.core.v1.stub;

import java.util.List;
import java.util.Map;

/**
 * 'Tuples.Pair' of the Eclipse compiler: a generic record, whose components are read with the type argument
 */
public class EcjGenericRecord {
    record Pair<T extends CharSequence>(T left, T right) {
    }

    record Holder<K, V>(Map<K, V> values, List<? extends K>[] keys, K[] array) {
    }

    int length(List<Pair<String>> pairs) {
        int total = 0;

        for (Pair<String> pair : pairs) {
            total += pair.left().trim().length() + pair.right().length();
        }
        return total;
    }
}
