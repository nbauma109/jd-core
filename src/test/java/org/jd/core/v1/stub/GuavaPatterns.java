package org.jd.core.v1.stub;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collector;

/** Shapes which made Guava 33.7.2 decompile to code which did not print or did not recompile. */
@SuppressWarnings({"unchecked", "rawtypes"})
public class GuavaPatterns {
    int modCount;

    /** A class declared in an anonymous class writes a field of the anonymous class */
    public Iterator<String> anonymousWithInnerClass() {
        return new Iterator<String>() {
            int expectedModCount = modCount;
            String toRemove;

            class Entry {
                String node = "a";

                String setValue(String value) {
                    expectedModCount = modCount;
                    if (toRemove == node) {
                        toRemove = value;
                    }
                    return node;
                }
            }

            @Override
            public boolean hasNext() {
                return new Entry().setValue("b") != null;
            }

            @Override
            public String next() {
                return toRemove;
            }
        };
    }

    /** An anonymous class declared in an anonymous class calls a method of the outer one */
    public Iterable<String> anonymousInAnonymous() {
        return new Iterable<String>() {
            boolean compatible(Object value) {
                return value != null;
            }

            @Override
            public Iterator<String> iterator() {
                return new Iterator<String>() {
                    @Override
                    public boolean hasNext() {
                        return compatible("x") && compatible(this);
                    }

                    @Override
                    public String next() {
                        return "next";
                    }
                };
            }
        };
    }

    abstract static class Cut<C extends Comparable> implements Comparable<Cut<C>> {
        final C endpoint;

        Cut(C endpoint) {
            this.endpoint = endpoint;
        }

        static <C extends Comparable> Cut<C> belowAll() {
            return new Cut<C>(null) {
            };
        }

        static <C extends Comparable> Cut<C> belowValue(C endpoint) {
            return new Cut<C>(endpoint) {
            };
        }

        @Override
        public int compareTo(Cut<C> other) {
            return 0;
        }
    }

    static final class Bounds<C extends Comparable> {
        final Cut<C> lowerBound;
        final Cut<C> upperBound;

        private Bounds(Cut<C> lowerBound, Cut<C> upperBound) {
            this.lowerBound = lowerBound;
            this.upperBound = upperBound;
        }

        static <C extends Comparable<?>> Bounds<C> create(Cut<C> lowerBound, Cut<C> upperBound) {
            return new Bounds<C>(lowerBound, upperBound);
        }

        /** The type variable of a generic method which is bound to the bound of a type variable of the caller */
        public static <C extends Comparable<?>> Bounds<C> lessThan(C endpoint) {
            return create(Cut.belowAll(), Cut.belowValue(endpoint));
        }
    }

    /** A local class which creates instances of itself */
    public static <T> Iterator<T> localClassCreatingItself(T value) {
        class Local implements Iterator<T> {
            private final boolean last;

            Local(boolean last) {
                this.last = last;
            }

            Local trySplit() {
                return last ? null : new Local(true);
            }

            @Override
            public boolean hasNext() {
                return !last && value != null;
            }

            @Override
            public T next() {
                return value;
            }
        }
        return new Local(false).trySplit();
    }

    /** The empty varargs array of the characteristics follows a lambda with a block body */
    public static <T> Collector<T, ?, List<T>> blockLambdaThenEmptyVarargs(Supplier<List<T>> supplier) {
        return Collector.of(supplier, (list, item) -> list.add(item), (first, second) -> {
            first.addAll(second);
            return first;
        });
    }

    public static <T> Collector<T, ?, List<T>> expressionLambdaThenEmptyVarargs(Supplier<List<T>> supplier) {
        return Collector.of(supplier, (list, item) -> list.add(item), (first, second) -> first);
    }

    static <T, K> void merge(Map<K, T> map, K key, T value, BinaryOperator<T> mergeFunction) {
        map.merge(key, value, mergeFunction);
    }

    /** Both lambdas capture variables of the method */
    public static <T, K, M extends Map<K, T>> Collector<T, ?, M> capturingLambdas(Function<? super T, ? extends K> keyFunction,
            BinaryOperator<T> mergeFunction, Supplier<M> supplier) {
        return Collector.of(
                supplier,
                (map, input) ->
                        merge(
                                map,
                                keyFunction.apply(input),
                                input,
                                mergeFunction),
                (first, second) -> {
                    for (Map.Entry<K, T> entry : second.entrySet()) {
                        merge(first, entry.getKey(), entry.getValue(), mergeFunction);
                    }
                    return first;
                });
    }

    abstract static class Indexed<T> implements Iterator<T> {
        final Iterator<T> from;
        long index;

        Indexed(Iterator<T> from, long index) {
            this.from = from;
            this.index = index;
        }

        @Override
        public boolean hasNext() {
            return from.hasNext();
        }
    }

    /** A local class which captures a variable and whose constructor takes a long */
    public static Iterator<String> localClassWithLongParameter(Iterator<String> source, Function<String, String> function) {
        class Splitr extends Indexed<String> {
            Splitr(Iterator<String> from, long index) {
                super(from, index);
            }

            @Override
            public String next() {
                return function.apply(from.next()) + index++;
            }
        }
        return new Splitr(source, 0L);
    }

    /** The generic signature of the constructor of an inner class leaves out the outer instance */
    class GenericInner<X> {
        final List<X> items;

        GenericInner(List<X> items) {
            this.items = items;
        }

        int size() {
            return items.size() + modCount;
        }
    }

    public int genericInner(String first) {
        List<String> items = new ArrayList<>();
        items.add(first);
        return new GenericInner<>(items).size();
    }

    static class NumberBounded<X extends Number> {
        NumberBounded(Number number) {
        }
    }

    /** The type argument of the created class is known: the constructor parameter is a Number, not a X */
    public static <U extends Number> NumberBounded<Integer> knownTypeArgument(U number) {
        return new NumberBounded<Integer>(number) {
        };
    }

    /** An anonymous class with a method which only looks like an override: no diamond */
    public static ArrayList<String> overloadInAnonymousClass() {
        return new ArrayList<String>() {
            public boolean add(Integer number) {
                return add(String.valueOf(number));
            }
        };
    }

    interface Element<E> {
        E self();
    }

    static class Matrix<T extends Element<T>> {
        Matrix(T[][] data, boolean copy) {
        }
    }

    static class Decomposition<T extends Element<T>> {
        Decomposition(Matrix<T> matrix) {
        }
    }

    /** The type argument of an argument created from an array of the type variable, which is bounded by itself */
    public static <T extends Element<T>> Decomposition<T> decompose(T[][] data) {
        return new Decomposition<T>(new Matrix<T>(data, false));
    }

    static <T> T first(List<? extends List<? extends T>> values) {
        return values.get(0).get(0);
    }

    /** The wildcard which faces the type variable is nested: the cast is needed to get a String */
    public static String firstOfWildcards(List<List<?>> values) {
        return (String) first(values);
    }

    static class RawBox<T> {
        T content;

        T get() {
            return content;
        }
    }

    static RawBox rawBox() {
        return new RawBox();
    }

    /** What a raw receiver which is a method call returns is erased */
    public static String fromRawFactory() {
        return (String) rawBox().get();
    }

    abstract static class Getter<T> {
        abstract T get(T value);
    }

    /** The bridge method get(Object) belongs to get(String): the other get is a new method, there is no diamond */
    public static Getter<String> getterWithAnotherOverload() {
        return new Getter<String>() {
            @Override
            String get(String value) {
                return value;
            }

            String get(Integer value) {
                return String.valueOf(value);
            }
        };
    }

    static class Box<T> {
        final T content;

        Box(T content) {
            this.content = content;
        }
    }

    static <T> Box<List<T>> wrap(List<T> values) {
        return new Box<List<T>>(values);
    }

    /** The type variable which is captured is nested in the returned type */
    public static Box<List<String>> wrapped(List<?> values) {
        return (Box<List<String>>) (Box<?>) wrap(values);
    }

    interface Adapter2<T> {
        T get();
    }

    static class Comparing<X extends Comparable<String>> implements Adapter2<X> {
        Comparing(X value) {
        }

        @Override
        public X get() {
            return null;
        }
    }

    /** The bounds Comparable<Integer> and Comparable<String> have the same raw type */
    public static <U extends Comparable<Integer>> Adapter2<U> comparing(U value) {
        return (Adapter2<U>) (Adapter2) new Comparing(value);
    }

    static boolean isOk() {
        return modCounter > 0;
    }

    static boolean await(boolean reentrant) throws InterruptedException {
        return reentrant;
    }

    static void signal() {
        modCounter++;
    }

    static int modCounter;

    /** A finally block which contains a try and a finally, and which reads the variables set in the try block */
    public static boolean nestedFinally(ReentrantLock lock, boolean reentrant) throws InterruptedException {
        boolean satisfied = false;
        boolean threw = true;
        try {
            satisfied = isOk() || await(reentrant);
            threw = false;
            return satisfied;
        } finally {
            if (!satisfied) {
                try {
                    if (threw && !reentrant) {
                        signal();
                    }
                } finally {
                    lock.unlock();
                }
            }
        }
    }

    static boolean satisfiedNow() {
        return modCounter < 0;
    }

    static boolean awaitNanos(long nanos, boolean reentrant) {
        return reentrant && nanos > 0;
    }

    static long remainingNanos(long start, long timeout) {
        return timeout - start;
    }

    /** The second operand of the '||' has a ternary operator as an argument, and the result is stored before it is returned */
    public static boolean orWithTernaryArgument(long start, long timeout, boolean reentrant) {
        boolean satisfied = satisfiedNow() || awaitNanos((start == 0L) ? timeout : remainingNanos(start, timeout), reentrant);
        return satisfied;
    }

    final boolean fair = modCounter > 5;
    final ReentrantLock lock = new ReentrantLock();

    /** A labeled block left by a 'break', then a try with a finally which contains a try and a finally (Guava's Monitor.enterWhen) */
    public boolean labeledBlockThenNestedFinally(long time, TimeUnit unit) throws InterruptedException {
        final long timeoutNanos = unit.toNanos(time);
        final ReentrantLock lock = this.lock;
        boolean reentrant = lock.isHeldByCurrentThread();
        long startTime = 0L;

        locked:
        {
            if (!fair) {
                if (Thread.interrupted()) {
                    throw new InterruptedException();
                }
                if (lock.tryLock()) {
                    break locked;
                }
            }
            startTime = System.nanoTime();
            if (!lock.tryLock(time, unit)) {
                return false;
            }
        }

        boolean satisfied = false;
        boolean threw = true;
        try {
            satisfied = satisfiedNow() || awaitNanos((startTime == 0L) ? timeoutNanos : remainingNanos(startTime, timeoutNanos), reentrant);
            threw = false;
            return satisfied;
        } finally {
            if (!satisfied) {
                try {
                    if (threw && !reentrant) {
                        signal();
                    }
                } finally {
                    lock.unlock();
                }
            }
        }
    }

    static GuavaPatterns of(Type[] bounds) {
        return new GuavaPatterns();
    }

    boolean isSubtypeOf(Type type) {
        return type != null;
    }

    final Type runtimeType = null;

    /** The cast is the receiver of a call whose result is the argument of another call: it must not be typed as the parameter (an array) */
    public boolean castReceiverInArgument(Type supertype) {
        return runtimeType.equals(supertype) || of(((TypeVariable<?>) runtimeType).getBounds()).isSubtypeOf(supertype);
    }

    static class Monitor2 {
        abstract static class Guard {
            final Monitor2 monitor;

            Guard(Monitor2 monitor) {
                this.monitor = monitor;
            }

            abstract boolean isSatisfied();
        }
    }

    final Monitor2 monitor2 = new Monitor2();

    /** The first parameter of the constructor of the static nested class is of the type of its outer class: it is not an outer instance */
    final class IsGuard extends Monitor2.Guard {
        IsGuard() {
            super(monitor2);
        }

        @Override
        boolean isSatisfied() {
            return modCount == 0;
        }
    }

    abstract static class Ord<T> {
        @SuppressWarnings("rawtypes")
        static <C extends Comparable> Ord<C> natural() {
            return null;
        }
    }

    /** E is not a Comparable: the type variable of 'natural' cannot be inferred from the type of the variable */
    public static <E> Ord<E> naturalOrder(Iterable<? extends E> elements) {
        Ord<E> naturalOrder = (Ord<E>) Ord.natural();
        return naturalOrder;
    }

    static <E> List<E> copyOf(java.util.Comparator<? super E> comparator, Iterable<? extends E> elements) {
        return null;
    }

    /** The array of the arguments of a varargs call, of a type variable which has a bound, is not cast */
    public static <E extends Comparable<? super E>> List<E> twoOf(E first, E second) {
        return copyOf(null, Arrays.asList(first, second));
    }

    /** An anonymous class in a static generic method does not see the type variable of the class: nor does the argument of requireNonNull */
    public static <K, V> Comparator<K> byValue(final Map<K, V> map, final Comparator<? super V> valueComparator) {
        return new Comparator<K>() {
            @Override
            public int compare(K left, K right) {
                return valueComparator.compare(Objects.requireNonNull(map.get(left)), Objects.requireNonNull(map.get(right)));
            }
        };
    }

    static final class ArrayItr<E> {
        /** The type variable of the class is not visible to its static fields */
        static final ArrayItr<Object> EMPTY = new ArrayItr<>(new Object[0], 0);
        final E[] array;

        ArrayItr(E[] array, int position) {
            this.array = array;
        }
    }

    /** A primitive cannot be cast to a type variable: its box is */
    @SuppressWarnings("unchecked")
    public static <T> T defaultValue(Class<T> type) {
        if (type == char.class) {
            return (T) Character.valueOf('\0');
        }
        if (type == int.class) {
            return (T) Integer.valueOf(0);
        }
        return null;
    }

    /** The counter of the first loop is still used by the second one, which does not initialize it */
    public static int counterSharedByTwoLoops(byte[] input, int off, int len) {
        int h1 = 1;
        int i;
        for (i = 0; i + 4 <= len; i += 4) {
            h1 = h1 * 31 + input[off + i];
        }
        int k1 = 0;
        for (int shift = 0; i < len; i++, shift += 8) {
            k1 ^= input[off + i] << shift;
        }
        return h1 ^ k1;
    }

    public static class GraphBuilder<N, V> {
        static <N, V> GraphBuilder<N, V> from(java.util.Map<N, V> graph) {
            return new GraphBuilder<>();
        }

        GraphBuilder<N, V> expectedNodeCount(int count) {
            return this;
        }

        <N1 extends N, V1 extends V> java.util.Map<N1, V1> build() {
            return new java.util.HashMap<>();
        }
    }

    /** The variable is declared with the type of a ternary of generic calls whose own type variables are not bound */
    public static <N, V> java.util.Map<N, V> inducedSubgraph(java.util.Map<N, V> graph, Iterable<? extends N> nodes) {
        java.util.Map<N, V> subgraph = (nodes instanceof java.util.Collection)
                ? GraphBuilder.from(graph).expectedNodeCount(((java.util.Collection<?>) nodes).size()).build()
                : GraphBuilder.from(graph).build();
        for (N node : nodes) {
            subgraph.put(node, null);
        }
        return subgraph;
    }

    public abstract static class FactorySet<E> extends java.util.AbstractSet<E> {
        static <E> FactorySet<E> of(E element) {
            return null;
        }

        static <E> FactorySet<E> of(E element1, E element2) {
            return null;
        }
    }

    /** 'Set.of(E)' and 'Set.of(E...)' are not inherited by the static factories of a set */
    public static FactorySet<String> factoryOfOne(String value) {
        return FactorySet.of(value);
    }
}
