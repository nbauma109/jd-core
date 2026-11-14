/*
 * Copyright (c) 2026 Nicolas Baumann (@nbauma109), with Claude Sonnet 5.5.
 * This project is distributed under the GPLv3 license.
 * This is a Copyleft license that gives the user the right to use,
 * copy and modify the code freely for non-commercial purposes.
 */

package org.jd.core.test;

/** Stub of the commons-imaging FormatCompliance used by TryResourcesImaging, so that the test does not depend on the library */
public class FormatCompliance {
    private final String description;

    public FormatCompliance(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
