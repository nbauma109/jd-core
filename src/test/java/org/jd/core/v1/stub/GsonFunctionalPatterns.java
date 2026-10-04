package org.jd.core.v1.stub;

import java.util.List;
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

    public interface DefaultGet<T> {
        default T get() {
            return null;
        }
    }

    /** An abstract method which re-declares an inherited default method */
    public interface Reabstracted<T> extends DefaultGet<T> {
        @Override
        T get();
    }

    public interface ObjectGet {
        Object get();
    }

    public interface TypedGet<T> {
        T get();
    }

    /** Both inherited methods have the same erasure: the one which returns the type variable is the most specific */
    public interface Covariant<T> extends ObjectGet, TypedGet<T> {
    }

    public static <T> Reabstracted<T> reabstracted(Object value) {
        return () -> (T) value;
    }

    public static <T> Covariant<T> covariant(Object value) {
        return () -> (T) value;
    }

    /** The erased return type of the lambda is the bound of the type variable */
    public static <T extends Number> Source<T> bounded(Number value) {
        return () -> (T) value;
    }

    public interface ObjectGetToo {
        Object get();
    }

    /** The same method seen through two paths */
    public interface Left<T> extends TypedGet<T> {
    }

    public interface Right<T> extends TypedGet<T> {
    }

    public interface Diamond<T> extends Left<T>, Right<T> {
    }

    /** The declaration which returns the type variable comes first, then the one which returns Object */
    public interface ReverseCovariant<T> extends TypedGet<T>, ObjectGet {
    }

    /** Override-equivalent declarations, none of which returns a type variable */
    public interface Unrelated extends ObjectGet, ObjectGetToo {
    }

    public static <T> Diamond<T> diamond(Object value) {
        return () -> (T) value;
    }

    public static <T> ReverseCovariant<T> reverseCovariant(Object value) {
        return () -> (T) value;
    }

    public static Unrelated unrelated(Object value) {
        return () -> value;
    }

    /** The sub interface comes first: it overrides the default method of the interface which comes next */
    public interface MixedReverse<T> extends Sibling<T>, Base<T> {
        T mixed();
    }

    public interface TypedGetToo<T> {
        T get();
    }

    /** Both inherited declarations return a type variable */
    public interface BothTyped<T> extends TypedGet<T>, TypedGetToo<T> {
    }

    /** The method of the functional interface comes from a super interface which is not generic */
    public interface RunnableOf<T> extends Runnable {
    }

    public interface SupplierOf<T> extends ObjectGet {
    }

    public static <T> MixedReverse<T> mixedReverse(Object value) {
        return () -> (T) value;
    }

    public static <T> BothTyped<T> bothTyped(Object value) {
        return () -> (T) value;
    }

    public static <T> SupplierOf<T> supplierOf(Object value) {
        return () -> value;
    }

    public static <T> RunnableOf<T> runnableOf() {
        return () -> {
        };
    }

    public interface CovariantA<T> {
        T get();
    }

    public interface CovariantB {
        Number get();
    }

    /** The inherited methods only differ by their (covariant) returned type: Object for the first one, Number for the second one */
    public interface CovariantC<T extends Number> extends CovariantA<T>, CovariantB {
    }

    public static <T extends Number> CovariantC<T> covariantC(Number value) {
        return () -> (T) value;
    }

    /** The compiler adds a bridge method to the interface: it must not replace the abstract method */
    public interface BridgedGet<T extends CharSequence> extends ObjectGet {
        @Override
        T get();
    }

    public static <T extends CharSequence> BridgedGet<T> bridgedGet(CharSequence value) {
        return () -> (T) value;
    }

    /** The returned type contains the type variable of the interface */
    public interface NestedGet<T> {
        List<T> get();
    }

    public static <T> NestedGet<T> nestedGet(List<?> unknown) {
        return () -> (List<T>) unknown;
    }

    public interface Merging<X, T> {
        T apply(X first);
    }

    public interface Merged<T> {
        T apply(String first);
    }

    /** Once the type arguments are substituted, both methods have the same parameters: apply(Object) and apply(String) */
    public interface Combined<T> extends Merging<String, T>, Merged<T> {
    }

    public static <T> Combined<T> combined(Object value) {
        return first -> (T) value;
    }

    public interface BridgedBase<T> {
        void a(T value);
    }

    /** The default method has a bridge a(Object), which implements the abstract method of the super interface */
    public interface BridgedDefault extends BridgedBase<String> {
        @Override
        default void a(String value) {
        }
    }

    public interface Producer<T> {
        T d();
    }

    public interface Bridged<T> extends BridgedDefault, Producer<T> {
    }

    public static <T> Bridged<T> bridged(Object value) {
        return () -> (T) value;
    }

    public interface ArrayA<T> {
        Object[] get();
    }

    public interface ArrayB<T> {
        T[] get();
    }

    /** The covariant declaration returns an array of the type variable */
    public interface ArrayC<T> extends ArrayA<T>, ArrayB<T> {
    }

    public static <T> ArrayC<T> arrayC(Object[] value) {
        return () -> (T[]) value;
    }

    public interface PutA<T> {
        void put(T[] first, int second, long[][] third);
    }

    public interface PutB {
        void put(String[] first, int second, long[][] third);
    }

    /** Override-equivalent once T is String: the parameters are arrays and primitives */
    public interface PutC extends PutA<String>, PutB {
    }

    public interface InnerGet<T> {
        GuavaOuter<T, T>.Wrapped get();
    }

    public interface ThrowingGet<E extends Throwable> {
        Object get() throws E;
    }

    public interface GenericGet<T> {
        T get();
    }

    /** The generic exception of one declaration does not make its return type generic */
    public interface ThrowingAndGeneric<T, E extends Throwable> extends ThrowingGet<E>, GenericGet<T> {
    }
}
