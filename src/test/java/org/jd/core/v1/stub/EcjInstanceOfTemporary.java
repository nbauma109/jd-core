/*
 * Copyright (c) 2026 Nicolas Baumann (@nbauma109), with Claude Sonnet 5.5.
 * This project is distributed under the GPLv3 license.
 * This is a copyleft license that gives the user the right to use,
 * copy and modify the code freely for non-commercial purposes.
 */

package org.jd.core.v1.stub;

import java.util.function.Supplier;

/**
 * Compiled with the Eclipse compiler (-17 -g) into src/test/resources/jar/ecj-instanceof-temporary-ecj-17.jar, which EcjInstanceOfTemporaryTest decompiles:
 * the compiler of the JDK does not keep the tested value in a variable ('dup; astore').
 */
public class EcjInstanceOfTemporary {

    interface Context {
    }

    static class Method implements Context {
        int bodyStart = 3;
    }

    static class Type implements Context {
        String[] fields = {"a"};
    }

    Context context;
    int last;

    /** 'Parser.buildInitialRecoveryState' of the Eclipse compiler: the field which is tested is kept in a variable, which only the pattern reads */
    Object build() {
        Object element = null;
        if (this.context instanceof Method method) {
            this.last = method.bodyStart;
            element = method;
        } else {
            if (this.context instanceof Type type) {
                for (int i = 0; i < type.fields.length; i++) {
                    this.last += type.fields[i].length();
                }
            }
        }
        return element;
    }

    static Context shared;
    Supplier<Object> supplier;

    Context provide() {
        return this.context;
    }

    static Context make() {
        return shared;
    }

    /** The tested value is a static field, the result of a call of an instance, of a static method, of an interface: the type of the value is the one of the member */
    Object members() {
        if (shared instanceof Method method) {
            return method;
        }
        if (provide() instanceof Type type) {
            return type;
        }
        if (make() instanceof Method method) {
            return method.bodyStart;
        }
        if (this.supplier.get() instanceof Type type) {
            return type.fields;
        }
        return null;
    }
}
