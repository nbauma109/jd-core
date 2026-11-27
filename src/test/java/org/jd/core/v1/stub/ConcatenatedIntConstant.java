/*
 * Copyright (c) 2026 Nicolas Baumann (@nbauma109), with Claude Sonnet 5.5.
 * This project is distributed under the GPLv3 license.
 * This is a copyleft license that gives the user the right to use,
 * copy and modify the code freely for non-commercial purposes.
 */

package org.jd.core.v1.stub;

/**
 * 'TemporaryFolder.createTemporaryFolderIn' of JUnit: constants of type 'int' and 'char' in string concatenations
 */
public class ConcatenatedIntConstant {
    private static final int ATTEMPTS = 10000;

    String attempts(String name) {
        return name + " tried " + ATTEMPTS + " times, " + 65;
    }

    String first(String name) {
        return ATTEMPTS + name;
    }

    String characters(String name) {
        return name + 'A' + '✐';
    }
}
