/*
 * Copyright (c) 2008, 2019 Emmanuel Dupuy.
 * This project is distributed under the GPLv3 license.
 * This is a Copyleft license that gives the user the right to use,
 * copy and modify the code freely for non-commercial purposes.
 */

package org.jd.core.v1;

import org.jd.core.v1.compiler.CompilerUtil;
import org.jd.core.v1.compiler.InMemoryJavaSourceFileObject;
import org.jd.core.v1.loader.ClassPathLoader;
import org.jd.core.v1.printer.PlainTextPrinter;
import org.jd.core.v1.regex.PatternMaker;
import org.junit.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class NullAndTypeCastRegressionTest extends AbstractJdTest {
    @Test
    public void testOverloadedNullArgumentsKeepDisambiguatingCast() throws Exception {
        String internalClassName = NullOverloadCalls.class.getName().replace('.', '/');
        String source = decompileSuccess(new ClassPathLoader(), new PlainTextPrinter(), internalClassName);

        assertTrue(source.matches(PatternMaker.make("return readField(field, (Object)null, forceAccess);")));
        assertTrue(source.matches(PatternMaker.make("writeField(field, (Object)null, value, false);")));
        assertTrue(source.matches(PatternMaker.make("writeField(field, (Object)null, value, forceAccess);")));

        assertTrue(CompilerUtil.compile("1.8", new InMemoryJavaSourceFileObject(internalClassName, source)));
    }

    @Test
    public void testInstanceofAndCloneCastsArePreserved() throws Exception {
        String internalClassName = TypeAndCloneHelpers.class.getName().replace('.', '/');
        String source = decompileSuccess(new ClassPathLoader(), new PlainTextPrinter(), internalClassName);

        assertTrue(source.matches(PatternMaker.make("return (object instanceof Type) ? toString((Type)object) : object.toString();")));
        assertTrue(source.matches(PatternMaker.make("return classToString((Class<?>)type);")));
        assertTrue(source.matches(PatternMaker.make("typeVarAssigns.put((TypeVariable<?>)typeArg, typeVarAssigns.get(typeVar));")));
        assertTrue(source.matches(PatternMaker.make("TypeAndCloneHelpers cloned = (TypeAndCloneHelpers)super.clone();"))
                || source.matches(PatternMaker.make("TypeAndCloneHelpers cloned = (TypeAndCloneHelpers)clone();")));

        assertTrue(CompilerUtil.compile("1.8", new InMemoryJavaSourceFileObject(internalClassName, source)));
    }

    @SuppressWarnings("unused")
    static final class NullOverloadCalls {
        static Object readField(Field field, Object target, boolean forceAccess) {
            return field;
        }

        static Object readField(Field field, String fieldName, boolean forceAccess) {
            return fieldName;
        }

        static void writeField(Field field, Object target, Object value, boolean forceAccess) {
        }

        static void writeField(Field field, String fieldName, Object value, boolean forceAccess) {
        }

        static Object readStaticField(Field field, boolean forceAccess) {
            Objects.requireNonNull(field, "field");
            return readField(field, (Object) null, forceAccess);
        }

        static void writeDeclaredStaticField(Field field, Object value) {
            writeField(field, (Object) null, value, false);
        }

        static void writeStaticField(Field field, Object value, boolean forceAccess) {
            Objects.requireNonNull(field, "field");
            writeField(field, (Object) null, value, forceAccess);
        }
    }

    @SuppressWarnings("unused")
    static class TypeAndCloneHelpers implements Cloneable {
        private static <T> String anyToString(T object) {
            return object instanceof Type ? toString((Type) object) : object.toString();
        }

        static String toString(Type type) {
            return type.getTypeName();
        }

        static <T> String classToString(Class<T> type) {
            return type.getName();
        }

        static String typeToString(Type type) {
            if (type instanceof Class<?>) {
                return classToString((Class<?>) type);
            }
            return toString(type);
        }

        static void remap(Map<TypeVariable<?>, Type> typeVarAssigns, TypeVariable<?>[] typeVars, Type[] typeArgs, List<TypeVariable<?>> typeVarList) {
            for (int i = 0; i < typeVars.length; i++) {
                TypeVariable<?> typeVar = typeVars[i];
                Type typeArg = typeArgs[i];
                if (typeVarList.contains(typeArg) && typeVarAssigns.containsKey(typeVar)) {
                    typeVarAssigns.put((TypeVariable<?>) typeArg, typeVarAssigns.get(typeVar));
                }
            }
        }

        Object cloneReset() throws CloneNotSupportedException {
            TypeAndCloneHelpers cloned = (TypeAndCloneHelpers) super.clone();
            return cloned;
        }
    }
}
