package org.jd.core.v1.stub;

import java.util.function.Function;
import java.util.function.Supplier;

/** Functional interfaces of every shape a lambda can target: where the type returned by the lambda comes from. */
@SuppressWarnings({"unchecked", "rawtypes"})
public class GsonFunctionalPatterns {

    public interface Source<T> {
        T get();
    }

    /** The type variable which is returned is the second one */
    public interface Pair<A, B> {
        B second(A first);
    }

    /** Not a type variable which is returned */
    public interface Named<T> {
        String name(T value);
    }

    /** The abstract method of a super interface is implemented by a default method of a sub interface */
    public interface Base<T> {
        T base();
    }

    public interface Defaulted<T> extends Base<T> {
        @Override
        default T base() {
            return null;
        }

        T own();
    }

    /** Siblings: the default method is met after the abstract method it implements */
    public interface Sibling<T> extends Base<T> {
        @Override
        default T base() {
            return null;
        }
    }

    public interface Mixed<T> extends Base<T>, Sibling<T> {
        T mixed();
    }

    /** The only abstract method has other kinds of methods around it */
    public interface Busy<T> {
        static <X> X make(X value) {
            return value;
        }

        T run();

        @Override
        String toString();

        @Override
        boolean equals(Object other);

        @Override
        int hashCode();
    }

    public interface Fixed<T> extends Function<T, String> {
    }

    public static <T> Source<T> source(Object value) {
        return () -> (T) value;
    }

    public static <A, B> Pair<A, B> pair(Object value) {
        return first -> (B) value;
    }

    public static <T> Named<T> named() {
        return value -> String.valueOf(value);
    }

    public static <T> Defaulted<T> defaulted(Object value) {
        return () -> (T) value;
    }

    public static <T> Mixed<T> mixed(Object value) {
        return () -> (T) value;
    }

    public static <T> Busy<T> busy(Object value) {
        return () -> (T) value;
    }

    public static <T> Fixed<T> fixed() {
        return value -> "fixed";
    }

    public static <T> Runnable plain(Object value) {
        return () -> source(value).get();
    }

    /** A lambda in a lambda: the outer one keeps being a lambda after the inner one */
    public static <T> Supplier<Supplier<T>> nested(Object value) {
        return () -> {
            Supplier<T> inner = () -> (T) value;
            T result = inner.get();
            return () -> result;
        };
    }

    public static <T> Source<T> viaMethodReference(Object value) {
        Supplier<Object> supplier = () -> value;
        return () -> (T) supplier.get();
    }

    /** The type argument of the target is a wildcard */
    public static <T> Supplier<? extends T> wildcard(Object value) {
        return () -> (T) value;
    }
}
