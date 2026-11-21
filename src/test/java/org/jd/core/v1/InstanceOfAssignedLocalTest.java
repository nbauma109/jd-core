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

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 'dup; astore x; instanceof' is the way ECJ keeps the value which a pattern tests in a variable of its own, and the way javac assigns the variable of the
 * source in '(x = field) instanceof String': the assignment is removed only for the first.
 */
public class InstanceOfAssignedLocalTest extends AbstractJdTest {

    private String decompile() throws Exception {
        String internalClassName = InstanceOfAssignedLocal.class.getName().replace('.', '/');
        String printed = decompileSuccess(new ClassPathLoader(), new NoLineNumberPrinter(), internalClassName);

        assertTrue(printed, CompilerUtil.compile("17", new InMemoryJavaSourceFileObject(internalClassName, printed)));
        return printed.replaceAll("\\s+", " ");
    }

    /** What the source of each method of the fixture says, by the case which it is */
    private static final Map<String, String> EXPECTED = new LinkedHashMap<>();

    static {
        EXPECTED.put("the variable which the code uses before the test stays declared",
                "Object x = new Object(); consume(x); if ((x = this.field) instanceof String) {");
        EXPECTED.put("the variable which the code reads after the test is assigned",
                "if ((x = this.field) instanceof String) { String s = (String)x; consume(s); } return x.hashCode();");
        EXPECTED.put("the test of a value which has the type of the pattern stays: 'this.text instanceof String s' is no legal pattern before Java 21",
                "if ((x = this.text) instanceof String) { String s = (String)x;");
        EXPECTED.put("the result of a generic call has another type for the source: 'strings.get(0)' is a String, an Object for the byte code",
                "if ((x = this.strings.get(0)) instanceof String) { String s = (String)x;");
        EXPECTED.put("the object which a constructor initializes is the value of the test",
                "new Object() instanceof String s");
        EXPECTED.put("a store which a condition may not execute does not hide the read after it",
                "if ((x = this.field) instanceof String) { String s = (String)x; consume(s); } if (flag) x = new Object(); return x;");
    }

    @Test
    public void testTheAssignmentOfATestIsRemovedOnlyWhenItsVariableIsNotNeeded() throws Exception {
        String source = decompile();

        for (Map.Entry<String, String> expected : EXPECTED.entrySet()) {
            assertTrue(expected.getKey() + ":\n" + source, source.contains(expected.getValue()));
        }
        assertFalse(source, source.contains("this.text instanceof String s"));
    }
}
