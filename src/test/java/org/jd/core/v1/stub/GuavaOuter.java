/*
 * Copyright (c) 2026 Nicolas Baumann (@nbauma109), with Claude Sonnet 5.5.
 * This project is distributed under the GPLv3 license.
 * This is a Copyleft license that gives the user the right to use,
 * copy and modify the code freely for non-commercial purposes.
 */

package org.jd.core.v1.stub;

import java.util.Iterator;

/** An outer class whose inner classes are created by its subclasses. */
public abstract class GuavaOuter<K, V> {
    protected abstract V delegate();

    public static void shared(String text) {
    }

    int hidden(String text) {
        return text.length();
    }

    class Wrapped {
        final K key;
        final V value;

        Wrapped(K key, V value) {
            this.key = key;
            this.value = value;
        }

        V get() {
            return delegate();
        }

        class Walker implements Iterator<V> {
            @Override
            public boolean hasNext() {
                return delegate() != null;
            }

            @Override
            public V next() {
                return get();
            }
        }
    }

    class Marker {
    }

    class Special extends Wrapped {
        Special(K key, V value) {
            super(key, value);
        }

        Iterator<V> walk() {
            return new Walker();
        }
    }
}
