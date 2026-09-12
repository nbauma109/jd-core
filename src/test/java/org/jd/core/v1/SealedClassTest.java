package org.jd.core.v1;

import java.io.IOException;

import org.jd.core.v1.api.loader.Loader;
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
        String internalName = "org/jd/core/v1/SealedExample$OpenChild";
        String source = decompileSuccess(new ClassPathLoader(), new StringBuilderPrinter(), internalName);

        assertTrue(source.contains("sealed class SealedExample"));
        assertTrue(source.contains("non-sealed class OpenChild"));
        assertTrue(source.contains("OpenChild self(OpenChild"));
        assertTrue(CompilerUtil.compile("17", new InMemoryJavaSourceFileObject("org/jd/core/v1/SealedExample", source)));
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
        String internalName = "org/jd/core/v1/SealedInterfaceExample$OpenBranch";
        String source = decompileSuccess(new ClassPathLoader(), new StringBuilderPrinter(), internalName);

        assertTrue(source.contains("sealed interface SealedInterfaceExample"));
        assertTrue(source.contains("non-sealed interface OpenBranch"));
        assertTrue(CompilerUtil.compile("17", new InMemoryJavaSourceFileObject("org/jd/core/v1/SealedInterfaceExample", source)));
    }

    @Test
    public void testSinglePermittedSubclass() throws Exception {
        String source = decompileSuccess(new ClassPathLoader(), new StringBuilderPrinter(), "org/jd/core/v1/SinglePermitExample");

        assertTrue(source.contains("sealed class SinglePermitExample permits SinglePermitExample.OnlyChild"));
        assertTrue(source.contains("final class OnlyChild"));
        assertTrue(CompilerUtil.compile("17", new InMemoryJavaSourceFileObject("org/jd/core/v1/SinglePermitExample", source)));
    }

    @Test
    public void testPreviewVersionPermittedChild() throws Exception {
        String internalName = "org/jd/core/v1/SealedExample$OpenChild";
        String source = decompileSuccess(loaderWithChildVersion(60, 0xFFFF), new StringBuilderPrinter(), internalName);

        assertTrue(source.contains("non-sealed class OpenChild"));
        assertTrue(CompilerUtil.compile("17", new InMemoryJavaSourceFileObject("org/jd/core/v1/SealedExample", source)));
    }

    @Test
    public void testLegacyVersionPermittedChild() throws Exception {
        String internalName = "org/jd/core/v1/SealedExample$OpenChild";
        String source = decompileSuccess(loaderWithChildVersion(52, 0), new StringBuilderPrinter(), internalName);

        assertTrue(source.contains("non-sealed class OpenChild"));
        assertTrue(CompilerUtil.compile("17", new InMemoryJavaSourceFileObject("org/jd/core/v1/SealedExample", source)));
    }

    private Loader loaderWithChildVersion(int major, int minor) {
        String internalName = "org/jd/core/v1/SealedExample$OpenChild";
        ClassPathLoader classPathLoader = new ClassPathLoader();
        return new Loader() {
            @Override
            public boolean canLoad(String name) {
                return classPathLoader.canLoad(name);
            }

            @Override
            public byte[] load(String name) throws IOException {
                byte[] bytes = classPathLoader.load(name);
                if (internalName.equals(name)) {
                    bytes = bytes.clone();
                    bytes[4] = (byte) (minor >>> 8);
                    bytes[5] = (byte) minor;
                    bytes[6] = (byte) (major >>> 8);
                    bytes[7] = (byte) major;
                }
                return bytes;
            }
        };
    }

    @Test
    public void testStandaloneNestedSealedInterface() throws Exception {
        String internalName = "org/jd/core/v1/NestedSealedExample$Branch";
        String source = decompileSuccess(new ClassPathLoader(), new StringBuilderPrinter(), internalName);

        assertTrue(source.contains("class NestedSealedExample"));
        assertTrue(source.contains("sealed interface Branch"));
        assertTrue(CompilerUtil.compile("17", new InMemoryJavaSourceFileObject("org/jd/core/v1/NestedSealedExample", source)));
    }

    @Test
    public void testStandalonePermittedRecord() throws Exception {
        String internalName = "org/jd/core/v1/NestedSealedExample$Branch$RecordChild";
        String source = decompileSuccess(new ClassPathLoader(), new StringBuilderPrinter(), internalName);

        assertTrue(source.contains("sealed interface Branch"));
        assertTrue(source.contains("record RecordChild(int value, RecordChild previous)"));
        assertTrue(CompilerUtil.compile("17", new InMemoryJavaSourceFileObject("org/jd/core/v1/NestedSealedExample", source)));
    }

    @Test
    public void testStandalonePermittedEnum() throws Exception {
        String internalName = "org/jd/core/v1/NestedSealedExample$Branch$EnumChild";
        String source = decompileSuccess(new ClassPathLoader(), new StringBuilderPrinter(), internalName);

        assertTrue(source.contains("sealed interface Branch"));
        assertTrue(source.contains("enum EnumChild"));
        assertTrue(CompilerUtil.compile("17", new InMemoryJavaSourceFileObject("org/jd/core/v1/NestedSealedExample", source)));
    }

    @Test
    public void testStandaloneNestedEnumWithConstantBody() throws Exception {
        String internalName = "org/jd/core/v1/NestedSealedExample$BehavioralEnum";
        String source = decompileSuccess(new ClassPathLoader(), new StringBuilderPrinter(), internalName);

        assertTrue(source.contains("enum BehavioralEnum"));
        assertFalse(source.contains("class NestedSealedExample"));
    }
}
