package org.jd.core.v1.stub;

import java.util.concurrent.Callable;

/** The arguments which follow the anonymous class of a call, after its body. */
public class AnonymousClassArguments {
    static <T> T call(Callable<T> task, Object extra, Object last) throws Exception {
        return extra == last ? null : task.call();
    }

    public static String run(final String text) throws Exception {
        return call(new Callable<String>() {
            @Override
            public String call() {
                return text.trim();
            }
        }, null,
                "last");
    }
}
