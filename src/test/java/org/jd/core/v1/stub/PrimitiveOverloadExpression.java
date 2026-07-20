/*
 * Copyright (c) 2008, 2019 Emmanuel Dupuy.
 * This project is distributed under the GPLv3 license.
 * This is a Copyleft license that gives the user the right to use,
 * copy and modify the code freely for non-commercial purposes.
 */
package org.jd.core.v1.stub;

public class PrimitiveOverloadExpression {
    @SuppressWarnings({"java:S1172", "unused"})
    private static void consume(int value) {
        // Overload target only: the body is irrelevant to the decompilation fixture.
    }

    @SuppressWarnings({"java:S1144", "java:S1172", "unused"})
    private static void consume(Integer value) {
        // Boxed overload only exists to make the primitive call ambiguous when unboxed.
    }

    public void consumeSum(int left, int right) {
        consume(left + right);
    }
}
