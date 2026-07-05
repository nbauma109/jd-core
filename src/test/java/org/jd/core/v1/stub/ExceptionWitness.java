package org.jd.core.v1.stub;

import java.time.Duration;
import java.time.Instant;

@SuppressWarnings("all")
public class ExceptionWitness {

    public interface FailableConsumer<T, E extends Throwable> {
        void accept(T t) throws E;
    }

    public interface FailableRunnable<E extends Throwable> {
        void run() throws E;
    }

    public static <E extends Throwable> Duration of(FailableConsumer<Instant, E> consumer) throws E {
        Instant start = Instant.now();
        consumer.accept(start);
        return Duration.between(start, Instant.now());
    }

    public static <E extends Throwable> Duration of(FailableRunnable<E> runnable) throws E {
        return of(start -> runnable.run());
    }
}
