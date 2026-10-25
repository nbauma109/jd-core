/*
 * Copyright (c) 2026 Nicolas Baumann (@nbauma109), with Claude Sonnet 5.5.
 * This project is distributed under the GPLv3 license.
 * This is a Copyleft license that gives the user the right to use,
 * copy and modify the code freely for non-commercial purposes.
 */

package org.jd.core.v1;

import org.jd.core.v1.api.loader.Loader;
import org.jd.core.v1.compiler.CompilerUtil;
import org.jd.core.v1.compiler.InMemoryJavaSourceFileObject;
import org.jd.core.v1.loader.ClassPathLoader;
import org.jd.core.v1.printer.PlainTextPrinter;
import org.jd.core.v1.stub.VineflowerRegressions;
import org.junit.Test;

/**
 * Cases of the Vineflower output comparison which jd-core got wrong.
 */
public class VineflowerRegressionsTest extends AbstractJdTest {

    @Test
    public void test() throws Exception {
        String internalClassName = VineflowerRegressions.class.getName().replace('.', '/');
        Loader loader = new ClassPathLoader();
        String source = decompileSuccess(loader, new PlainTextPrinter(), internalClassName);

        // 'double x = 0.2F' is folded into 0.20000000298023224D by the compiler
        assertTrue(source.contains("return 0.2F;"));
        assertFalse(source.contains("0.20000000298023224"));
        // ... also when the value of the assignment is used (the double occupies two slots of the stack)
        assertTrue(source.contains("this.a = this.b = 0.2F;"));
        assertTrue(source.contains("return this.a = 0.2F;"));

        // The result of a switch expression is yielded by the arrow, not after a 'break'
        assertTrue(source.contains("default -> \"Unknown\";"));
        assertFalse(source.contains("yield"));

        // A compound condition which is the test of a ternary operator keeps all its operands
        assertTrue(source.contains("(!(n < 1.0D - n) || !(n > 3.0D)) ? ((n < 5.0D)) : ((n > 2.0D))"));
        assertTrue(source.contains("(n < 1.0D - n || n > 3.0D) ? ((n < 5.0D)) : ((n > 2.0D))"));
        assertTrue(source.contains("(n < 1.0D - n && n > 3.0D) ? ((n < 5.0D)) : ((n > 2.0D))"));

        // Recompile decompiled source code and check errors
        assertTrue(CompilerUtil.compile("17", new InMemoryJavaSourceFileObject(internalClassName, source)));
    }
}
