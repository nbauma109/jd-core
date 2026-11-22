/*
 * Copyright (c) 2026 Nicolas Baumann (@nbauma109), with Claude Sonnet 5.5.
 * This project is distributed under the GPLv3 license.
 * This is a copyleft license that gives the user the right to use,
 * copy and modify the code freely for non-commercial purposes.
 */

package org.jd.core.v1.stub;

/**
 * 'IModuleAwareNameEnvironment.LookupStrategy' of the Eclipse compiler: the body of a constant of an enum, and an inner class, use an assert, whose
 * flag '$assertionsDisabled' is the one of the enum or of the outer class
 */
public enum EcjNestedAssert {
    Named {
        @Override
        public boolean matches(Object element, Object name) {
            assert name != null : "name match needs a name";
            return element.equals(name);
        }
    },
    Any {
        @Override
        public boolean matches(Object element, Object name) {
            return element != null;
        }
    };

    public abstract boolean matches(Object element, Object name);

    static class Checker {
        int check(Object value) {
            assert value != null;
            return value.hashCode();
        }
    }
}
