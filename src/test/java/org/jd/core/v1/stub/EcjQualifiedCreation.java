/*
 * Copyright (c) 2026 Nicolas Baumann (@nbauma109), with Claude Sonnet 5.5.
 * This project is distributed under the GPLv3 license.
 * This is a copyleft license that gives the user the right to use,
 * copy and modify the code freely for non-commercial purposes.
 */

package org.jd.core.v1.stub;

/**
 * 'ExternalAnnotationProvider.synthesizeForMethod' of the Eclipse compiler: an inner class created with an outer instance which a static field of a top level class holds
 */
public class EcjQualifiedCreation {
    private static final EcjQualifiedCreation OUTER = new EcjQualifiedCreation();

    class Walker {
        final String source;

        Walker(String source) {
            this.source = source;
        }

        Walker(Runnable action) {
            this.source = action.toString();
        }

        class Step {
        }
    }

    static Walker walk(String source) {
        return OUTER.new Walker(source);
    }

    /** the outer instance is of an inner class */
    static Object step(Walker walker) {
        return walker.new Step();
    }

    /** the check which the source makes precedes a creation whose argument is a lambda */
    static Object lambda(Object value, EcjQualifiedCreation outer) {
        value.getClass();
        return outer.new Walker(() -> {
        });
    }

    /** the check which the source makes, in a switch expression, runs while the creation is pending */
    Object pending(Object value, int selector) {
        return new Walker(switch (selector) {
            default -> {
                value.getClass();
                yield "x";
            }
        });
    }

    static int length(EcjQualifiedCreation outer, String source) {
        outer.getClass();
        return outer.new Walker(source).source.length();
    }
}
