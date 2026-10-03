package org.jd.core.v1;

import org.jd.core.v1.compiler.CompilerUtil;
import org.jd.core.v1.compiler.InMemoryJavaSourceFileObject;
import org.jd.core.v1.loader.ClassPathLoader;
import org.jd.core.v1.printer.PlainTextPrinter;
import org.jd.core.v1.stub.GuavaPatterns;
import org.junit.Test;

public class GuavaPatternsTest extends AbstractJdTest {
    @Test
    public void test() throws Exception {
        String internalClassName = GuavaPatterns.class.getName().replace('.', '/');
        String source = decompileSuccess(new ClassPathLoader(), new PlainTextPrinter(), internalClassName);

        assertEquals(-1, source.indexOf("null."));
        assertEquals(-1, source.indexOf("throw null"));
        assertEquals(-1, source.indexOf("Decompilation failed"));
        assertTrue(CompilerUtil.compile("17", new InMemoryJavaSourceFileObject(internalClassName, source)));
    }
}
