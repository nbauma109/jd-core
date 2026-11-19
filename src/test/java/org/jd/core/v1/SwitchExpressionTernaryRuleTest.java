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

public class SwitchExpressionTernaryRuleTest extends AbstractJdTest {

    /** 'SwitchStatement.typeSwitchSignature' of the Eclipse compiler: the branches of a ternary operator are the results of the rules, not returned values */
    @Test
    public void testTernaryOperatorsAsTheRulesOfASwitchExpression() throws Exception {
        String internalClassName = SwitchExpressionTernaryRule.class.getName().replace('.', '/');
        String printed = decompileSuccess(new ClassPathLoader(), new PlainTextPrinter(), internalClassName);
        String source = printed.replaceAll("/\\*[^*]*\\*/", "").replaceAll("\\s+", " ");

        // a 'return' cannot leave a switch expression
        assertFalse(printed, source.contains("return name.toCharArray()"));
        assertTrue(printed, source.contains("case 26, 27, 28 -> this.primitive ? name.toCharArray() : \"Ljava/lang/Object;\".toCharArray();"));
        assertTrue(printed, source.contains("yield name.trim().toCharArray();"));
        assertTrue(printed, source.contains("yield name.toCharArray();"));
        assertTrue(printed, CompilerUtil.compile("17", new InMemoryJavaSourceFileObject(internalClassName, printed)));
    }
}
