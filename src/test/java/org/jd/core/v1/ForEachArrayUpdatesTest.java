/*
 * Copyright (c) 2026 Nicolas Baumann (@nbauma109).
 * This project is distributed under the GPLv3 license.
 * This is a Copyleft license that gives the user the right to use,
 * copy and modify the code freely for non-commercial purposes.
 */

package org.jd.core.v1;

import org.jd.core.v1.api.loader.Loader;
import org.jd.core.v1.loader.ClassPathLoader;
import org.jd.core.v1.loader.ZipLoader;
import org.jd.core.v1.printer.PlainTextPrinter;
import org.junit.Test;

import java.io.InputStream;
import java.util.Collections;
import java.util.Map;

/**
 * The loops on arrays are rebuilt as 'for (T t : array)' whatever the compiler: ECJ evaluates the array in the
 * condition ('(array$ = array).length'), and the update of the index is only copied before each 'continue' in a loop
 * whose body never falls through.
 */
public class ForEachArrayUpdatesTest extends AbstractJdTest {
    private static final String INTERNAL_NAME = "org/jd/core/v1/stub/ForEachArrayUpdates";
    private static final Map<String, Object> REALIGN = Collections.singletonMap("realignLineNumbers", Boolean.TRUE);

    private String decompileJavac() throws Exception {
        return decompileSuccess(new ClassPathLoader(), new PlainTextPrinter(), INTERNAL_NAME, REALIGN);
    }

    private String decompileEcj() throws Exception {
        try (InputStream is = this.getClass().getResourceAsStream("/jar/foreach-array-updates-ecj-8.jar")) {
            Loader loader = new ZipLoader(is);
            return decompileSuccess(loader, new PlainTextPrinter(), INTERNAL_NAME, REALIGN);
        }
    }

    private static void assertRebuilt(String source) {
        assertTrue(source, source.contains("for (int[] range : ranges) {"));
        assertTrue(source, source.contains("for (String field : fields) {"));
        assertTrue(source, source.contains("for (String[] row : rows) {"));
        assertTrue(source, source.contains("for (String c : row) {"));
        // The updates of the indexes are the ones of the loops
        assertFalse(source, source.contains("++; continue;"));
        assertFalse(source, source.contains("arrayOf"));
        // The jump to the update of the outer loop
        assertTrue(source, source.contains("continue label"));
        assertFalse(source, source.contains("throw null"));
    }

    @Test
    public void testJavac() throws Exception {
        assertRebuilt(decompileJavac());
    }

    @Test
    public void testEcj() throws Exception {
        assertRebuilt(decompileEcj());
    }
}
