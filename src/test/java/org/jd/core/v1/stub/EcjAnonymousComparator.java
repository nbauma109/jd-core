/*
 * Copyright (c) 2026 Nicolas Baumann (@nbauma109), with Claude Sonnet 5.5.
 * This project is distributed under the GPLv3 license.
 * This is a copyleft license that gives the user the right to use,
 * copy and modify the code freely for non-commercial purposes.
 */

package org.jd.core.v1.stub;

import java.util.Arrays;
import java.util.Comparator;

/**
 * 'LookupEnvironment.createIntersectionType18' of the Eclipse compiler: the anonymous comparator is of a supertype of the elements which are sorted
 */
public class EcjAnonymousComparator {
    static class Node {
        String name;

        boolean isClass() {
            return name.isEmpty();
        }
    }

    static class Leaf extends Node {
    }

    static Leaf[] sort(Leaf[] leaves) {
        if (leaves.length > 1 && !leaves[0].isClass()) {
            Arrays.sort(leaves, new Comparator<Node>() {
                @Override
                public int compare(Node first, Node second) {
                    return first.isClass() ? -1 : (second.isClass() ? 1 : first.name.compareTo(second.name));
                }
            });
        }
        return leaves;
    }
}
