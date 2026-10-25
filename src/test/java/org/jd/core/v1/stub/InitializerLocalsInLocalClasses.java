/*
 * Copyright (c) 2026 Nicolas Baumann (@nbauma109), with Claude Sonnet 5.5.
 * This project is distributed under the GPLv3 license.
 * This is a Copyleft license that gives the user the right to use,
 * copy and modify the code freely for non-commercial purposes.
 */

package org.jd.core.v1.stub;

/** A local class of the static initializer which only the code of another local class creates (not meant to be decompiled) */
public class InitializerLocalsInLocalClasses {
    static Object created;

    static {
        class Local {
            Local(InitializerLocalsInLocalClasses first, String second) {
            }
        }
        class Factory {
            Object apply() {
                return new Local(null, "value");
            }
        }
        created = new Factory().apply();
    }
}
