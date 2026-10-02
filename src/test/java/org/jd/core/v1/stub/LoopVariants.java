package org.jd.core.v1.stub;

/** Loops whose update is only reached by 'continue', with every kind of nested loop and every kind of update. */
public class LoopVariants {
    /** The update is a compound assignment. */
    public static int skipPairs(int[] values, int start) {
        for (int j = start; j < values.length; j += 2) {
            if (values[j] == 0) {
                continue;
            }
            if (values[j] == 1) {
                return j;
            }
            break;
        }
        return -1;
    }

    /** The update is a decrement. */
    public static int lastNonZero(int[] values) {
        int i = values.length - 1;
        for (; i >= 0; i--) {
            if (values[i] == 0) {
                continue;
            }
            return i;
        }
        return -1;
    }

    /** 'continue outer' from every kind of nested loop. */
    public static int find(int[][] rows, int target) {
        int found = -1;
        outer:
        for (int i = 0; i < rows.length; i++) {
            for (int value : rows[i]) {
                if (value == target) {
                    continue outer;
                }
            }
            int k = 0;
            while (k < 3) {
                k++;
                if (k == 2) {
                    continue outer;
                }
            }
            for (int j = 0; j < 2; j++) {
                if (rows[i].length == j) {
                    continue outer;
                }
            }
            found = i;
        }
        return found;
    }

    /** The 'continue' are not all preceded by the same update. */
    public static int differentUpdates(int[] values, int n) {
        int i = 0;
        for (; i < n; i++) {
            if (values[i] == 1) {
                i++;
                continue;
            }
            if (values[i] == 2) {
                i += 2;
                continue;
            }
            if (values[i] == 3) {
                continue;
            }
            return i;
        }
        return -1;
    }

    /** A 'continue' of the nested loop is its own: it is not preceded by the update of the main loop. */
    public static int nestedContinue(int[][] rows) {
        int total = 0;
        for (int i = 0; i < rows.length; i++) {
            for (int j = 0; j < rows[i].length; j++) {
                if (rows[i][j] == 0) {
                    continue;
                }
                total += rows[i][j];
            }
            if (total > 100) {
                continue;
            }
            return total;
        }
        return -1;
    }

    /** The update is a variable which is not in the condition. */
    public static int updateOfAnotherVariable(int[] values) {
        int count = 0;
        for (int i = 0; i < values.length; ) {
            if (values[i] == 0) {
                count++;
                continue;
            }
            return count;
        }
        return -1;
    }
}
