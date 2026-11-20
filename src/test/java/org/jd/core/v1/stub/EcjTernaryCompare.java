/*
 * Copyright (c) 2026 Nicolas Baumann (@nbauma109), with Claude Sonnet 5.5.
 * This project is distributed under the GPLv3 license.
 * This is a copyleft license that gives the user the right to use,
 * copy and modify the code freely for non-commercial purposes.
 */

package org.jd.core.v1.stub;

/**
 * 'Scope.minimalErasedCandidates' of the Eclipse compiler: an int ternary compared to a constant
 */
public class EcjTernaryCompare {
    static final int OBJECT = 1;

    static class Binding {
        int id;

        Binding leaf() {
            return this;
        }
    }

    static Binding first(Binding[] mecs, int count, int dim) {
        switch (count) {
            case 0:
                return null;
            case 1:
                return mecs[0];
            case 2:
                if ((dim == 0 ? mecs[1].id : mecs[1].leaf().id) == OBJECT) {
                    return mecs[0];
                }
                if ((dim == 0 ? mecs[0].id : mecs[0].leaf().id) == OBJECT) {
                    return mecs[1];
                }
        }
        return null;
    }
}
