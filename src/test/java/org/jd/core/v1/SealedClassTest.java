package org.jd.core.v1;

import org.jd.core.v1.compiler.CompilerUtil;
import org.jd.core.v1.compiler.InMemoryJavaSourceFileObject;
import org.jd.core.v1.loader.ClassPathLoader;
import org.jd.core.v1.printer.StringBuilderPrinter;
import org.junit.Test;

public class SealedClassTest extends AbstractJdTest {
    @Test
    public void testSealedClassAndPermittedSubclasses() throws Exception {
        ClassPathLoader loader = new ClassPathLoader();
        String source = decompileSuccess(loader, new StringBuilderPrinter(), "org/jd/core/v1/SealedExample");

        assertTrue(source.contains("sealed class SealedExample"));
        assertTrue(source.contains("permits SealedExample.FinalChild, SealedExample.OpenChild"));
        assertTrue(source.contains("non-sealed class OpenChild"));
        assertTrue(source.contains("final class FinalChild"));
        assertTrue(CompilerUtil.compile("17", new InMemoryJavaSourceFileObject("org/jd/core/v1/SealedExample", source)));
    }

    @Test
    public void testNonSealedChildOnItsOwn() throws Exception {
        String source = decompileSuccess(new ClassPathLoader(), new StringBuilderPrinter(), "org/jd/core/v1/SealedExample$OpenChild");

        assertTrue(source.contains("non-sealed class OpenChild"));
    }

    @Test
    public void testSealedInterface() throws Exception {
        ClassPathLoader loader = new ClassPathLoader();
        String source = decompileSuccess(loader, new StringBuilderPrinter(), "org/jd/core/v1/SealedInterfaceExample");

        assertTrue(source.contains("sealed interface SealedInterfaceExample"));
        assertTrue(source.contains("permits SealedInterfaceExample.OpenBranch, SealedInterfaceExample.FinalBranch"));
        assertTrue(source.contains("non-sealed interface OpenBranch"));
        assertTrue(CompilerUtil.compile("17", new InMemoryJavaSourceFileObject("org/jd/core/v1/SealedInterfaceExample", source)));
    }

    @Test
    public void testNonSealedInterfaceOnItsOwn() throws Exception {
        String source = decompileSuccess(new ClassPathLoader(), new StringBuilderPrinter(), "org/jd/core/v1/SealedInterfaceExample$OpenBranch");

        assertTrue(source.contains("non-sealed interface OpenBranch"));
    }
}
