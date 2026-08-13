package org.jd.core.v1.stub;

import java.util.AbstractMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@SuppressWarnings({ "rawtypes", "unchecked", "java:S1172" })
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

    public static List<String> constrainedReturn(Box<?> box) {
        return (List<String>) box.get(Object.class);
    }

    public static List<String> constrainedByInvocation(Box<?> box) {
        return (List<String>) box.get(objectClass());
    }

    public static List<String> constrainedByGenericInvocation(Box<?> box) {
        return (List<String>) box.get(identity(Object.class));
    }

    public static List<String> constrainedByGenericInvocationList(Box<?> box) {
        return (List<String>) box.getWithFlag((Class<Object>) identity(), true);
    }

    public static List<String> concreteGenericReturn(Box<?> box) {
        return (List<String>) box.get((Class<Object>) genericObject("x"));
    }

    public static List<String> mixedGenericInvocation(Box<?> box) {
        return (List<String>) box.get(choose(Object.class, () -> {}));
    }

    public static List<String> boundedArgumentCast(Box<?> box) {
        return (List<String>) box.getText((CharSequence) genericObject("x"));
    }

    public static List<String> independentlyConstrainedArgument(Box<?> box) {
        return (List<String>) box.getText((String) identity(new Object()));
    }

    public static List<String> parameterizedArgumentCast(Box<?> box) {
        return (List<String>) box.getMap((Map<String, Object>) genericObject("x"));
    }

    public static List<String> concreteFunctionalReturn() {
        return (List<String>) identity(generic(() -> "x"));
    }

    public static List<String> lambdaResultConstraint(Box<?> box) {
        return (List<String>) box.get(clazz(() -> new Object()));
    }

    public static List<String> overloadedArgumentCast() {
        return (List<String>) use((String) identity());
    }

    private static <T> T identity(T value) {
        return value;
    }

    private static <T> T identity() {
        return null;
    }

    public static <T, U extends CharSequence> T use(U value) {
        return null;
    }

    public static <T, U extends Number> T use(U value) {
        return null;
    }

    private static <V> Object genericObject(V value) {
        return value;
    }

    private static <T> Class<T> choose(Class<T> type, Runnable action) {
        action.run();
        return type;
    }

    private static <V> Object generic(Supplier<V> supplier) {
        return supplier.get();
    }

    private static <T> Class<T> clazz(Supplier<T> supplier) {
        supplier.get();
        return null;
    }

    private static Class<Object> objectClass() {
        return Object.class;
    }

    public static List<String> objectReturn(Box<?> box) {
        return (List<String>) box.getObject(Object.class);
    }

    public static List<String> classOwnedReturn(Box<?> box) {
        return (List<String>) box.getOwned(Object.class);
    }

    public static List<String> transitivelyConstrainedReturn(Box<?> box) {
        return (List<String>) box.getBound(new Object());
    }

    public static void nonGenericReceiver(Object value) {
        for (CharSequence string : ((Holder) value).strings) {
            string.length();
        }
    }

    public static void primitiveReceiver(Object value) {
        for (int number : ((PrimitiveHolder<Integer>) value).values) {
            System.out.println(number);
        }
    }

    public static void enclosingReceiver(Object value) {
        for (String string : ((Outer<String>.Inner) value).values) {
            System.out.println(string);
        }
    }

    public static void arrayReceiver(Object value) {
        for (String string : ((ArrayHolder<String>) value).values) {
            System.out.println(string);
        }
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

    public static class Box<T> {
        public <U> U get(Class<U> type) {
            return null;
        }

        public <U> U getWithFlag(Class<U> type, boolean flag) {
            return null;
        }

        public <U> Object getObject(Class<U> type) {
            return null;
        }

        public <U> T getOwned(Class<U> type) {
            return null;
        }

        public <T, U extends T> T getBound(U value) {
            return null;
        }

        public <V, U extends CharSequence> V getText(U value) {
            return null;
        }

        public <V, U> V getMap(Map<String, U> value) {
            return null;
        }
    }

    public static class Holder {
        private final List<String> strings = new LinkedList<>();
    }

    public static class PrimitiveHolder<T> {
        private final List<Integer> values = new LinkedList<>();
    }

    public static class ArrayHolder<T> {
        private final T[] values = (T[]) new Object[0];
    }

    public static class Outer<T> {
        public class Inner {
            private final List<T> values = new LinkedList<>();
        }
    }
}
