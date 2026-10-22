package org.jd.core.v1;

import org.jd.core.v1.compiler.CompilerUtil;
import org.jd.core.v1.compiler.InMemoryJavaSourceFileObject;
import org.jd.core.v1.loader.ClassPathLoader;
import org.jd.core.v1.printer.PlainTextPrinter;
import org.jd.core.v1.stub.GuavaPatterns;
import org.jd.core.v1.stub.GuavaSub;
import org.jd.core.v1.stub.TypeAnnotationPatterns;
import org.junit.Test;

public class GuavaPatternsTest extends AbstractJdTest {
    @Test
    public void test() throws Exception {
        String internalClassName = GuavaPatterns.class.getName().replace('.', '/');
        String source = decompileSuccess(new ClassPathLoader(), new PlainTextPrinter(), internalClassName);

        assertEquals(-1, source.indexOf("null."));
        assertEquals(-1, source.indexOf("throw null"));
        assertEquals(-1, source.indexOf("Decompilation failed"));
        assertEquals(-1, source.indexOf("new GuavaOuter.Wrapped(outer"));
        assertTrue(source.contains("for (GuavaPatterns.OneWayCollection<String> collection : collections)") || source.contains("for (OneWayCollection<String> collection : collections)"));
        assertTrue(CompilerUtil.compile("17", new InMemoryJavaSourceFileObject(internalClassName, source)));
    }

    @Test
    public void testInnerClassOfTheSuperclass() throws Exception {
        String internalClassName = GuavaSub.class.getName().replace('.', '/');
        String source = decompileSuccess(new ClassPathLoader(), new PlainTextPrinter(), internalClassName);

        assertEquals(-1, source.indexOf("(this,"));
        assertTrue(source.contains("new GuavaOuter.Wrapped(key, value)"));
        assertTrue(source.contains("new GuavaOuter.Marker()"));
    }

    @Test
    public void testTypeAnnotationsOfWholeTypes() throws Exception {
        String internalClassName = TypeAnnotationPatterns.class.getName().replace('.', '/');
        String printed = decompileSuccess(new ClassPathLoader(), new PlainTextPrinter(), internalClassName);
        String source = printed.replaceAll("/\\*[^*]*\\*/", "").replaceAll("\\s+", " ");

        assertTrue(source.contains("@Maybe T generic;"));
        assertTrue(source.contains("@Maybe public T pick(@Maybe T value)"));
        assertTrue(source.contains("@Maybe Object object;"));
        assertTrue(source.contains("(String first, int second, @Maybe Object third)"));
        assertTrue(source.contains("public String find(@Maybe Object key, int count, String other, List<String> values)"));
        // written in front of a declaration, they would not annotate the same type
        assertFalse(source.contains("@Maybe String[] annotatedElementsOfArray"));
        assertFalse(source.contains("@Maybe Map.Entry"));
        assertFalse(source.contains("@Maybe Entry"));
        assertFalse(source.contains("@Maybe Object[]"));
        assertTrue(CompilerUtil.compile("17", new InMemoryJavaSourceFileObject(internalClassName, printed)));
    }
}
