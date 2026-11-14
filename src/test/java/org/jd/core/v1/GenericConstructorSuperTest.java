/*
 * Copyright (c) 2026 Nicolas Baumann (@nbauma109), with Claude Sonnet 5.5.
 * This project is distributed under the GPLv3 license.
 * This is a Copyleft license that gives the user the right to use,
 * copy and modify the code freely for non-commercial purposes.
 */

package org.jd.core.v1;

import org.jd.core.v1.compiler.CompilerUtil;
import org.jd.core.v1.compiler.InMemoryJavaSourceFileObject;
import org.jd.core.v1.loader.ClassPathLoader;
import org.jd.core.v1.printer.PlainTextPrinter;
import org.junit.Test;

/**
 * The argument of super(...) is not cast to the parameter type of a generic constructor with the type parameter of the
 * constructor (it is inferred, and may share its name with a type parameter of the class).
 */
public class GenericConstructorSuperTest extends AbstractJdTest {

    @Test
    public void testSuperCallOfGenericConstructor() throws Exception {
        String internalClassName = "org/jd/core/v1/stub/GenericConstructorSuper";
        String source = decompileSuccess(new ClassPathLoader(), new PlainTextPrinter(), internalClassName);

        assertFalse(source, source.contains("AbstractBuilder<T>)"));
        assertTrue(CompilerUtil.compile("1.8", new InMemoryJavaSourceFileObject(internalClassName, source)));
    }
}
