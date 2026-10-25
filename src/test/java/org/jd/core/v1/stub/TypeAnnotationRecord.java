/*
 * Copyright (c) 2026 Nicolas Baumann (@nbauma109), with Claude Sonnet 5.5.
 * This project is distributed under the GPLv3 license.
 * This is a Copyleft license that gives the user the right to use,
 * copy and modify the code freely for non-commercial purposes.
 */

package org.jd.core.v1.stub;

/** A record component whose type has an annotation which is only for types: the class file has it on the component, nowhere else */
public record TypeAnnotationRecord(@Maybe Object value, int count) {
}
