package org.jd.core.v1.stub;

import java.security.SecureRandom;
import java.util.Random;
import java.util.function.Supplier;

@SuppressWarnings("all")
public class ForwardStaticReference {

    private static final Supplier<Random> SECURE_STRONG_SUPPLIER = () -> ForwardStaticReference.SECURE_RANDOM_STRONG.get();

    private static final ThreadLocal<SecureRandom> SECURE_RANDOM_STRONG = ThreadLocal.withInitial(() -> {
        try {
            return SecureRandom.getInstanceStrong();
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    });
}
