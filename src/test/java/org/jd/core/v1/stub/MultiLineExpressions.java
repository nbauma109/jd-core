package org.jd.core.v1.stub;

public class MultiLineExpressions {
    private String helper = " helper ";
    private int count;
    private boolean flag;

    public String chain(String suffix) {
        return this.helper
                .trim()
                .concat(suffix);
    }

    public String arguments(String a) {
        return format(
                a.trim(),
                this.helper.trim(),
                this.helper.length());
    }

    public boolean finallyCopies(int x) {
        boolean saved = this.flag;
        try {
            if (x == 1) {
                this.count = 1;
                return false;
            }
            if (x == 2) {
                this.count = 2;
                return false;
            }
            this.count = 3;
            this.count++;
            this.flag = x > 3;
            return true;
        } finally {
            this.flag = saved;
        }
    }

    public int loop(int x) {
        int n = 0;
        do { /* reduce */
            if (x > 100) {
                n++;
            }

            n += this.count;
            x = x - n;

            if (x == 17) {
                return n;
            }
        } while (x > 50);
        return n;
    }

    static String format(String a, String b, int c) {
        return a + b + c;
    }

    public boolean wrapped(Object[] values, int i) {
        return values[i] instanceof String
                && isBlank((String) values[i]);
    }

    static boolean isBlank(String s) {
        return s.trim().isEmpty();
    }

    int one() {
        return 1;
    }
    int two() {
        return 2;
    }
}
