package org.jd.core.v1.stub;

import java.io.Serializable;
import java.util.List;

/**
 * What a variable is declared with is not what javac infers when a method type variable has a bound which the declared type does not
 * satisfy ('Ordering&lt;E&gt; o = Ordering.natural()'): the bound is what the variable stands for.
 */
@SuppressWarnings({"rawtypes", "unchecked"})
public class BoundedBindings {
    public static final class Ord<T> {
        Ord<T> reverse() {
            return this;
        }
    }

    public static final class Pair<A, B> {
    }

    static <C extends Comparable<?>> Ord<C> natural() {
        return new Ord<>();
    }

    static <C extends Serializable> Ord<C> serializable() {
        return new Ord<>();
    }

    static <C extends Comparable<?>, D extends Comparable<?>> Pair<C, D> pair() {
        return new Pair<>();
    }

    /** The variable is a type variable with no bound */
    static <E> Ord<E> ofAVariable() {
        Ord<E> ordering = (Ord) natural();
        return ordering.reverse();
    }

    /** The variable is a type variable which has the bound */
    static <E extends Comparable<E>> Ord<E> ofABoundedVariable() {
        Ord<E> ordering = natural();
        return ordering.reverse();
    }

    static Ord<Object> ofAnObject() {
        Ord<Object> ordering = (Ord) natural();
        return ordering.reverse();
    }

    static Ord<String> ofAComparable() {
        Ord<String> ordering = natural();
        return ordering.reverse();
    }

    static Ord<String[]> ofAnArray() {
        Ord<String[]> ordering = (Ord) natural();
        return ordering.reverse();
    }

    static Ord<int[]> ofAnArrayWhichIsSerializable() {
        Ord<int[]> ordering = (Ord) serializable();
        return ordering.reverse();
    }

    static <E> Pair<E, String> ofVariables() {
        Pair<E, String> pair = (Pair) pair();
        return pair;
    }

    static Pair<Object, List<String>> ofObjects() {
        Pair<Object, List<String>> pair = (Pair) pair();
        return pair;
    }
}
