/*
 * Copyright (c) 2008, 2019 Emmanuel Dupuy.
 * This project is distributed under the GPLv3 license.
 * This is a Copyleft license that gives the user the right to use,
 * copy and modify the code freely for non-commercial purposes.
 */

package org.jd.core.v1.stub;

public interface OverloadLogger {
    boolean isEnabled(CharSequence message, Throwable t);

    boolean isEnabled(Object message, Throwable t);

    @SuppressWarnings("unused")
    abstract class TestOverload implements OverloadLogger {
        public boolean isEnabled() {
            return isEnabled((Object) null, null);
        }
    }
}
