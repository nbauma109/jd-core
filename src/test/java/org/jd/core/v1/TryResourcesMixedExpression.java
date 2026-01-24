package org.jd.core.v1;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

public class TryResourcesMixedExpression {
    void run(InputStream in) throws Exception {
        InputStream wrapped = new ByteArrayInputStream(new byte[0]);
        try (in; InputStream is = wrapped) {
            in.read();
            is.read();
        }
    }
}
