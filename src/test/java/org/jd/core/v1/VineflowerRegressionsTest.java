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
import org.jd.core.v1.stub.VineflowerRegressions;
import org.junit.Test;

/**
 * Cases of the Vineflower output comparison which jd-core got wrong.
 */
public class VineflowerRegressionsTest extends AbstractJdTest {
    private static final String INTERNAL_CLASS_NAME = VineflowerRegressions.class.getName().replace('.', '/');

    private String decompile() throws Exception {
        return decompileSuccess(new ClassPathLoader(), new PlainTextPrinter(), INTERNAL_CLASS_NAME);
    }

    @Test
    public void testFloatsAndConditions() throws Exception {
        String source = decompile();

        // 'double x = 0.2F' is folded into 0.20000000298023224D by the compiler
        assertTrue(source.contains("return 0.2F;"));
        // ... but not where it would change the boxed type or the selected overload
        assertTrue(source.contains("return 0.20000000298023224D;"));
        assertTrue(source.contains("which(0.20000000298023224D)"));
        // ... also when the value of the assignment is used (the double occupies two slots of the stack)
        assertTrue(source.contains("this.a = this.b = 0.2F;"));
        assertTrue(source.contains("return this.a = 0.2F;"));
        // ... and where a double is expected
        assertTrue(source.contains("double local = 0.2F;"));
        assertTrue(source.contains("local = 0.3F;"));

        // The result of a switch expression is yielded by the arrow, not after a 'break'
        assertTrue(source.contains("default -> \"Unknown\";"));
        assertFalse(source.contains("yield"));

        // A compound condition which is the test of a ternary operator keeps all its operands
        assertTrue(source.contains("(!(n < 1.0D - n) || !(n > 3.0D)) ? ((n < 5.0D)) : ((n > 2.0D))"));
        assertTrue(source.contains("(n < 1.0D - n || n > 3.0D) ? ((n < 5.0D)) : ((n > 2.0D))"));
        assertTrue(source.contains("(n < 1.0D - n && n > 3.0D) ? ((n < 5.0D)) : ((n > 2.0D))"));

        // The increment which precedes the 'break' is evaluated by every iteration, not once before the loop
        assertTrue(source.contains("while (i > 10 &&"));
        assertTrue(source.contains("++i != 15)"));

        // The call which discards a box stays
        assertTrue(source.contains("Boolean.valueOf("));
    }

    @Test
    public void testEnumsLocalTypesAndVar() throws Exception {
        String source = decompile();

        // The initializer of the body of an enum constant
        assertTrue(source.contains("System.out.println(\"A\");"));

        // Local enum, interface and record are declared in the method
        assertTrue(source.contains("enum Type {"));
        assertTrue(source.contains("interface Named {"));
        assertTrue(source.contains("record Point(int x, int y) {}"));

        // The anonymous class is only reachable through 'var', unless the call is made on a member of its super type
        assertTrue(source.contains("var printer = new Object() {"));
        assertTrue(source.contains("Object object = new Object() {"));

        // 'var' for an anonymous class of a field or of an interface
        assertTrue(source.contains("var holder = new Object() {"));
        assertTrue(source.contains("var runner = new Runnable() {"));
    }

    @Test
    public void testInstanceOfPatterns() throws Exception {
        String source = decompile();

        // Whatever the shape of the test
        assertTrue(source.contains("accept(o, (o instanceof String s && s.length() > 5));"));
        assertTrue(source.contains("boolean matches = (o instanceof String s && s.length() > 5);"));
        assertTrue(source.contains("(o instanceof String s) ? s.isEmpty() : false"));
        assertTrue(source.contains("if (!(o instanceof String s))"));

        // Several patterns, a chain of tests and a pattern which is negated before a 'throw'
        assertTrue(source.contains("a instanceof String s && b instanceof String t && s.equals(t)"));
        assertTrue(source.contains("if (o instanceof Integer i)"));
        assertTrue(source.contains("throw new IllegalArgumentException();"));
        assertTrue(source.contains("if (o instanceof String)"));

        // An assignment to a parameter is no pattern variable, the increment of a test is evaluated before the first use of the variable
        assertTrue(source.contains("s = (String)o;"));
        assertTrue(source.contains("++i * 2 <= i)"));

        // A field which hides the one of the super type does not need 'var'
        assertTrue(source.contains("ShadowedBase base = new ShadowedBase() {"));
    }

    @Test
    public void testRecompilation() throws Exception {
        // Recompile decompiled source code and check errors
        assertTrue(CompilerUtil.compile("17", new InMemoryJavaSourceFileObject(INTERNAL_CLASS_NAME, decompile())));
    }
}
