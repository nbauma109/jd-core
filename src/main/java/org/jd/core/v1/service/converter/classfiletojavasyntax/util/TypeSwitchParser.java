/*
 * Copyright (c) 2026 Nicolas Baumann (@nbauma109), with Claude Sonnet 5.5.
 * This project is distributed under the GPLv3 license.
 * This is a Copyleft license that gives the user the right to use,
 * copy and modify the code freely for non-commercial purposes.
 */
package org.jd.core.v1.service.converter.classfiletojavasyntax.util;

import org.apache.bcel.classfile.BootstrapMethod;
import org.apache.bcel.classfile.Constant;
import org.apache.bcel.classfile.ConstantCP;
import org.apache.bcel.classfile.ConstantClass;
import org.apache.bcel.classfile.ConstantDynamic;
import org.apache.bcel.classfile.ConstantInteger;
import org.apache.bcel.classfile.ConstantMethodHandle;
import org.apache.bcel.classfile.ConstantPool;
import org.apache.bcel.classfile.ConstantString;
import org.apache.bcel.classfile.Method;
import org.apache.bcel.generic.GotoInstruction;
import org.apache.bcel.generic.ISTORE;
import org.apache.bcel.generic.InstructionHandle;
import org.apache.bcel.generic.InstructionList;
import org.jd.core.v1.model.javasyntax.expression.BaseExpression;
import org.jd.core.v1.model.javasyntax.expression.Expression;
import org.jd.core.v1.model.javasyntax.expression.Expressions;
import org.jd.core.v1.service.converter.classfiletojavasyntax.model.javasyntax.expression.ClassFileLocalVariableReferenceExpression;
import org.jd.core.v1.service.converter.classfiletojavasyntax.model.javasyntax.expression.ClassFileTypeSwitchExpression;

import java.util.ArrayList;
import java.util.List;

import static org.apache.bcel.Const.CONSTANT_Class;
import static org.apache.bcel.Const.CONSTANT_String;

/**
 * The call site {@code typeSwitch(selector, restartIndex)} of a 'switch' with patterns, when its labels can be written back: classes, strings, integers
 * and enum constants. Anything else (a guard, an unknown kind of label, a lambda whose method is named 'typeSwitch') is not rebuilt.
 */
final class TypeSwitchParser {
    private static final String SWITCH_BOOTSTRAPS = "java/lang/runtime/SwitchBootstraps";

    private final TypeMaker typeMaker;
    private final Method method;

    TypeSwitchParser(TypeMaker typeMaker, Method method) {
        this.typeMaker = typeMaker;
        this.method = method;
    }

    /** @return the call, null if the call site is not a switch with patterns which can be rebuilt */
    ClassFileTypeSwitchExpression parse(ConstantPool constants, BootstrapMethod[] bootstrapMethods, int bootstrapIndex, String name,
            BaseExpression parameters, int lineNumber, int invokeOffset) {
        BootstrapMethod bootstrapMethod = bootstrapMethods[bootstrapIndex];

        if (!"typeSwitch".equals(name) || !isSwitchBootstrap(constants, bootstrapMethod)
                || !(parameters instanceof Expressions switchParameters) || switchParameters.size() != 2
                || isRestartIndexReassigned(switchParameters.get(1), invokeOffset)) {
            return null;
        }

        List<Object> labels = new ArrayList<>();

        for (int argument : bootstrapMethod.getBootstrapArguments()) {
            Object label = label(constants, bootstrapMethods, argument);

            if (label == null) {
                // A label which is not known would be written as the index of its case
                return null;
            }
            labels.add(label);
        }
        return new ClassFileTypeSwitchExpression(lineNumber, name, switchParameters.get(0), switchParameters.get(1), labels, parameters);
    }

    /** The bootstrap method of the call site is the one of the switches with patterns, not the one of a lambda whose method is named 'typeSwitch' */
    private static boolean isSwitchBootstrap(ConstantPool constants, BootstrapMethod bootstrapMethod) {
        ConstantMethodHandle handle = constants.getConstant(bootstrapMethod.getBootstrapMethodRef());
        ConstantCP reference = constants.getConstant(handle.getReferenceIndex());

        return SWITCH_BOOTSTRAPS.equals(constants.getConstantString(reference.getClassIndex(), CONSTANT_Class));
    }

    /**
     * A guard ('case String s when s.isEmpty()') sets the restart index and jumps back before the call which selects the case: the switch is then a
     * loop which is not rebuilt. javac reuses the slot of the index for the next switch, whose initialization is no such store.
     */
    private boolean isRestartIndexReassigned(Expression restartIndex, int invokeOffset) {
        if (!(restartIndex instanceof ClassFileLocalVariableReferenceExpression reference)) {
            // ECJ passes the constant 0 when there is no guard: nothing can change it
            return !restartIndex.isIntegerConstantExpression();
        }

        InstructionHandle[] handles = new InstructionList(method.getCode().getCode()).getInstructionHandles();

        for (int i = 0; i < handles.length - 1; i++) {
            if (handles[i].getInstruction() instanceof ISTORE store && store.getIndex() == reference.getLocalVariable().getIndex()
                    && handles[i + 1].getInstruction() instanceof GotoInstruction jump && jump.getTarget().getPosition() < invokeOffset) {
                return true;
            }
        }
        return false;
    }

    /** @return a type, a string, an integer or an {@link ClassFileTypeSwitchExpression.EnumLabel}; null if the label is none of them */
    private Object label(ConstantPool constants, BootstrapMethod[] bootstrapMethods, int argument) {
        Constant constant = constants.getConstant(argument);

        if (constant instanceof ConstantClass) {
            return typeMaker.makeFromDescriptorOrInternalTypeName(constants.getConstantString(argument, CONSTANT_Class));
        }
        if (constant instanceof ConstantString) {
            return constants.getConstantString(argument, CONSTANT_String);
        }
        if (constant instanceof ConstantInteger integer) {
            return integer.getBytes();
        }
        return constant instanceof ConstantDynamic enumDescriptor ? enumLabel(constants, bootstrapMethods, enumDescriptor) : null;
    }

    /** 'Enum.EnumDesc.of(ClassDesc.of("p.E"), "A")', which javac gives to a qualified enum constant of a switch on an object */
    private ClassFileTypeSwitchExpression.EnumLabel enumLabel(ConstantPool constants, BootstrapMethod[] bootstrapMethods, ConstantDynamic enumDescriptor) {
        int[] arguments = bootstrapMethods[enumDescriptor.getBootstrapMethodAttrIndex()].getBootstrapArguments();

        if (arguments.length != 3 || !(constants.getConstant(arguments[1]) instanceof ConstantDynamic classDescriptor)
                || !(constants.getConstant(arguments[2]) instanceof ConstantString)) {
            return null;
        }

        int[] classArguments = bootstrapMethods[classDescriptor.getBootstrapMethodAttrIndex()].getBootstrapArguments();

        if (classArguments.length != 2 || !(constants.getConstant(classArguments[1]) instanceof ConstantString)) {
            return null;
        }

        String internalName = constants.getConstantString(classArguments[1], CONSTANT_String).replace('.', '/');

        return new ClassFileTypeSwitchExpression.EnumLabel(typeMaker.makeFromInternalTypeName(internalName), constants.getConstantString(arguments[2], CONSTANT_String));
    }
}
