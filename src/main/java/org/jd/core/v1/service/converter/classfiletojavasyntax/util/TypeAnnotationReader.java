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
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Reads the type annotations (JSR 308) that BCEL leaves as unknown attributes, such as the '@Nullable' of 'void put(@Nullable Object key)'.
 * Only the marker annotations of the whole type of a field, a method result or a parameter are kept: those can be written in front of the declaration.
 */
public final class TypeAnnotationReader {
    private static final int TARGET_FIELD = 0x13;
    private static final int TARGET_METHOD_RETURN = 0x14;
    private static final int TARGET_FORMAL_PARAMETER = 0x16;
    private static final int NO_PARAMETER = -1;

    /** Descriptors of the annotations of the type of the field or of the result of the method */
    private final List<String> declaration = new ArrayList<>();
    /** Descriptors of the annotations of the type of each parameter, by index */
    private final Map<Integer, List<String>> parameters = new TreeMap<>();

    private TypeAnnotationReader() {
    }

    public static TypeAnnotationReader of(FieldOrMethod fieldOrMethod) {
        TypeAnnotationReader reader = new TypeAnnotationReader();
        ConstantPool constants = fieldOrMethod.getConstantPool();

        for (Attribute attribute : fieldOrMethod.getAttributes()) {
            if (attribute instanceof Unknown unknown && unknown.getName().startsWith("Runtime") && unknown.getName().endsWith("TypeAnnotations")) {
                try {
                    reader.read(unknown.getBytes(), constants);
                } catch (IOException | RuntimeException e) {
                    // an attribute which cannot be read is dropped, as it was before
                }
            }
        }
        return reader;
    }

    /** @return the descriptors of the annotations of the type of the field, or of the result of the method */
    public List<String> getDeclarationAnnotations() {
        return declaration;
    }

    public List<String> getParameterAnnotations(int parameterIndex) {
        return parameters.getOrDefault(parameterIndex, Collections.emptyList());
    }

    public boolean hasParameterAnnotations() {
        return !parameters.isEmpty();
    }

    private void read(byte[] bytes, ConstantPool constants) throws IOException {
        DataInputStream in = new DataInputStream(new ByteArrayInputStream(bytes));

        for (int count = in.readUnsignedShort(); count > 0; count--) {
            int target = in.readUnsignedByte();
            int parameterIndex = skipTargetInfo(in, target);
            int pathLength = in.readUnsignedByte();

            in.skipBytes(pathLength * 2);

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
                }
            }
        }
    }

    /** @return the index of the parameter for a parameter target, else NO_PARAMETER */
    private static int skipTargetInfo(DataInputStream in, int target) throws IOException {
        switch (target) {
            case TARGET_FORMAL_PARAMETER:
                return in.readUnsignedByte();
            case 0x00, 0x01:
                in.skipBytes(1);
                break;
            case 0x10, 0x11, 0x12, 0x17, 0x42, 0x43, 0x44, 0x45, 0x46:
                in.skipBytes(2);
                break;
            case 0x47, 0x48, 0x49, 0x4A, 0x4B:
                in.skipBytes(3);
                break;
            case 0x40, 0x41:
                in.skipBytes(in.readUnsignedShort() * 6);
                break;
            default:
                // 0x13, 0x14, 0x15: empty
                break;
        }
        return NO_PARAMETER;
    }

    private static void skipElementValue(DataInputStream in) throws IOException {
        int tag = in.readUnsignedByte();

        switch (tag) {
            case 'e':
                in.skipBytes(4);
                break;
            case '@':
                in.skipBytes(2);
                for (int pairs = in.readUnsignedShort(); pairs > 0; pairs--) {
                    in.skipBytes(2);
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
                in.skipBytes(2);
                break;
        }
    }
}
