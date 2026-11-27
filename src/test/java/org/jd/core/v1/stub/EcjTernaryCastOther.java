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

    /** the array which an anonymous class captures is a copy of the variable, of the erasure of its type: the bounds of the type variable are lost */
    <T extends Node & Runnable> Runnable captured(T[] values) {
        return new Runnable() {
            @Override
            public void run() {
                Object temporary;
                if ((temporary = values[0]) instanceof Runnable) {
                    Runnable runnable = (Runnable) temporary;
                    runnable.run();
                }
            }
        };
    }

    /** an interface which the erasure of the captured array omits makes the test of an array of it unconditional */
    <T extends Node & Runnable> Runnable capturedArrays(T[][] values) {
        return new Runnable() {
            @Override
            public void run() {
                Object temporary;
                if ((temporary = values[0]) instanceof Runnable[]) {
                    Runnable[] runnables = (Runnable[]) temporary;
                    runnables[0].run();
                }
            }
        };
    }

    /** an array of references keeps its test: the hierarchy of the elements may be unknown */
    Runnable capturedClassArrays(final Node[][] values) {
        return new Runnable() {
            @Override
            public void run() {
                Object temporary;
                if ((temporary = values[0]) instanceof Wrapper[]) {
                    Wrapper[] wrappers = (Wrapper[]) temporary;
                    items = wrappers;
                }
            }
        };
    }
}
