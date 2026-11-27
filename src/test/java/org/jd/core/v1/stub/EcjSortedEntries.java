/*
 * Copyright (c) 2026 Nicolas Baumann (@nbauma109), with Claude Sonnet 5.5.
 * This project is distributed under the GPLv3 license.
 * This is a copyleft license that gives the user the right to use,
 * copy and modify the code freely for non-commercial purposes.
 */

package org.jd.core.v1.stub;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 'Main.Logger.logOptions' of the Eclipse compiler: an array of a generic type, which the byte code only knows as raw, sorted with a comparator of the generic type
 */
public class EcjSortedEntries {
    @SuppressWarnings("unchecked")
    Object[] anonymous(Map<String, String> options) {
        final Set<Map.Entry<String, String>> entriesSet = options.entrySet();
        Map.Entry<String, String>[] entries = entriesSet.toArray(new Map.Entry[entriesSet.size()]);

        Arrays.sort(entries, new Comparator<Map.Entry<String, String>>() {
            @Override
            public int compare(Map.Entry<String, String> first, Map.Entry<String, String> second) {
                return first.getKey().compareTo(second.getKey());
            }
        });
        return entries;
    }

    /** the arrays which need no cast */
    void others(String[] names, Map.Entry<String, String>[] typed, Object[] objects, Map.Entry[] raw, int[] numbers, Comparator<String> byName,
            Comparator<Map.Entry<String, String>> byKey, Comparator<Object> byObject, Comparator untyped) {
        Arrays.sort(numbers);
        Arrays.sort(names, byName);
        Arrays.sort(names, 0, 1, byName);
        Arrays.sort(typed, byKey);
        Arrays.sort(objects, byObject);
        Arrays.sort(raw, untyped);
        Arrays.sort(names, String.CASE_INSENSITIVE_ORDER);
        Arrays.sort(names, null);
    }

    /** a generic array, and a comparator which is a type variable */
    <T, C extends Comparator<String>> void generic(T[] values, Comparator<T> byValue, String[] names, C byName, List<String>[] lists, Comparator<List<String>> bySize,
            Comparator<Comparable<String>> naturally) {
        Arrays.sort(values, byValue);
        Arrays.sort(names, byName);
        Arrays.sort(lists, bySize);
        Arrays.sort(names, naturally);
    }

    /** the type argument of the class of this comparator is not the type which it compares */
    static class ByText<T> implements Comparator<Map.Entry<String, String>> {
        T context;

        @Override
        public int compare(Map.Entry<String, String> first, Map.Entry<String, String> second) {
            return first.getKey().compareTo(second.getKey());
        }
    }

    void custom(Map.Entry<String, String>[] entries, ByText<Map.Entry<Integer, Integer>> byText) {
        Arrays.sort(entries, byText);
    }

    static Map.Entry<String, String>[] shared;

    static final Comparator<Map.Entry<?, ?>> BY_HASH = null;

    static ByText<Map.Entry<Integer, Integer>> byText;

    /** the generic signature of the field gives the type arguments of the array: no cast */
    void shared() {
        Arrays.sort(shared, BY_HASH);
        Arrays.sort(shared, byText);
    }

    /** a raw array does not take a comparator of the wildcard type */
    void wildcards(Map.Entry<?, ?>[] anyEntries, Comparator<Map.Entry<?, ?>> byAnyKey) {
        Arrays.sort(anyEntries, byAnyKey);
    }

    /** a bounded wildcard is not what the raw type is: the cast is needed */
    void bounded(Map.Entry<String, Integer>[] counts, Comparator<Map.Entry<String, ? extends Number>> byNumber) {
        Arrays.sort(counts, byNumber);
    }

    /** comparators which are created where they sort */
    void created(Map.Entry<?, ?>[] anyEntries, Map.Entry<String, String>[] entries) {
        Arrays.sort(anyEntries, new Comparator<Map.Entry<?, ?>>() {
            @Override
            public int compare(Map.Entry<?, ?> first, Map.Entry<?, ?> second) {
                return Integer.compare(first.hashCode(), second.hashCode());
            }
        });
        Arrays.sort(entries, new ByText<Map.Entry<Integer, Integer>>());
    }
}
