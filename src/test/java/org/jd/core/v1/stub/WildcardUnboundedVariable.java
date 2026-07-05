package org.jd.core.v1.stub;

@SuppressWarnings("all")
public class WildcardUnboundedVariable {

    public interface FF<I, O, E> {
        O apply(I in);
    }

    public static <I, O, E> O app(FF<I, O, E> f, I in) {
        return f.apply(in);
    }

    public <T> T call(FF<String, T, ?> f) {
        return app(f, "x");
    }
}
