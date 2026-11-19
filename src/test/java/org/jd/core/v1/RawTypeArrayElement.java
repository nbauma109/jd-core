/*
 * Copyright (c) 2026 Nicolas Baumann (@nbauma109), with Claude Sonnet 5.5.
 * This project is distributed under the GPLv3 license.
 * This is a copyleft license that gives the user the right to use,
 * copy and modify the code freely for non-commercial purposes.
 */

package org.jd.core.v1;

import java.util.Map;

/** An element of an array of raw types, whose result is cast: {@link RawTypeArrayElementTest} */
@SuppressWarnings({"rawtypes", "unchecked"})
public class RawTypeArrayElement {

    Map[] synthetics = new Map[2];

    String field(Object key) {
        return (String) this.synthetics[1].get(key);
    }

    String[] methods(Object key) {
        return (String[]) this.synthetics[0].get(key);
    }

    String local(Map[] maps, Object key) {
        return (String) maps[0].get(key);
    }
}
