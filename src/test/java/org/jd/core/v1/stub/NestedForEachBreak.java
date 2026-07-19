/*
 * Copyright (c) 2008, 2019 Emmanuel Dupuy.
 * This project is distributed under the GPLv3 license.
 * This is a Copyleft license that gives the user the right to use,
 * copy and modify the code freely for non-commercial purposes.
 */
package org.jd.core.v1.stub;

import java.util.List;

public class NestedForEachBreak {
    public int connect(List<StringBuilder> segments) {
        int connected = 0;
        for (StringBuilder segment : segments) {
            if (segment.length() == 0) {
                for (StringBuilder candidate : segments) {
                    if (candidate.length() > 0) {
                        segment.append(candidate);
                        connected++;
                        break;
                    }
                }
            }
        }
        return connected;
    }
}
