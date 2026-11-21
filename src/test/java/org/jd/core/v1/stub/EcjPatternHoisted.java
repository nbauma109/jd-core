/*
 * Copyright (c) 2026 Nicolas Baumann (@nbauma109), with Claude Sonnet 5.5.
 * This project is distributed under the GPLv3 license.
 * This is a copyleft license that gives the user the right to use,
 * copy and modify the code freely for non-commercial purposes.
 */

package org.jd.core.v1.stub;

/**
 * 'Expression.forcedToBeRaw' of the Eclipse compiler: two variables with the same name in different branches, one of them after an instanceof
 */
public class EcjPatternHoisted {
    static class Holder {
        String text;
    }

    static class Reference {
        Object binding;
        Holder holder;
    }

    boolean raw(Object subject, Object other) {
        if (subject instanceof String) {
            Object value = ((Reference) other).binding;
            if (value instanceof Holder) {
                Holder holder = (Holder) value;
                if (holder.text != null) {
                    return holder.text.isEmpty();
                }
            }
        } else if (subject instanceof Integer) {
            Holder holder = ((Reference) other).holder;
            if (holder.text == null) {
                return true;
            }
            Holder holder1 = holder.text.isEmpty() ? holder : null;
            if (holder1 != null) {
                return false;
            }
        }
        return false;
    }
}
