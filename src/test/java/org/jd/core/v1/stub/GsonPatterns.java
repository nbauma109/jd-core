package org.jd.core.v1.stub;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Supplier;

/**
 * The shapes which made Gson 2.14.0 decompile to code which did not recompile or did not behave: each method below was
 * decompiled to something wrong (an unchecked cast lost, a try/catch dropped, statements reordered, ...).
 */
@SuppressWarnings({"unchecked", "rawtypes"})
public class GsonPatterns {

    public interface Adapter<T> {
        T get();
    }

    public interface Factory {
        <T> Adapter<T> create(Class<T> type);
    }

    static class ObjectAdapter implements Adapter<Object> {
        @Override
        public Object get() {
            return null;
        }
    }

    static class TextAdapter implements Adapter<String> {
        private final Adapter<Integer> delegate;

        TextAdapter(Adapter<Integer> delegate) {
            this.delegate = delegate;
        }

        @Override
        public String get() {
            return String.valueOf(delegate.get());
        }
    }

    static class BoundedAdapter<T extends Number> implements Adapter<T> {
        BoundedAdapter(Class<T> type) {
        }

        @Override
        public T get() {
            return null;
        }
    }

    static class ReflectiveAdapter<T> implements Adapter<T> {
        ReflectiveAdapter(Class<T> type, boolean flag) {
        }

        @Override
        public T get() {
            return null;
        }
    }

    /** A 'new' of a class which is not generic is never an Adapter<T> */
    public static Factory nonGenericClassesAreNotAdaptersOfT() {
        return new Factory() {
            @Override
            public <T> Adapter<T> create(Class<T> type) {
                if (type == Object.class) {
                    return (Adapter<T>) new ObjectAdapter();
                }
                return (Adapter<T>) new TextAdapter(null);
            }
        };
    }

    /** A captured variable has an erased type in the anonymous class: it is an Adapter<X> there, not a raw Adapter */
    public static <X> Factory capturedVariable(Class<X> type, Adapter<X> adapter) {
        return new Factory() {
            @Override
            public <T> Adapter<T> create(Class<T> requested) {
                return requested == type ? (Adapter<T>) adapter : null;
            }
        };
    }

    /** The anonymous class itself needs the cast: it is an Adapter<T1> */
    public static <T1, T2> Adapter<T2> anonymousClass(Adapter<T1> adapter) {
        return (Adapter<T2>) new Adapter<T1>() {
            @Override
            public T1 get() {
                return adapter.get();
            }
        };
    }

    /** A diamond cannot infer a type argument out of the bounds of a type parameter: the raw type is used */
    public static <T> Adapter<T> rawConstructor(Class<T> raw) {
        return (Adapter<T>) new BoundedAdapter(raw);
    }

    /** A wildcard parameterized argument makes it a constructor call with a captured type argument */
    public static <T> Adapter<T> capturedConstructor(Class<? super T> raw) {
        return (Adapter<T>) new ReflectiveAdapter<>(raw, true);
    }

    static <T> Adapter<T> adapterOf(Class<T> type) {
        return type == null ? null : new ReflectiveAdapter<>(type, false);
    }

    /** A call with a wildcard parameterized argument returns a capture: assigning it to an Adapter<Object> needs a cast */
    public static Object capturedMethodCall(Object value) {
        Adapter<Object> adapter = (Adapter<Object>) adapterOf(value.getClass());
        Adapter<Object> other = (Adapter<Object>) adapterOf(value.getClass());
        return adapter.get() == other.get();
    }

    /** What a constructor of a wildcard super parameterized class returns is not a T, and this is also true in a lambda */
    public static <T> Supplier<T> capturedInLambda(Class<? super T> raw) throws NoSuchMethodException {
        Constructor<? super T> constructor = raw.getDeclaredConstructor();
        return () -> {
            try {
                T instance = (T) constructor.newInstance();
                return Objects.requireNonNull(instance);
            } catch (ReflectiveOperationException e) {
                throw new IllegalStateException(e);
            }
        };
    }

    /** A field hides the type of the same name used for a static call */
    public static final Adapter<UUID> UUID = new Adapter<UUID>() {
        @Override
        public UUID get() {
            return java.util.UUID.fromString("00000000-0000-0000-0000-000000000000");
        }
    };

    public static class Outer<T> {
        public class Inner extends Outer<T> {
        }

        public boolean isInner(Object o) {
            return o instanceof Outer.Inner;
        }
    }

    /** A 'break' goes over the code which follows the chain of 'if': it is not an arm of one of them */
    public static Object breakOverContinuation(Object[] lower, Object[] upper, Object original, Object resolved, Map<Object, Object> resolutions, Object resolving) {
        while (true) {
            if (resolved == null) {
                if (lower.length == 1) {
                    Object bound = lower[0];
                    if (bound != lower[0]) {
                        resolved = bound;
                        break;
                    }
                } else if (upper.length == 1) {
                    Object bound = upper[0];
                    if (bound != upper[0]) {
                        resolved = bound;
                        break;
                    }
                }
                resolved = original;
                break;
            } else {
                break;
            }
        }
        if (resolving != null) {
            resolutions.put(resolving, resolved);
        }
        return resolved;
    }

    /** A 'return' which also follows the loop is not part of the loop */
    public static Object returnSharedWithTheCodeAfterTheLoop(Object value) {
        while (true) {
            if (value instanceof String) {
                value = value.toString();
                if (value == null) {
                    break;
                }
            } else if (value instanceof Class) {
                value = ((Class<?>) value).getName();
                break;
            } else {
                break;
            }
        }
        return value;
    }

    private final Object lock = new Object();

    static int read() throws IOException {
        return 1;
    }

    /** The catch of a try which returns, inside a synchronized block, must not be dropped */
    public int tryCatchInSynchronized() {
        synchronized (lock) {
            try {
                return read();
            } catch (IOException e) {
                throw new IllegalStateException(e);
            }
        }
    }

    static int parse(String text, int from, int to) {
        return text.isEmpty() ? 0 : to - from;
    }

    /** The compound assignment is evaluated after the first argument: it has to stay an expression */
    public static int compoundAssignmentInArguments(String text) {
        int offset = 0;
        int year = parse(text, offset, offset += 4);
        int hour = parse(text, ++offset, offset += 2);
        return year * 100 + hour + offset;
    }

    /** A long is captured before the wildcard parameterized variable: it takes two slots of the lambda method */
    public static <T> Supplier<T> capturedAfterALong(long count, Class<? super T> raw) throws NoSuchMethodException {
        Constructor<? super T> constructor = raw.getDeclaredConstructor();
        return () -> {
            try {
                return (T) constructor.newInstance();
            } catch (ReflectiveOperationException e) {
                throw new IllegalStateException(count + e.getMessage(), e);
            }
        };
    }

    /** The type returned by the single abstract method of a generic interface is one of its type variables */
    public static <A, B> java.util.function.Function<A, B> castingFunction() {
        return a -> (B) a;
    }

    /** A raw receiver returns the erasure of the type variable of its class */
    public static <T> T rawReceiver(Constructor constructor) throws ReflectiveOperationException {
        T instance = (T) constructor.newInstance();
        return Objects.requireNonNull(instance);
    }

    /** 'iinc' by more than a byte, and a decrement whose value is used */
    public static int wideAndPostDecrement(String text) {
        int offset = 10;
        int wide = parse(text, offset, offset += 1000);
        int before = parse(text, offset--, offset);
        return wide + before + offset;
    }

    /** A monitor kept in a high local variable slot */
    public int tryCatchInSynchronizedWithManyLocals(int a, int b, int c, int d) {
        long e = a + b;
        double f = c + d;
        synchronized (lock) {
            try {
                return read() + (int) (e + f);
            } catch (IOException ex) {
                throw new IllegalStateException(ex);
            }
        }
    }

    static final class StringAdapter implements Adapter<String> {
        @Override
        public String get() {
            return null;
        }
    }

    /** The fixed type arguments may be disjoint from the bound of the type variable: the cast goes through the raw type */
    public static <T extends Number> Adapter<T> boundedTarget() {
        return (Adapter<T>) (Adapter) new StringAdapter();
    }

    public interface Parent<T> {
        T take();
    }

    public interface Child<T> extends Parent<T> {
    }

    /** The single abstract method is inherited from the parent of the functional interface */
    public static <T> Child<T> inheritedFunctionalMethod(Object value) {
        return () -> (T) value;
    }

    /** The compound assignment is on a local variable which has no short form of 'iload' */
    public static int compoundAssignmentOnHigherSlot(int a, int b, int c, int d, String text) {
        int offset = a + b + c + d;
        return parse(text, offset, offset += 4);
    }

    /** The abstract method of Comparator which is not equals returns an int, not a type variable */
    public static <T> java.util.Comparator<T> nonGenericFunctionalResult() {
        return (first, second) -> first == second ? 0 : 1;
    }

    static int sideEffects;

    static int sideEffect() {
        return ++sideEffects;
    }

    static int use(int first, int second) {
        return first * 10 + second;
    }

    /** The increment is evaluated after the first argument, and its value is loaded again */
    public static int incrementAfterAPendingArgument(int i) {
        return use(sideEffect(), ++i) + use(sideEffect(), --i);
    }
}
