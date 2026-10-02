package org.jd.core.v1.stub;

/** The inner loop is left with 'break' to statements of the main loop, or with 'break main'. */
public class BreakToOuterTail {
    public static int compress(int[] src, int limit) {
        int anchor = 0;
        int sOff = 1;
        main:
        while (true) {
            int ref;
            do {
                if (sOff > limit) {
                    break main;
                }
                ref = src[sOff];
                sOff++;
            } while (src[ref] != 1);

            while (true) {
                sOff += 2;
                if (sOff > limit - 5) {
                    anchor = sOff;
                    break main;
                }
                if (src[sOff] != 3) {
                    break;
                }
                anchor++;
            }
            anchor = sOff++;
        }
        return anchor;
    }
}
