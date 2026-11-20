/*
 * Copyright (c) 2026 Nicolas Baumann (@nbauma109), with Claude Sonnet 5.5.
 * This project is distributed under the GPLv3 license.
 * This is a copyleft license that gives the user the right to use,
 * copy and modify the code freely for non-commercial purposes.
 */

package org.jd.core.v1.stub;

/**
 * 'Scope.leastContainingTypeArgument' of the Eclipse compiler: the same name is declared in the cases of several switches, in different branches
 */
public class EcjSwitchLocals {
    static String[] pair(String a, String b) {
        return new String[] { a, b };
    }

    static String least(String u, String v, int kindU, int kindV) {
        if (u == null) {
            return v;
        }
        if (v.length() > 3) {
            if (u.length() > 3) {
                switch (kindU) {
                    case 1:
                        switch (kindV) {
                            case 1:
                                return u + v;
                            case 2:
                                return u;
                        }
                        break;
                    case 2:
                        if (kindV == 2) {
                            String[] glb = pair(u, v);
                            if (glb == null) {
                                return null;
                            }
                            return glb[0];
                        }
                        break;
                }
            } else {
                switch (kindV) {
                    case 1:
                        String lub = u + v;
                        return lub;
                    case 2:
                        String[] glb = pair(u, v);
                        if (glb == null) {
                            return null;
                        }
                        return glb[0];
                }
            }
        } else if (u.length() > 3) {
            switch (kindU) {
                case 1:
                    String lub = u + v;
                    return lub;
                case 2:
                    String[] glb = pair(v, u);
                    if (glb == null) {
                        return null;
                    }
                    return glb[0];
            }
        }
        return v;
    }
}
