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
}
