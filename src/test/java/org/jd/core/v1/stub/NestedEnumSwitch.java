/*
 * Copyright (c) 2026 Nicolas Baumann (@nbauma109), with Claude Sonnet 5.5.
 * This project is distributed under the GPLv3 license.
 * This is a Copyleft license that gives the user the right to use,
 * copy and modify the code freely for non-commercial purposes.
 */

package org.jd.core.v1.stub;

/**
 * Minimal reproduction of a switch-enum inside another switch-enum, with generic locals declared in a single statement
 * (the shape of Guava 12's BstMutationResult.lift, which this fixture replaces).
 */
public class NestedEnumSwitch<N extends NestedEnumSwitch.Node<N>> {

    public interface Node<N> {
        N childOrNull(NestedEnumSwitchSide side);

        N create(N left, N right);
    }

    private N changedRoot;
    private NestedEnumSwitchKind kind;

    public NestedEnumSwitch<N> lift(N originalRoot, NestedEnumSwitchSide side) {
        switch (kind) {
            case IDENTITY:
                this.changedRoot = originalRoot;
                return this;
            case REBUILDING_CHANGE:
            case REBALANCING_CHANGE:
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
                this.changedRoot = originalRoot.create(resultLeft, resultRight);
                return this;
            default:
                throw new AssertionError();
        }
    }
}
