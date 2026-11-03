/*
 * Copyright (c) 2026 Nicolas Baumann (@nbauma109), with Claude Sonnet 5.5.
 * This project is distributed under the GPLv3 license.
 * This is a Copyleft license that gives the user the right to use,
 * copy and modify the code freely for non-commercial purposes.
 */

package org.jd.core.v1.stub;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/** A type which is a 'List&lt;IOException&gt;' without type arguments of its own gives its exception to a generic call, which a raw cast of the decompiler would not */
public class ConcreteContainerThrows {

    static final class IoFailures extends ArrayList<IOException> {
        private static final long serialVersionUID = 1L;
    }

    static <E extends Exception> void raiseFrom(List<E> failures) throws E {
        // throws one of them
    }

    public int genericThrowsOfAConcreteContainer(IoFailures failures) {
        try (var concrete = new AutoCloseable() {
            @Override
            public void close() {
                // closes nothing
            }
        }) {
            raiseFrom(failures);
            return 1;
        } catch (IOException e) {
            return 2;
        }
    }
}
