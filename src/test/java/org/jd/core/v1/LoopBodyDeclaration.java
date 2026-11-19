/*
 * Copyright (c) 2026 Nicolas Baumann (@nbauma109), with Claude Sonnet 5.5.
 * This project is distributed under the GPLv3 license.
 * This is a copyleft license that gives the user the right to use,
 * copy and modify the code freely for non-commercial purposes.
 */

package org.jd.core.v1;

/** A variable which the condition of the only statement of the body of a loop assigns, decompiled and recompiled by {@link LoopBodyDeclarationTest} */
public class LoopBodyDeclaration {

    char[][] keyTable = new char[4][];

    void put(char[] key) {
    }

    void rehash() {
        char[] currentKey;
        for (int i = this.keyTable.length; --i >= 0;) {
            if ((currentKey = this.keyTable[i]) != null) {
                put(currentKey);
            }
        }
    }
}
