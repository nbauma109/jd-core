package org.jd.core.v1.stub;

@SuppressWarnings("all")
public class WildcardObjectBound {

    public interface Holder<T> {
        T get();
    }

    static <T> T first(Holder<T> holder) {
        return holder.get();
    }

    public Object call(Holder<?> holder) {
        return first(holder);
    }
}
