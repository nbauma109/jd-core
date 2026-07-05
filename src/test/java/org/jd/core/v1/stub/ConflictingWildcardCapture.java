package org.jd.core.v1.stub;

@SuppressWarnings("all")
public class ConflictingWildcardCapture {

    public interface Holder<T> {
        T get();
    }

    static <T extends Number> void m(Holder<T> a, Holder<T> b) {
    }

    public void call(Holder<? extends Integer> x, Holder<Number> y) {
        m((Holder) x, y);
    }
}
