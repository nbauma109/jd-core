/*
 * Copyright (c) 2026 Nicolas Baumann (@nbauma109), with Claude Sonnet 5.5.
 * This project is distributed under the GPLv3 license.
 * This is a copyleft license that gives the user the right to use,
 * copy and modify the code freely for non-commercial purposes.
 */

package org.jd.core.v1.stub;

/**
 * The binding of a pattern is not renamed in a method which has an anonymous class: the variables which it captures are fields of the class
 */
public class EcjPatternHoistedClassBody {
    static class Holder {
        String text;
    }

    static class Reference {
        Object binding;
        Holder holder;
    }

    Runnable raw(Object subject, Object other) {
        if (subject instanceof String) {
            Object value = ((Reference) other).binding;
            if (value instanceof Holder) {
                Holder holder = (Holder) value;
                if (holder.text != null) {
                    return new Runnable() {
                        @Override
                        public void run() {
                            System.out.println(holder.text);
                        }
                    };
                }
            }
        } else if (subject instanceof Integer) {
            Holder holder = ((Reference) other).holder;
            if (holder.text == null) {
                return null;
            }
        }
        return null;
    }
}
