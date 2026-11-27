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

public class EcjSortedEntriesTest extends AbstractJdTest {

    /** the raw array which a comparator of a generic type sorts is cast to the array of that type */
    @Test
    public void testRawArraySortedWithATypedComparator() throws Exception {
        String printed;

        try (InputStream is = getClass().getResourceAsStream("/jar/ecj-sorted-entries-ecj-17.jar")) {
            printed = decompileSuccess(new ZipLoader(is), new NoLineNumberPrinter(), "org/jd/core/v1/stub/EcjSortedEntries");
        }

        assertTrue(printed, printed.contains("Arrays.sort(entries, (Comparator)byText);"));
        // the array which has its type arguments is not cast
        assertTrue(printed, printed.contains("Arrays.sort(shared, BY_HASH);"));
        assertTrue(printed, CompilerUtil.compile("17", new InMemoryJavaSourceFileObject("org/jd/core/v1/stub/EcjSortedEntries", printed)));
    }
}
