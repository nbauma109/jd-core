package org.jd.core.v1.stub;

import java.io.IOException;
import java.util.function.Consumer;

@SuppressWarnings("all")
public class ErasedMethodReference<T> {

    public interface IOConsumer<T> {
        void accept(T t) throws IOException;
    }

    public interface IOIntConsumer {
        void accept(int i) throws IOException;
    }

    static class Uncheck {
        static <T> void accept(IOConsumer<T> consumer, T t) {
        }

        static void accept(IOIntConsumer consumer, int i) {
        }
    }

    interface DelegateLike<T> {
        void forEachRemaining(Consumer<? super T> action);
    }

    private DelegateLike<T> delegate;

    public void forEachRemaining(Consumer<? super T> action) {
        Uncheck.accept(delegate::forEachRemaining, action::accept);
    }
}
