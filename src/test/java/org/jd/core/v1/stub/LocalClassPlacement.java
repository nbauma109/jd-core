/*
 * Copyright (c) 2026 Nicolas Baumann (@nbauma109), with Claude Sonnet 5.5.
 * This project is distributed under the GPLv3 license.
 * This is a Copyleft license that gives the user the right to use,
 * copy and modify the code freely for non-commercial purposes.
 */

package org.jd.core.v1.stub;

/** Local classes whose declaration must stay before every class and statement that needs it */
public class LocalClassPlacement {
    /** The class used in the signature of another local class (a field here) stays visible to it */
    public static Object itemInTheSignatureOfAnotherLocalClass(boolean flag) {
        class Item {
        }
        Object first = null;

        if (flag) {
            first = new Item();
        }
        class Holder {
            Item value;

            Item get(Item other) throws IllegalStateException {
                return value == null ? other : value;
            }
        }
        return first == null ? new Holder().get(null) : first;
    }

    /** The class used in the bound of the type parameter of another local class stays visible to it */
    public static Object itemInTheBoundOfAnotherLocalClass(boolean flag) {
        class Item {
        }
        Object first = null;

        if (flag) {
            first = new Item();
        }
        class Bounded<T extends Item> {
            T value;
        }
        return first == null ? new Bounded<Item>().value : first;
    }

    /** Local classes mentioned in every place a class can be: implements clauses, multi-catch, throws lists, type arguments and constructor references */
    public static Object mentionedEverywhere(boolean flag) throws Exception {
        class Problem extends Exception {
            private static final long serialVersionUID = 1L;
        }
        class Marker {
        }
        class Tagged implements java.io.Serializable, Comparable<Marker> {
            @Override
            public int compareTo(Marker other) {
                return 0;
            }

            void fail(Marker marker) throws Problem, java.io.IOException {
                if (marker == null) {
                    throw new Problem();
                }
                if (marker.hashCode() == 0) {
                    throw new java.io.IOException("none");
                }
            }
        }
        Object first = null;

        if (flag) {
            try {
                first = new Marker();
                new Tagged().fail((Marker) first);
            } catch (Problem | IllegalStateException e) {
                first = e;
            }
            java.util.List<? extends Marker> markers = new java.util.ArrayList<Marker>();
            java.util.List<? super Marker> sink = new java.util.ArrayList<Object>();
            java.util.function.Supplier<Marker> supplier = Marker::new;
            Marker[] array = {supplier.get()};

            sink.add(array[0]);
            first = markers.isEmpty() ? sink : first;
        }
        return first == null ? new Tagged() : first;
    }

    /** The class used in the declaration of a 'for' statement stays visible to it */
    public static Object itemInTheDeclarationOfAForStatement(boolean flag) {
        class Item {
        }
        Object first = null;

        if (flag) {
            first = new Item();
        }
        Object last = null;

        for (Item item = null; flag; flag = false) {
            last = item;
        }
        return first == null ? last : first;
    }

    static class Holder<T> {
    }

    /** The class is only in the type argument of the superclass of an anonymous class, after the block of its only instance */
    public static Object itemInTheTypeArgumentOfAnAnonymousClass(boolean flag) {
        class Item {
        }
        Object first = null;

        if (flag) {
            first = new Item();
        }
        return first == null ? new Holder<Item>() {
        } : first;
    }

    /** The class used in the resource of a 'try' stays visible to it */
    public static Object itemInTheResourceOfATry(boolean flag) {
        class Item implements AutoCloseable {
            @Override
            public void close() {
                // nothing to release
            }
        }
        Object first = null;

        if (flag) {
            first = new Item();
        }
        try (Item resource = null) {
            first = resource;
        }
        return first;
    }
}
