package org.jd.core.v1;

import org.apache.commons.io.IOUtils;
import org.jd.core.v1.compiler.CompilerUtil;
import org.jd.core.v1.compiler.InMemoryJavaSourceFileObject;
import org.jd.core.v1.loader.ClassPathLoader;
import org.jd.core.v1.printer.PlainTextPrinter;
import org.jd.core.v1.stub.ForEachArray;
import org.junit.Test;

import java.nio.charset.StandardCharsets;

public class ForEachArrayTest extends AbstractJdTest {

    @Test
    public void testForEachArray() throws Exception {
        String internalClassName = ForEachArray.class.getName().replace('.', '/');
        String source = decompileSuccess(new ClassPathLoader(), new PlainTextPrinter(), internalClassName);

        // Check decompiled source code
        assertEqualsIgnoreEOL(IOUtils.toString(getClass().getResource("/txt/ForEachArray.txt"), StandardCharsets.UTF_8), source);

        // Recompile decompiled source code and check errors
        assertTrue(CompilerUtil.compile("1.8", new InMemoryJavaSourceFileObject(internalClassName, source)));
    }

}
