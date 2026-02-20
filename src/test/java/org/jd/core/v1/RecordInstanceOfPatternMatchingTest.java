package org.jd.core.v1;

import org.junit.Test;

public class RecordInstanceOfPatternMatchingTest extends AbstractJdTest {

    @Test
    public void testJAVAC21() throws Exception {
        test("/jar/RecordInstanceOfPatternMatching-JAVAC-21.jar", "org/jd/core/v1/stub/RecordInstanceOfPatternMatching", "/txt/RecordInstanceOfPatternMatching.txt", "21");
    }

    // TODO Re-enable this test after upgrading ECJ; current dependency rejects source level 25.
    // public void testJAVAC25() throws Exception {
    //     test("/jar/RecordInstanceOfPatternMatching-JAVAC-25.jar", "org/jd/core/v1/stub/RecordInstanceOfPatternMatching", "/txt/RecordInstanceOfPatternMatching.txt", "25");
    // }

    @Test
    public void testECJ21() throws Exception {
        test("/jar/RecordInstanceOfPatternMatching-ECJ-21.jar", "org/jd/core/v1/stub/RecordInstanceOfPatternMatching", "/txt/RecordInstanceOfPatternMatchingECJ.txt", "21");
    }

    // TODO Re-enable this test after upgrading ECJ; current dependency rejects source level 25.
//    public void testECJ25() throws Exception {
//        test("/jar/RecordInstanceOfPatternMatching-ECJ-25.jar", "org/jd/core/v1/stub/RecordInstanceOfPatternMatching", "/txt/RecordInstanceOfPatternMatchingECJ.txt", "25");
//    }

}
