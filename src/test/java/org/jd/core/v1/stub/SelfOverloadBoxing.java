package org.jd.core.v1.stub;

@SuppressWarnings("all")
public class SelfOverloadBoxing {

    public static String toString(char ch) {
        return String.valueOf(ch);
    }

    public static String toString(Character ch) {
        return ch != null ? toString(ch.charValue()) : null;
    }

    static class Range<T> {
        T fit(T element) {
            return element;
        }
    }

    static class LongRange extends Range<Long> {
        long fit(long element) {
            return super.fit(Long.valueOf(element)).longValue();
        }
    }

    static final LongRange LONG_TO_INT_RANGE = new LongRange();

    public static int toMillisInt(long millis) {
        return LONG_TO_INT_RANGE.fit(Long.valueOf(millis)).intValue();
    }
}
