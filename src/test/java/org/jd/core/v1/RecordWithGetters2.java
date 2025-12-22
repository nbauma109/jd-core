package org.jd.core.v1;

public record RecordWithGetters2(String a, double d) {

    public String a() {
        return a;
    }

    public double d() {
        return d;
    }
}
