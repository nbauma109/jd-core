package org.jd.core.v1;

public record RecordWithConstructor(Object a, Object b) {

    public RecordWithConstructor {
        if (a == null || b == null) {
            throw new IllegalArgumentException("neither a nor b can be null");
        }
    }
}
