/*
 * Copyright (c) 2026 Nicolas Baumann (@nbauma109), with Claude Sonnet 5.5.
 * This project is distributed under the GPLv3 license.
 * This is a Copyleft license that gives the user the right to use,
 * copy and modify the code freely for non-commercial purposes.
 */

package org.jd.core.v1.service.converter.classfiletojavasyntax.model.javasyntax.declaration;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class ClassFileAnnotationDeclarationTest {
    @Test
    public void testFlagsCanBeReplaced() {
        ClassFileAnnotationDeclaration declaration = new ClassFileAnnotationDeclaration(null, 1, "a/B", "B", null);

        declaration.setFlags(1 | 8);

        assertEquals(1 | 8, declaration.getFlags());
        assertEquals(0, declaration.getFirstLineNumber());
    }
}
