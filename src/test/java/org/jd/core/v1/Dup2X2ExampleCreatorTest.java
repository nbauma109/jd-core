package org.jd.core.v1;

import org.apache.commons.io.IOUtils;
import org.jd.core.v1.api.loader.Loader;
import org.jd.core.v1.loader.ClassPathLoader;
import org.jd.core.v1.printer.StringBuilderPrinter;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

public class Dup2X2ExampleCreatorTest extends AbstractJdTest {

    @Test
    public void test() throws Exception {
        Dup2X2ExampleCreator creator = new Dup2X2ExampleCreator();
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        creator.create(out);
        Loader loader = new ClassPathLoader() {
            
            @Override
            public byte[] load(String internalName) throws IOException {
                return "org/jd/core/v1/Dup2X2Example".equals(internalName) ? out.toByteArray() : super.load(internalName);
            }
            
            @Override
            public boolean canLoad(String internalName) {
                return "org/jd/core/v1/Dup2X2Example".equals(internalName) || super.canLoad(internalName);
            }
        };
        StringBuilderPrinter printer = new StringBuilderPrinter();
        String output = decompileSuccess(loader, printer, "org/jd/core/v1/Dup2X2Example");
        assertEqualsIgnoreEOL(IOUtils.toString(getClass().getResource("/txt/Dup2X2Example.txt"), StandardCharsets.UTF_8), output);
    }

    @Test
    public void testDouble() throws Exception {
        Dup2X2DoubleExampleCreator creator = new Dup2X2DoubleExampleCreator();
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        creator.create(out);
        Loader loader = new ClassPathLoader() {
            
            @Override
            public byte[] load(String internalName) throws IOException {
                return "org/jd/core/v1/Dup2X2Example".equals(internalName) ? out.toByteArray() : super.load(internalName);
            }
            
            @Override
            public boolean canLoad(String internalName) {
                return "org/jd/core/v1/Dup2X2Example".equals(internalName) || super.canLoad(internalName);
            }
        };
        StringBuilderPrinter printer = new StringBuilderPrinter();
        String output = decompileSuccess(loader, printer, "org/jd/core/v1/Dup2X2Example");
        assertEqualsIgnoreEOL(IOUtils.toString(getClass().getResource("/txt/Dup2X2DoubleExample.txt"), StandardCharsets.UTF_8), output);
    }
}
