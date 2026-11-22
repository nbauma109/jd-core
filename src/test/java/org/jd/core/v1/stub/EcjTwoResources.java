/*
 * Copyright (c) 2026 Nicolas Baumann (@nbauma109), with Claude Sonnet 5.5.
 * This project is distributed under the GPLv3 license.
 * This is a copyleft license that gives the user the right to use,
 * copy and modify the code freely for non-commercial purposes.
 */

package org.jd.core.v1.stub;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * 'ArchiveFileObject.openInputStream' of the Eclipse compiler: a try-with-resources of two resources, the second one is made from the first
 */
public class EcjTwoResources {
    static class Archive implements AutoCloseable {
        InputStream open(String name) throws IOException {
            return new ByteArrayInputStream(name.getBytes());
        }

        @Override
        public void close() {
            // nothing
        }
    }

    InputStream read(String name) throws IOException {
        try (Archive archive = new Archive();
                InputStream input = archive.open(name)) {
            ByteArrayInputStream buffer = new ByteArrayInputStream(input.readAllBytes());
            return buffer;
        }
    }
}
