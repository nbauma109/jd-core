package org.jd.core.v1;

public record RecordWithGetters(String a, double d) {

    public String getA() {
        return a;
    }

    public double getD() {
        return d;
    }
}
