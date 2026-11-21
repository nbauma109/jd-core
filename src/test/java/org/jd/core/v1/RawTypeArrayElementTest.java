/*
 * Copyright (c) 2026 Nicolas Baumann (@nbauma109), with Claude Sonnet 5.5.
 * This project is distributed under the GPLv3 license.
 * This is a copyleft license that gives the user the right to use,
 * copy and modify the code freely for non-commercial purposes.
 */

package org.jd.core.v1;

import org.jd.core.v1.compiler.CompilerUtil;
import org.jd.core.v1.compiler.InMemoryJavaSourceFileObject;
import org.jd.core.v1.loader.ClassPathLoader;
import org.jd.core.v1.printer.NoLineNumberPrinter;
import org.junit.Test;

public class RawTypeArrayElementTest extends AbstractJdTest {

    /** 'SourceTypeBinding' of the Eclipse compiler: 'raw[1].get(key)' is an Object, which the code casts; the type variable of the raw type is not bound by the cast */
    @Test
    public void testTheCastOfTheResultOfAnElementOfAnArrayOfRawTypesIsKept() throws Exception {
        String internalClassName = RawTypeArrayElement.class.getName().replace('.', '/');
        String printed = decompileSuccess(new ClassPathLoader(), new NoLineNumberPrinter(), internalClassName);
        String source = printed.replaceAll("\\s+", " ");

        assertTrue(printed, source.contains("(String)this.synthetics[1].get(key)"));
        assertTrue(printed, source.contains("(String[])this.synthetics[0].get(key)"));
        assertTrue(printed, source.contains("(String)maps[0].get(key)"));
        assertTrue(printed, CompilerUtil.compile("17", new InMemoryJavaSourceFileObject(internalClassName, printed)));
    }
}
