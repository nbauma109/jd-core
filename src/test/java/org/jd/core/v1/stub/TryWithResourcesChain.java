package org.jd.core.v1.stub;

import java.io.IOException;
import java.io.StringReader;

public class TryWithResourcesChain {
    static int read(String text) throws IOException {
        try (StringReader reader = new StringReader(text.trim()
                .toLowerCase()
                .concat("x"))) {
            return reader.read();
        }
    }
}
