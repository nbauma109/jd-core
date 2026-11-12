/*
 * Copyright (c) 2008, 2019 Emmanuel Dupuy.
 * This project is distributed under the GPLv3 license.
 * This is a Copyleft license that gives the user the right to use,
 * copy and modify the code freely for non-commercial purposes.
 */
package org.jd.core.v1.service.converter.classfiletojavasyntax.util;

import org.apache.bcel.classfile.AnnotationEntry;
import org.apache.bcel.classfile.Attribute;
import org.apache.bcel.classfile.FieldOrMethod;
import org.apache.bcel.classfile.TypeAnnotationEntry;
import org.apache.bcel.classfile.TypeAnnotations;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Reads the type annotations (JSR 308) of the attributes of BCEL, such as the '@Nullable' of 'void put(@Nullable Object key)'.
 * Only the marker annotations of the whole type of a field, a method result or a parameter are kept: those can be written in front of the declaration.
 */
public final class TypeAnnotationReader {
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
        return of(fieldOrMethod.getAttributes());
    }

    public static TypeAnnotationReader of(Attribute[] attributes) {
        TypeAnnotationReader reader = new TypeAnnotationReader();

        for (Attribute attribute : attributes) {
            if (attribute instanceof TypeAnnotations typeAnnotations) {
                for (TypeAnnotationEntry entry : typeAnnotations) {
                    reader.read(entry);
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

    private void read(TypeAnnotationEntry entry) {
        AnnotationEntry annotation = entry.getAnnotationEntry();

        if (entry.getTypePathLength() == 0 && annotation.getNumElementValuePairs() == 0) {
            keep(entry.getTargetType(), entry.getTargetInfo(), annotation.getAnnotationType());
        }
    }

    /** Keeps the annotation of a whole type: the others (the path is not empty, the annotation has elements) are not represented */
    private void keep(int target, byte[] info, String descriptor) {
        if (target == TypeAnnotationEntry.TARGET_FIELD || target == TypeAnnotationEntry.TARGET_METHOD_RETURN) {
            declaration.add(descriptor);
        } else if (target == TypeAnnotationEntry.TARGET_METHOD_FORMAL_PARAMETER && info.length > 0) {
            parameters.computeIfAbsent(info[0] & 0xFF, k -> new ArrayList<>()).add(descriptor);
        } else if ((target == TypeAnnotationEntry.TARGET_CLASS_TYPE_PARAMETER_BOUND || target == TypeAnnotationEntry.TARGET_METHOD_TYPE_PARAMETER_BOUND)
                && info.length > 1 && (info[1] & 0xFF) <= 1) {
            ((info[1] & 0xFF) == 0 ? bounds : interfaceBounds).computeIfAbsent(info[0] & 0xFF, k -> new ArrayList<>()).add(descriptor);
        }
    }
}
