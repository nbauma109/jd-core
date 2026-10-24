package org.jd.core.v1;

import org.jd.core.v1.compiler.CompilerUtil;
import org.jd.core.v1.compiler.InMemoryClassLoader;
import org.jd.core.v1.compiler.InMemoryJavaSourceFileObject;
import org.jd.core.v1.printer.PlainTextPrinter;
import org.junit.Test;

/** javac 18+ does not store the outer instance of an inner class in 'this$0' when it only passes it to the constructor of an inner superclass. */
public class GuavaCompiledForJava21Test extends AbstractJdTest {
    private static final String SOURCE = String.join("\n",
            "package guava;",
            "import java.util.Iterator;",
            "public abstract class Wrapping<V> {",
            "    abstract V delegate();",
            "    class Collection {",
            "        final V value;",
            "        Collection(V value) {",
            "            this.value = value;",
            "        }",
            "        V get() {",
            "            return delegate();",
            "        }",
            "    }",
            "    class Special extends Collection {",
            "        Special(V value) {",
            "            super(value);",
            "        }",
            "    }",
            "    class Listing extends Collection {",
            "        Listing(Iterator<V> iterator) {",
            "            super(iterator.next());",
            "        }",
            "        Listing(Wrapping<V> other, V value) {",
            "            super(other.delegate());",
            "        }",
            "        Listing(Collection source) {",
            "            super(Wrapping.this.delegate());",
            "        }",
            "    }",
            "}");

    @Test
    public void testOuterInstanceUsedByTheConstructor() throws Exception {
        String internalClassName = "guava/Wrapping";
        InMemoryClassLoader classLoader = new InMemoryClassLoader();

        assertTrue(CompilerUtil.compile("21", classLoader, new InMemoryJavaSourceFileObject(internalClassName, SOURCE)));

        String source = decompileSuccess(classLoader, new PlainTextPrinter(), internalClassName);

        assertEquals(-1, source.indexOf("this$0"));
        assertTrue(CompilerUtil.compile("21", new InMemoryJavaSourceFileObject(internalClassName, source)));
    }
}
