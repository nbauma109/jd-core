/*
 * This project is distributed under the GPLv3 license.
 * This is a Copyleft license that gives the user the right to use,
 * copy and modify the code freely for non-commercial purposes.
 */

package org.jd.core.v1;

import org.jd.core.v1.api.loader.Loader;
import org.jd.core.v1.compiler.CompilerUtil;
import org.jd.core.v1.compiler.InMemoryJavaSourceFileObject;
import org.jd.core.v1.loader.ZipLoader;
import org.jd.core.v1.printer.PlainTextPrinter;
import org.junit.Test;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * Decompiles inputs compiled by recent javac versions (17, 21, 25) and recompiles the result.
 * Only inputs producing bytecode patterns that older compilers do not emit are kept:
 * <ul>
 * <li>javac 17: nestmates (no synthetic accessors), {@code Enum.$values()}, indy string concatenation,
 * javac 9+ try-with-resources, elided checkcasts;</li>
 * <li>javac 21: enum switch without {@code $SwitchMap$}, unused {@code this$0} dropped;</li>
 * <li>javac 25: {@code Objects.requireNonNull(outer)} after the outer instance is stored, captured variables of
 * local classes, lambda numbering.</li>
 * </ul>
 */
public class RecentJavacTest extends AbstractJdTest {

    private void testZip(String zipPath) throws Exception {
        for (String internalTypeName : listTopLevelClasses(zipPath)) {
            testClass(zipPath, internalTypeName);
        }
    }

    private List<String> listTopLevelClasses(String zipPath) throws Exception {
        List<String> names = new ArrayList<>();
        try (InputStream is = this.getClass().getResourceAsStream(zipPath); ZipInputStream zis = new ZipInputStream(is)) {
            for (ZipEntry entry = zis.getNextEntry(); entry != null; entry = zis.getNextEntry()) {
                String name = entry.getName();
                if (name.endsWith(".class") && name.indexOf('$') == -1) {
                    names.add(name.substring(0, name.length() - ".class".length()));
                }
            }
        }
        assertFalse(names.isEmpty());
        return names;
    }

    private void testClass(String zipPath, String internalTypeName) throws Exception {
        try (InputStream is = this.getClass().getResourceAsStream(zipPath)) {
            Loader loader = new ZipLoader(is);
            String source = decompileSuccess(loader, new PlainTextPrinter(), internalTypeName);

            // Synthetic outer instance parameters must not leak into the decompiled code
            assertFalse(internalTypeName + " leaks a synthetic outer instance", source.contains("this$"));
            assertTrue("Recompilation failed for " + internalTypeName + " from " + zipPath + ":\n" + source,
                    CompilerUtil.compile("17", new InMemoryJavaSourceFileObject(internalTypeName, source)));
        }
    }

    @Test
    public void testJdk17Classes() throws Exception {
        testZip("/zip/data-java-jdk-17.0.17.zip");
    }

    @Test
    public void testJdk21Classes() throws Exception {
        testZip("/zip/data-java-jdk-21.0.6.zip");
    }

    @Test
    public void testJdk25Classes() throws Exception {
        testZip("/zip/data-java-jdk-25.0.2.zip");
    }

    @Test
    public void testJdk17MapLambda() throws Exception {
        testZip("/jar/map-lambda-jdk17.0.17.jar");
    }

    @Test
    public void testJdk17StringMap() throws Exception {
        testZip("/jar/string-map-jdk17.0.17.jar");
    }

    @Test
    public void testJdk17AutoUnboxingInLoop() throws Exception {
        testZip("/jar/auto-unboxing-in-loop-jdk17.0.17.jar");
    }

    @Test
    public void testJdk17StaticAccessFromInstance() throws Exception {
        testZip("/jar/static-access-from-instance-jdk17.0.17.jar");
    }

    @Test
    public void testJdk17InnerClassConstructorCall() throws Exception {
        testZip("/jar/inner-class-constructor-call-jdk17.0.17.jar");
    }

    @Test
    public void testJdk17TryResourcesGeneric() throws Exception {
        testZip("/jar/try-resources-generic-jdk17.0.17.jar");
    }

    @Test
    public void testJdk17BoundsAnonymous() throws Exception {
        testZip("/jar/bounds-anonymous-jdk17.0.17.jar");
    }

    @Test
    public void testJdk17BoundsLambda() throws Exception {
        testZip("/jar/bounds-lambda-jdk17.0.17.jar");
    }

    @Test
    public void testJdk21SwitchEnum() throws Exception {
        testZip("/jar/switch-enum-jdk21.0.6.jar");
    }

    @Test
    public void testJdk25BoundsAnonymous() throws Exception {
        testZip("/jar/bounds-anonymous-jdk25.0.2.jar");
    }
}
