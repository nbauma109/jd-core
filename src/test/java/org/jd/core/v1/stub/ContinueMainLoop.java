package org.jd.core.v1.stub;

/** The inner loop is left with 'continue' of the main loop, or ends with a 'return'. */
public class ContinueMainLoop {
    public static boolean contains(char[] data, char c, int start) {
        int ptr = start;
        main_loop:
        while (true) {
            int count = data[ptr];
            if (count == 0) {
                return false;
            }
            int branchEnd = ptr + (count << 1);
            for (ptr += 4; ptr < branchEnd; ptr += 2) {
                if (data[ptr] == c) {
                    ptr = data[ptr + 1];
                    continue main_loop;
                }
            }
            return false;
        }
    }

    /** The main loop is a 'do ... while': 'continue main_loop' jumps to its condition. */
    public static boolean containsInDoWhile(char[] data, char c) {
        int ptr = 0;
        main_loop:
        do {
            int count = data[ptr++];
            if (count >= 4) {
                int branchEnd = ptr + (count << 1);
                for (ptr += 4; ptr < branchEnd; ptr += 2) {
                    if (data[ptr] == c) {
                        ptr = data[ptr + 1];
                        continue main_loop;
                    }
                }
                return false;
            }
            ptr = data[ptr];
        } while (ptr != 0);
        return true;
    }
}
