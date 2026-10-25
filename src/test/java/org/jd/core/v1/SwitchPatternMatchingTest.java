/*
 * Copyright (c) 2026 Nicolas Baumann (@nbauma109), with Claude Sonnet 5.5.
 * This project is distributed under the GPLv3 license.
 * This is a Copyleft license that gives the user the right to use,
 * copy and modify the code freely for non-commercial purposes.
 */
package org.jd.core.v1;

import org.junit.Test;

/** 'switch' with patterns, which javac 21 compiles to 'invokedynamic typeSwitch' */
public class SwitchPatternMatchingTest extends AbstractJdTest {

    @Test
    public void testJAVAC21() throws Exception {
        test("/jar/SwitchPatternMatching-JAVAC-21.jar", "org/jd/core/v1/stub/SwitchPatternMatching", "/txt/SwitchPatternMatching.txt", "21");
    }

    /** ECJ compiles the same switches, without the record patterns whose lowered form is not the one of javac */
    @Test
    public void testECJ21() throws Exception {
        test("/jar/SwitchPatternMatching-ECJ-21.jar", "org/jd/core/v1/stub/SwitchPatternMatchingEcj", "/txt/SwitchPatternMatchingEcj.txt", "21");
    }
}
