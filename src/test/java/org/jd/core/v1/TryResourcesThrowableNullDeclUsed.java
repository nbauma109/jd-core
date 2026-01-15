package org.jd.core.v1;

import java.io.InputStream;

public class TryResourcesThrowableNullDeclUsed {
    void run(InputStream in) throws Exception {
        Throwable t = null;
        try (InputStream is = in) {
            is.read();
        }
        if (t != null) {
            t.printStackTrace();
        }
    }
}
