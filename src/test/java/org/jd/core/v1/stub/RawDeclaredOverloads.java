package org.jd.core.v1.stub;

@SuppressWarnings("all")
public class RawDeclaredOverloads {

    public interface F1<T> {
        void a(T t);
    }

    public interface F2<T> {
        void b(T t);
    }

    static void use(F1 f) {
    }

    static void use(F2 f) {
    }

    void foo(Object o) {
    }

    void call() {
        use((F1) this::foo);
    }
}
