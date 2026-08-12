package org.jd.core.v1.stub;

import java.util.AbstractMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@SuppressWarnings({ "rawtypes", "unchecked" })
public class CommonsCollections46<E> implements Iterator<E> {
    private final Queue<Iterator<? extends E>> iteratorQueue = new LinkedList<>();

    public void addIterator(Iterator<? extends E> iterator) {
        if (iterator instanceof UnmodifiableIterator) {
            Iterator<? extends E> underlyingIterator = ((UnmodifiableIterator) iterator).unwrap();
            if (underlyingIterator instanceof CommonsCollections46) {
                for (Iterator<? extends E> nestedIterator :
                        ((CommonsCollections46<? extends E>) underlyingIterator).iteratorQueue) {
                    iteratorQueue.add(nestedIterator);
                }
            }
        }
    }

    public static Set<Map.Entry<Object, Object>> entries(Stream<Object> keys) {
        return keys.map(key -> new AbstractMap.SimpleEntry<>(key, key))
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    @Override
    public boolean hasNext() {
        return false;
    }

    @Override
    public E next() {
        return null;
    }

    private abstract static class UnmodifiableIterator<E> implements Iterator<E> {
        abstract Iterator<E> unwrap();
    }
}
