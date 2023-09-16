package org.jd.core.v1.stub;

public class NumericConstants {

    public static final Long LONG_INT_MAX_VALUE = Long.valueOf(Integer.MAX_VALUE);
    public static final Long LONG_INT_MIN_VALUE = Long.valueOf(Integer.MIN_VALUE);

    boolean isInteger(double d) {
        return d >= Integer.MIN_VALUE && d <= Integer.MAX_VALUE;
    }

    boolean isInteger(long l) {
        return l >= Integer.MIN_VALUE && l <= Integer.MAX_VALUE;
    }

    boolean isInteger(float f) {
        return f >= Integer.MIN_VALUE && f <= Integer.MAX_VALUE;
    }
}
