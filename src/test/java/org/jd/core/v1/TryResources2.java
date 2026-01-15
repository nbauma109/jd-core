package org.jd.core.v1;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

public abstract class TryResources2 {
    void copyInputStreamToFile(InputStream source, File destination) throws IOException {
        try (InputStream inputStream = source) {
            copyToFile(inputStream, destination);
        }
    }

    abstract void copyToFile(InputStream inputStream, File destination);
}
