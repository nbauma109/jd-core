package org.jd.core.v1;

import org.jd.core.v1.api.loader.Loader;
import org.jd.core.v1.compiler.CompilerUtil;
import org.jd.core.v1.compiler.InMemoryJavaSourceFileObject;
import org.jd.core.v1.loader.ZipLoader;
import org.jd.core.v1.printer.PlainTextPrinter;
import org.jd.core.v1.regex.PatternMaker;
import org.jd.core.v1.stub.Shift;
import org.junit.Test;

import java.io.InputStream;
import java.util.Collections;

public class ShiftTest extends AbstractJdTest {

    @Test
    public void test() throws Exception {
        try (InputStream is = this.getClass().getResourceAsStream("/jar/break-statement-jdk17.0.6.jar")) {
            Loader loader = new ZipLoader(is);
            String internalClassName = Shift.class.getName().replace('.', '/');
            String source = decompileSuccess(loader, new PlainTextPrinter(), internalClassName, Collections.singletonMap("realignLineNumbers", "true"));

            // Check decompiled source code
            assertTrue(source.matches(PatternMaker.make(":  0 */       break;")));

            // Recompile decompiled source code and check errors
            assertTrue(CompilerUtil.compile("1.8", new InMemoryJavaSourceFileObject(internalClassName, source)));
        }
    }

    @Test
    public void testECJ() throws Exception {
        try (InputStream is = this.getClass().getResourceAsStream("/jar/break-statement-ecj17.jar")) {
            Loader loader = new ZipLoader(is);
            String internalClassName = Shift.class.getName().replace('.', '/');
            String source = decompileSuccess(loader, new PlainTextPrinter(), internalClassName, Collections.singletonMap("realignLineNumbers", "true"));

            // Check decompiled source code
            assertTrue(source.matches(PatternMaker.make(":  0 */       break;")));

            // Recompile decompiled source code and check errors
            assertTrue(CompilerUtil.compile("1.8", new InMemoryJavaSourceFileObject(internalClassName, source)));
        }
    }
}
