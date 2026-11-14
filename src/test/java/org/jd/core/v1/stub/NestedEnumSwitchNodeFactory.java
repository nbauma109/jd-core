/*
 * Copyright (c) 2026 Nicolas Baumann (@nbauma109), with Claude Sonnet 5.5.
 * This project is distributed under the GPLv3 license.
 * This is a Copyleft license that gives the user the right to use,
 * copy and modify the code freely for non-commercial purposes.
 */

package org.jd.core.v1.stub;
/** Bounded by a node of any key, unlike the node type of NestedEnumSwitch: the bounds of the same-named variables differ */
public interface NestedEnumSwitchNodeFactory<N extends NestedEnumSwitchNode<?, N>> {
    N createNode(N source, N left, N right);
}
