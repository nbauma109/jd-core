package org.jd.core.v1.stub;

import java.util.Iterator;
import java.util.Map;

/** Shapes which made Guava 33.7.2 decompile to code which did not print or did not recompile. */
@SuppressWarnings({"unchecked", "rawtypes"})
public class GuavaPatterns {
    int modCount;

    /** A class declared in an anonymous class writes a field of the anonymous class */
    public Iterator<String> anonymousWithInnerClass() {
        return new Iterator<String>() {
            int expectedModCount = modCount;
            String toRemove;

            class Entry {
                String node = "a";

                String setValue(String value) {
                    expectedModCount = modCount;
                    if (toRemove == node) {
                        toRemove = value;
                    }
                    return node;
                }
            }

            @Override
            public boolean hasNext() {
                return new Entry().setValue("b") != null;
            }

            @Override
            public String next() {
                return toRemove;
            }
        };
    }

    /** An anonymous class declared in an anonymous class calls a method of the outer one */
    public Iterable<String> anonymousInAnonymous() {
        return new Iterable<String>() {
            boolean compatible(Object value) {
                return value != null;
            }

            @Override
            public Iterator<String> iterator() {
                return new Iterator<String>() {
                    @Override
                    public boolean hasNext() {
                        return compatible("x") && compatible(this);
                    }

                    @Override
                    public String next() {
                        return "next";
                    }
                };
            }
        };
    }

    abstract static class Cut<C extends Comparable> implements Comparable<Cut<C>> {
        static <C extends Comparable> Cut<C> belowAll() {
            return null;
        }

        static <C extends Comparable> Cut<C> belowValue(C endpoint) {
            return null;
        }

        @Override
        public int compareTo(Cut<C> other) {
            return 0;
        }
    }

    static final class Bounds<C extends Comparable> {
        final Cut<C> lowerBound;
        final Cut<C> upperBound;

        private Bounds(Cut<C> lowerBound, Cut<C> upperBound) {
            this.lowerBound = lowerBound;
            this.upperBound = upperBound;
        }

        static <C extends Comparable<?>> Bounds<C> create(Cut<C> lowerBound, Cut<C> upperBound) {
            return new Bounds<C>(lowerBound, upperBound);
        }

        /** The type variable of a generic method which is bound to the bound of a type variable of the caller */
        public static <C extends Comparable<?>> Bounds<C> lessThan(C endpoint) {
            return create(Cut.belowAll(), Cut.belowValue(endpoint));
        }
    }

    /** A local class which creates instances of itself */
    public static <T> Iterator<T> localClassCreatingItself(T value) {
        class Local implements Iterator<T> {
            private final boolean last;

            Local(boolean last) {
                this.last = last;
            }

            Local trySplit() {
                return last ? null : new Local(true);
            }

            @Override
            public boolean hasNext() {
                return !last && value != null;
            }

            @Override
            public T next() {
                return value;
            }
        }
        return new Local(false).trySplit();
    }
}
