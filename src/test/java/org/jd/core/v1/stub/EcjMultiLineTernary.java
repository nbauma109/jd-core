/*
 * Copyright (c) 2026 Nicolas Baumann (@nbauma109), with Claude Sonnet 5.5.
 * This project is distributed under the GPLv3 license.
 * This is a copyleft license that gives the user the right to use,
 * copy and modify the code freely for non-commercial purposes.
 */

package org.jd.core.v1.stub;

/**
 * 'Parser.consumeAssignment' of the Eclipse compiler: a ternary which creates objects, written on several lines
 */
public class EcjMultiLineTernary {
    static class Node {
        Node left;
        int end;

        Node(Node left, int end) {
            this.left = left;
            this.end = end;
        }
    }

    static class Compound extends Node {
        int operator;

        Compound(Node left, int operator, int end) {
            super(left, end);
            this.operator = operator;
        }
    }

    Node[] stack = new Node[10];
    int pointer;
    Node recovered;

    void consume(int operator) {
        Node expression = this.stack[this.pointer + 1];
        this.stack[this.pointer] =
            (operator == 30) ?
                new Node(
                    this.stack[this.pointer],
                    expression.end):
                new Compound(
                    this.stack[this.pointer],
                    operator,
                    expression.end);

        if (this.recovered != null) {
            if (this.recovered.end > 0 &&
                    this.pointer - 1 <= this.recovered.end) {
                this.stack[this.pointer] = this.recovered;
                this.recovered = null;
                return;
            }
            this.recovered = null;
        }
    }
}
