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
import org.jd.core.v1.printer.PlainTextPrinter;
import org.junit.Test;

/**
 * 'dup; astore x; instanceof' is the way ECJ keeps the value which a pattern tests in a variable of its own, and the way javac assigns the variable of the
 * source in '(x = field) instanceof String': the assignment is removed only for the first.
 */
public class InstanceOfAssignedLocalTest extends AbstractJdTest {

    private String decompile() throws Exception {
        String internalClassName = InstanceOfAssignedLocal.class.getName().replace('.', '/');
        String printed = decompileSuccess(new ClassPathLoader(), new PlainTextPrinter(), internalClassName);

        assertTrue(printed, CompilerUtil.compile("17", new InMemoryJavaSourceFileObject(internalClassName, printed)));
        return printed.replaceAll("/\\*[^*]*\\*/", "").replaceAll("\\s+", " ");
    }

    @Test
    public void testTheVariableWhichTheCodeUsesBeforeTheTestStaysDeclared() throws Exception {
        String source = decompile();

        assertTrue(source, source.contains("Object x = new Object(); consume(x); if ((x = this.field) instanceof String) {"));
    }

    @Test
    public void testTheVariableWhichTheCodeReadsAfterTheTestIsAssigned() throws Exception {
        String source = decompile();

        assertTrue(source, source.contains("if ((x = this.field) instanceof String) { String s = (String)x; consume(s); } return x.hashCode();"));
    }

    @Test
    public void testTheTestOfAValueWhichHasTheTypeOfThePatternStays() throws Exception {
        String source = decompile();

        // 'this.text instanceof String s' is no legal pattern before Java 21: the type of the value which survives is the one of the field, not the one of the variable
        assertTrue(source, source.contains("if ((x = this.text) instanceof String) { String s = (String)x;"));
        assertFalse(source, source.contains("this.text instanceof String s"));
    }
}
