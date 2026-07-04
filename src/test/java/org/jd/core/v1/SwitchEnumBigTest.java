package org.jd.core.v1;

import org.jd.core.v1.compiler.CompilerUtil;
import org.jd.core.v1.compiler.InMemoryClassLoader;
import org.jd.core.v1.compiler.InMemoryJavaSourceFileObject;
import org.jd.core.v1.printer.PlainTextPrinter;
import org.junit.Test;

import java.nio.file.Files;
import java.nio.file.Paths;

/**
 * Reproduces a bug where the decompiler emits an invalid {@code case null:} label
 * instead of the enum constant name for a switch-on-enum statement.
 *
 * <p>The Eclipse compiler (ECJ) generates the synthetic {@code $SWITCH_TABLE$...}
 * lookup table with its {@code try}/{@code catch(NoSuchFieldError)} blocks sorted
 * alphabetically by enum constant name, not in declaration order. {@link
 * org.jd.core.v1.service.converter.classfiletojavasyntax.util.SwitchStatementMaker}
 * assumed the first 3 statements of that synthetic method were unrelated setup code
 * and skipped them via {@code statements.listIterator(3)}, which dropped the mapping
 * for whichever constant happens to be alphabetically first among those referenced by
 * any switch in the class (here, {@code ALARM_CONFIG}). That constant's case label
 * then printed as {@code case null:}, which does not recompile.
 */
public class SwitchEnumBigTest extends AbstractJdTest {
    @Test
    public void test() throws Exception {
        String internalClassName = "org/jd/core/test/SwitchEnumBig";
        String src = new String(Files.readAllBytes(Paths.get("src/test/resources/java/org/jd/core/test/SwitchEnumBig.java")));

        InMemoryClassLoader classLoader = new InMemoryClassLoader();
        InMemoryJavaSourceFileObject sourceFileObject = new InMemoryJavaSourceFileObject(internalClassName.replace('/', '.'), src);
        assertTrue("Fixture failed to compile", CompilerUtil.compile("17", classLoader, sourceFileObject));

        String source = decompileSuccess(classLoader, new PlainTextPrinter(), internalClassName);

        assertFalse("Decompiled source contains an invalid 'case null:' label:\n" + source, source.contains("case null:"));

        // Recompile decompiled source code and check errors
        assertTrue(CompilerUtil.compile("17", new InMemoryJavaSourceFileObject(internalClassName, source)));
    }
}
