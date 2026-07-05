package org.jd.core.v1.stub;

import java.util.List;

@SuppressWarnings("all")
public class NullArgumentWitness {

    static <T> T id(T t) {
        return t;
    }

    public static int call() {
        return NullArgumentWitness.<List>id(null).size();
    }
}
