/*
 * Copyright (c) 2026 Nicolas Baumann (@nbauma109), with Claude Sonnet 5.5.
 * This project is distributed under the GPLv3 license.
 * This is a Copyleft license that gives the user the right to use,
 * copy and modify the code freely for non-commercial purposes.
 */

package org.jd.core.v1.stub;

/** A field of a generic grandparent which an intermediate class hides: the owner of the access is cast with its type arguments */
public class InheritedGenericField {
    static class A<K, V> {
        V value;
    }

    static class B<K, V> extends A<K, V> {
        V value;
    }

    static class C<K, V> extends B<K, V> {
        V ofTheGrandparent() {
            return ((A<K, V>) this).value;
        }

        V ofTheParent() {
            return ((B<K, V>) this).value;
        }
    }
}
