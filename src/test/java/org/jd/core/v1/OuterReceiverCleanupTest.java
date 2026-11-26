package org.jd.core.v1;

import java.nio.charset.StandardCharsets;
import java.util.Map;

import org.jd.core.v1.api.loader.Loader;
import org.jd.core.v1.compiler.CompilerUtil;
import org.jd.core.v1.compiler.InMemoryJavaSourceFileObject;
import org.jd.core.v1.printer.PlainTextPrinter;
import org.junit.Test;

public class OuterReceiverCleanupTest extends AbstractJdTest {
    @Test
    public void restoresStableOuterReceiverWithoutRepeatingEffectfulReceiver() throws Exception {
        String internalName = "sweepup/SweepUpFixture";
        String fixture;
        try (var source = getClass().getResourceAsStream("/sweepup/SweepUpFixture.java")) {
            fixture = new String(source.readAllBytes(), StandardCharsets.UTF_8);
        }
        Loader loader = RestoreMethodReferenceTest.compileForJava8("-g", Map.of(internalName, fixture));
        String source = decompileSuccess(loader, new PlainTextPrinter(), internalName);

        assertTrue(source, source.contains("combine(SweepUpFixture.this, SweepUpFixture.this.value())"));
        // One declaration and one call to next; duplicating the receiver would add another call.
        assertEquals(source, 3, source.split("next\\(\\)", -1).length);
        assertTrue(CompilerUtil.compile("17", new InMemoryJavaSourceFileObject(internalName, source)));
    }
}
