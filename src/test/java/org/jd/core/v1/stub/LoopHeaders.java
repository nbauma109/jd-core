package org.jd.core.v1.stub;

/** The update of a 'for' stays in its header, whatever the body is. */
public class LoopHeaders {
    /** The body never falls through: it ends with a 'break', the update is only reached by 'continue'. */
    public static int lastIndexOfData(byte[] map, String text) {
        int j = text.length() - 1;
        for (; j >= 0; j--) {
            byte code = map[text.charAt(j)];
            if (code == 127) {
                continue;
            }
            if (code == -1) {
                return -1;
            }
            break;
        }
        return j;
    }

    /** The condition of the loop contains a ternary operator: ECJ compiles it as a loop which starts with a test. */
    public static int sum(int[] values, int[] extra) {
        int total = 0;
        for (int i = 0; i < (extra == null ? 0 : extra.length); i++) {
            int value;
            total += (value = values[i]);
            switch (value) {
                case 1:
                    total += 2;
                    break;
                default:
                    total++;
                    break;
            }
        }
        return total;
    }

    /** The first statement of the body is on the line of the header. */
    public static int count(char[] descriptor, int parameters) {
        int index = 0;
        int end = 0;
        for (int i = 0; i < parameters; i++) { while (descriptor[++end] == '[') { }
            if (descriptor[end] == 'L') {
                while (descriptor[++end] != ';') { }
            }
            index = end + 1;
        }
        return index;
    }

    /** The update is a statement of the body, on its own line: it is not the update of a 'for'. */
    public static int skipRun(String pattern, int position, char c, StringBuilder buffer) {
        while (position + 1 < pattern.length()) {
            char peek = pattern.charAt(position + 1);
            if (peek == c) {
                buffer.append(c);
                position++;
            } else {
                break;
            }
        }
        return position;
    }
}
