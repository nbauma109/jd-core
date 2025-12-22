/*
 * Copyright (c) 2025 GPLv3.
 * This project is distributed under the GPLv3 license.
 * This is a Copyleft license that gives the user the right to use,
 * copy and modify the code freely for non-commercial purposes.
 */

package org.jd.core.v1.service.converter.classfiletojavasyntax.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.bcel.classfile.Attribute;
import org.apache.bcel.classfile.BootstrapMethod;
import org.apache.bcel.classfile.BootstrapMethods;
import org.apache.bcel.classfile.Code;
import org.apache.bcel.classfile.Constant;
import org.apache.bcel.classfile.ConstantClass;
import org.apache.bcel.classfile.ConstantInvokeDynamic;
import org.apache.bcel.classfile.ConstantMethodHandle;
import org.apache.bcel.classfile.ConstantMethodref;
import org.apache.bcel.classfile.ConstantPool;
import org.apache.bcel.classfile.ConstantUtf8;
import org.apache.bcel.classfile.Field;
import org.apache.bcel.classfile.Method;
import org.apache.bcel.classfile.Utility;
import org.apache.bcel.generic.ALOAD;
import org.apache.bcel.generic.ConstantPoolGen;
import org.apache.bcel.generic.GETFIELD;
import org.apache.bcel.generic.INVOKEDYNAMIC;
import org.apache.bcel.generic.INVOKESPECIAL;
import org.apache.bcel.generic.Instruction;
import org.apache.bcel.generic.InstructionHandle;
import org.apache.bcel.generic.InstructionList;
import org.apache.bcel.generic.LoadInstruction;
import org.apache.bcel.generic.MethodGen;
import org.apache.bcel.generic.NOP;
import org.apache.bcel.generic.POP;
import org.apache.bcel.generic.POP2;
import org.apache.bcel.generic.PUTFIELD;
import org.apache.bcel.generic.ReturnInstruction;
import org.apache.bcel.generic.TargetLostException;
import org.apache.bcel.generic.Type;
import org.jd.core.v1.model.classfile.ClassFile;

import static org.jd.core.v1.util.StringConstants.INSTANCE_CONSTRUCTOR;

public final class RecordHelper {

    private RecordHelper() {
    }

    /* ============================================================
     * Entry points
     * ============================================================ */

    public static Method[] removeImplicitDefaultRecordMethods(ClassFile ownerClass) {
        if (ownerClass == null) {
            return new Method[0];
        }

        Method[] methods = ownerClass.getMethods();
        List<Method> recordMethods = toMutableList(methods);

        ConstantPoolGen constantPoolGen =
                new ConstantPoolGen(ownerClass.getConstantPool());
        Map<String, String> recordComponentNamesByFieldName =
                buildFieldNameToComponentNameMap(ownerClass);

        removeImplicitDefaultRecordMethods(
                recordMethods,
                ownerClass,
                constantPoolGen,
                recordComponentNamesByFieldName
        );

        stripImplicitRecordConstructorTailAssignments(
                recordMethods,
                ownerClass,
                constantPoolGen
        );

        return recordMethods.toArray(Method[]::new);
    }

    public static void removeImplicitDefaultRecordMethods(
            List<Method> methods,
            ClassFile ownerClass,
            ConstantPoolGen constantPoolGen,
            Map<String, String> recordComponentNamesByFieldName) {

        if (methods == null
                || methods.isEmpty()
                || ownerClass == null
                || constantPoolGen == null
                || recordComponentNamesByFieldName == null) {
            return;
        }

        BootstrapMethods bootstrapMethods =
                findBootstrapMethodsAttribute(ownerClass);

        for (Iterator<Method> iterator = methods.iterator(); iterator.hasNext();) {
            Method method = iterator.next();
            if (matchesDefaultImplicitRecordDefinition(
                    method,
                    ownerClass,
                    bootstrapMethods,
                    constantPoolGen,
                    recordComponentNamesByFieldName)) {
                iterator.remove();
            }
        }
    }

    /* ============================================================
     * Default implicit method detection
     * ============================================================ */

    private static boolean matchesDefaultImplicitRecordDefinition(
            Method method,
            ClassFile ownerClass,
            BootstrapMethods bootstrapMethods,
            ConstantPoolGen constantPoolGen,
            Map<String, String> recordComponentNamesByFieldName) {

        if (method == null) {
            return false;
        }

        if (isStrictImplicitCanonicalConstructor(
                method,
                ownerClass,
                constantPoolGen)
        || isImplicitObjectMethodsBootstrap(
                method,
                bootstrapMethods,
                constantPoolGen)) {
            return true;
        }

        return isImplicitRecordComponentAccessor(
                method,
                constantPoolGen,
                recordComponentNamesByFieldName);
    }

    private static boolean isStrictImplicitCanonicalConstructor(
            Method method,
            ClassFile ownerClass,
            ConstantPoolGen constantPoolGen) {

        if (ownerClass == null || constantPoolGen == null) {
            return false;
        }
        if (!INSTANCE_CONSTRUCTOR.equals(method.getName())
                || method.isStatic()
                || !method.isPublic()) {
            return false;
        }

        List<Field> recordFields = getRecordInstanceFields(ownerClass);
        if (recordFields.isEmpty()) {
            return false;
        }

        String expectedSignature =
                buildCanonicalConstructorSignature(recordFields);
        if (!expectedSignature.equals(method.getSignature())) {
            return false;
        }

        InstructionList instructionList = toInstructionList(method);
        if (instructionList == null) {
            return false;
        }

        try {
            List<Instruction> instructions =
                    nonNopInstructions(instructionList);

            int expectedInstructionCount =
                    3 * recordFields.size() + 3;
            if (instructions.size() != expectedInstructionCount) {
                return false;
            }

            int index = 0;

            if (!isAload0(instructions.get(index))) {
                return false;
            }
            index++;

            Instruction superInit = instructions.get(index);
            if (!(superInit instanceof INVOKESPECIAL invokespecial)
                    || !isRecordSuperInit(invokespecial, constantPoolGen)) {
                return false;
            }
            index++;

            int[] paramSlots =
                    computeConstructorParameterSlots(method);

            for (int fieldIndex = 0;
                 fieldIndex < recordFields.size();
                 fieldIndex++) {

                Field field = recordFields.get(fieldIndex);

                if (!isAload0(instructions.get(index))) {
                    return false;
                }
                index++;

                Instruction load = instructions.get(index);
                if (!(load instanceof LoadInstruction loadInstruction)
                        || loadInstruction.getIndex() != paramSlots[fieldIndex]) {
                    return false;
                }
                index++;

                Instruction put = instructions.get(index);
                if (!(put instanceof PUTFIELD putfield)) {
                    return false;
                }

                String putFieldName =
                        putfield.getFieldName(constantPoolGen);
                String putFieldSignature =
                        putfield.getSignature(constantPoolGen);

                if (!field.getName().equals(putFieldName)
                        || !field.getSignature().equals(putFieldSignature)) {
                    return false;
                }
                index++;
            }

            return instructions.get(index) instanceof ReturnInstruction;
        } finally {
            instructionList.dispose();
        }
    }

    private static boolean isRecordSuperInit(
            INVOKESPECIAL invokespecial,
            ConstantPoolGen constantPoolGen) {

        String className =
                invokespecial.getClassName(constantPoolGen);
        String methodName =
                invokespecial.getMethodName(constantPoolGen);
        String signature =
                invokespecial.getSignature(constantPoolGen);

        return "java.lang.Record".equals(className)
                && INSTANCE_CONSTRUCTOR.equals(methodName)
                && "()V".equals(signature);
    }

    /* ============================================================
     * Constructor tail stripping
     * ============================================================ */

    private static void stripImplicitRecordConstructorTailAssignments(
            List<Method> methods,
            ClassFile ownerClass,
            ConstantPoolGen constantPoolGen) {

        if (methods == null || methods.isEmpty()) {
            return;
        }

        List<Field> recordFields = getRecordInstanceFields(ownerClass);
        if (recordFields.isEmpty()) {
            return;
        }

        String expectedConstructorSignature =
                buildCanonicalConstructorSignature(recordFields);

        for (int i = 0; i < methods.size(); i++) {
            Method method = methods.get(i);
            if (!isConstructor(method, expectedConstructorSignature)) {
                continue;
            }

            Method updated =
                    removeImplicitConstructorTailPutFields(
                            method,
                            ownerClass,
                            constantPoolGen,
                            recordFields);

            methods.set(i, updated);
        }
    }

    private static boolean isConstructor(
            Method method,
            String expectedConstructorSignature) {

        if (method == null
                || !INSTANCE_CONSTRUCTOR.equals(method.getName())
                || method.isStatic()
                || !method.isPublic()) {
            return false;
        }
        return expectedConstructorSignature.equals(method.getSignature());
    }

    /**
     * Removes the compiler-synthesized trailing block:
     * for each record component field in declared order:
     *   aload_0
     *   load <corresponding parameter slot>
     *   putfield <that field>
     * followed by return
     *
     * Only removes if the method ends with a perfect match of that pattern.
     */
    private static Method removeImplicitConstructorTailPutFields(
            Method constructor,
            ClassFile ownerClass,
            ConstantPoolGen constantPoolGen,
            List<Field> recordFields) {

        MethodGen methodGen =
                new MethodGen(
                        constructor,
                        toClassName(ownerClass),
                        constantPoolGen);

        InstructionList editable =
                methodGen.getInstructionList();
        if (editable == null) {
            return constructor;
        }

        List<InstructionHandle> effective =
                nonNopInstructionHandles(editable);
        if (effective.isEmpty()) {
            return constructor;
        }

        InstructionHandle last =
                effective.get(effective.size() - 1);
        if (!(last.getInstruction() instanceof ReturnInstruction)) {
            return constructor;
        }

        int[] paramSlots =
                computeConstructorParameterSlots(constructor);

        int cursor = effective.size() - 2;
        List<InstructionHandle> putfieldHandles =
                new ArrayList<>();

        for (int fieldIndex = recordFields.size() - 1;
             fieldIndex >= 0;
             fieldIndex--) {

            if (cursor < 2) {
                return constructor;
            }

            InstructionHandle putHandle =
                    effective.get(cursor);
            InstructionHandle loadHandle =
                    effective.get(cursor - 1);
            InstructionHandle aloadHandle =
                    effective.get(cursor - 2);

            if (!(putHandle.getInstruction() instanceof PUTFIELD putfield)
                    || !(loadHandle.getInstruction() instanceof LoadInstruction loadInstruction)
                    || !isAload0(aloadHandle.getInstruction())) {
                return constructor;
            }

            Field field = recordFields.get(fieldIndex);

            String putFieldName =
                    putfield.getFieldName(constantPoolGen);
            String putFieldSignature =
                    putfield.getSignature(constantPoolGen);

            if (!field.getName().equals(putFieldName)
                    || !field.getSignature().equals(putFieldSignature)) {
                return constructor;
            }

            int expectedSlot = paramSlots[fieldIndex];
            int actualSlot = loadInstruction.getIndex();
            if (expectedSlot != actualSlot) {
                return constructor;
            }

            putfieldHandles.add(putHandle);
            cursor -= 3;
        }

        InstructionHandle firstToDelete =
                effective.get(cursor + 1);
        InstructionHandle lastToDelete =
                effective.get(effective.size() - 2);

        if (canDeleteRange(firstToDelete, lastToDelete)) {
            try {
                editable.delete(firstToDelete, lastToDelete);
            } catch (TargetLostException e) {
                assert ExceptionUtil.printStackTrace(e);
            }
        } else {
            neutralizePutFields(editable, putfieldHandles, constantPoolGen);
        }

        methodGen.setInstructionList(editable);
        methodGen.setMaxStack();
        methodGen.setMaxLocals();

        return methodGen.getMethod();
    }

    private static boolean canDeleteRange(
            InstructionHandle firstToDelete,
            InstructionHandle lastToDelete) {

        if (firstToDelete == null || lastToDelete == null) {
            return false;
        }

        for (InstructionHandle h = firstToDelete; h != null; h = h.getNext()) {
            if (h.hasTargeters()) {
                return false;
            }
            if (h == lastToDelete) {
                return true;
            }
        }

        return false;
    }

    private static void neutralizePutFields(
            InstructionList editable,
            List<InstructionHandle> putfieldHandles,
            ConstantPoolGen constantPoolGen) {

        if (editable == null
                || putfieldHandles == null
                || putfieldHandles.isEmpty()) {
            return;
        }

        for (InstructionHandle putHandle : putfieldHandles) {
            if (!(putHandle.getInstruction() instanceof PUTFIELD putfield)) {
                continue;
            }

            Type fieldType =
                    Type.getType(
                            putfield.getSignature(constantPoolGen));
            boolean needsPop2 =
                    fieldType == Type.LONG
                            || fieldType == Type.DOUBLE;

            putHandle.setInstruction(
                    needsPop2 ? new POP2() : new POP());

            /*
             * We still have objectref on stack from the preceding aload_0.
             * Insert a POP after the value-pop instruction to remove objectref too.
             */
            editable.insert(putHandle, new POP());
        }
    }

    /* ============================================================
     * Utilities
     * ============================================================ */

    private static int[] computeConstructorParameterSlots(Method constructor) {
        Type[] args =
                Type.getArgumentTypes(constructor.getSignature());
        int[] slots = new int[args.length];

        int slot = 1;
        for (int i = 0; i < args.length; i++) {
            slots[i] = slot;
            slot += args[i].getSize();
        }

        return slots;
    }

    private static String toClassName(ClassFile ownerClass) {
        return Utility.pathToPackage(
                ownerClass.getInternalTypeName());
    }

    private static Map<String, String> buildFieldNameToComponentNameMap(
            ClassFile ownerClass) {

        Map<String, String> map = new HashMap<>();
        Field[] fields = ownerClass.getFields();

        if (fields == null || fields.length == 0) {
            return map;
        }

        for (Field field : fields) {
            if (field == null
                    || field.isStatic()
                    || !field.isPrivate()
                    || !field.isFinal()) {
                continue;
            }

            String name = field.getName();
            if (name == null || name.isEmpty()) {
                continue;
            }

            map.put(name, name);
        }

        return map;
    }

    private static List<Field> getRecordInstanceFields(
            ClassFile ownerClass) {

        List<Field> result = new ArrayList<>();
        Field[] fields = ownerClass.getFields();

        if (fields == null || fields.length == 0) {
            return result;
        }

        for (Field field : fields) {
            if (field == null
                    || field.isStatic()
                    || !field.isPrivate()
                    || !field.isFinal()) {
                continue;
            }
            result.add(field);
        }

        return result;
    }

    private static String buildCanonicalConstructorSignature(
            List<Field> recordFields) {

        StringBuilder sb = new StringBuilder();
        sb.append('(');
        for (Field field : recordFields) {
            sb.append(field.getSignature());
        }
        sb.append(")V");
        return sb.toString();
    }

    private static BootstrapMethods findBootstrapMethodsAttribute(
            ClassFile ownerClass) {

        Attribute[] attributes = ownerClass.getAttributes();
        if (attributes == null || attributes.length == 0) {
            return null;
        }

        for (Attribute attribute : attributes) {
            if (attribute instanceof BootstrapMethods bootstrapMethods) {
                return bootstrapMethods;
            }
        }

        return null;
    }

    private static boolean isImplicitObjectMethodsBootstrap(
            Method method,
            BootstrapMethods bootstrapMethods,
            ConstantPoolGen constantPoolGen) {

        if (bootstrapMethods == null
                || !isEqualsHashCodeToStringSignature(method)) {
            return false;
        }

        InstructionList instructionList =
                toInstructionList(method);
        if (instructionList == null) {
            return false;
        }

        try {
            for (InstructionHandle handle = instructionList.getStart();
                 handle != null;
                 handle = handle.getNext()) {

                Instruction instruction =
                        handle.getInstruction();
                if (instruction instanceof INVOKEDYNAMIC invokedynamic
                        && referencesObjectMethodsBootstrap(
                                invokedynamic,
                                bootstrapMethods,
                                constantPoolGen)) {
                    return true;
                }
            }
            return false;
        } finally {
            instructionList.dispose();
        }
    }

    private static boolean isEqualsHashCodeToStringSignature(Method method) {
        String name = method.getName();
        String signature = method.getSignature();

        return isToString(name, signature)
                || isHashCode(name, signature)
                || isEquals(name, signature);
    }

    private static boolean isEquals(String name, String signature) {
        return "equals".equals(name)
                && "(Ljava/lang/Object;)Z".equals(signature);
    }

    private static boolean isHashCode(String name, String signature) {
        return "hashCode".equals(name)
                && "()I".equals(signature);
    }

    private static boolean isToString(String name, String signature) {
        return "toString".equals(name)
                && "()Ljava/lang/String;".equals(signature);
    }

    private static boolean referencesObjectMethodsBootstrap(
            INVOKEDYNAMIC invokedynamic,
            BootstrapMethods bootstrapMethods,
            ConstantPoolGen constantPoolGen) {

        ConstantPool constantPool =
                constantPoolGen.getConstantPool();

        Constant invokedynamicConstant =
                constantPool.getConstant(invokedynamic.getIndex());
        if (!(invokedynamicConstant instanceof ConstantInvokeDynamic cid)) {
            return false;
        }

        int bootstrapIndex =
                cid.getBootstrapMethodAttrIndex();

        BootstrapMethod[] bootstrapMethodEntries =
                bootstrapMethods.getBootstrapMethods();
        if (bootstrapMethodEntries == null
                || bootstrapIndex < 0
                || bootstrapIndex >= bootstrapMethodEntries.length) {
            return false;
        }

        BootstrapMethod bootstrapMethod =
                bootstrapMethodEntries[bootstrapIndex];
        int bootstrapMethodRef =
                bootstrapMethod.getBootstrapMethodRef();

        Constant methodHandleConstant =
                constantPool.getConstant(bootstrapMethodRef);
        if (!(methodHandleConstant instanceof ConstantMethodHandle methodHandle)) {
            return false;
        }

        int referenceIndex =
                methodHandle.getReferenceIndex();

        Constant referenceConstant =
                constantPool.getConstant(referenceIndex);
        if (!(referenceConstant instanceof ConstantMethodref methodRef)) {
            return false;
        }

        Constant ownerClassConstant =
                constantPool.getConstant(methodRef.getClassIndex());
        if (!(ownerClassConstant instanceof ConstantClass ownerClass)) {
            return false;
        }

        Constant ownerNameConstant =
                constantPool.getConstant(ownerClass.getNameIndex());
        if (!(ownerNameConstant instanceof ConstantUtf8)) {
            return false;
        }

        String ownerInternalName =
                ((ConstantUtf8) ownerNameConstant).getBytes();

        return "java/lang/runtime/ObjectMethods".equals(ownerInternalName);
    }

    private static InstructionList toInstructionList(Method method) {
        Code code = method.getCode();
        if (code == null) {
            return null;
        }
        return new InstructionList(code.getCode());
    }

    private static boolean isImplicitRecordComponentAccessor(
            Method method,
            ConstantPoolGen constantPoolGen,
            Map<String, String> recordComponentNamesByFieldName) {

        if (method.isStatic() || !method.isPublic()) {
            return false;
        }

        String methodName = method.getName();
        String methodSignature = method.getSignature();

        InstructionList instructionList =
                toInstructionList(method);
        if (instructionList == null) {
            return false;
        }

        try {
            List<Instruction> instructions =
                    nonNopInstructions(instructionList);

            if (instructions.size() != 3
                    || !isAload0(instructions.get(0))
                    || !(instructions.get(1) instanceof GETFIELD)
                    || !(instructions.get(2) instanceof ReturnInstruction)) {
                return false;
            }

            GETFIELD getfield =
                    (GETFIELD) instructions.get(1);
            String fieldName =
                    getfield.getFieldName(constantPoolGen);
            String fieldSignature =
                    getfield.getSignature(constantPoolGen);

            String componentName =
                    recordComponentNamesByFieldName.get(fieldName);
            if (componentName == null
                    || !componentName.equals(methodName)) {
                return false;
            }

            String expectedSignature =
                    accessorMethodSignatureFromFieldSignature(
                            fieldSignature);
            return expectedSignature.equals(methodSignature);
        } finally {
            instructionList.dispose();
        }
    }

    private static boolean isAload0(Instruction instruction) {
        if (!(instruction instanceof ALOAD)) {
            return false;
        }
        return ((ALOAD) instruction).getIndex() == 0;
    }

    private static String accessorMethodSignatureFromFieldSignature(
            String fieldSignature) {
        return "()" + fieldSignature;
    }

    private static List<Instruction> nonNopInstructions(
            InstructionList instructionList) {

        List<Instruction> result = new ArrayList<>();
        for (InstructionHandle handle = instructionList.getStart();
             handle != null;
             handle = handle.getNext()) {

            Instruction instruction =
                    handle.getInstruction();
            if (!(instruction instanceof NOP)) {
                result.add(instruction);
            }
        }
        return result;
    }

    private static List<InstructionHandle> nonNopInstructionHandles(
            InstructionList instructionList) {

        List<InstructionHandle> result = new ArrayList<>();
        for (InstructionHandle handle = instructionList.getStart();
             handle != null;
             handle = handle.getNext()) {

            Instruction instruction =
                    handle.getInstruction();
            if (!(instruction instanceof NOP)) {
                result.add(handle);
            }
        }
        return result;
    }

    public static List<Method> toMutableList(Method[] methods) {
        List<Method> result = new ArrayList<>();
        if (methods != null && methods.length > 0) {
            Collections.addAll(result, methods);
        }
        return result;
    }
}
