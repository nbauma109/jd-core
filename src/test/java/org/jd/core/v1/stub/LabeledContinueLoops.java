package org.jd.core.v1.stub;

/** An inner loop left with 'continue outer' and followed by statements of the outer loop. */
public class LabeledContinueLoops {
    public static int addMissing(int[] items, int[] known) {
        int count = 0;
        next:
        for (int a = 0; a < items.length; a++) {
            int item = items[a];
            for (int b = 0; b < count; b++) {
                if (item == known[b]) {
                    continue next;
                }
            }
            known[count++] = item;
        }
        return count;
    }

    /** The body of the outer loop never falls through: the update of the outer loop is only reached by 'continue outer'. */
    public static int indexOf(byte[] array, byte[] target) {
        outer:
        for (int i = 0; i < array.length - target.length + 1; i++) {
            for (int j = 0; j < target.length; j++) {
                if (array[i + j] != target[j]) {
                    continue outer;
                }
            }
            return i;
        }
        return -1;
    }
}
