package org.jd.core.v1;

import org.jd.core.v1.loader.ClassPathLoader;
import org.jd.core.v1.printer.PlainTextPrinter;
import org.jd.core.v1.regex.PatternMaker;
import org.junit.Test;

public class BinaryOpTest extends AbstractJdTest {

    @Test
    public void testSubtract() throws Exception {
        class Subtract {
            @SuppressWarnings("unused")
            int subtract(int i, int j, int k) {
                return i - (j - k);
            }
        }
        String internalClassName = Subtract.class.getName().replace('.', '/');
        String source = decompileSuccess(new ClassPathLoader(), new PlainTextPrinter(), internalClassName);
        
        // Check decompiled source code
        assertTrue(source.matches(PatternMaker.make("return i - (j - k);")));
    }

    @Test
    public void testSubtractMultiply() throws Exception {
        class SubtractMultiply {
            @SuppressWarnings("unused")
            int subtract(int i, int j, int k) {
                return i - (j * k);
            }
        }
        String internalClassName = SubtractMultiply.class.getName().replace('.', '/');
        String source = decompileSuccess(new ClassPathLoader(), new PlainTextPrinter(), internalClassName);
        
        // Check decompiled source code
        assertTrue(source.matches(PatternMaker.make("return i - j * k;")));
    }

    @Test
    public void testAdd() throws Exception {
        class Add {
            @SuppressWarnings("unused")
            int add(int i, int j, int k) {
                return i + (j + k);
            }
        }
        String internalClassName = Add.class.getName().replace('.', '/');
        String source = decompileSuccess(new ClassPathLoader(), new PlainTextPrinter(), internalClassName);
            
        // Check decompiled source code
        assertTrue(source.matches(PatternMaker.make("return i + j + k;")));
    }

    @Test
    public void testMultiply() throws Exception {
        class Multiply {
            @SuppressWarnings("unused")
            int multiply(int i, int j, int k) {
                return i * (j * k);
            }
        }
        String internalClassName = Multiply.class.getName().replace('.', '/');
        String source = decompileSuccess(new ClassPathLoader(), new PlainTextPrinter(), internalClassName);
            
        // Check decompiled source code
        assertTrue(source.matches(PatternMaker.make("return i * j * k;")));
    }

    @Test
    public void testDivide() throws Exception {
        class Divide {
            @SuppressWarnings("unused")
            int divide(int i, int j, int k) {
                return i / (j / k);
            }
        }
        String internalClassName = Divide.class.getName().replace('.', '/');
        String source = decompileSuccess(new ClassPathLoader(), new PlainTextPrinter(), internalClassName);
            
        // Check decompiled source code
        assertTrue(source.matches(PatternMaker.make("return i / (j / k);")));
    }

    @Test
    public void testModulo() throws Exception {
        class Modulo {
            @SuppressWarnings("unused")
            int modulo(int i, int j, int k) {
                return i % (j % k);
            }
        }
        String internalClassName = Modulo.class.getName().replace('.', '/');
        String source = decompileSuccess(new ClassPathLoader(), new PlainTextPrinter(), internalClassName);
            
        // Check decompiled source code
        assertTrue(source.matches(PatternMaker.make("return i % (j % k);")));
    }

    @Test
    public void testDivideMultiply() throws Exception {
        class DivideMultiply {
            @SuppressWarnings("unused")
            int divide(int i, int j, int k) {
                return i / (j * k);
            }
        }
        String internalClassName = DivideMultiply.class.getName().replace('.', '/');
        String source = decompileSuccess(new ClassPathLoader(), new PlainTextPrinter(), internalClassName);
            
        // Check decompiled source code
        assertTrue(source.matches(PatternMaker.make("return i / (j * k);")));
    }

    @Test
    public void testMultiplyDivide() throws Exception {
        class MultiplyDivide {
            @SuppressWarnings("unused")
            int multiplyDivide(int i, int j, int k) {
                return i * (j / k);
            }
        }
        String internalClassName = MultiplyDivide.class.getName().replace('.', '/');
        String source = decompileSuccess(new ClassPathLoader(), new PlainTextPrinter(), internalClassName);
            
        // Check decompiled source code
        assertTrue(source.matches(PatternMaker.make("return i * j / k;")));
    }
    @Test
    public void testMultiplyAdd() throws Exception {
        class MultiplyAdd {
            @SuppressWarnings("unused")
            int multiplyAdd(int i, int j, int k) {
                return i * (j + k);
            }
        }
        String internalClassName = MultiplyAdd.class.getName().replace('.', '/');
        String source = decompileSuccess(new ClassPathLoader(), new PlainTextPrinter(), internalClassName);
        
        // Check decompiled source code
        assertTrue(source.matches(PatternMaker.make("return i * (j + k);")));
    }

    @Test
    public void testAnd() throws Exception {
        class And {
            @SuppressWarnings("unused")
            int and(int i, int j, int k) {
                return i & (j & k);
            }
        }
        String internalClassName = And.class.getName().replace('.', '/');
        String source = decompileSuccess(new ClassPathLoader(), new PlainTextPrinter(), internalClassName);
            
        // Check decompiled source code
        assertTrue(source.matches(PatternMaker.make("return i & j & k;")));
    }

    @Test
    public void testAssignment() throws Exception {
        class Assignments {
            @SuppressWarnings("unused")
            int assign(int i, int j) {
                i = j = 1;
                return i + j;
            }
        }
        String internalClassName = Assignments.class.getName().replace('.', '/');
        String source = decompileSuccess(new ClassPathLoader(), new PlainTextPrinter(), internalClassName);

        // Check decompiled source code
        assertTrue(source.matches(PatternMaker.make("i = j = 1;")));
    }

}
