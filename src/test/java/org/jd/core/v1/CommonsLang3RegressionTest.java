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

import java.lang.reflect.Array;
import java.text.Format;
import java.time.Duration;
import java.util.Locale;
import java.util.TimeZone;
import java.util.function.Supplier;

public class CommonsLang3RegressionTest extends AbstractJdTest {
    @Test
    public void testArrayUtilsTypedArrayHelpersDoNotGainObjectArrayCasts() throws Exception {
        String internalClassName = ArrayUtilsShapes.class.getName().replace('.', '/');
        String source = decompileSuccess(new ClassPathLoader(), new PlainTextPrinter(), internalClassName);

        assertTrue(source.matches(PatternMaker.make("T[] joinedArray = ArrayUtilsShapes.<T[]>arraycopy(array1, 0, 0, array1.length, () -> newInstance(type1, array1.length + array2.length));"))
                || source.matches(PatternMaker.make("T[] joinedArray = arraycopy(array1, 0, 0, array1.length, () -> newInstance(type1, array1.length + array2.length));")));
        assertTrue(source.matches(PatternMaker.make("return ArrayUtilsShapes.<T[]>arraycopy(array, startIndexInclusive, 0, newSize, () -> newInstance(type, newSize));"))
                || source.matches(PatternMaker.make("return arraycopy(array, startIndexInclusive, 0, newSize, () -> newInstance(type, newSize));")));
        assertFalse(source.contains("(Object[])"));

        assertTrue(CompilerUtil.compile("1.8", new InMemoryJavaSourceFileObject(internalClassName, source)));
    }

    @Test
    public void testAbstractFormatCacheKeepsBoxedOverloadSelection() throws Exception {
        String internalClassName = FormatCacheShapes.class.getName().replace('.', '/');
        String source = decompileSuccess(new ClassPathLoader(), new PlainTextPrinter(), internalClassName);

        assertTrue(source.matches(PatternMaker.make("return getDateTimeInstance(Integer.valueOf(dateStyle), null, timeZone, locale);"))
                || source.matches(PatternMaker.make("return getDateTimeInstance(Integer.valueOf(dateStyle), (Integer)null, timeZone, locale);"))
                || source.matches(PatternMaker.make("return FormatCacheShapes.this.getDateTimeInstance(Integer.valueOf(dateStyle), null, timeZone, locale);"))
                || source.matches(PatternMaker.make("return (F)getDateTimeInstance(Integer.valueOf(dateStyle), null, timeZone, locale);"))
                || source.matches(PatternMaker.make("return (F)getDateTimeInstance(Integer.valueOf(dateStyle), (Integer)null, timeZone, locale);")));
        assertTrue(source.matches(PatternMaker.make("return getDateTimeInstance(Integer.valueOf(dateStyle), Integer.valueOf(timeStyle), timeZone, locale);"))
                || source.matches(PatternMaker.make("return (F)getDateTimeInstance(Integer.valueOf(dateStyle), Integer.valueOf(timeStyle), timeZone, locale);")));
        assertTrue(source.matches(PatternMaker.make("return getDateTimeInstance(null, Integer.valueOf(timeStyle), timeZone, locale);"))
                || source.matches(PatternMaker.make("return getDateTimeInstance((Integer)null, Integer.valueOf(timeStyle), timeZone, locale);"))
                || source.matches(PatternMaker.make("return (F)getDateTimeInstance(null, Integer.valueOf(timeStyle), timeZone, locale);"))
                || source.matches(PatternMaker.make("return (F)getDateTimeInstance((Integer)null, Integer.valueOf(timeStyle), timeZone, locale);")));

        assertTrue(CompilerUtil.compile("1.8", new InMemoryJavaSourceFileObject(internalClassName, source)));
    }

    @Test
    public void testDurationUtilsKeepsGenericConsumerAndNumericOverloadValid() throws Exception {
        String internalClassName = DurationUtilsShapes.class.getName().replace('.', '/');
        String source = decompileSuccess(new ClassPathLoader(), new PlainTextPrinter(), internalClassName);

        assertFalse(source.contains("consumer.accept((T)duration.toMillis()"));
        assertTrue(source.matches(PatternMaker.make("consumer.accept(duration.toMillis(), getNanosOfMilli(duration));"))
                || source.matches(PatternMaker.make("consumer.accept(Long.valueOf(duration.toMillis()), Integer.valueOf(getNanosOfMilli(duration)));"))
                || source.matches(PatternMaker.make("consumer.accept(Long.valueOf(duration.toMillis()), getNanosOfMilli(duration));"))
                || source.matches(PatternMaker.make("consumer.accept(duration.toMillis(), Integer.valueOf(getNanosOfMilli(duration)));")));
        assertTrue(source.matches(PatternMaker.make("return LONG_TO_INT_RANGE.fit(Long.valueOf(duration.toMillis())).intValue();")));

        assertTrue(CompilerUtil.compile("1.8", new InMemoryJavaSourceFileObject(internalClassName, source)));
    }

    @SuppressWarnings("unused")
    static final class ArrayUtilsShapes {
        static <T> Class<T> getComponentType(T[] array) {
            return (Class<T>) array.getClass().getComponentType();
        }

        static <T> T arraycopy(T source, int sourcePos, int destPos, int length, Supplier<T> allocator) {
            return arraycopy(source, sourcePos, allocator.get(), destPos, length);
        }

        static <T> T arraycopy(T source, int sourcePos, T dest, int destPos, int length) {
            return dest;
        }

        static <T> T[] newInstance(Class<T> componentType, int length) {
            return (T[]) Array.newInstance(componentType, length);
        }

        static <T> T[] addAll(T[] array1, T... array2) {
            Class<T> type1 = getComponentType(array1);
            T[] joinedArray = arraycopy(array1, 0, 0, array1.length, () -> newInstance(type1, array1.length + array2.length));
            System.arraycopy(array2, 0, joinedArray, array1.length, array2.length);
            return joinedArray;
        }

        static <T> T[] subarray(T[] array, int startIndexInclusive, int endIndexExclusive) {
            int newSize = endIndexExclusive - startIndexInclusive;
            Class<T> type = getComponentType(array);
            if (newSize <= 0) {
                return newInstance(type, 0);
            }
            return arraycopy(array, startIndexInclusive, 0, newSize, () -> newInstance(type, newSize));
        }
    }

    @SuppressWarnings("unused")
    abstract static class FormatCacheShapes<F extends Format> {
        F getDateInstance(int dateStyle, TimeZone timeZone, Locale locale) {
            return getDateTimeInstance(Integer.valueOf(dateStyle), null, timeZone, locale);
        }

        F getDateTimeInstance(int dateStyle, int timeStyle, TimeZone timeZone, Locale locale) {
            return getDateTimeInstance(Integer.valueOf(dateStyle), Integer.valueOf(timeStyle), timeZone, locale);
        }

        private F getDateTimeInstance(Integer dateStyle, Integer timeStyle, TimeZone timeZone, Locale locale) {
            return null;
        }

        F getTimeInstance(int timeStyle, TimeZone timeZone, Locale locale) {
            return getDateTimeInstance(null, Integer.valueOf(timeStyle), timeZone, locale);
        }
    }

    @SuppressWarnings("unused")
    static final class DurationUtilsShapes {
        interface FailableBiConsumer<T, U, E extends Throwable> {
            void accept(T object1, U object2) throws E;
        }

        static class NumberRange<T extends Number> {
            T fit(T element) {
                return element;
            }
        }

        static final class LongRange extends NumberRange<Long> {
            long fit(long element) {
                return fit(Long.valueOf(element)).longValue();
            }
        }

        static final LongRange LONG_TO_INT_RANGE = new LongRange();

        static int getNanosOfMilli(Duration duration) {
            return duration.getNano() % 1000000;
        }

        static <T extends Throwable> void accept(FailableBiConsumer<Long, Integer, T> consumer, Duration duration) throws T {
            if (consumer != null && duration != null) {
                consumer.accept(duration.toMillis(), getNanosOfMilli(duration));
            }
        }

        static int toMillisInt(Duration duration) {
            return LONG_TO_INT_RANGE.fit(Long.valueOf(duration.toMillis())).intValue();
        }
    }
}
