package org.jd.core.v1.stub;

import java.util.function.Predicate;

@SuppressWarnings("all")
public class AmbiguousMethodReference {

    public interface ThreadPredicate {
        boolean test(Thread thread);
    }

    public static int find(Predicate<Thread> predicate) {
        return 1;
    }

    public static int find(ThreadPredicate predicate) {
        return 2;
    }

    public static int call(ThreadPredicate predicate) {
        return find((Predicate<Thread>) predicate::test);
    }
}
