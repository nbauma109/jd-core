/*
 * Copyright (c) 2026 GPLv3.
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
import java.util.regex.Pattern;

/**
 * Line number realignment of expressions spanning several lines. The class files come from ECJ, which records a
 * line number for nearly every sub-expression (javac only does it for statements and calls).
 */
public class MultiLineRealignTest extends AbstractJdTest {
    private static final String JAR = "/jar/multi-line-expressions-ecj-8.jar";
    private static final Map<String, Object> REALIGN = Collections.singletonMap("realignLineNumbers", Boolean.TRUE);

    private String decompileRealigned(String internalClassName) throws Exception {
        try (InputStream is = this.getClass().getResourceAsStream(JAR)) {
            Loader loader = new ZipLoader(is);
            return decompileSuccess(loader, new PlainTextPrinter(), internalClassName, REALIGN);
        }
    }

    /** Asserts that the given text is printed on the given line, and that this line matches the original line number. */
    private static void assertLine(String source, int lineNumber, String text) {
        Pattern pattern = Pattern.compile("(?m)^/\\*\\s*" + lineNumber + ":\\s*" + lineNumber + " \\*/\\s*" + Pattern.quote(text));

        assertTrue("Expected '" + text + "' on line " + lineNumber + " in:\n" + source, pattern.matcher(source).find());
    }

    @Test
    public void testReceiverOnItsOwnLine() throws Exception {
        String source = decompileRealigned("org/jd/core/v1/stub/MultiLineExpressions");

        // return this.helper
        //         .trim()
        //         .concat(suffix);
        assertLine(source, 9, "return this.helper");
        assertLine(source, 10, ".trim()");
        assertLine(source, 11, ".concat(suffix);");
    }

    @Test
    public void testArgumentsOnTheirOwnLines() throws Exception {
        String source = decompileRealigned("org/jd/core/v1/stub/MultiLineExpressions");

        assertLine(source, 15, "return format(");
        assertLine(source, 16, "a.trim(),");
        assertLine(source, 17, "this.helper.trim(),");
        assertLine(source, 18, "this.helper.length());");
    }

    @Test
    public void testDoWhileConditionDoesNotDisturbTheLayout() throws Exception {
        // ECJ attributes the condition of a 'do ... while' loop to the line of 'do', before the first statement of the body
        String source = decompileRealigned("org/jd/core/v1/stub/MultiLineExpressions");

        assertLine(source, 44, "if (x > 100) {");
        assertLine(source, 48, "n += this.count;");
        assertLine(source, 55, "return n;");
    }

    @Test
    public void testFinallyCopiesDoNotCollapseTheLayout() throws Exception {
        // ECJ inlines a copy of the 'finally' block, with the line of the 'finally' block, in the middle of the method.
        // This out-of-order line number must not hide the line numbers of the statements that follow.
        String source = decompileRealigned("org/jd/core/v1/stub/FinallyCopies");

        assertLine(source, 80, "this.count = 6;");
        assertLine(source, 84, "this.count = 7;");
        assertLine(source, 88, "this.count = 8;");
        assertLine(source, 90, "return false;");

        for (String line : source.split("\n")) {
            assertTrue("Statements were stacked on a single line: " + line, line.length() < 200);
        }
    }

    @Test
    public void testWrappedConditionKeepsTheCallOnItsOwnLineWithEcj() throws Exception {
        String source = decompileRealigned("org/jd/core/v1/stub/MultiLineExpressions");

        assertLine(source, 63, "return (values[i] instanceof String &&");
        assertLine(source, 64, "isBlank((String)values[i]));");
    }

    @Test
    public void testWrappedConditionKeepsTheCallOnItsOwnLineWithJavac() throws Exception {
        // javac only records a line number for statements and calls: the argument 'values[i]' merely inherits the line
        // of the previous entry and must not pull the call 'isBlank(...)' up onto the line of 'instanceof'
        String source = decompileSuccess(new ClassPathLoader(), new PlainTextPrinter(), "org/jd/core/v1/stub/MultiLineExpressions", REALIGN);

        assertLine(source, 64, "isBlank((String)values[i]));");
    }

    @Test
    public void testPackedSingleStatementMethodsKeepTheirHeaderOnTheLineAbove() throws Exception {
        // No blank line between the members in the source: the header is one line above the body, the previous member
        // ends on the line above the header
        String ecj = decompileRealigned("org/jd/core/v1/stub/MultiLineExpressions");

        assertLine(ecj, 72, "return 1;");
        assertLine(ecj, 75, "return 2;");
        assertTrue(Pattern.compile("(?m)^/\\*\\s*\\d+:\\s*0 \\*/\\s*int two\\(\\) \\{\\s*$").matcher(ecj).find());

        String javac = decompileSuccess(new ClassPathLoader(), new PlainTextPrinter(), "org/jd/core/v1/stub/MultiLineExpressions", REALIGN);

        assertLine(javac, 72, "return 1;");
        assertLine(javac, 75, "return 2;");
        assertTrue(Pattern.compile("(?m)^/\\*\\s*\\d+:\\s*0 \\*/\\s*int two\\(\\) \\{\\s*$").matcher(javac).find());
    }

    @Test
    public void testTryWithResourcesChainOfCallsOnSeveralLines() throws Exception {
        // The resource of the try is created with a chain of calls over three lines
        String source = decompileSuccess(new ClassPathLoader(), new PlainTextPrinter(), "org/jd/core/v1/stub/TryWithResourcesChain", REALIGN);

        assertLine(source, 8, "try (StringReader reader = new StringReader(text.trim()");
        assertLine(source, 9, ".toLowerCase()");
        assertLine(source, 10, ".concat(\"x\")))");
    }

    @Test
    public void testOneLinerConstructorKeepsItsHeaderOnTheLineOfItsBody() throws Exception {
        // The header of a constructor is not separated from its body: the source is often written on a single line
        String source = decompileSuccess(new ClassPathLoader(), new PlainTextPrinter(), "org/jd/core/v1/stub/OneLinerConstructor", REALIGN);

        assertLine(source, 7, "public OneLinerConstructor(int v) { this.name = String.valueOf(System.nanoTime()); this.value = v; }");
    }
}
