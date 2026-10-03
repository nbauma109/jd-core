package org.jd.core.v1.stub;

import java.util.Iterator;

/** An outer class whose inner classes are created by its subclasses. */
public abstract class GuavaOuter<K, V> {
    protected abstract V delegate();

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
