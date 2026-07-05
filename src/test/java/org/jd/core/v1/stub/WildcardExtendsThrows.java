package org.jd.core.v1.stub;

import java.io.IOException;

@SuppressWarnings("all")
public class WildcardExtendsThrows {

    public interface FF<I, O, E extends Throwable> {
        O apply(I in) throws E;
    }

    public static <I, O, E extends Throwable> O app(FF<I, O, E> f, I in) throws E {
        return f.apply(in);
    }

    public <T> T call(FF<String, T, ? extends IOException> f) throws IOException {
        return app(f, "x");
    }
}
