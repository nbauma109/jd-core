package org.jd.core.v1.stub;

public class NumericConstants {

    static final Long LONG_INT_MAX_VALUE = Long.valueOf(Integer.MAX_VALUE);
    static final Long LONG_INT_MIN_VALUE = Long.valueOf(Integer.MIN_VALUE);
    static final Double DOUBLE_FLOAT_MIN_VALUE = Double.valueOf(Float.MIN_VALUE);
    static final Double DOUBLE_FLOAT_MAX_VALUE = Double.valueOf(Float.MAX_VALUE);
    static final Float FLOAT_MIN_VALUE = Float.MIN_VALUE;
    static final Double DOUBLE_MIN_VALUE = Double.MIN_VALUE;

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
