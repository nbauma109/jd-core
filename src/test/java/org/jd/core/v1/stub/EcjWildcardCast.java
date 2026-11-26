/*
 * Copyright (c) 2026 Nicolas Baumann (@nbauma109), with Claude Sonnet 5.5.
 * This project is distributed under the GPLv3 license.
 * This is a copyleft license that gives the user the right to use,
 * copy and modify the code freely for non-commercial purposes.
 */

package org.jd.core.v1.stub;

import java.util.List;

/**
 * 'AnnotationValueImpl.accept' of the Eclipse compiler: a field of type 'Object' passed to a parameter whose type has a wildcard, by a call whose result is a type variable
 */
public class EcjWildcardCast {
    interface Visitor<R, P> {
        R visitList(List<? extends CharSequence> values, P parameter);
    }

    Object value;

    @SuppressWarnings("unchecked")
    <R, P> R accept(Visitor<R, P> visitor, P parameter) {
        return visitor.visitList((List<String>) value, parameter);
    }
}
