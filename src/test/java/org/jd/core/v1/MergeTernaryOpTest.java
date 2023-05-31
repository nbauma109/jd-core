package org.jd.core.v1;

import org.jd.core.v1.api.loader.Loader;
import org.jd.core.v1.compiler.CompilerUtil;
import org.jd.core.v1.compiler.InMemoryJavaSourceFileObject;
import org.jd.core.v1.loader.ClassPathLoader;
import org.jd.core.v1.printer.PlainTextPrinter;
import org.jd.core.v1.stub.MergeTernaryOp;
import org.junit.Test;

public class MergeTernaryOpTest extends AbstractJdTest {
    @Test
    public void test() throws Exception {
        String internalClassName = MergeTernaryOp.class.getName().replace('.', '/');
        Loader loader = new ClassPathLoader();
        String source = decompileSuccess(loader, new PlainTextPrinter(), internalClassName);

        // Recompile decompiled source code and check errors
        assertTrue(CompilerUtil.compile("1.8", new InMemoryJavaSourceFileObject(internalClassName, source)));
    }
}
