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

import java.io.File;
import java.lang.reflect.Array;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.function.Function;
import java.util.function.IntFunction;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.StreamSupport;

public class GenericArrayInferenceTest extends AbstractJdTest {
    @Test
    public void testGenericArraycopyHelpers() throws Exception {
        String internalClassName = ArrayCopyHelpers.class.getName().replace('.', '/');
        String source = decompileSuccess(new ClassPathLoader(), new PlainTextPrinter(), internalClassName);

        assertTrue(source.contains("return arraycopy(source, sourcePos, allocator.apply(Integer.valueOf(length)), destPos, length);")
                || source.contains("return arraycopy(source, sourcePos, allocator.apply(length), destPos, length);"));
        assertTrue(source.matches(PatternMaker.make("return ArrayCopyHelpers.<boolean[]>arraycopy(array, startIndexInclusive, 0, newSize, boolean[]::new);"))
                || source.matches(PatternMaker.make("return arraycopy(array, startIndexInclusive, 0, newSize, boolean[]::new);")));
        assertFalse(source.contains("(Object)allocator.apply"));
        assertFalse(source.contains("arraycopy((Object)array"));

        assertTrue(CompilerUtil.compile("1.8", new InMemoryJavaSourceFileObject(internalClassName, source)));
    }

    @Test
    public void testGenericVarargsInsertAndJoin() throws Exception {
        String internalClassName = ArrayInsertHelpers.class.getName().replace('.', '/');
        String source = decompileSuccess(new ClassPathLoader(), new PlainTextPrinter(), internalClassName);

        assertTrue(source.matches(PatternMaker.make("return (array == null) ? ArrayInsertHelpers.<T>add(array, element) : ArrayInsertHelpers.<T>insert(0, array, element);"))
                || source.matches(PatternMaker.make("return (array == null) ? add(array, element) : insert(0, array, element);")));
        assertTrue(source.matches(PatternMaker.make("T[] joinedArray = ArrayInsertHelpers.<T[]>arraycopy(array1, 0, 0, array1.length, () -> newArray(type1, array1.length + array2.length));"))
                || source.matches(PatternMaker.make("T[] joinedArray = arraycopy(array1, 0, 0, array1.length, () -> newArray(type1, array1.length + array2.length));")));
        assertFalse(source.contains("new Object[] { element }"));
        assertFalse(source.contains("(Object[])"));

        assertTrue(CompilerUtil.compile("1.8", new InMemoryJavaSourceFileObject(internalClassName, source)));
    }

    @Test
    public void testOverloadedGenericVarArgsInsert() throws Exception {
        String internalClassName = OverloadedArrayInsertHelpers.class.getName().replace('.', '/');
        String source = decompileSuccess(new ClassPathLoader(), new PlainTextPrinter(), internalClassName);

        assertTrue(source.matches(PatternMaker.make("return (array == null) ? OverloadedArrayInsertHelpers.<T>add(array, element) : OverloadedArrayInsertHelpers.<T>insert(0, array, element);"))
                || source.matches(PatternMaker.make("return (array == null) ? add(array, element) : insert(0, array, element);")));
        assertFalse(source.contains("new Object[] { element }"));

        assertTrue(CompilerUtil.compile("1.8", new InMemoryJavaSourceFileObject(internalClassName, source)));
    }

    @Test
    public void testTypedArraySetAllAndMethodReferenceAdapter() throws Exception {
        String internalClassName = ArrayMethodReferenceHelpers.class.getName().replace('.', '/');
        String source = decompileSuccess(new ClassPathLoader(), new PlainTextPrinter(), internalClassName);

        assertTrue(source.matches(PatternMaker.make("Arrays.setAll(fr, i -> () -> resources[i].run());")));
        assertTrue(source.matches(PatternMaker.make("return findThreadGroups(threadGroup, recurse, (Predicate<ThreadGroup>)predicate::test);")));
        assertTrue(source.matches(PatternMaker.make("tryWithResources(action, fr);"))
                || source.matches(PatternMaker.make("ArrayMethodReferenceHelpers.tryWithResources(action, fr);")));
        assertFalse(source.contains("(Object[])fr"));
        assertFalse(source.contains("(FailableRunnable)fr"));
        assertFalse(source.contains("(Predicate)predicate::test"));

        assertTrue(CompilerUtil.compile("1.8", new InMemoryJavaSourceFileObject(internalClassName, source)));
    }

    @Test
    public void testParameterizedArrayConstructorReferenceCastIsPreserved() throws Exception {
        String internalClassName = ComparatorArrayFactory.class.getName().replace('.', '/');
        String source = decompileSuccess(new ClassPathLoader(), new PlainTextPrinter(), internalClassName);

        assertFalse(source.contains("(Object[])"));
        assertTrue(CompilerUtil.compile("1.8", new InMemoryJavaSourceFileObject(internalClassName, source)));
    }

    @Test
    public void testWildcardArrayReturnCastIsPreserved() throws Exception {
        String internalClassName = WildcardComparatorArrayFactory.class.getName().replace('.', '/');
        String source = decompileSuccess(new ClassPathLoader(), new PlainTextPrinter(), internalClassName);

        assertFalse(source.contains("return EMPTY_COMPARATOR_ARRAY;"));
        assertTrue(CompilerUtil.compile("1.8", new InMemoryJavaSourceFileObject(internalClassName, source)));
    }

    @SuppressWarnings("unused")
    static final class ArrayCopyHelpers {
        static <T> T arraycopy(T source, int sourcePos, int destPos, int length, Function<Integer, T> allocator) {
            return arraycopy(source, sourcePos, allocator.apply(length), destPos, length);
        }

        static <T> T arraycopy(T source, int sourcePos, T dest, int destPos, int length) {
            return dest;
        }

        static boolean[] subarray(boolean[] array, int startIndexInclusive, int endIndexExclusive) {
            int newSize = endIndexExclusive - startIndexInclusive;
            return arraycopy(array, startIndexInclusive, 0, newSize, boolean[]::new);
        }
    }

    @SuppressWarnings("unused")
    static final class ArrayInsertHelpers {
        static <T> T[] add(T[] array, T element) {
            return array;
        }

        @SafeVarargs
        static <T> T[] insert(int index, T[] array, T... values) {
            return array;
        }

        static <T> T[] addFirst(T[] array, T element) {
            return array == null ? add(array, element) : insert(0, array, element);
        }

        static <T> T arraycopy(T source, int sourcePos, int destPos, int length, Supplier<T> allocator) {
            return allocator.get();
        }

        static <T> T[] newArray(Class<T> type, int length) {
            return (T[]) Array.newInstance(type, length);
        }

        static <T> T[] addAll(T[] array1, T... array2) {
            Class<T> type1 = (Class<T>) array1.getClass().getComponentType();
            T[] joinedArray = arraycopy(array1, 0, 0, array1.length, () -> newArray(type1, array1.length + array2.length));
            System.arraycopy(array2, 0, joinedArray, array1.length, array2.length);
            return joinedArray;
        }
    }

    @SuppressWarnings("unused")
    static final class ArrayMethodReferenceHelpers {
        @FunctionalInterface
        interface FailableRunnable<E extends Throwable> {
            void run() throws E;
        }

        @FunctionalInterface
        interface ThreadGroupPredicate {
            boolean test(ThreadGroup threadGroup);
        }

        static void copy(FailableRunnable<? extends Throwable>... resources) {
            FailableRunnable<?>[] fr = new FailableRunnable[resources.length];
            Arrays.setAll(fr, i -> () -> resources[i].run());
        }

        static void tryWithResources(FailableRunnable<? extends Throwable> action, FailableRunnable<? extends Throwable>... resources) {
        }

        static void copyAndRun(FailableRunnable<? extends Throwable> action, FailableRunnable<? extends Throwable>... resources) {
            FailableRunnable<?>[] fr = new FailableRunnable[resources.length];
            Arrays.setAll(fr, i -> () -> resources[i].run());
            tryWithResources(action, fr);
        }

        static Collection<ThreadGroup> findThreadGroups(ThreadGroup threadGroup, boolean recurse, Predicate<ThreadGroup> predicate) {
            return Collections.emptyList();
        }

        static Collection<ThreadGroup> findThreadGroups(ThreadGroup threadGroup, boolean recurse, ThreadGroupPredicate predicate) {
            return findThreadGroups(threadGroup, recurse, (Predicate<ThreadGroup>) predicate::test);
        }
    }

    @SuppressWarnings("unused")
    static final class OverloadedArrayInsertHelpers {
        static <T> T[] add(T[] array, T element) {
            return array;
        }

        static boolean[] insert(int index, boolean[] array, boolean... values) {
            return array;
        }

        static byte[] insert(int index, byte[] array, byte... values) {
            return array;
        }

        static char[] insert(int index, char[] array, char... values) {
            return array;
        }

        static double[] insert(int index, double[] array, double... values) {
            return array;
        }

        static float[] insert(int index, float[] array, float... values) {
            return array;
        }

        static int[] insert(int index, int[] array, int... values) {
            return array;
        }

        static long[] insert(int index, long[] array, long... values) {
            return array;
        }

        static short[] insert(int index, short[] array, short... values) {
            return array;
        }

        @SafeVarargs
        static <T> T[] insert(int index, T[] array, T... values) {
            return array;
        }

        static <T> T[] addFirst(T[] array, T element) {
            return array == null ? add(array, element) : insert(0, array, element);
        }
    }

    @SuppressWarnings("unused")
    static final class ComparatorArrayFactory {
        private static final Comparator<String>[] EMPTY_COMPARATOR_ARRAY = new Comparator[0];

        private final Comparator<String>[] factories;

        ComparatorArrayFactory(Iterable<Comparator<String>> factories) {
            this.factories = factories == null ? emptyArray()
                    : StreamSupport.stream(factories.spliterator(), false)
                    .toArray((IntFunction<Comparator<String>[]>) Comparator[]::new);
        }

        private Comparator<String>[] emptyArray() {
            return EMPTY_COMPARATOR_ARRAY;
        }
    }

    @SuppressWarnings("unused")
    static final class WildcardComparatorArrayFactory {
        private static final Comparator<?>[] EMPTY_COMPARATOR_ARRAY = new Comparator[0];

        private Comparator<File>[] emptyArray() {
            return (Comparator<File>[]) EMPTY_COMPARATOR_ARRAY;
        }
    }
}
