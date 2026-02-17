package org.jd.core.v1;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Map;

import org.apache.commons.io.IOUtils;
import org.jd.core.v1.api.loader.Loader;
import org.jd.core.v1.api.printer.Printer;
import org.jd.core.v1.compiler.CompilerUtil;
import org.jd.core.v1.compiler.InMemoryClassLoader;
import org.jd.core.v1.compiler.InMemoryJavaSourceFileObject;
import org.jd.core.v1.loader.ZipLoader;
import org.jd.core.v1.printer.PlainTextPrinter;

import junit.framework.TestCase;

public abstract class AbstractJdTest extends TestCase {
    protected ClassFileToJavaSourceDecompiler classFileToJavaSourceDecompiler = new ClassFileToJavaSourceDecompiler();

    protected String decompile(Loader loader, Printer printer, String internalTypeName, Map<String, Object> configuration) throws Exception {
        classFileToJavaSourceDecompiler.decompile(loader, printer, internalTypeName, configuration);

        String source = printer.toString();

        printSource(source);

        return source;
    }

    protected String decompile(Loader loader, Printer printer, String internalTypeName) throws Exception {
        return decompile(loader, printer, internalTypeName, Collections.emptyMap());
    }

    protected String decompileSuccess(Loader loader, Printer printer, String internalTypeName, Map<String, Object> configuration) throws Exception {
        String source = decompile(loader, printer, internalTypeName, configuration);
        assertEquals(-1, source.indexOf("// Byte code:"));
        assertEquals(-1, source.indexOf("Decompilation failed at line #"));
        return source;
    }

    protected String decompileSuccess(Loader loader, Printer printer, String internalTypeName) throws Exception {
        return decompileSuccess(loader, printer, internalTypeName, Collections.emptyMap());
    }

    protected void printSource(String source) {
        System.out.println("- - - - - - - - ");
        System.out.println(source);
        System.out.println("- - - - - - - - ");
    }

    protected void assertEqualsIgnoreEOL(String expected, String actual) {
        assertEquals(expected.replaceAll("\s*\r?\n", "\n"), actual.replaceAll("\s*\r?\n", "\n"));
    }

    protected String getResourceAsString(String path) throws IOException {
        return IOUtils.toString(getClass().getResource(path), StandardCharsets.UTF_8);
    }

    protected void test(String jarPath, String internalClassName, String expectedOutput, String compilerVersion) throws Exception {
        test(jarPath, internalClassName, expectedOutput, compilerVersion, new PlainTextPrinter());
    }

    protected void test(String jarPath, String internalClassName, String expectedOutput, String compilerVersion, Printer printer) throws Exception {
        try (InputStream is = this.getClass().getResourceAsStream(jarPath)) {
            Loader loader = new ZipLoader(is);
            String source = decompileSuccess(loader, printer, internalClassName);
            
            // Check decompiled source code
            String expected = getResourceAsString(expectedOutput);
            assertEqualsIgnoreEOL(expected, source);
            
            // Recompile decompiled source code and check errors
            assertTrue(CompilerUtil.compile(compilerVersion, new InMemoryJavaSourceFileObject(internalClassName, source)));
        }
    }
    
    protected void testECJ(String jarPath, String internalClassName, String expectedOutput, String compilerVersion) throws Exception {
        test(jarPath, internalClassName, expectedOutput, compilerVersion, new PlainTextPrinter());
    }
    
    protected void testECJ(String internalClassName, String expectedOutput, String compilerVersion, Printer printer) throws Exception {
        InMemoryClassLoader classLoader = new InMemoryClassLoader();
        String expectedSource = getResourceAsString(expectedOutput);
        InMemoryJavaSourceFileObject object = new InMemoryJavaSourceFileObject(internalClassName, expectedSource);
        assertTrue(CompilerUtil.compile(compilerVersion, classLoader, object));
        assertTrue(classLoader.canLoad(internalClassName));
        assertNotNull(classLoader.load(internalClassName));
        String actualSource = decompileSuccess(classLoader, printer, internalClassName);

        // Check decompiled source code
        assertEqualsIgnoreEOL(expectedSource, actualSource);
    }
}
