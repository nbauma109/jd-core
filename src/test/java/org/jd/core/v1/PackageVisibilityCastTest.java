/*
 * Copyright (c) 2008, 2019 Emmanuel Dupuy.
 * This project is distributed under the GPLv3 license.
 * This is a Copyleft license that gives the user the right to use,
 * copy and modify the code freely for non-commercial purposes.
 */

package org.jd.core.v1;

import org.jd.core.v1.compiler.CompilerUtil;
import org.jd.core.v1.compiler.InMemoryJavaSourceFileObject;
import org.jd.core.v1.loader.ClassPathLoader;
import org.jd.core.v1.printer.PlainTextPrinter;
import org.jd.core.v1.regex.PatternMaker;
import org.junit.Test;

public class PackageVisibilityCastTest extends AbstractJdTest {
    @Test
    public void testConstructorCallDoesNotCastToInaccessibleSupertype() throws Exception {
        String internalClassName = "org/jd/core/test/inaccessible/nodes/NodeUtils";
        String source = decompileSuccess(new ClassPathLoader(), new PlainTextPrinter(), internalClassName);

        assertTrue(source.matches(PatternMaker.make("return new Parser(new HtmlTreeBuilder());")));
        assertFalse(source.contains("import org.jd.core.test.inaccessible.parser.TreeBuilder;"));
        assertFalse(source.contains("(TreeBuilder)new HtmlTreeBuilder()"));

        assertTrue(CompilerUtil.compile("1.8", new InMemoryJavaSourceFileObject(internalClassName, source)));
    }
}
