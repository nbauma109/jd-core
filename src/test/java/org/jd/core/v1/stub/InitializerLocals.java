/*
 * Copyright (c) 2026 Nicolas Baumann (@nbauma109), with Claude Sonnet 5.5.
 * This project is distributed under the GPLv3 license.
 * This is a Copyleft license that gives the user the right to use,
 * copy and modify the code freely for non-commercial purposes.
 */

package org.jd.core.v1.stub;

import java.util.function.Supplier;

/** Local classes of initializers, created directly or only by a lambda */
public class InitializerLocals {
    static Object created;
    Object instanceCreated;

    static {
        class OfTheStaticInitializer {
            OfTheStaticInitializer(String first) {
            }
        }
        Supplier<Object> factory = () -> new OfTheStaticInitializer("value");
        created = factory.get();
    }

    {
        class OfTheInstanceInitializer {
            OfTheInstanceInitializer(String first) {
            }
        }
        instanceCreated = new OfTheInstanceInitializer("value");
    }
}
