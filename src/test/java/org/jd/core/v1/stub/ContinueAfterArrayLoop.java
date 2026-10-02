package org.jd.core.v1.stub;

import java.util.ArrayList;
import java.util.List;

/** An inner loop on an array is left with 'continue' of the outer loop in two places, and is followed by a 'throw'. */
public class ContinueAfterArrayLoop {
    private final List<String> messages = new ArrayList<>();

    public void check(int[] numbers, int[] positions) {
        List<Integer> seen = new ArrayList<>();
        lineNumberLoop: for (final int number : numbers) {
            for (final int position : positions) {
                if (position == number) {
                    if (seen.contains(number)) {
                        messages.add("twice " + number);
                    } else {
                        seen.add(number);
                    }
                    continue lineNumberLoop;
                }
            }
            throw new IllegalStateException("missing " + number);
        }
    }
}
