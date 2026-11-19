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
 * Compiled with the Eclipse compiler (-17 -g) into src/test/resources/jar/ecj-try-with-resources-ecj-17.jar, which EcjTryWithResourcesTest decompiles: the
 * compiler of the JDK generates other code for a try-with-resources.
 */
public class EcjTryWithResources {

    private static byte[] read(InputStream in) throws IOException {
        return new byte[] { (byte) in.read() };
    }

    /** 'Parser.readTable' of the Eclipse compiler: the variable is assigned in the try-with-resources, and read after it */
    static byte[] readTable(String filename) throws IOException {
        byte[] bytes = null;
        try (InputStream stream = EcjTryWithResources.class.getResourceAsStream(filename)) {
            if (stream == null) {
                throw new IOException("missing " + filename);
            }
            bytes = read(stream);
        }
        return bytes;
    }

    /** 'EclipseFileManager.initialize' of the Eclipse compiler: the body of the try-with-resources is empty */
    static void closeAtOnce(InputStream stream) throws IOException {
        try (InputStream previous = stream) {
            // nothing
        }
    }

    /** The resource is created by the try-with-resources: the Eclipse compiler does not guard its close by a null check */
    static void createAndClose() throws IOException {
        try (InputStream created = new ByteArrayInputStream(new byte[0])) {
            // nothing
        }
    }

    /**
     * The same as 'createAndClose': the jar of the test is patched, the null check of the close (a 'aload_2; ifnull' of this method) is replaced by
     * 'nop's, as the Eclipse compiler does when it knows that the resource is not null
     */
    static void createAndCloseUnguarded() throws IOException {
        try (InputStream created = new ByteArrayInputStream(new byte[1])) {
            // nothing
        }
    }
}
