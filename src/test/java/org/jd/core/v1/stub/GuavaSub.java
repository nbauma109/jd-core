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
}
