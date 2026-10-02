package org.jd.core.v1.stub;

/** A 'return' or a 'throw' which is reached from several branches. */
public class SharedExits {
    public static boolean allSet(Object a, Object b, Object c) {
        return a != null
                && b != null
                && c != null;
    }

    public static boolean sameKind(String a, String b, int level) {
        return a.length() == b.length()
                && a.charAt(0) == b.charAt(0)
                && (level >= 8
                        ? a.indexOf('x') == b.indexOf('x')
                                && a.indexOf('y') == b.indexOf('y')
                        : a.equals(b))
                && a.hashCode() == b.hashCode();
    }

    /** The statements are on their own lines: it is not a 'return' of a boolean expression. */
    public static boolean separateLines(Object a, Object b) {
        if (a != null
                && b != null) {
            return true;
        }
        return false;
    }

    @SuppressWarnings("java:S6208") // The classic form of the labels is the one under test
    public static int parse(String text) {
        int sign = 1;
        loop:
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            switch (c) {
                case '-':
                    sign = -1;
                    break;
                case ':':
                    if (sign < 0) {
                        if (i == 0) {
                            break loop;
                        }
                        sign = 0;
                    }
                    return i;
                default:
                    break;
            }
        }
        throw new IllegalArgumentException("invalid text: " + text);
    }

    private int flags;

    public void flags(String text) {
        int sign = 1;
        boolean sawFlag = false;
        int position = 0;
        loop:
        while (position < text.length()) {
            char c = text.charAt(position++);
            switch (c) {
                default:
                    break loop;
                case 'i':
                    flags |= 1;
                    sawFlag = true;
                    continue;
                case '-':
                    if (sign < 0) {
                        break loop;
                    }
                    sign = -1;
                    sawFlag = false;
                    continue;
                case ':':
                case ')':
                    break;
            }
            if (sign < 0) {
                if (!sawFlag) {
                    break loop;
                }
                flags = ~flags;
            }
            if (c == ':') {
                flags++;
            }
            return;
        }

        throw new IllegalArgumentException("invalid flags: " + text);
    }
}
