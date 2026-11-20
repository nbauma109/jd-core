/*
 * Copyright (c) 2026 Nicolas Baumann (@nbauma109), with Claude Sonnet 5.5.
 * This project is distributed under the GPLv3 license.
 * This is a copyleft license that gives the user the right to use,
 * copy and modify the code freely for non-commercial purposes.
 */

package org.jd.core.v1;

import java.util.function.Function;

/** 'Scope.filterValidTypes(types, ReferenceBinding[]::new)' of the Eclipse compiler */
public class ArrayConstructorReferenceArgument {

    static class Binding {}

    static class Reference extends Binding {}

    static <T extends Binding> T[] filter(T[] all, Function<Integer, T[]> creator) {
        return creator.apply(all.length);
    }

    static <T> T[] copy(T[] all, Function<Integer, T[]> creator) {
        return creator.apply(all.length);
    }

    static Reference[] references(Reference[] types) {
        return filter(types, Reference[]::new);
    }

    static Binding[] bindings(Binding[] types) {
        return filter(types, Binding[]::new);
    }

    static String[] strings(String[] values) {
        return copy(values, String[]::new);
    }
}
