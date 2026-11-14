/*
 * Copyright (c) 2026 Nicolas Baumann (@nbauma109), with Claude Sonnet 5.5.
 * This project is distributed under the GPLv3 license.
 * This is a Copyleft license that gives the user the right to use,
 * copy and modify the code freely for non-commercial purposes.
 */

package org.jd.core.v1.stub;

/**
 * A constructor with its own type parameter, the shape of commons-lang3 3.21.0's AbstractReflection.
 */
public abstract class GenericConstructorSuperBase {

    public abstract static class AbstractBuilder<B extends AbstractBuilder<B>> {
        boolean flag;
    }

    <T extends AbstractBuilder<T>> GenericConstructorSuperBase(AbstractBuilder<T> builder) {
        System.out.println(builder.flag);
    }
}
