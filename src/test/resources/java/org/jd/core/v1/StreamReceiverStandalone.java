package org.jd.core.v1;

import java.util.stream.Stream;

interface StreamReceiverStandaloneBase<T, B extends java.util.stream.BaseStream<T, B>> {
    B unwrap();
}

@SuppressWarnings("unused")
interface StreamReceiverStandalone<T> extends StreamReceiverStandaloneBase<T, Stream<T>> {
    @FunctionalInterface
    interface IOPredicate<T> {
        boolean test(T value);
    }

    final class Erase {
        static <T> boolean test(IOPredicate<? super T> predicate, T value) {
            return predicate.test(value);
        }
    }

    default boolean allMatch(IOPredicate<? super T> predicate) {
        return unwrap().allMatch(t -> Erase.test(predicate, t));
    }
}
