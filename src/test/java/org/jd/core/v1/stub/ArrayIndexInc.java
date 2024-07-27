package org.jd.core.v1.stub;

public class ArrayIndexInc {

    static void postIncTest(int i, int[] postInc, int[] idx) {
        postInc[idx[0]++] = i;
    }

    static void preIncTest(int i, int[] preInc, int[] idx) {
        preInc[++idx[0]] = i;
    }
}