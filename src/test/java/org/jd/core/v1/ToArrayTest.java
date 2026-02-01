package org.jd.core.v1;

import org.jd.core.v1.compiler.CompilerUtil;
import org.jd.core.v1.compiler.InMemoryJavaSourceFileObject;
import org.jd.core.v1.loader.ClassPathLoader;
import org.jd.core.v1.printer.PlainTextPrinter;
import org.jd.core.v1.regex.PatternMaker;
import org.junit.Test;

import java.util.List;

public class ToArrayTest extends AbstractJdTest {

    @Test
    public void testToArray() throws Exception { // https://github.com/java-decompiler/jd-core/issues/16
        @SuppressWarnings("unused")
        class ToArray {
            String[] toArray(List<String> list) {
                return list.toArray(String[]::new);
            }
        }
        String internalClassName = ToArray.class.getName().replace('.', '/');
        String source = decompileSuccess(new ClassPathLoader(), new PlainTextPrinter(), internalClassName);
        
        // Check decompiled source code
        assertTrue(source.matches(PatternMaker.make("return list.toArray(String[]::new);")));
        
        // Recompile decompiled source code and check errors
        assertTrue(CompilerUtil.compile("1.8", new InMemoryJavaSourceFileObject(internalClassName, source)));
    }
}
