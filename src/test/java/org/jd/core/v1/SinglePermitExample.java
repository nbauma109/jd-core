package org.jd.core.v1;

public sealed class SinglePermitExample permits SinglePermitExample.OnlyChild {
    public static final class OnlyChild extends SinglePermitExample {}
}
