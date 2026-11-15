/*
 * Copyright (c) 2026 Nicolas Baumann (@nbauma109), with Claude Sonnet 5.5.
 * This project is distributed under the GPLv3 license.
 * This is a copyleft license that gives the user the right to use,
 * copy and modify the code freely for non-commercial purposes.
 */

package org.jd.core.v1;

import org.jd.core.v1.compiler.CompilerUtil;
import org.jd.core.v1.compiler.InMemoryJavaSourceFileObject;
import org.jd.core.v1.printer.PlainTextPrinter;
import org.junit.Test;

import java.util.Map;

/** 'ZipArchiveInputStream.readFirstLocalFileHeader' of Commons Compress: a labeled break out of a loop with an inner loop, in a 'try' */
public class LabeledBreakOutOfNestedLoopsInTryTest extends AbstractJdTest {

    private static final String INTERNAL_CLASS_NAME = "labeled/Fixture";

    private static final String SOURCE = String.join("\n",
            "package labeled;",
            "import java.io.EOFException;",
            "import java.io.IOException;",
            "public class Fixture {",
            "    private final byte[] buffer = new byte[30];",
            "    private boolean signature(byte[] header, int j) { return header[j] == 1; }",
            "    private boolean end(byte[] header, int j) { return header[j] == 2; }",
            "    private void readFully(byte[] bytes, int from) throws IOException { }",
            "    boolean noTry(byte[] header) throws IOException {",
            "        READ_LOOP: for (int i = 0; ; ) {",
            "            for (int j = 0; i <= 4000 && j <= header.length - 4; ++j, ++i) {",
            "                if (signature(header, j)) {",
            "                    System.arraycopy(header, j, header, 0, header.length - j);",
            "                    readFully(header, header.length - j);",
            "                    break READ_LOOP;",
            "                }",
            "                if (end(header, j)) {",
            "                    return false;",
            "                }",
            "            }",
            "            if (i >= 4000) {",
            "                throw new IOException(\"no signature\");",
            "            }",
            "            System.arraycopy(header, header.length - 3, header, 0, 3);",
            "            readFully(header, 3);",
            "        }",
            "        System.arraycopy(header, 0, buffer, 0, header.length);",
            "        readFully(buffer, header.length);",
            "        return true;",
            "    }",
            "    boolean read(byte[] header) throws IOException {",
            "        try {",
            "            READ_LOOP: for (int i = 0; ; ) {",
            "                for (int j = 0; i <= 4000 && j <= header.length - 4; ++j, ++i) {",
            "                    if (signature(header, j)) {",
            "                        System.arraycopy(header, j, header, 0, header.length - j);",
            "                        readFully(header, header.length - j);",
            "                        break READ_LOOP;",
            "                    }",
            "                    if (end(header, j)) {",
            "                        return false;",
            "                    }",
            "                }",
            "                if (i >= 4000) {",
            "                    throw new IOException(\"no signature\");",
            "                }",
            "                System.arraycopy(header, header.length - 3, header, 0, 3);",
            "                readFully(header, 3);",
            "            }",
            "            System.arraycopy(header, 0, buffer, 0, header.length);",
            "            readFully(buffer, header.length);",
            "        } catch (final EOFException ex) {",
            "            throw new IOException(\"no signature in the file\");",
            "        }",
            "        return true;",
            "    }",
            "}");

    @Test
    public void testLabeledBreakOutOfNestedLoops() throws Exception {
        String source = decompileSuccess(RestoreMethodReferenceTest.compileForJava8("-g", Map.of(INTERNAL_CLASS_NAME, SOURCE)), new PlainTextPrinter(), INTERNAL_CLASS_NAME);

        // The loop which is left from the inner loop is left with a 'break': the code after it is written once, after the loop
        assertEquals(2, source.split("break label", -1).length - 1);
        assertEquals(2, source.split("System.arraycopy\\(header, 0, this.buffer, 0, header.length\\);", -1).length - 1);
        assertTrue(CompilerUtil.compile("17", new InMemoryJavaSourceFileObject(INTERNAL_CLASS_NAME, source)));
    }
}
