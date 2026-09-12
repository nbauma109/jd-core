package org.jd.core.v1;

public sealed class SealedExample permits SealedExample.FinalChild, SealedExample.OpenChild {
    public static final class FinalChild extends SealedExample {}

    public static non-sealed class OpenChild extends SealedExample {
        public OpenChild self(OpenChild value) { return value; }
    }
}
