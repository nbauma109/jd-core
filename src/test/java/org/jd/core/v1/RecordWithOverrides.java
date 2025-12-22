package org.jd.core.v1;

public record RecordWithOverrides(String a, double d) {
    @Override
    public final int hashCode() {
        throw new UnsupportedOperationException();
    }
    
    @Override
    public final boolean equals(Object o) {
        throw new UnsupportedOperationException();
    }

    @Override
    public final String toString() {
        throw new UnsupportedOperationException();
    }
}
