package org.jd.core.v1.stub;

import java.util.AbstractMap.SimpleEntry;
import java.util.List;
import java.util.Map.Entry;

@SuppressWarnings("all")
public class MultiWitness {

    static <K, V> Entry<K, V> pair(K k, V v) {
        return new SimpleEntry<>(k, v);
    }

    public static Entry<String, Integer> strings() {
        return MultiWitness.<String, Integer>pair(null, null);
    }

    public static Entry<List, Integer> raw() {
        return MultiWitness.<List, Integer>pair(null, null);
    }

    public static int rawQualifier() {
        return MultiWitness.<List, Integer>pair(null, null).getKey().size();
    }
}
