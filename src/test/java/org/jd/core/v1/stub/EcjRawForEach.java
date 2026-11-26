/*
 * Copyright (c) 2026 Nicolas Baumann (@nbauma109), with Claude Sonnet 5.5.
 * This project is distributed under the GPLv3 license.
 * This is a copyleft license that gives the user the right to use,
 * copy and modify the code freely for non-commercial purposes.
 */

package org.jd.core.v1.stub;

import java.util.ArrayList;

/**
 * 'FlowContext' of the Eclipse compiler: a loop over a raw list, whose elements are added after its declaration
 */
public class EcjRawForEach {
    static class Context {
        Context parent;
        int visits;
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    void visit(Context context, boolean exit) {
        ArrayList exited = null;

        while (context != null) {
            if (exit && exited != null) {
                for (Object loop : exited) {
                    ((Context) loop).visits++;
                }
            }
            if (context.visits > 0) {
                if (exited == null) {
                    exited = new ArrayList(5);
                }
                exited.add(context);
            }
            context = context.parent;
        }
    }
}
