package org.jd.core.v1;

import org.jd.core.v1.api.loader.Loader;
import org.jd.core.v1.compiler.CompilerUtil;
import org.jd.core.v1.compiler.InMemoryJavaSourceFileObject;
import org.jd.core.v1.loader.ZipLoader;
import org.jd.core.v1.printer.PlainTextPrinter;
import org.junit.Test;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.Map;

public class SealedDemoTest extends AbstractJdTest {

    @Test
    public void testSealedDemo() throws Exception {
        String internalClassName = "org/jd/core/v1/SealedDemo";
        try (InputStream is = this.getClass().getResourceAsStream("/jar/sealed-demo-jdk21.0.8.jar")) {
            Loader loader = new ZipLoader(is);
            Map<String, Object> configuration = Collections.singletonMap("realignLineNumbers", Boolean.TRUE);
            String source = decompileSuccess(loader, new PlainTextPrinter(), internalClassName, configuration);

            // Check decompiled source code
            String expected = Files.readString(Paths.get(getClass().getResource("/txt/SealedDemo.txt").toURI()));
            assertEqualsIgnoreEOL(expected, source);

            // Recompile decompiled source code and check errors
            assertTrue(CompilerUtil.compile("21", new InMemoryJavaSourceFileObject(internalClassName, source)));
        }
    }
}
