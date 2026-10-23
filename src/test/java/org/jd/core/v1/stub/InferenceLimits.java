package org.jd.core.v1.stub;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.ToIntFunction;
import java.util.stream.Collector;

/** Calls whose source depends on the type arguments the bytecode does not keep: the bytecode casts must not be turned into something javac rejects */
public class InferenceLimits {
    public static class Value {
        int answer() {
            return 42;
        }
    }

    public static class Box<E> {
        List<List<E>> lists() {
            return new ArrayList<>();
        }
    }

    static <E> ArrayList<E> create() {
        return new ArrayList<>();
    }

    static <T> int count(List<T> list, ToIntFunction<T> size) {
        return list.size() + size.applyAsInt(list.get(0));
    }

    /** Reading through '? super Value' gives an Object: the cast is the source's */
    public static int lowerBounded(Map<String, ? super Value> values) {
        return ((Value) values.get("key")).answer();
    }

    /** The finisher changes the element type: the accumulator is not an ArrayList of the result's elements */
    public static Collector<Integer, ?, List<String>> mapped() {
        return Collector.of(InferenceLimits::create, ArrayList::add, (first, second) -> first, accumulated -> accumulated.stream().map(Object::toString).toList());
    }

    /** The lambda needs the type of the list's elements: the argument is not raw */
    public static int lambdaOverCaptures(Box<? extends Number> box) {
        return count(box.lists(), elements -> elements.size());
    }
}
