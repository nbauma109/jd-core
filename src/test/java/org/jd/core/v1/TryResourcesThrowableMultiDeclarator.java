package org.jd.core.v1;

import java.io.InputStream;

public class TryResourcesThrowableMultiDeclarator {
    void run(InputStream in) throws Exception {
        Throwable t = null, u = null;
        try (InputStream is = in) {
            is.read();
        }
        if (u != null) {
            u.printStackTrace();
        }
    }
}
