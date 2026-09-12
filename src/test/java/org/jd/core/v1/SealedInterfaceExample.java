package org.jd.core.v1;

public sealed interface SealedInterfaceExample permits SealedInterfaceExample.OpenBranch, SealedInterfaceExample.FinalBranch {
    non-sealed interface OpenBranch extends SealedInterfaceExample {}

    final class FinalBranch implements SealedInterfaceExample {}
}
