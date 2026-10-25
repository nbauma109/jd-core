/*
 * Copyright (c) 2026 Nicolas Baumann (@nbauma109), with Claude Sonnet 5.5.
 * This project is distributed under the GPLv3 license.
 * This is a Copyleft license that gives the user the right to use,
 * copy and modify the code freely for non-commercial purposes.
 */

package org.jd.core.v1.stub;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

/** A 'try ... finally' around an infinite loop whose 'catch' sets the flag the 'finally' tests: the shape of Guava's Uninterruptibles.getUninterruptibly */
public class FinallyAroundALoop {
    public static <V> V getUninterruptibly(Future<V> future) throws ExecutionException {
        boolean interrupted = false;

        try {
            while (true) {
                try {
                    return future.get();
                } catch (InterruptedException e) {
                    interrupted = true;
                }
            }
        } finally {
            if (interrupted) {
                Thread.currentThread().interrupt();
            }
        }
    }

    /** The shape of Guava's Monitor.enterWhenUninterruptibly with a timeout: the 'finally' releases the lock if the guard was not satisfied */
    public static boolean enterWhenSatisfied(Future<Boolean> guard, Future<Boolean> other, java.util.concurrent.locks.Lock lock, boolean fair, long timeoutNanos)
            throws ExecutionException {
        boolean interrupted = Thread.interrupted();

        try {
            if (fair || !lock.tryLock()) {
                long remainingNanos = timeoutNanos;

                while (true) {
                    try {
                        if (lock.tryLock(remainingNanos, java.util.concurrent.TimeUnit.NANOSECONDS)) {
                            break;
                        }
                        return false;
                    } catch (InterruptedException interrupt) {
                        interrupted = true;
                        remainingNanos = remainingNanos - 1;
                    }
                }
            }
            boolean satisfied = false;

            try {
                while (true) {
                    try {
                        if (guard.get()) {
                            satisfied = true;
                        } else {
                            satisfied = other.get();
                        }
                        return satisfied;
                    } catch (InterruptedException interrupt) {
                        interrupted = true;
                    }
                }
            } finally {
                if (!satisfied) {
                    lock.unlock();
                }
            }
        } finally {
            if (interrupted) {
                Thread.currentThread().interrupt();
            }
        }
    }
}
