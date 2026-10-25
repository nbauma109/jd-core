/*
 * Copyright (c) 2026 Nicolas Baumann (@nbauma109), with Claude Sonnet 5.5.
 * This project is distributed under the GPLv3 license.
 * This is a Copyleft license that gives the user the right to use,
 * copy and modify the code freely for non-commercial purposes.
 */

package org.jd.core.v1.stub;

import java.util.Iterator;

/** Creates inner classes of its superclass: the outer instance is implicit. */
public class GuavaSub<K, V> extends GuavaOuter<K, V> {
    @Override
    protected V delegate() {
        return null;
    }

    Wrapped wrap(K key, V value) {
        return new Wrapped(key, value);
    }

    Iterator<V> walk(K key, V value) {
        return new Special(key, value).walk();
    }

    Object marker() {
        return new Marker();
    }
}
