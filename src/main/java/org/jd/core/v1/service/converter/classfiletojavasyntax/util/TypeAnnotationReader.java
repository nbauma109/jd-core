/*
 * Copyright (c) 2008, 2019 Emmanuel Dupuy.
 * This project is distributed under the GPLv3 license.
 * This is a Copyleft license that gives the user the right to use,
 * copy and modify the code freely for non-commercial purposes.
 */
package org.jd.core.v1.service.converter.classfiletojavasyntax.util;

import org.apache.bcel.classfile.Attribute;
import org.apache.bcel.classfile.ConstantPool;
import org.apache.bcel.classfile.FieldOrMethod;
import org.apache.bcel.classfile.Unknown;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.EOFException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Reads the type annotations (JSR 308) that BCEL leaves as unknown attributes, such as the '@Nullable' of 'void put(@Nullable Object key)'.
 * To be replaced by the attributes of BCEL once it has them (apache/commons-bcel#539).
 * Only the marker annotations of the whole type of a field, a method result or a parameter are kept: those can be written in front of the declaration.
 */
public final class TypeAnnotationReader {
    private static final int TARGET_FIELD = 0x13;
    private static final int TARGET_METHOD_RETURN = 0x14;
    private static final int TARGET_CLASS_TYPE_PARAMETER_BOUND = 0x11;
    private static final int TARGET_METHOD_TYPE_PARAMETER_BOUND = 0x12;
    private static final int TARGET_FORMAL_PARAMETER = 0x16;
    private static final int NO_PARAMETER = -1;

    /** Descriptors of the annotations of the type of the field or of the result of the method */
    private final List<String> declaration = new ArrayList<>();
    /** Descriptors of the annotations of the type of each parameter, by index */
    private final Map<Integer, List<String>> parameters = new TreeMap<>();
    /** Descriptors of the annotations of the class bound of each type parameter, by index of the type parameter */
    private final Map<Integer, List<String>> bounds = new TreeMap<>();
    private final Map<Integer, List<String>> interfaceBounds = new TreeMap<>();

    private TypeAnnotationReader() {
    }

    public static TypeAnnotationReader of(FieldOrMethod fieldOrMethod) {
        return of(fieldOrMethod.getAttributes(), fieldOrMethod.getConstantPool());
    }

    public static TypeAnnotationReader of(Attribute[] attributes, ConstantPool constants) {
        TypeAnnotationReader reader = new TypeAnnotationReader();

        for (Attribute attribute : attributes) {
            if (attribute instanceof Unknown unknown && unknown.getName().startsWith("Runtime") && unknown.getName().endsWith("TypeAnnotations")) {
                TypeAnnotationReader read = new TypeAnnotationReader();

                try {
                    read.read(unknown.getBytes(), constants);
                    reader.merge(read);
                } catch (IOException | RuntimeException e) {
                    // an attribute which cannot be read is dropped as a whole, as it was before
                }
            }
        }
        return reader;
    }

    private void merge(TypeAnnotationReader other) {
        declaration.addAll(other.declaration);
        other.parameters.forEach((index, descriptors) -> parameters.computeIfAbsent(index, k -> new ArrayList<>()).addAll(descriptors));
        other.bounds.forEach((index, descriptors) -> bounds.computeIfAbsent(index, k -> new ArrayList<>()).addAll(descriptors));
        other.interfaceBounds.forEach((index, descriptors) -> interfaceBounds.computeIfAbsent(index, k -> new ArrayList<>()).addAll(descriptors));
    }

    /** @return the descriptors of the annotations of the type of the field, or of the result of the method */
    public List<String> getDeclarationAnnotations() {
        return declaration;
    }

    public List<String> getParameterAnnotations(int parameterIndex) {
        return parameters.getOrDefault(parameterIndex, Collections.emptyList());
    }

    /** @return the descriptors of the annotations of the bound of the type parameter ('K extends @Nullable Object') */
    public List<String> getBoundAnnotations(int typeParameterIndex) {
        return bounds.getOrDefault(typeParameterIndex, Collections.emptyList());
    }

    /**
     * @return the descriptors of the annotations of the first interface bound of the type parameter, when it has no class bound
     * ('C extends @Nullable Closeable': the index of the bound is 1, the index 0 being the one of the class bound)
     */
    public List<String> getInterfaceBoundAnnotations(int typeParameterIndex) {
        return interfaceBounds.getOrDefault(typeParameterIndex, Collections.emptyList());
    }

    public boolean hasBoundAnnotations() {
        return !bounds.isEmpty() || !interfaceBounds.isEmpty();
    }

    public boolean hasParameterAnnotations() {
        return !parameters.isEmpty();
    }

    private void read(byte[] bytes, ConstantPool constants) throws IOException {
        DataInputStream in = new DataInputStream(new ByteArrayInputStream(bytes));

        for (int count = in.readUnsignedShort(); count > 0; count--) {
            int target = in.readUnsignedByte();
            int[] info = readTargetInfo(in, target);
            int parameterIndex = info[0];
            int pathLength = in.readUnsignedByte();

            skip(in, pathLength * 2);

            String descriptor = constants.getConstantUtf8(in.readUnsignedShort()).getBytes();
            int pairs = in.readUnsignedShort();

            for (int i = 0; i < pairs; i++) {
                in.readUnsignedShort();
                skipElementValue(in);
            }
            if (pathLength == 0 && pairs == 0) {
                if (target == TARGET_FIELD || target == TARGET_METHOD_RETURN) {
                    declaration.add(descriptor);
                } else if (target == TARGET_FORMAL_PARAMETER && parameterIndex != NO_PARAMETER) {
                    parameters.computeIfAbsent(parameterIndex, k -> new ArrayList<>()).add(descriptor);
                } else if ((target == TARGET_CLASS_TYPE_PARAMETER_BOUND || target == TARGET_METHOD_TYPE_PARAMETER_BOUND) && info[1] <= 1) {
                    (info[1] == 0 ? bounds : interfaceBounds).computeIfAbsent(parameterIndex, k -> new ArrayList<>()).add(descriptor);
                }
            }
        }
    }

    /** @return the index of the parameter and, for a bound of a type parameter, the index of the bound (else NO_PARAMETER for both) */
    private static int[] readTargetInfo(DataInputStream in, int target) throws IOException {
        switch (target) {
            case TARGET_FORMAL_PARAMETER:
                return new int[] {in.readUnsignedByte(), NO_PARAMETER};
            case TARGET_CLASS_TYPE_PARAMETER_BOUND, TARGET_METHOD_TYPE_PARAMETER_BOUND:
                return new int[] {in.readUnsignedByte(), in.readUnsignedByte()};
            case 0x00, 0x01:
                skip(in, 1);
                break;
            case 0x10, 0x17, 0x42, 0x43, 0x44, 0x45, 0x46:
                skip(in, 2);
                break;
            case 0x47, 0x48, 0x49, 0x4A, 0x4B:
                skip(in, 3);
                break;
            case 0x40, 0x41:
                skip(in, in.readUnsignedShort() * 6);
                break;
            default:
                // 0x13, 0x14, 0x15: empty
                break;
        }
        return new int[] {NO_PARAMETER, NO_PARAMETER};
    }

    /** Skips the given number of bytes, which must all be there: a truncated attribute is not read */
    private static void skip(DataInputStream in, int count) throws IOException {
        if (in.skipBytes(count) != count) {
            throw new EOFException();
        }
    }

    private static void skipElementValue(DataInputStream in) throws IOException {
        int tag = in.readUnsignedByte();

        switch (tag) {
            case 'e':
                skip(in, 4);
                break;
            case '@':
                skip(in, 2);
                for (int pairs = in.readUnsignedShort(); pairs > 0; pairs--) {
                    skip(in, 2);
                    skipElementValue(in);
                }
                break;
            case '[':
                for (int values = in.readUnsignedShort(); values > 0; values--) {
                    skipElementValue(in);
                }
                break;
            default:
                // constants and class literals: one index
                skip(in, 2);
                break;
        }
    }
}
