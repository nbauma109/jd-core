package org.jd.core.v1;

import org.jd.core.v1.api.loader.Loader;
import org.jd.core.v1.compiler.CompilerUtil;
import org.jd.core.v1.compiler.InMemoryJavaSourceFileObject;
import org.jd.core.v1.loader.ClassPathLoader;
import org.jd.core.v1.printer.PlainTextPrinter;
import org.jd.core.v1.stub.GsonFunctionalPatterns;
import org.jd.core.v1.stub.GsonPatterns;
import org.jd.core.v1.stub.GsonWidePatterns;
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
        // The increment is not moved before the first argument
        assertTrue(source.contains("use(sideEffect(), ++i)"));
        assertTrue(source.contains("use(sideEffect(), --i)"));

        // Recompile decompiled source code and check errors
        assertTrue(CompilerUtil.compile("17", new InMemoryJavaSourceFileObject(internalClassName, source)));
    }

    @Test
    public void testFunctionalInterfaces() throws Exception {
        String internalClassName = GsonFunctionalPatterns.class.getName().replace('.', '/');
        String source = decompileSuccess(new ClassPathLoader(), new PlainTextPrinter(), internalClassName);

        assertEquals(-1, source.indexOf("Decompilation failed"));
        assertTrue(CompilerUtil.compile("17", new InMemoryJavaSourceFileObject(internalClassName, source)));
    }

    @Test
    public void testWideLocalVariables() throws Exception {
        String internalClassName = GsonWidePatterns.class.getName().replace('.', '/');
        String source = decompileSuccess(new ClassPathLoader(), new PlainTextPrinter(), internalClassName);

        assertEquals(-1, source.indexOf("Decompilation failed"));
        assertEquals(-1, source.indexOf("throw null"));
        assertTrue(source.contains("catch (IOException e)"));
        assertTrue(java.util.regex.Pattern.compile("parse\\(text, (\\w+), \\1 \\+= 4\\)").matcher(source).find());
        assertTrue(CompilerUtil.compile("17", new InMemoryJavaSourceFileObject(internalClassName, source)));
    }
}
