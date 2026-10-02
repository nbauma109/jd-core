package org.jd.core.v1.stub;

public class ForEachArrayUpdates {
    /** The body never falls through: the compiler only copies the update of the index before each 'continue' */
    public boolean noFallThrough(int[][] ranges, int r) {
        for (int[] range : ranges) {
            if (r > range[1]) {
                continue;
            }
            if (r < range[0]) {
                return false;
            }
            return ((r - range[0]) % range[2]) == 0;
        }
        return false;
    }

    /** ECJ evaluates the array once with '(array$ = fields).length' */
    public Object find(String[] fields, String name) {
        if (fields != null) {
            for (String field : fields) {
                if (name.equals(field)) {
                    return field;
                }
            }
        }
        return null;
    }

    /** The 'continue outer' jumps to the update of the index of the outer loop */
    public int nested(String[][] rows) {
        int n = 0;
        outer:
        for (String[] row : rows) {
            for (String c : row) {
                if (c == null) {
                    continue outer;
                }
                if (c.isEmpty()) {
                    continue;
                }
                n++;
            }
            if (n > 5) {
                continue;
            }
            return n;
        }
        return n;
    }
}
