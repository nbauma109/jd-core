package org.jd.core.v1;

import java.io.InputStream;

public class TryResourcesThrowableNullDecl {
    void run(InputStream in) throws Exception {
        Throwable t = null;
        try (InputStream is = in) {
            is.read();
        }
    }
}
