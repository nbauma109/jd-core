/*
 * Copyright (c) 2026 Nicolas Baumann (@nbauma109), with Claude Sonnet 5.5.
 * This project is distributed under the GPLv3 license.
 * This is a copyleft license that gives the user the right to use,
 * copy and modify the code freely for non-commercial purposes.
 */

package org.jd.core.v1.stub;

import java.util.ArrayList;
import java.util.List;

/**
 * 'Scope.minimalErasedCandidates' of the Eclipse compiler: a switch with an empty default case, in an if, in a loop whose iterations continue in several places
 */
public class EcjSwitchInIf {
    static class Binding {
        int id;
        int dimensions;
        Binding bound;
        Binding[] interfaces;

        boolean isArray() {
            return dimensions > 0;
        }

        Binding erasure() {
            return this;
        }
    }

    Binding create(Binding binding, int dim) {
        return binding;
    }

    Binding[] visit(Binding[] types) {
        List<Binding> toVisit = new ArrayList<>();
        int max = 1;

        toVisit.add(types[0]);
        for (int i = 0; i < max; i++) {
            Binding current = toVisit.get(i);
            int dim = current.dimensions;

            if (dim > 0) {
                switch (current.id) {
                    case 1:
                        if (dim > 1) {
                            max++;
                        }
                        continue;
                    case 2:
                        Binding superType = current.bound;
                        if (!toVisit.contains(superType)) {
                            toVisit.add(superType);
                            max++;
                        }
                        continue;
                    default:
                }
                current = current.bound;
            }
            Binding first = current.bound;

            if (first != null && first.isArray()) {
                Binding superType = dim == 0 ? first : (Binding) create(first, dim);

                if (!toVisit.contains(superType)) {
                    toVisit.add(superType);
                    max++;
                    Binding erasure = first.id == 3 ? superType : superType.erasure();

                    if (erasure != superType) {
                        max++;
                    }
                }
                continue;
            }
            if (current.interfaces != null) {
                for (Binding itsInterface : current.interfaces) {
                    Binding superType = dim == 0 ? itsInterface : (Binding) create(itsInterface, dim);

                    if (!toVisit.contains(superType)) {
                        toVisit.add(superType);
                        max++;
                    }
                }
            }
        }
        return toVisit.toArray(new Binding[0]);
    }

    /** the default case is empty: the code which follows the switch is the one which the test of the 'if' jumps to */
    int skip(List<Binding> types) {
        int count = 0;

        for (int i = 0; i < types.size(); i++) {
            Binding current = types.get(i);

            if (current.dimensions > 0) {
                switch (current.id) {
                    case 1:
                        continue;
                    case 2:
                        count++;
                        continue;
                    default:
                }
            }
            count += 2;
        }
        return count;
    }
}
