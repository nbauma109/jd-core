package org.jd.core.v1.stub;

import java.util.List;

@SuppressWarnings("all")
public class RawDeclaredMethodReference {

    public interface F1<T> {
        void a(T t);
    }

    public interface F2<T> {
        void b(T t);
    }

    static void use(F1<List> f) {
    }

    static void use(F2<List> f) {
    }

    void foo(List l) {
    }

    void call() {
        use((F1<List>) this::foo);
    }
}
