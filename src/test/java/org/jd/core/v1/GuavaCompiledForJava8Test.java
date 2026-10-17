package org.jd.core.v1;

import org.jd.core.v1.compiler.CompilerUtil;
import org.jd.core.v1.compiler.InMemoryClassLoader;
import org.jd.core.v1.compiler.InMemoryJavaSourceFileObject;
import org.jd.core.v1.printer.PlainTextPrinter;
import org.junit.Test;

/** Shapes of Guava, which is compiled for Java 8: a private constructor is called through a synthetic one. */
public class GuavaCompiledForJava8Test extends AbstractJdTest {
    private static final String SOURCE = String.join("\n",
            "package guava;",
            "import java.util.function.BinaryOperator;",
            "public class Accumulators {",
            "    private static final class Accumulator<V> {",
            "        private final BinaryOperator<V> merge;",
            "        private Accumulator(BinaryOperator<V> merge) {",
            "            this.merge = merge;",
            "        }",
            "        Accumulator<V> combine(Accumulator<V> other) {",
            "            return this;",
            "        }",
            "    }",
            "    public static <T> Object direct() {",
            "        return new Accumulator<T>((first, second) -> first);",
            "    }",
            "}");

    @Test
    public void testAccessConstructorParameterOfAnotherType() throws Exception {
        String internalClassName = "guava/Accumulators";
        InMemoryClassLoader classLoader = new InMemoryClassLoader();

        assertTrue(CompilerUtil.compile("1.8", classLoader, new InMemoryJavaSourceFileObject(internalClassName, SOURCE)));

        String source = decompileSuccess(classLoader, new PlainTextPrinter(), internalClassName);

        assertEquals(-1, source.indexOf("null)"));
        assertTrue(CompilerUtil.compile("17", new InMemoryJavaSourceFileObject(internalClassName, source)));
    }
}
