/*
 * Copyright (c) 2026 Nicolas Baumann (@nbauma109), with Claude Sonnet 5.5.
 * This project is distributed under the GPLv3 license.
 * This is a copyleft license that gives the user the right to use,
 * copy and modify the code freely for non-commercial purposes.
 */

package org.jd.core.v1;

import org.jd.core.v1.compiler.CompilerUtil;
import org.jd.core.v1.compiler.InMemoryJavaSourceFileObject;
import org.jd.core.v1.loader.ZipLoader;
import org.jd.core.v1.printer.NoLineNumberPrinter;
import org.junit.Test;

import java.io.InputStream;

public class EcjGenericRecordTest extends AbstractJdTest {

    /** the components of a record have the generic types of their signature */
    @Test
    public void testComponentsOfAGenericRecord() throws Exception {
        String printed;

        try (InputStream is = getClass().getResourceAsStream("/jar/ecj-generic-record-ecj-17.jar")) {
            printed = decompileSuccess(new ZipLoader(is), new NoLineNumberPrinter(), "org/jd/core/v1/stub/EcjGenericRecord");
        }

        String source = printed.replaceAll("\\s+", " ");

        assertTrue(printed, source.contains("record Pair<T extends CharSequence>(T left, T right)"));
        assertTrue(printed, source.contains("Map<K, V> values, List<? extends K>[] keys, K[] array)"));
        assertTrue(printed, CompilerUtil.compile("17", new InMemoryJavaSourceFileObject("org/jd/core/v1/stub/EcjGenericRecord", printed)));
    }
}
