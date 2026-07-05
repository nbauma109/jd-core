package org.jd.core.v1.stub;

import java.io.IOException;

@SuppressWarnings("all")
public class ExceptionBoundViolation {

    public interface Marker {
    }

    static <T extends IOException & Marker> void multiBound() throws T {
    }

    static <T extends IOException> void ioBound() throws T {
    }

    static <E extends Throwable> void caught() {
        try {
            ioBound();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    static <E extends IOException> void violated() throws E {
        try {
            multiBound();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
