/*
 * Copyright (c) 2026 Nicolas Baumann (@nbauma109), with Claude Sonnet 5.5.
 * This project is distributed under the GPLv3 license.
 * This is a copyleft license that gives the user the right to use,
 * copy and modify the code freely for non-commercial purposes.
 */

package org.jd.core.v1.stub;

/** 'InferenceContext18.pushBoundsTo' of the Eclipse compiler: the lambda uses 'this' and captures the variables of the method */
public class EcjLambdaCapture {

    boolean flag;
    StringBuilder inbox;

    void push(EcjLambdaCapture outer, String toPush, boolean copy) {
        Runnable job = () -> {
            if (outer.flag) {
                outer.inbox.append(toPush).append(this.flag);
            } else {
                outer.inbox = new StringBuilder(copy ? toPush : toPush.trim());
            }
        };
        job.run();
    }
}
