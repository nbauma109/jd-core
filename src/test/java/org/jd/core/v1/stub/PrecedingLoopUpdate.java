/*
 * Copyright (c) 2008, 2019 Emmanuel Dupuy.
 * This project is distributed under the GPLv3 license.
 * This is a Copyleft license that gives the user the right to use,
 * copy and modify the code freely for non-commercial purposes.
 */

package org.jd.core.v1.stub;

public class PrecedingLoopUpdate {
    public int updateOnce(boolean condition, boolean skip) {
        int value = 3;
        value--;
        while (condition) {
            if (skip) {
                continue;
            }
            break;
        }
        return value;
    }
}
