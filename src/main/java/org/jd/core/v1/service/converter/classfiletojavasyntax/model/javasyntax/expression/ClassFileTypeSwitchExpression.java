/*
 * Copyright (c) 2026 Nicolas Baumann (@nbauma109), with Claude Sonnet 5.5.
 * This project is distributed under the GPLv3 license.
 * This is a Copyleft license that gives the user the right to use,
 * copy and modify the code freely for non-commercial purposes.
 */
package org.jd.core.v1.service.converter.classfiletojavasyntax.model.javasyntax.expression;

import org.jd.core.v1.model.javasyntax.expression.BaseExpression;
import org.jd.core.v1.model.javasyntax.expression.Expression;
import org.jd.core.v1.model.javasyntax.expression.ObjectTypeReferenceExpression;
import org.jd.core.v1.model.javasyntax.type.ObjectType;
import org.jd.core.v1.model.javasyntax.type.PrimitiveType;

import java.util.List;

/**
 * The call of {@code SwitchBootstraps.typeSwitch(selector, restartIndex)} which selects the case of a 'switch' with patterns: the 'switch' is on
 * the index of the label which matches. The labels are the arguments of the bootstrap method: types ({@link org.jd.core.v1.model.javasyntax.type.Type}),
 * strings, integers, or null when unknown.
 */
@SuppressWarnings("java:S110") // the call has to be a method invocation: the visitors which look for the calls and their arguments find it
public class ClassFileTypeSwitchExpression extends ClassFileMethodInvocationExpression {
    private static final ObjectType SWITCH_BOOTSTRAPS = new ObjectType("java/lang/runtime/SwitchBootstraps", "java.lang.runtime.SwitchBootstraps", "SwitchBootstraps");

    /** A qualified enum constant of a label: 'case E.A' */
    public record EnumLabel(ObjectType type, String name) {}

    private final Expression selector;
    private final Expression restartIndex;
    private final List<Object> labels;

    public ClassFileTypeSwitchExpression(int lineNumber, String name, Expression selector, Expression restartIndex, List<Object> labels, BaseExpression parameters) {
        super(lineNumber, PrimitiveType.TYPE_INT, new ObjectTypeReferenceExpression(lineNumber, SWITCH_BOOTSTRAPS), SWITCH_BOOTSTRAPS.getInternalName(), name,
                "(Ljava/lang/Object;I)I", parameters, null);
        this.selector = selector;
        this.restartIndex = restartIndex;
        this.labels = labels;
    }

    public Expression getSelector() {
        return selector;
    }

    public Expression getRestartIndex() {
        return restartIndex;
    }

    public List<Object> getLabels() {
        return labels;
    }
}
