package org.jd.core.v1.stub;

@SuppressWarnings("all")
public class WildcardCapture {

    public interface FF<I, O, E extends Throwable> {
        O apply(I in) throws E;
    }

    public static <I, O, E extends Throwable> O app(FF<I, O, E> f, I in) throws E {
        return f.apply(in);
    }

    public <T> T call(FF<String, T, ?> f) throws Throwable {
        return app(f, "x");
    }
}
