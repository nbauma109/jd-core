/*
 * Copyright (c) 2026 Nicolas Baumann (@nbauma109), with Claude Sonnet 5.5.
 * This project is distributed under the GPLv3 license.
 * This is a Copyleft license that gives the user the right to use,
 * copy and modify the code freely for non-commercial purposes.
 */

package org.jd.core.v1.stub;

/** A 'synchronized' block which returns, in a 'try' with a 'finally': the compiler copies the 'finally' before the 'return', inside the block */
public class SynchronizedReturns {
    private int misses;

    private Object load(Object value) {
        return value;
    }

    private Object await(Object value) {
        return value;
    }

    /** The shape of Guava's LocalCache.Segment.lockedGetOrLoad */
    public Object get(boolean create, Object entry) {
        if (create) {
            try {
                synchronized (entry) {
                    return load(entry);
                }
            } finally {
                misses++;
            }
        } else {
            return await(entry);
        }
    }

    public Object getAfterAStatement(Object entry) {
        try {
            synchronized (entry) {
                misses += 2;
                return load(entry);
            }
        } finally {
            misses++;
        }
    }

    /** The copy of this 'finally' is a synchronized block which completes normally: it is removed, not taken for a returning block */
    public void cleanedUpUnderALock(Object lock) {
        try {
            misses += 3;
        } finally {
            synchronized (lock) {
                misses--;
            }
        }
    }

    /** The copy of this 'finally' is an empty synchronized block */
    @SuppressWarnings("java:S108")
    public void emptyUnderALock(Object lock) {
        try {
            misses += 4;
        } finally {
            synchronized (lock) {
                // nothing: the block is only a lock
            }
        }
    }

    /** The copy of this 'finally' is a synchronized block which returns: it is the copy, the call is made once */
    @SuppressWarnings("finally")
    public Object returnsInTheFinally(Object lock) {
        try {
            misses += 6;
        } finally {
            synchronized (lock) {
                return await(lock);
            }
        }
    }
}
