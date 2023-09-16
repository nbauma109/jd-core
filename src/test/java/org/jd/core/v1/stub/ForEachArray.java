package org.jd.core.v1.stub;

public class ForEachArray {

    void truePositive(String[] args) {
        String[] arr$;
        int j, i$;
        for (arr$ = args, j = arr$.length, i$ = 0; i$ < j; ++i$) { String s = arr$[i$];
            System.out.println(s);
        }
    }

    void falsePositive1(String[] args) {
        String[] arr$;
        int j, i$;
        for (arr$ = args, j = arr$.length, i$ = 0; i$ < j; ) { String s = arr$[i$];
            System.out.println(s);
            ++i$;
        }
    }

    void falsePositive2(String[] args) {
        String[] arr$;
        int j, i$;
        for (arr$ = args, j = arr$.length, i$ = 0; i$ < j; i$ = i$ * 1) { String s = arr$[i$];
            System.out.println(s);
        }
    }
}
