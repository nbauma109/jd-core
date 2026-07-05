package org.jd.core.v1.stub;

@SuppressWarnings("all")
public class WildcardExtendsBound {

    public interface Holder<T> {
        T get();
    }

    static <T extends Number> T pick(Holder<T> holder) {
        return holder.get();
    }

    public Number call(Holder<? extends Number> holder) {
        return pick(holder);
    }
}
