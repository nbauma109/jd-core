/*
 * Copyright (c) 2008, 2019 Emmanuel Dupuy.
 * This project is distributed under the GPLv3 license.
 * This is a Copyleft license that gives the user the right to use,
 * copy and modify the code freely for non-commercial purposes.
 */

package org.jd.core.v1.stub;

import java.util.List;

public class ParameterizedCastReceiver {
    private static class Holder<T> {
        private T value;
    }

    @SuppressWarnings("unchecked")
    public String first(Object value) {
        return ((List<String>) value).get(0);
    }

    @SuppressWarnings("unchecked")
    public String value(Object value) {
        return ((Holder<String>) value).value;
    }
}
