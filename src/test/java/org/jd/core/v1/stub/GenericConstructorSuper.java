/*
 * Copyright (c) 2026 Nicolas Baumann (@nbauma109), with Claude Sonnet 5.5.
 * This project is distributed under the GPLv3 license.
 * This is a Copyleft license that gives the user the right to use,
 * copy and modify the code freely for non-commercial purposes.
 */

package org.jd.core.v1.stub;

import java.util.Objects;

/**
 * The constructor of the super class has a type parameter named like the one of this class: the argument of super(...) is cast to
 * the erased parameter type by the bytecode, and the source does not spell that cast with the type parameter of the constructor.
 */
public class GenericConstructorSuper<T> extends GenericConstructorSuperBase {

    public static final class Builder<T> extends GenericConstructorSuperBase.AbstractBuilder<Builder<T>> {
        String name;
    }

    String label;

    private GenericConstructorSuper(Builder<T> builder) {
        super(Objects.requireNonNull(builder, "builder"));
    }

    private <U extends GenericConstructorSuperBase.AbstractBuilder<U>> GenericConstructorSuper(GenericConstructorSuperBase.AbstractBuilder<U> builder, int length) {
        super(builder);
        builder.flag = length > 0;
    }

    GenericConstructorSuper(Builder<T> builder, String name) {
        this(Objects.requireNonNull(builder, "builder"), name.length());
    }

    GenericConstructorSuper() {
        this("default");
    }

    GenericConstructorSuper(int length) {
        this();
        this.label = String.valueOf(length);
    }

    GenericConstructorSuper(String name) {
        this(new Builder<T>());
        this.label = name;
    }
}
