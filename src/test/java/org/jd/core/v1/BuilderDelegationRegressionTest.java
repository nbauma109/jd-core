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

public class BuilderDelegationRegressionTest extends AbstractJdTest {
    @Test
    public void testGenericHashCodeBuilderDelegatesOmitSyntheticEmptyVarArgsAndNullClassCasts() throws Exception {
        String internalClassName = HashCodeDelegates.class.getName().replace('.', '/');
        String source = decompileSuccess(new ClassPathLoader(), new PlainTextPrinter(), internalClassName);

        assertTrue(source.matches(PatternMaker.make("return reflectionHashCode(initialNonZeroOddNumber, multiplierNonZeroOddNumber, object, false, null);")));
        assertTrue(source.matches(PatternMaker.make("return reflectionHashCode(initialNonZeroOddNumber, multiplierNonZeroOddNumber, object, testTransients, null);")));
        assertTrue(source.matches(PatternMaker.make("return reflectionHashCode(17, 37, object, testTransients, null);")));
        assertTrue(source.matches(PatternMaker.make("return reflectionHashCode(17, 37, object, false, null, excludeFields);")));
        assertFalse(source.contains("(Class<?>)null"));
        assertFalse(source.contains("new String[0]"));

        assertTrue(CompilerUtil.compile("1.8", new InMemoryJavaSourceFileObject(internalClassName, source)));
    }

    @Test
    public void testGenericReflectionToStringDelegatesKeepPlainNullClassArgument() throws Exception {
        String internalClassName = ReflectionToStringDelegates.class.getName().replace('.', '/');
        String source = decompileSuccess(new ClassPathLoader(), new PlainTextPrinter(), internalClassName);

        assertTrue(source.matches(PatternMaker.make("return toString(object, null, false, false, null);")));
        assertTrue(source.matches(PatternMaker.make("return toString(object, style, false, false, null);")));
        assertTrue(source.matches(PatternMaker.make("return toString(object, style, outputTransients, false, null);")));
        assertTrue(source.matches(PatternMaker.make("return toString(object, style, outputTransients, outputStatics, null);")));
        assertTrue(source.matches(PatternMaker.make("return ReflectionToStringDelegates.toString(object, style, outputTransients, false, null);"))
                || source.matches(PatternMaker.make("return toString(object, style, outputTransients, false, null);")));
        assertFalse(source.contains("(Class<?>)null"));

        assertTrue(CompilerUtil.compile("1.8", new InMemoryJavaSourceFileObject(internalClassName, source)));
    }

    @SuppressWarnings("unused")
    static final class HashCodeDelegates {
        public static int reflectionHashCode(int initialNonZeroOddNumber, int multiplierNonZeroOddNumber, Object object) {
            return reflectionHashCode(initialNonZeroOddNumber, multiplierNonZeroOddNumber, object, false, null);
        }

        public static int reflectionHashCode(int initialNonZeroOddNumber, int multiplierNonZeroOddNumber, Object object,
                boolean testTransients) {
            return reflectionHashCode(initialNonZeroOddNumber, multiplierNonZeroOddNumber, object, testTransients, null);
        }

        public static <T> int reflectionHashCode(int initialNonZeroOddNumber, int multiplierNonZeroOddNumber, T object,
                boolean testTransients, Class<? super T> reflectUpToClass, String... excludeFields) {
            return 0;
        }

        public static int reflectionHashCode(Object object, boolean testTransients) {
            return reflectionHashCode(17, 37, object, testTransients, null);
        }

        public static int reflectionHashCode(Object object, String... excludeFields) {
            return reflectionHashCode(17, 37, object, false, null, excludeFields);
        }
    }

    @SuppressWarnings("unused")
    static final class ReflectionToStringDelegates {
        static final class Style {
        }

        public static String toString(Object object) {
            return toString(object, null, false, false, null);
        }

        public static String toString(Object object, Style style) {
            return toString(object, style, false, false, null);
        }

        public static String toString(Object object, Style style, boolean outputTransients) {
            return toString(object, style, outputTransients, false, null);
        }

        public static String toString(Object object, Style style, boolean outputTransients, boolean outputStatics) {
            return toString(object, style, outputTransients, outputStatics, null);
        }

        public static <T> String toString(T object, Style style, boolean outputTransients, boolean outputStatics,
                Class<? super T> reflectUpToClass) {
            return "";
        }

        public static String reflectionToString(Object object, Style style, boolean outputTransients) {
            return ReflectionToStringDelegates.toString(object, style, outputTransients, false, null);
        }
    }
}
