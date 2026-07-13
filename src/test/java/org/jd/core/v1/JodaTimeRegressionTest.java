/*
 * Copyright (c) 2008, 2019 Emmanuel Dupuy.
 * This project is distributed under the GPLv3 license.
 * This is a Copyleft license that gives the user the right to use,
 * copy and modify the code freely for non-commercial purposes.
 */
package org.jd.core.v1;

import org.jd.core.v1.loader.ClassPathLoader;
import org.jd.core.v1.compiler.CompilerUtil;
import org.jd.core.v1.compiler.InMemoryJavaSourceFileObject;
import org.jd.core.v1.printer.PlainTextPrinter;
import org.junit.Test;

import java.util.Collections;

public class JodaTimeRegressionTest extends AbstractJdTest {
    @Test
    public void testTimeZoneOffset() throws Exception {
        String internalName = "org/joda/time/format/DateTimeFormatterBuilder$TimeZoneOffset";
        String source = decompileSuccess(new ClassPathLoader(), new PlainTextPrinter(), internalName, Collections.emptyMap());
        assertFalse(source.contains("goto line number"));
        assertTrue(CompilerUtil.compile("17", new InMemoryJavaSourceFileObject(internalName, source)));
    }

    @Test
    public void testZoneInfoCompiler() throws Exception {
        String internalName = "org/joda/time/tz/ZoneInfoCompiler";
        String source = decompileSuccess(new ClassPathLoader(), new PlainTextPrinter(), internalName, Collections.emptyMap());
        assertTrue(source.contains("parseDataFile(bufferedReader"));
        assertTrue(CompilerUtil.compile("17", new InMemoryJavaSourceFileObject(internalName, source)));
    }

    @Test
    public void testInheritedToStringDispatch() throws Exception {
        String internalName = "org/joda/time/base/AbstractPartial";
        String source = decompileSuccess(new ClassPathLoader(), new PlainTextPrinter(), internalName, Collections.emptyMap());
        assertTrue(source.contains("return toString();"));
        assertFalse(source.contains("return super.toString();"));
    }

    @Test
    public void testProtectedDefaultConstructor() throws Exception {
        String internalName = "org/joda/time/convert/CalendarConverter";
        String source = decompileSuccess(new ClassPathLoader(), new PlainTextPrinter(), internalName, Collections.emptyMap());
        assertTrue(source.contains("protected CalendarConverter()"));
    }

    @Test
    public void testInstanceOfBeforeCast() throws Exception {
        String internalName = "org/joda/time/format/PeriodFormatterBuilder";
        String source = decompileSuccess(new ClassPathLoader(), new PlainTextPrinter(), internalName, Collections.emptyMap());
        assertFalse(source.contains("= (FieldFormatter)this.iElementPairs.get"));
        assertTrue(CompilerUtil.compile("17", new InMemoryJavaSourceFileObject(internalName, source)));
    }
}
