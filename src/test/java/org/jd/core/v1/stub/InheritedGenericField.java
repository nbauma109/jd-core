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
