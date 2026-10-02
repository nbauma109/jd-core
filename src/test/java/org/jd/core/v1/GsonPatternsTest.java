package org.jd.core.v1;

import org.jd.core.v1.api.loader.Loader;
import org.jd.core.v1.compiler.CompilerUtil;
import org.jd.core.v1.compiler.InMemoryJavaSourceFileObject;
import org.jd.core.v1.loader.ClassPathLoader;
import org.jd.core.v1.printer.PlainTextPrinter;
import org.jd.core.v1.stub.GsonPatterns;
import org.junit.Test;

public class GsonPatternsTest extends AbstractJdTest {
    @Test
    public void test() throws Exception {
        String internalClassName = GsonPatterns.class.getName().replace('.', '/');
        Loader loader = new ClassPathLoader();
        String source = decompileSuccess(loader, new PlainTextPrinter(), internalClassName);

        // An anonymous class has no name to qualify a static call with
        assertEquals(-1, source.indexOf("null."));
        // No control flow which could not be structured, nor any which was wrongly rebuilt
        assertEquals(-1, source.indexOf("throw null"));
        assertEquals(-1, source.indexOf("Decompilation failed"));
        // The catch of the try which returns is kept in the synchronized block
        assertTrue(source.contains("catch (IOException e)"));
        // The compound assignment of the second argument is evaluated after the first
        assertTrue(source.contains("parse(text, offset, offset += 4)"));
        assertTrue(source.contains("parse(text, ++offset, offset += 2)"));

        // Recompile decompiled source code and check errors
        assertTrue(CompilerUtil.compile("17", new InMemoryJavaSourceFileObject(internalClassName, source)));
    }
}
