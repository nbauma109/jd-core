/*
 * Copyright (c) 2026 Nicolas Baumann (@nbauma109), with Claude Sonnet 5.5.
 * This project is distributed under the GPLv3 license.
 * This is a copyleft license that gives the user the right to use,
 * copy and modify the code freely for non-commercial purposes.
 */

package org.jd.core.v1.stub;

import java.util.List;

/**
 * 'Scope.findMethodInSuperInterfaces' of the Eclipse compiler: the 'continue' of the outer loop is in a nested loop, and the last loop of the outer
 * body ends where the outer loop is updated
 */
public class EcjLabeledContinue {
    static void visit(String[] initial, List<String> visited, List<String> found) {
        String[] toVisit = initial;
        int next = toVisit.length;
        interfaces: for (int i = 0; i < next; i++) {
            String current = toVisit[i];
            if (visited != null) {
                for (String v : visited) {
                    if (current.equals(v)) {
                        continue interfaces;
                    }
                }
                visited.add(current);
            }
            found.add(current);
            String[] supers = current.split(",");
            if (supers.length > 1) {
                int length = supers.length;
                if (next + length >= toVisit.length) {
                    System.arraycopy(toVisit, 0, toVisit = new String[next + length + 5], 0, next);
                }
                int a;
                label: for (a = 0; a < length; a++) {
                    String s = supers[a];
                    for (int b = 0; b < next; b++) {
                        if (s.equals(toVisit[b])) {
                            continue label;
                        }
                    }
                    toVisit[next++] = s;
                }
            }
        }
    }
}
