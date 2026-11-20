/*
 * Copyright (c) 2026 Nicolas Baumann (@nbauma109), with Claude Sonnet 5.5.
 * This project is distributed under the GPLv3 license.
 * This is a copyleft license that gives the user the right to use,
 * copy and modify the code freely for non-commercial purposes.
 */

package org.jd.core.v1.stub;

import java.util.HashSet;
import java.util.Set;

/**
 * 'Scope.connectTypeVariables' of the Eclipse compiler: a loop over the bounds, whose variable was assigned before the loop
 */
public class EcjBoundsLoop {
    static class Ref {
        String resolved;

        String resolve(boolean check, int location) {
            return check ? "a" + location : null;
        }

        String resolve(int location) {
            return "b" + location;
        }
    }

    static class Param {
        Ref type;
        Ref[] bounds;
        int tags;
        int rank;
    }

    int kind;

    boolean connect(Param[] params, boolean checkCollisions) {
        boolean noProblems = true;
        int length = params.length;
        nextVariable: for (int i = 0; i < length; i++) {
            Param param = params[i];
            Ref ref = param.type;
            if (ref == null) {
                continue nextVariable;
            }
            boolean firstIsVariable = false;
            String superType = this.kind == 1 ? ref.resolve(false, 3) : ref.resolve(3);
            if (superType == null) {
                param.tags |= 1;
            } else {
                ref.resolved = superType;
                firstBound: {
                    switch (superType.length()) {
                        case 1:
                            param.tags |= 2;
                            break firstBound;
                        case 3:
                            break firstBound;
                        case 2:
                            firstIsVariable = true;
                            if (param.rank >= superType.length()) {
                                Set<String> set = new HashSet<>();
                                set.add(ref.resolved);
                                if (set.contains(superType)) {
                                    param.tags |= 4;
                                    break firstBound;
                                }
                            }
                            break;
                        default:
                            if (superType.isEmpty() && param.rank == 0) {
                                param.tags |= 8;
                            }
                            break;
                    }
                    String superRef = superType;
                    if (!superType.endsWith(";")) {
                        param.rank = superRef.length();
                    } else {
                        param.rank = 0;
                    }
                    param.tags |= superType.length() & 6;
                }
            }
            Ref[] bounds = param.bounds;
            if (bounds != null) {
                nextBound: for (int j = 0, boundLength = bounds.length; j < boundLength; j++) {
                    ref = bounds[j];
                    superType = this.kind == 1 ? ref.resolve(false, 4) : ref.resolve(4);
                    if (superType == null) {
                        param.tags |= 1;
                        continue nextBound;
                    } else {
                        param.tags |= superType.length() & 6;
                        boolean complained = !ref.resolved.isEmpty();
                        if (firstIsVariable && j == 0) {
                            param.tags |= 16;
                            complained = true;
                        } else if (superType.startsWith("[")) {
                            if (!complained) {
                                param.tags |= 32;
                            }
                            continue nextBound;
                        } else {
                            if (!superType.endsWith(";")) {
                                if (!complained) {
                                    param.tags |= 64;
                                }
                                continue nextBound;
                            }
                        }
                        if (checkCollisions && superType.length() == param.rank && superType.length() > 5) {
                            continue nextBound;
                        }
                        for (int index = params.length; --index >= 0;) {
                            if (params[index].rank == superType.length()) {
                                param.tags |= 128;
                                continue nextBound;
                            }
                            if (checkCollisions && params[index].tags == superType.length()) {
                                continue nextBound;
                            }
                        }
                        param.rank = superType.length();
                    }
                }
            }
            noProblems &= (param.tags & 1) == 0;
        }
        return noProblems;
    }
}
