package org.jd.core.v1;

import org.apache.commons.io.IOUtils;
import org.jd.core.v1.compiler.CompilerUtil;
import org.jd.core.v1.compiler.InMemoryJavaSourceFileObject;
import org.jd.core.v1.loader.ClassPathLoader;
import org.jd.core.v1.printer.PlainTextPrinter;
import org.jd.core.v1.stub.ParsePUT;
import org.junit.Test;

import java.nio.charset.StandardCharsets;

public class ParsePUTTest extends AbstractJdTest {
    @Test
    public void test() throws Exception {
        String internalClassName = ParsePUT.class.getName().replace('.', '/');
        String output = decompileSuccess(new ClassPathLoader(), new PlainTextPrinter(), internalClassName);
        
        // Check output
        assertEquals(IOUtils.toString(getClass().getResource("/txt/ParsePUT.txt"), StandardCharsets.UTF_8), output);

        // Recompile decompiled source code and check errors
        assertTrue(CompilerUtil.compile("1.8", new InMemoryJavaSourceFileObject(internalClassName, output)));
    }
}
