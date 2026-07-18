/*
 * Copyright (c) 2008, 2019 Emmanuel Dupuy.
 * This project is distributed under the GPLv3 license.
 * This is a Copyleft license that gives the user the right to use,
 * copy and modify the code freely for non-commercial purposes.
 */

package org.jd.core.v1.stub;

public class BoundedGenericReturnCast<T extends Number> {
    private T get() {
        return null;
    }

    public Integer narrow() {
        return (Integer) get();
    }
}
