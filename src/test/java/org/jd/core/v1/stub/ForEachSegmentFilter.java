/*
 * Copyright (c) 2008, 2019 Emmanuel Dupuy.
 * This project is distributed under the GPLv3 license.
 * This is a Copyleft license that gives the user the right to use,
 * copy and modify the code freely for non-commercial purposes.
 */
package org.jd.core.v1.stub;

import java.util.ArrayList;
import java.util.List;

public class ForEachSegmentFilter {
    public List<Object> filter(List<Object> pieces, boolean reverse) {
        List<Object> result = new ArrayList<>();
        for (Object piece : pieces) {
            if (piece instanceof String) {
                if (piece.equals("segment") == reverse) {
                    result.add(piece);
                }
            } else if (!reverse) {
                result.add(piece);
            }
        }
        return result;
    }
}
