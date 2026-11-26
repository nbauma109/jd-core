/*
 * Copyright (c) 2026 Nicolas Baumann (@nbauma109), with Claude Sonnet 5.5.
 * This project is distributed under the GPLv3 license.
 * This is a copyleft license that gives the user the right to use,
 * copy and modify the code freely for non-commercial purposes.
 */

package org.jd.core.v1.stub;

/**
 * 'Parser.recoverStatements' of the Eclipse compiler: two local classes whose fields refer to each other
 */
public class EcjSiblingLocalClass {
    interface Visitor {
        int visit(int depth);
    }

    int run(int depth) {
        class MethodVisitor implements Visitor {
            Visitor types;
            int[] seen = new int[1];

            @Override
            public int visit(int level) {
                seen[0]++;
                return level > 0 ? types.visit(level - 1) : seen[0];
            }
        }
        class TypeVisitor implements Visitor {
            MethodVisitor methods;

            @Override
            public int visit(int level) {
                return level > 0 ? methods.visit(level - 1) : methods.seen[0];
            }
        }
        MethodVisitor methodVisitor = new MethodVisitor();
        TypeVisitor typeVisitor = new TypeVisitor();

        methodVisitor.types = typeVisitor;
        typeVisitor.methods = methodVisitor;
        return methodVisitor.visit(depth);
    }
}
