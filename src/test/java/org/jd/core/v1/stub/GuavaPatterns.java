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

    public static class OrderedBuilder<N, E> {
        java.util.List<N> nodeOrder;

        @SuppressWarnings("unchecked")
        private <N1 extends N, E1 extends E> OrderedBuilder<N1, E1> cast() {
            return (OrderedBuilder<N1, E1>) this;
        }

        /** The field of another builder is declared with the type variables of its class, which that builder binds differently */
        public <N1 extends N> OrderedBuilder<N1, E> nodeOrder(java.util.List<N1> order) {
            OrderedBuilder<N1, E> newBuilder = cast();
            newBuilder.nodeOrder = java.util.Objects.requireNonNull(order);
            return newBuilder;
        }
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    static <C extends Comparable> java.util.Comparator<C> naturalComparator() {
        return (java.util.Comparator<C>) java.util.Comparator.naturalOrder();
    }

    /** A bounded type variable of a generic method is inferred from its bound, never from the wildcard target type */
    @SuppressWarnings({"rawtypes", "unchecked"})
    public static <E> java.util.Comparator<E> comparatorOf(java.util.SortedSet<E> set) {
        java.util.Comparator<? super E> result = set.comparator();
        if (result == null) {
            result = (java.util.Comparator<? super E>) naturalComparator();
        }
        return (java.util.Comparator<E>) result;
    }

    /** The local class captures a variable of the loop: it is declared in the block of its only use */
    public static Object localClassInLoop(java.util.Iterator<java.util.Map.Entry<String, String>> iterator) {
        while (iterator.hasNext()) {
            final java.util.Map.Entry<String, String> entry = iterator.next();
            if (entry.getValue() != null) {
                final class EntryView {
                    String key() {
                        return entry.getKey();
                    }
                }
                return new EntryView();
            }
        }
        return null;
    }

    public abstract static class Wrapper<V> {
        abstract class Coll extends java.util.AbstractCollection<V> {
            java.util.List<V> delegate() {
                return null;
            }

            class It implements java.util.Iterator<V> {
                final java.util.Iterator<V> iterator;

                It() {
                    this.iterator = delegate().iterator();
                }

                It(java.util.Iterator<V> iterator) {
                    this.iterator = iterator;
                }

                public boolean hasNext() {
                    return iterator.hasNext();
                }

                public V next() {
                    return iterator.next();
                }
            }
        }

        class ListColl extends Coll {
            java.util.List<V> listDelegate() {
                return null;
            }

            public java.util.Iterator<V> iterator() {
                return new ListIt();
            }

            public int size() {
                return 0;
            }

            /** The outer instance is stored in 'this$1' and the constructor passes it on to the inner superclass */
            private final class ListIt extends Coll.It {
                ListIt() {
                }

                ListIt(int index) {
                    super(listDelegate().listIterator(index));
                }
            }
        }
    }

    interface Terminator {
        void exit(int status);
    }

    static final class Exiter implements Thread.UncaughtExceptionHandler {
        private final Terminator terminator;

        Exiter(Terminator terminator) {
            this.terminator = terminator;
        }

        @Override
        public void uncaughtException(Thread thread, Throwable exception) {
            terminator.exit(1);
        }
    }

    /** The receiver of the bound method reference is no outer instance of the static nested class */
    public static Thread.UncaughtExceptionHandler systemExit() {
        return new Exiter(Runtime.getRuntime()::exit);
    }

    static final class Bound<C> {
    }

    static final class Interval<C> {
        final Bound<C> lowerBound = null;
    }

    abstract static class KeyedEntry<A, B> implements java.util.Map.Entry<A, B> {
        @Override
        public A getKey() {
            return null;
        }
    }

    static final class IntervalEntry<K, V> extends KeyedEntry<Interval<K>, V> {
        @Override
        public V getValue() {
            return null;
        }

        @Override
        public V setValue(V value) {
            return value;
        }
    }

    /** The value of the entry is an IntervalEntry whose key is read through the erased class of the bytecode cast */
    public static <K> Bound<K> lowerBoundOfFirst(java.util.NavigableMap<Bound<K>, IntervalEntry<K, String>> entries) {
        java.util.Map.Entry<Bound<K>, IntervalEntry<K, String>> first = entries.firstEntry();
        return first.getValue().getKey().lowerBound;
    }

    static final class Fluent<E> {
        static <E> Fluent<E> from(Iterable<E> iterable) {
            return new Fluent<>();
        }

        static <E> Fluent<E> from(Fluent<E> fluent) {
            return fluent;
        }

        static <E> Fluent<E> from(E[] elements) {
            return new Fluent<>();
        }

        <T> Fluent<T> transform(java.util.function.Function<? super E, T> function) {
            return new Fluent<>();
        }

        java.util.List<E> toList() {
            return null;
        }
    }

    static final class Pending<V> {
        final String name;

        Pending(String name) {
            this.name = name;
        }
    }

    /** The argument is cast to the parameterized type of the overload, a raw one would make the call unchecked */
    public static java.util.List<String> namesOf(java.util.List<Pending<?>> pendings) {
        return Fluent.from(pendings).transform(pending -> pending.name).toList();
    }

    /** The type of a variable initialized by a ternary with null is the one of the call */
    public static <T extends Comparable<?>> Object endpointOf(Wrapper2<T> range) {
        T endpoint = range.has() ? range.end() : null;
        return endpoint;
    }

    static final class Wrapper2<C extends Comparable<?>> {
        boolean has() {
            return true;
        }

        C end() {
            return null;
        }
    }

    enum AlwaysTrue implements java.util.function.Predicate<Object> {
        INSTANCE;

        @Override
        public boolean test(Object object) {
            return true;
        }

        @SuppressWarnings("unchecked")
        <T> java.util.function.Predicate<T> withNarrowedType() {
            return (java.util.function.Predicate<T>) this;
        }
    }

    abstract static class Base<E> {
    }

    static final class Regular<E> extends Base<E> {
        static final Regular<Object> EMPTY = new Regular<>();
    }

    /** The unchecked cast of a shared instance to the type of the method is in the source, not in the bytecode */
    @SuppressWarnings("unchecked")
    public static <E> Base<E> emptyBase() {
        return (Base<E>) Regular.EMPTY;
    }

    abstract static class Order<T> implements java.util.Comparator<T> {
        @SuppressWarnings("rawtypes")
        static <C extends Comparable> Order<C> natural() {
            return null;
        }

        Order(java.util.Comparator<? super T> comparator) {
        }

        @SuppressWarnings({"unchecked", "rawtypes"})
        static <E> java.util.Comparator<? super E> orNatural(java.util.Comparator<? super E> comparator) {
            if (comparator != null) {
                return comparator;
            }
            return (java.util.Comparator<E>) natural();
        }
    }

    abstract static class SubOrder<E> extends Order<E> {
        /** A bounded type variable cannot be inferred from the type variable of a wildcard target */
        @SuppressWarnings({"unchecked", "rawtypes"})
        SubOrder() {
            this((java.util.Comparator) natural());
        }

        SubOrder(java.util.Comparator<? super E> comparator) {
            super(comparator);
        }
    }

    interface Ranges<K, V> {
        java.util.Map<java.util.List<K>, V> asMap();
    }

    /** The elements of the entry set extend the type of the loop variable: the iterable is cast to the wildcard type */
    public static <K, V> void collectEntries(Ranges<K, ? extends V> ranges, java.util.List<Object> out) {
        for (java.util.Map.Entry<java.util.List<K>, ? extends V> entry : ranges.asMap().entrySet()) {
            out.add(entry.getKey());
            out.add(entry.getValue());
        }
    }

    abstract static class OneWayIterator<E> implements java.util.Iterator<E> {
        @Override
        public final void remove() {
            throw new UnsupportedOperationException();
        }
    }

    abstract static class OneWayCollection<E> extends java.util.AbstractCollection<E> {
        @Override
        public abstract OneWayIterator<E> iterator();
    }

    /** The collection returns a subtype of Iterator, which a for-each statement iterates all the same */
    public static int sizes(OneWayCollection<OneWayCollection<String>> collections) {
        int total = 0;
        for (OneWayCollection<String> collection : collections) {
            total += collection.size();
        }
        return total;
    }

    /** 'tryAdvance(i -> ...)' of an OfInt spliterator is ambiguous between a Consumer and an IntConsumer */
    public static <T> boolean advanceIndexed(java.util.Spliterator.OfInt delegate, java.util.function.IntFunction<T> function, java.util.function.Consumer<? super T> action) {
        return delegate.tryAdvance((java.util.function.IntConsumer) i -> action.accept(function.apply(i)));
    }

    static final class FlatMapper<In, Out, S extends java.util.Spliterator<Out>> {
        S prefix;
        java.util.function.Function<In, S> function;
        java.util.Spliterator<In> from;

        /** The bytecode casts the result of the function to the erasure of S, which is its bound */
        boolean advance() {
            return from.tryAdvance(element -> prefix = function.apply(element));
        }
    }

    /** The local class is only used by an anonymous subclass and by a class literal */
    public static String detectOwner() {
        class LocalClass<T> {
        }
        Class<?> subclass = new LocalClass<String>() {
        }.getClass();
        return LocalClass.class == subclass.getSuperclass() ? "same" : "other";
    }

    static final class AllOf<T> {
        AllOf(java.util.List<? extends java.util.function.Predicate<? super T>> components) {
        }
    }

    @SafeVarargs
    static <T> java.util.List<T> copyOf(T... array) {
        return new java.util.ArrayList<>(java.util.Arrays.asList(array));
    }

    /** The type arguments of the instance are written: they cannot be inferred from the target and from the generic call at once */
    @SafeVarargs
    public static <T> AllOf<T> allOf(java.util.function.Predicate<? super T>... components) {
        return new AllOf<T>(copyOf(components));
    }

    static final java.lang.invoke.MethodHandle LENGTH;

    static {
        try {
            LENGTH = java.lang.invoke.MethodHandles.lookup().findVirtual(String.class, "length", java.lang.invoke.MethodType.methodType(int.class));
        } catch (ReflectiveOperationException e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    /** A signature polymorphic call is typed by its call site: the result is cast to the returned type */
    public static int lengthOf(String value) {
        try {
            return (int) LENGTH.invokeExact(value);
        } catch (Throwable throwable) {
            throw new IllegalStateException(throwable);
        }
    }
}
