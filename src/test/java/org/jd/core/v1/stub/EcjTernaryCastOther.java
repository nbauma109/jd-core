/*
 * Copyright (c) 2026 Nicolas Baumann (@nbauma109), with Claude Sonnet 5.5.
 * This project is distributed under the GPLv3 license.
 * This is a copyleft license that gives the user the right to use,
 * copy and modify the code freely for non-commercial purposes.
 */

package org.jd.core.v1.stub;

import java.util.List;

/**
 * The elements of a field and of a list, which a ternary tests and casts: the type of the elements is not the one of a local array, the test stays
 */
public class EcjTernaryCastOther {
    static class Node {
    }

    static class Wrapper extends Node {
        Node wrapped;
    }

    Node[] items = new Node[2];

    Node first(List<Node> nodes, int i) {
        Node field = this.items[i] instanceof Wrapper wrapper ? wrapper.wrapped : this.items[i];
        return nodes.get(i) instanceof Wrapper wrapper ? wrapper.wrapped : field;
    }
}
