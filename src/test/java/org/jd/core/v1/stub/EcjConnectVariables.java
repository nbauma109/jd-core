/*
 * Copyright (c) 2026 Nicolas Baumann (@nbauma109), with Claude Sonnet 5.5.
 * This project is distributed under the GPLv3 license.
 * This is a copyleft license that gives the user the right to use,
 * copy and modify the code freely for non-commercial purposes.
 */

package org.jd.core.v1.stub;

/**
 * 'Scope.connectTypeVariables' of the Eclipse compiler: a switch in a labeled block, whose code after the switch is also in the block
 */
public class EcjConnectVariables {
    static int connect(String superType, int rank, boolean variable) {
        int tags = 0;
        firstBound: {
            switch (superType.length()) {
                case 1:
                    tags |= 2;
                    break firstBound;
                case 2:
                    if (rank > 0) {
                        if (variable) {
                            tags |= 4;
                            break firstBound;
                        }
                    }
                    break;
                default:
                    if (superType.isEmpty()) {
                        tags |= 8;
                    }
                    break;
            }
            if (!superType.endsWith(";")) {
                tags |= 16;
            } else {
                tags |= 32;
            }
            tags |= superType.length() & 6;
        }
        return tags;
    }
}
