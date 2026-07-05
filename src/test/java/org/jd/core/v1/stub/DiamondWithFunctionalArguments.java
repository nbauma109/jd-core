package org.jd.core.v1.stub;

import java.util.function.BiConsumer;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.function.Supplier;

@SuppressWarnings("all")
public class DiamondWithFunctionalArguments {

    static class SimpleCollector<T, A, R> {
        SimpleCollector(Supplier<A> supplier, BiConsumer<A, T> accumulator, BinaryOperator<A> combiner, Function<A, R> finisher) {
        }
    }

    public static SimpleCollector<Object, ?, String> joining() {
        return new SimpleCollector<>(StringBuilder::new, StringBuilder::append, StringBuilder::append, StringBuilder::toString);
    }
}
