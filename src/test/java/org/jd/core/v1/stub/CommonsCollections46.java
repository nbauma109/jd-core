package org.jd.core.v1.stub;

import java.io.Serializable;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.HashMap;
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

    public static List<String> boundedParameterizedArgumentCast(Box<?> box) {
        return (List<String>) box.getBoundMap((Map<String, Integer>) genericObject("x"));
    }

    public static List<String> concreteParameterizedArgumentCast(Box<?> box) {
        return (List<String>) box.getMap((HashMap<String, Object>) genericObject("x"));
    }

    public static List<String> wildcardExtendsArgumentCast(Box<?> box) {
        return (List<String>) box.getExtends((Map<? extends CharSequence, Object>) genericObject("x"));
    }

    public static List<String> wildcardSuperArgumentCast(Box<?> box) {
        return (List<String>) box.getSuper((Map<? super String, Object>) genericObject("x"));
    }

    public static List<String> unboundedWildcardArgumentCast(Box<?> box) {
        return (List<String>) box.getAny((Map<?, Object>) genericObject("x"));
    }

    public static <T> List<String> ownerParameterizedArgumentCast(Box<T> box) {
        return (List<String>) box.getOwnerMap((Map<T, Object>) genericObject("x"));
    }

    public static List<String> soleArgumentCast(Box<?> box) {
        return (List<String>) box.getText((String) sole());
    }

    public static List<String> narrowingParameterizedArgumentCast(Box<?> box) {
        return (List<String>) box.getArrayList((ArrayList<String>) (Object) soleList());
    }

    public static List<String> concreteFunctionalReturn() {
        return (List<String>) identity(generic(() -> "x"));
    }

    public static List<String> lambdaResultConstraint(Box<?> box) {
        return (List<String>) box.get(clazz(() -> new Object()));
    }

    public static List<String> constructorReferenceConstraint(Box<?> box) {
        return (List<String>) box.get(clazz(Object::new));
    }

    public static List<String> methodReferenceConstraint(Box<?> box) {
        return (List<String>) box.get(clazz(CommonsCollections46::newObject));
    }

    public static List<String> directConstructorReferenceConstraint() {
        return (List<String>) (Object) supply(Object::new);
    }

    public static List<String> directMethodReferenceConstraint() {
        return (List<String>) (Object) supply(CommonsCollections46::newObject);
    }

    public static List<String> recursiveBoundParameterizedArgumentCast(Box<?> box) {
        return (List<String>) box.getRecursiveMap((Map<String, String>) genericObject("x"));
    }

    public static List<String> intersectionBoundParameterizedArgumentCast(Box<?> box) {
        return (List<String>) box.getIntersectionMap((Map<String, String>) genericObject("x"));
    }

    public static List<String> blockLambdaResultConstraint(Box<?> box) {
        return (List<String>) box.get(clazz(() -> {
            System.out.println("result");
            return new Object();
        }));
    }

    public static List<String> nestedBlockLambdaResultConstraint(Box<?> box, boolean flag) {
        return (List<String>) box.get(clazz(() -> {
            if (flag) {
                return new Object();
            }
            return new Object();
        }));
    }

    public static List<String> nullLambdaResult(Box<?> box) {
        return (List<String>) box.get(clazz(() -> null));
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

    private static <T> T sole() {
        return null;
    }

    private static <T> List<T> soleList() {
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

    private static <T> T supply(Supplier<T> supplier) {
        return supplier.get();
    }

    private static Class<Object> objectClass() {
        return Object.class;
    }

    private static Object newObject() {
        return new Object();
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

    public static void typeVariableArrayReceiver(Object value) {
        for (String string : ((ValueHolder<String[]>) value).values) {
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

        public <V, U extends Number> V getBoundMap(Map<String, U> value) {
            return null;
        }

        public <V, U> V getArrayList(ArrayList<U> value) {
            return null;
        }

        public <V, U extends Comparable<U>> V getRecursiveMap(Map<String, U> value) {
            return null;
        }

        public <V, U extends Serializable & Comparable<U>> V getIntersectionMap(Map<String, U> value) {
            return null;
        }

        public <V, U> V getExtends(Map<? extends CharSequence, U> value) {
            return null;
        }

        public <V, U> V getSuper(Map<? super String, U> value) {
            return null;
        }

        public <V, U> V getAny(Map<?, U> value) {
            return null;
        }

        public <V, U> V getOwnerMap(Map<T, U> value) {
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

    public static class ValueHolder<T> {
        private final T values;

        public ValueHolder(T values) {
            this.values = values;
        }
    }

    public static class Outer<T> {
        public class Inner {
            private final List<T> values = new LinkedList<>();
        }
    }
}
