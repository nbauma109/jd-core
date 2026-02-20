package org.jd.core.v1;

import org.junit.Test;

public class RecordInstanceOfPatternMatchingNoDebugTest extends AbstractJdTest {

    @Test
    public void testJAVAC21NoDebugInfo() throws Exception {
        test("/jar/RecordInstanceOfPatternMatchingNoDebug-JAVAC-21-no-debug-info.jar", "org/jd/core/v1/stub/RecordInstanceOfPatternMatchingNoDebug", "/txt/RecordInstanceOfPatternMatchingNoDebug.txt", "21");
    }

    @Test
    public void testECJ21NoDebugInfo() throws Exception {
        test("/jar/RecordInstanceOfPatternMatchingNoDebug-ECJ-21-no-debug-info.jar", "org/jd/core/v1/stub/RecordInstanceOfPatternMatchingNoDebug", "/txt/RecordInstanceOfPatternMatchingNoDebug.txt", "21");
    }

}
