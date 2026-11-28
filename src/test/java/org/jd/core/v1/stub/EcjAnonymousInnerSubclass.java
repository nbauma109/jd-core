/*
 * Copyright (c) 2026 Nicolas Baumann (@nbauma109), with Claude Sonnet 5.5.
 * This project is distributed under the GPLv3 license.
 * This is a copyleft license that gives the user the right to use,
 * copy and modify the code freely for non-commercial purposes.
 */

package org.jd.core.v1.stub;

/**
 * Anonymous classes which extend an inner class: the constructor which the Eclipse compiler generates takes two enclosing instances
 */
public class EcjAnonymousInnerSubclass {
    public class Inner {
        int value;
        String[] parts;

        public Inner(int value, String... parts) {
            this.value = value;
            this.parts = parts;
        }

        public int run() {
            return value + parts.length;
        }
    }

    int capturing(final int added, String part) {
        Inner anonymous = new Inner(added, part) {
            @Override
            public int run() {
                return super.run() + added;
            }
        };
        return anonymous.run();
    }

    int plain(int value) {
        return new Inner(value) {
        }.run();
    }

    int qualified(EcjAnonymousInnerSubclass other, int value) {
        return other.new Inner(value) {
        }.run();
    }
}
