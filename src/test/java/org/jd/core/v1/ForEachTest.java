package org.jd.core.v1;

import org.jd.core.test.ForEach;
import org.jd.core.v1.api.loader.Loader;
import org.jd.core.v1.compiler.CompilerUtil;
import org.jd.core.v1.compiler.InMemoryJavaSourceFileObject;
import org.jd.core.v1.loader.ZipLoader;
import org.jd.core.v1.printer.PlainTextPrinter;
import org.junit.Test;

import java.io.InputStream;

public class ForEachTest extends AbstractJdTest {

    @Test
    public void testForEachGeneric() throws Exception {
        String internalClassName = ForEach.class.getName().replace('.', '/');
        try (InputStream is = this.getClass().getResourceAsStream("/jar/foreach-jdk8u331.jar")) {
            Loader loader = new ZipLoader(is);
            String source = decompileSuccess(loader, new PlainTextPrinter(), internalClassName);
            
            // Recompile decompiled source code and check errors
            assertTrue(CompilerUtil.compile("1.8", new InMemoryJavaSourceFileObject(internalClassName, source)));
        }
    }
}
