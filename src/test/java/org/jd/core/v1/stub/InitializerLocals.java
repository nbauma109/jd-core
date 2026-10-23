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
