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

public class LoopBodyDeclarationTest extends AbstractJdTest {

    /** 'HashtableOfType.rehash' of the Eclipse compiler: the declaration of the variable was lost with the list of statements of the body */
    @Test
    public void testVariableAssignedByTheConditionOfTheOnlyStatementOfTheBodyIsDeclared() throws Exception {
        String internalClassName = LoopBodyDeclaration.class.getName().replace('.', '/');
        String printed = decompileSuccess(new ClassPathLoader(), new PlainTextPrinter(), internalClassName);
        String source = printed.replaceAll("/\\*[^*]*\\*/", "").replaceAll("\\s+", " ");

        assertTrue(printed, source.contains("char[] currentKey; if ((currentKey = this.keyTable[i]) != null)"));
        assertTrue(printed, CompilerUtil.compile("17", new InMemoryJavaSourceFileObject(internalClassName, printed)));
    }
}
