/*
 * Copyright (c) 2026 Nicolas Baumann (@nbauma109), with Claude Sonnet 5.5.
 * This project is distributed under the GPLv3 license.
 * This is a copyleft license that gives the user the right to use,
 * copy and modify the code freely for non-commercial purposes.
 */

package org.jd.core.v1.stub;

import java.lang.invoke.MethodHandle;

/**
 * 'JavaWriter.beginControlFlow' of JavaWriter: an empty array passed to a variable arity method which has an overload without it
 */
public class EmptyVarArgsOverload {
    final String text;

    EmptyVarArgsOverload(String text) {
        this(text, new Object[0]);
    }

    EmptyVarArgsOverload(String text, Object... args) {
        this.text = String.format(text, args);
    }

    String begin(String flow) {
        return begin(flow, new Object[0]);
    }

    String begin(String flow, Object... args) {
        return String.format(flow, args);
    }

    static String none() {
        return none(new Object[0]);
    }

    static String none(Object... args) {
        return "none " + args.length;
    }

    String other(String flow) {
        return begin(flow, new Object[0]) + none(new Object[0]) + new EmptyVarArgsOverload(flow, new Object[0]).text;
    }

    String only(String flow) {
        return join(flow, new Object[0]);
    }

    String join(String flow, Object... args) {
        return flow + args.length;
    }

    /** a polymorphic signature without arguments, and an empty array without an overload */
    Object invoke(MethodHandle handle) throws Throwable {
        return handle.invoke() + last(new Object[0]);
    }

    String sized() {
        return last(new Object[2]);
    }

    String last(Object... args) {
        return "" + args.length;
    }

    /** the behaviour which the decompiled class has to keep */
    public static String run() {
        EmptyVarArgsOverload overload = new EmptyVarArgsOverload("a");

        return overload.text + overload.begin("b") + none() + overload.other("c") + overload.only("d");
    }
}
