package org.jd.core.v1.stub;

import static org.apache.commons.lang3.ArrayUtils.swap;

public class Shift {

    public static void shift(boolean[] array, int startIndexInclusive, int n, int offset) {
        while (n > 1 && offset > 0) {
            int n_offset = n - offset;

            if (offset > n_offset) {
                swap(array, startIndexInclusive, startIndexInclusive + n - n_offset,  n_offset);
                n = offset;
                offset -= n_offset;
            } else if (offset < n_offset) {
                swap(array, startIndexInclusive, startIndexInclusive + n_offset,  offset);
                startIndexInclusive += offset;
                n = n_offset;
            } else {
                swap(array, startIndexInclusive, startIndexInclusive + n_offset, offset);
                break;
            }
        }
    }
}
