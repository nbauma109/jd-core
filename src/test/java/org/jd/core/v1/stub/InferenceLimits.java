/*
 * Copyright (c) 2026 Nicolas Baumann (@nbauma109), with Claude Sonnet 5.5.
 * This project is distributed under the GPLv3 license.
 * This is a Copyleft license that gives the user the right to use,
 * copy and modify the code freely for non-commercial purposes.
 */

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

    public static class Pair<K, V> {
        @SuppressWarnings("unchecked")
        <V> V pick(Object value) {
            return (V) value;
        }
    }

    /** The type variable of the method hides the one of the class: it is inferred from the argument, the receiver says nothing of it */
    public static int hiddenByTheMethod(Pair<String, Value> pair, Object value) {
        return ((Value) pair.pick(value)).answer();
    }

    public static class Bounded<T extends Number & Runnable> {
        int accepted;

        void accept(T value) {
            accepted += value.hashCode() & 1;
        }
    }

    /** The bound of the wildcard alone does not satisfy the bounds of the variable of the class */
    @SuppressWarnings({"rawtypes", "unchecked"})
    public static void severalBounds(Bounded<? extends Number> bounded, Number number) {
        ((Bounded) bounded).accept(number);
    }

    /** A method reference needs the type of the elements of the list */
    public static int referenceOverCaptures(Box<? extends Number> box) {
        return count(box.lists(), List::size);
    }

    /** The same with a lambda which adds the element itself */
    public static Collector<Integer, ?, List<String>> mappedByALambda() {
        return Collector.of(InferenceLimits::create, (accumulated, element) -> accumulated.add(element), (first, second) -> first,
                accumulated -> accumulated.stream().map(Object::toString).toList());
    }

    static <T> T first(List<T> values) {
        return values.get(0);
    }

    static <R> R apply(java.util.function.Function<List<String>, R> function) {
        return function.apply(new ArrayList<>());
    }

    /** The result is read: a raw argument would make it an Object */
    public static int resultOfAnInvocationWhichIsReadAsAReceiver(Box<? extends Number> box) {
        return first(box.lists()).size();
    }

    /** The target gives the lambda the parameter type List<String> */
    public static String lambdaOverAParameterizedTarget() {
        return apply(strings -> strings.isEmpty() ? "" : strings.get(0));
    }

    public interface Fn<R, A> {
        R apply(A argument);
    }

    static <R> R applyFn(Fn<R, List<String>> function) {
        return function.apply(new ArrayList<>());
    }

    /** The parameter of the functional method is the second type argument of the target */
    public static String lambdaOverAParameterizedSecondArgument() {
        return applyFn(strings -> strings.isEmpty() ? "" : strings.get(0));
    }

    public interface ListFn {
        String apply(List<String> strings);
    }

    static <R> R applyListFn(ListFn function, java.util.function.Function<String, R> mapper) {
        return mapper.apply(function.apply(new ArrayList<>()));
    }

    /** The interface has no type argument: it declares the parameterized parameter type itself */
    public static int lambdaOverANonGenericTarget() {
        return applyListFn(strings -> strings.isEmpty() ? "" : strings.get(0), String::length);
    }

    static <R> java.util.Optional<R> optionalFn(Fn<R, List<String>> function) {
        return java.util.Optional.of(function.apply(new ArrayList<>()));
    }

    /** A call read as a receiver: the parameter of the functional method is the second type argument, which is no variable of the method */
    public static boolean lambdaOfAReceiverOverAParameterizedSecondArgument() {
        return optionalFn(strings -> strings.size()).isPresent();
    }

    public interface InheritedListFn extends java.util.function.Function<List<String>, String> {
    }

    static <R> R applyInherited(InheritedListFn function, java.util.function.Function<String, R> mapper) {
        return mapper.apply(function.apply(new ArrayList<>()));
    }

    /** The functional method is inherited from an interface the target extends with a parameterized type */
    public static int lambdaOverAnInheritingTarget() {
        return applyInherited(strings -> strings.isEmpty() ? "" : strings.get(0), String::length);
    }

    static <E extends Number> ArrayList<E> createNumbers() {
        return new ArrayList<>();
    }

    /** The factory has a bound which the element type of the result does not satisfy */
    public static Collector<Object, ?, List<Object>> boundedFactory() {
        return Collector.of(InferenceLimits::createNumbers, (accumulated, element) -> accumulated.add((Number) element), (first, second) -> first,
                accumulated -> new ArrayList<Object>(accumulated));
    }

    /** The type variable of the result is not bounded by what the factory requires */
    public static <T> Collector<T, ?, List<T>> boundedFactoryOfAVariable(java.util.function.Function<Number, T> mapper) {
        return Collector.of(InferenceLimits::createNumbers, (accumulated, element) -> accumulated.add(1), (first, second) -> first,
                accumulated -> accumulated.stream().map(mapper).toList());
    }

    static <E extends Comparable<String>> ArrayList<E> createStrings() {
        return new ArrayList<>();
    }

    /** The factory has a parameterized bound which Integer, a raw Comparable, does not satisfy */
    @SuppressWarnings({"rawtypes", "unchecked"})
    public static Collector<Integer, ?, List<Integer>> parameterizedBoundOfTheFactory() {
        return Collector.of(InferenceLimits::createStrings, (accumulated, element) -> accumulated.add("x"), (first, second) -> first,
                accumulated -> accumulated.stream().map(Object::hashCode).toList());
    }

    static <K extends CharSequence, V extends K> java.util.HashMap<K, V> createDependent() {
        return new java.util.HashMap<>();
    }

    /** The bound of a type variable of the factory is another one: Integer is no String */
    public static Collector<Integer, ?, java.util.Map<String, Integer>> dependentBoundsOfTheFactory() {
        return Collector.of(InferenceLimits::createDependent, (accumulated, element) -> accumulated.put("key", "value"), (first, second) -> first,
                accumulated -> {
                    java.util.Map<String, Integer> lengths = new java.util.HashMap<>();

                    accumulated.forEach((key, value) -> lengths.put(key.toString(), value.length()));
                    return lengths;
                });
    }

    public static class MultiBounded<T extends Number & Runnable> {
        <T> int count(List<T> values, java.util.function.ToIntFunction<T> size) {
            return values.size() + size.applyAsInt(values.get(0));
        }
    }

    /** A type variable of the method has the name of the one of the class, which has several bounds */
    public static int shadowedByTheMethod(MultiBounded<?> box, List<List<Integer>> values) {
        return box.count(values, elements -> elements.size());
    }

    public static class NumberBuilder<T extends Number> {
        void add(Integer value) {
            // nothing to keep
        }

        <T> List<T> finish() {
            return new ArrayList<>();
        }
    }

    /** The finisher declares a type variable with the name of the one of its class */
    public static Collector<Integer, ?, List<String>> finisherWithAHiddenVariable() {
        return Collector.of(NumberBuilder::new, NumberBuilder::add, (first, second) -> first, NumberBuilder::finish);
    }

    public static class Pair2<K, V> {
        V get() {
            return null;
        }
    }

    public static class Swap<K, V> extends Pair2<V, Object> {
    }

    /** The returned variable is the one of the superclass which declares the method, not the unrelated one of the receiver */
    @SuppressWarnings({"rawtypes", "unchecked"})
    public static int inheritedReturnVariable(Swap<String, Value> swap) {
        return ((Value) ((Pair2) swap).get()).answer();
    }

    public static class RunnableNumbers<T extends Number & Runnable> {
        int consume(java.util.function.ToIntFunction<T> function) {
            return function == null ? 0 : 1;
        }
    }

    /** The lambda gets its parameter type from the target: a raw receiver would make it an Object */
    public static int functionalArgumentOfACapture(RunnableNumbers<?> box) {
        return box.consume(number -> number.intValue());
    }
}
