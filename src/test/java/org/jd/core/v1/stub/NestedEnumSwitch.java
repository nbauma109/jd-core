/*
 * Copyright (c) 2026 Nicolas Baumann (@nbauma109), with Claude Sonnet 5.5.
 * This project is distributed under the GPLv3 license.
 * This is a Copyleft license that gives the user the right to use,
 * copy and modify the code freely for non-commercial purposes.
 */

package org.jd.core.v1.stub;

/**
 * Minimal reproduction of a switch-enum inside another switch-enum, with generic locals declared in a single statement
 * (the shape of Guava 12's BstMutationResult.lift, which this fixture replaces). The type returned by the factory of nodes is bound
 * from the call, whose bound ('NestedEnumSwitchNode&lt;?, N&gt;') differs from the one of the class ('NestedEnumSwitchNode&lt;K, N&gt;'): comparing them goes through the lists of type arguments.
 */
public class NestedEnumSwitch<K, N extends NestedEnumSwitchNode<K, N>> {

    private N changedRoot;
    private NestedEnumSwitchKind kind;

    public NestedEnumSwitch<K, N> lift(N originalRoot, NestedEnumSwitchSide side, NestedEnumSwitchNodeFactory<N> nodeFactory) {
        switch (kind) {
            case IDENTITY:
                this.changedRoot = originalRoot;
                return this;
            case REBUILDING_CHANGE, REBALANCING_CHANGE:
                N resultLeft = originalRoot.childOrNull(NestedEnumSwitchSide.LEFT);
                N resultRight = originalRoot.childOrNull(NestedEnumSwitchSide.RIGHT);
                switch (side) {
                    case LEFT:
                        resultLeft = changedRoot;
                        break;
                    case RIGHT:
                        resultRight = changedRoot;
                        break;
                    default:
                        throw new AssertionError();
                }
                this.changedRoot = nodeFactory.createNode(originalRoot, resultLeft, resultRight);
                return this;
            default:
                throw new AssertionError();
        }
    }
}
