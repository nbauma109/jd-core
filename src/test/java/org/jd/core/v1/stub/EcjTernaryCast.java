/*
 * Copyright (c) 2026 Nicolas Baumann (@nbauma109), with Claude Sonnet 5.5.
 * This project is distributed under the GPLv3 license.
 * This is a copyleft license that gives the user the right to use,
 * copy and modify the code freely for non-commercial purposes.
 */

package org.jd.core.v1.stub;

/**
 * 'CastExpression.checkAlternateBinding' of the Eclipse compiler: an element is tested and cast in a ternary, in a loop
 */
public class EcjTernaryCast {
    static class Node {
        Node inner;
    }

    static class Wrapper extends Node {
        Node wrapped;
    }

    /** the index is a constant */
    Node firstWrapped(Node[] arguments) {
        return arguments[0] instanceof Wrapper wrapper ? wrapper.wrapped : arguments[0];
    }

    Node[] unwrap(Node[] arguments) {
        int count = arguments == null ? 0 : arguments.length;
        Node[] result = new Node[count];
        for (int i = 0; i < count; i++) {
            result[i] = arguments[i] instanceof Wrapper wrapper ? wrapper.wrapped : arguments[i];
        }
        return result;
    }
}
