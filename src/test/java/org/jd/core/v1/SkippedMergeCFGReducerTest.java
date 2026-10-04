package org.jd.core.v1;

import org.jd.core.v1.service.converter.classfiletojavasyntax.util.cfg.MinDepthCFGReducer;
import org.jd.core.v1.service.converter.classfiletojavasyntax.util.cfg.SkippedMergeCFGReducer;
import org.junit.Test;

import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

public class SkippedMergeCFGReducerTest {
    @Test
    public void testLabelsTellTheReducersApart() {
        String label = new SkippedMergeCFGReducer(false).getLabel();

        assertTrue(label.contains("explicit jumps"));
        assertNotEquals(new MinDepthCFGReducer(false).getLabel(), label);
    }
}
