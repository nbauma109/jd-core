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
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collector;
import java.util.function.Predicate;
import java.util.function.Function;
import java.util.function.Supplier;

public class GenericHelperCastTest extends AbstractJdTest {
    @Test
    public void testPredicateAndSupplierHelpers() throws Exception {
        String internalClassName = FunctionalHelperCalls.class.getName().replace('.', '/');
        String source = decompileSuccess(new ClassPathLoader(), new PlainTextPrinter(), internalClassName);

        assertTrue(source.matches(PatternMaker.make("return getProperty(property, GenericHelperCastTest.FunctionalHelpers.nul());")));
        assertTrue(source.matches(PatternMaker.make("return findAll(GenericHelperCastTest.FunctionalHelpers.truePredicate());")));
        assertFalse(source.contains("(Supplier<String>)"));
        assertFalse(source.contains("(Predicate<String>)"));

        assertTrue(CompilerUtil.compile("1.8", new InMemoryJavaSourceFileObject(internalClassName, source)));
    }

    @Test
    public void testRequireNonNullCheckedFunction() throws Exception {
        String internalClassName = ThrowingFunctionCalls.class.getName().replace('.', '/');
        String source = decompileSuccess(new ClassPathLoader(), new PlainTextPrinter(), internalClassName);

        assertTrue(source.matches(PatternMaker.make("return (value != null) ? (R)Objects.requireNonNull(mapper, \"mapper\").apply(value) : null;")));
        assertFalse(source.contains("Objects.<ThrowingFunction>requireNonNull"));

        assertTrue(CompilerUtil.compile("1.8", new InMemoryJavaSourceFileObject(internalClassName, source)));
    }

    @Test
    public void testRequireNonNullSupplierHelper() throws Exception {
        String internalClassName = SupplierRequireNonNullCalls.class.getName().replace('.', '/');
        String source = decompileSuccess(new ClassPathLoader(), new PlainTextPrinter(), internalClassName);

        assertTrue(source.matches(PatternMaker.make("return Objects.requireNonNull(object, toSupplier(message, values));")));
        assertFalse(source.contains("(Supplier)toSupplier"));

        assertTrue(CompilerUtil.compile("1.8", new InMemoryJavaSourceFileObject(internalClassName, source)));
    }

    @Test
    public void testSingleElementObjectVarArgsMessageArgument() throws Exception {
        String internalClassName = MessageVarArgsCalls.class.getName().replace('.', '/');
        String source = decompileSuccess(new ClassPathLoader(), new PlainTextPrinter(), internalClassName);

        assertTrue(source.matches(PatternMaker.make("return validIndex(collection, index, \"The validated collection index is invalid: %d\", Integer.valueOf(index));"))
                || source.matches(PatternMaker.make("return validIndex(collection, index, \"The validated collection index is invalid: %d\", index);")));
        assertFalse(source.contains("(Object[])index"));

        assertTrue(CompilerUtil.compile("1.8", new InMemoryJavaSourceFileObject(internalClassName, source)));
    }

    @Test
    public void testThrowableFunctionalLambdaCast() throws Exception {
        String internalClassName = ThrowableLambdaCalls.class.getName().replace('.', '/');
        String source = decompileSuccess(new ClassPathLoader(), new PlainTextPrinter(), internalClassName);

        assertFalse(source.contains("(ThrowableConsumer<String, Throwable>)"));
        assertTrue(CompilerUtil.compile("1.8", new InMemoryJavaSourceFileObject(internalClassName, source)));
    }

    @Test
    public void testGenericArrayFactoryHelper() throws Exception {
        String internalClassName = ArrayCollector.class.getName().replace('.', '/');
        String source = decompileSuccess(new ClassPathLoader(), new PlainTextPrinter(), internalClassName);

        assertTrue(source.matches(PatternMaker.make("return list -> list.toArray(GenericHelperCastTest.ArrayFactory.newArray(this.elementType, list.size()));")));
        assertFalse(source.contains("(Object[])"));

        assertTrue(CompilerUtil.compile("1.8", new InMemoryJavaSourceFileObject(internalClassName, source)));
    }

    @Test
    public void testEnumSetLambdaRequireNonNull() throws Exception {
        String internalClassName = EnumLambdaHelpers.class.getName().replace('.', '/');
        String source = decompileSuccess(new ClassPathLoader(), new PlainTextPrinter(), internalClassName);

        assertTrue(source.matches(PatternMaker.make("values.forEach(constant -> condensed.add(Objects.requireNonNull(constant, \"constant\")));")));
        assertFalse(source.contains("(Enum)"));
        assertFalse(source.contains("Objects.<Enum>requireNonNull"));

        assertTrue(CompilerUtil.compile("1.8", new InMemoryJavaSourceFileObject(internalClassName, source)));
    }

    @SuppressWarnings("unused")
    static final class FunctionalHelpers {
        static <T> Supplier<T> nul() {
            return () -> null;
        }

        static <T> Predicate<T> truePredicate() {
            return ignored -> true;
        }
    }

    @FunctionalInterface
    interface ThrowingFunction<T, R, E extends Throwable> {
        R apply(T value) throws E;
    }

    @FunctionalInterface
    interface ThrowableConsumer<T, E extends Throwable> {
        void accept(T value) throws E;
    }

    @SuppressWarnings("unused")
    static final class ThrowingFunctionCalls {
        static <T, R, E extends Throwable> R applyNonNull(T value, ThrowingFunction<? super T, ? extends R, E> mapper) throws E {
            return value != null ? Objects.requireNonNull(mapper, "mapper").apply(value) : null;
        }
    }

    @SuppressWarnings("unused")
    static final class SupplierRequireNonNullCalls {
        static <T> T notNull(T object, String message, Object... values) {
            return Objects.requireNonNull(object, toSupplier(message, values));
        }

        private static Supplier<String> toSupplier(String message, Object... values) {
            return () -> String.format(message, values);
        }
    }

    @SuppressWarnings("unused")
    static final class MessageVarArgsCalls {
        static <T extends Collection<?>> T validIndex(T collection, int index, String message, Object... values) {
            return collection;
        }

        static <T extends Collection<?>> T validIndex(T collection, int index) {
            return validIndex(collection, index, "The validated collection index is invalid: %d", Integer.valueOf(index));
        }
    }

    @SuppressWarnings("unused")
    static final class ThrowableLambdaCalls {
        static String call(ThrowableConsumer<String, ? extends Throwable> consumer) {
            return "ok";
        }

        static String callUnchecked() {
            return call((ThrowableConsumer<String, Throwable>) value -> { });
        }
    }


    @SuppressWarnings("unused")
    static final class FunctionalHelperCalls {
        static String getProperty(String property, Supplier<String> supplier) {
            return supplier.get();
        }

        static String getProperty(String property, String defaultValue) {
            return defaultValue;
        }

        static String getProperty(String property) {
            return getProperty(property, FunctionalHelpers.nul());
        }

        static <T> Collection<T> findAll(Predicate<T> predicate) {
            return Collections.emptyList();
        }

        interface StringPredicate {
            boolean test(String value);
        }

        static Collection<String> findAll(StringPredicate predicate) {
            return Collections.emptyList();
        }

        static Collection<String> getAll() {
            return findAll(FunctionalHelpers.truePredicate());
        }
    }

    @SuppressWarnings("unused")
    static final class ArrayFactory {
        static <T> T[] newArray(Class<T> elementType, int size) {
            return (T[]) Array.newInstance(elementType, size);
        }
    }

    @SuppressWarnings("unused")
    static final class ArrayCollector<O> implements Collector<O, List<O>, O[]> {
        private final Class<O> elementType;
        private final Set<Characteristics> characteristics = Collections.emptySet();

        private ArrayCollector(Class<O> elementType) {
            this.elementType = elementType;
        }

        @Override
        public java.util.function.Supplier<List<O>> supplier() {
            return java.util.ArrayList::new;
        }

        @Override
        public java.util.function.BiConsumer<List<O>, O> accumulator() {
            return List::add;
        }

        @Override
        public java.util.function.BinaryOperator<List<O>> combiner() {
            return (left, right) -> {
                left.addAll(right);
                return left;
            };
        }

        @Override
        public Function<List<O>, O[]> finisher() {
            return list -> list.toArray(ArrayFactory.newArray(this.elementType, list.size()));
        }

        @Override
        public Set<Characteristics> characteristics() {
            return characteristics;
        }
    }

    enum Sample {
        A
    }

    @SuppressWarnings("unused")
    static final class EnumLambdaHelpers {
        static <E extends Enum<E>> void copyIntoEnumSet(Class<E> enumClass, Iterable<? extends E> values) {
            EnumSet<E> condensed = EnumSet.noneOf(enumClass);
            values.forEach(constant -> condensed.add(Objects.requireNonNull(constant, "constant")));
        }

        static void copySamples(Iterable<Sample> values) {
            copyIntoEnumSet(Sample.class, values);
        }
    }
}
