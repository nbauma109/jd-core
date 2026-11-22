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

    /** the index is computed from another array: the elements are not the ones of the array which the index reads */
    Node computed(Node[] values, int[] indices) {
        return values[indices.length] instanceof Wrapper wrapper ? wrapper.wrapped : values[0];
    }

    /** an array of a type variable: the bounds of the variable are lost with the erasure of the array */
    <T extends Node & Runnable> void bounded(T[] values, int i) {
        Object temporary;
        if ((temporary = values[i]) instanceof Runnable) {
            Runnable runnable = (Runnable) temporary;
            runnable.run();
        }
    }
}
