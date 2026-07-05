package org.jd.core.v1.stub;

@SuppressWarnings("all")
public class SneakyThrow {

    public static <E extends Throwable> void sneakyThrow(Throwable t) throws E {
        throw (E) t;
    }
}
