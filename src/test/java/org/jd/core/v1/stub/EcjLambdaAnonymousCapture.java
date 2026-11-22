/*
 * Copyright (c) 2026 Nicolas Baumann (@nbauma109), with Claude Sonnet 5.5.
 * This project is distributed under the GPLv3 license.
 * This is a copyleft license that gives the user the right to use,
 * copy and modify the code freely for non-commercial purposes.
 */

package org.jd.core.v1.stub;

import java.util.Iterator;
import java.util.List;

/**
 * 'EclipseFileManager.getFiles' of the Eclipse compiler: a lambda returns an anonymous class which captures a variable of the method
 */
public class EcjLambdaAnonymousCapture {
    abstract static class Counter {
        final int start;

        Counter(int start) {
            this.start = start;
        }

        abstract int next();
    }

    /** two variables which only the initializer reads, and an argument of the constructor of the superclass */
    static Counter counter(final int start, final int step, final String label) {
        return new Counter(start) {
            int current = start + step + label.length();

            @Override
            int next() {
                return this.current++;
            }
        };
    }

    static Iterable<String> names(final List<Integer> numbers) {
        if (numbers == null) {
            return null;
        }
        return () -> new Iterator<>() {
            Iterator<Integer> original = numbers.iterator();

            @Override
            public boolean hasNext() {
                return this.original.hasNext();
            }

            @Override
            public String next() {
                return String.valueOf(this.original.next());
            }
        };
    }
}
