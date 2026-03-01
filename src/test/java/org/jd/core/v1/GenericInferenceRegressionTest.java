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

import java.net.URI;
import java.nio.file.FileVisitor;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

public class GenericInferenceRegressionTest extends AbstractJdTest {
    @Test
    public void testGenericBoundReturnCastIsPreserved() throws Exception {
        String internalClassName = GenericBoundReturnCalls.class.getName().replace('.', '/');
        String source = decompileSuccess(new ClassPathLoader(), new PlainTextPrinter(), internalClassName);

        assertTrue(source.matches(PatternMaker.make("return (T)getConstant(index, Constant.class);"))
                || source.matches(PatternMaker.make("return (T)GenericInferenceRegressionTest.GenericBoundReturnCalls.getConstant(index, Constant.class);")));
        assertTrue(CompilerUtil.compile("1.8", new InMemoryJavaSourceFileObject(internalClassName, source)));
    }

    @Test
    public void testSelfTypeThisCastIsPreserved() throws Exception {
        String internalClassName = SelfTypedStreams.class.getName().replace('.', '/');
        String source = decompileSuccess(new ClassPathLoader(), new PlainTextPrinter(), internalClassName);

        assertTrue(source.matches(PatternMaker.make("return new UncheckedIOBaseStream<>((S)this);"))
                || source.matches(PatternMaker.make("return new GenericInferenceRegressionTest.UncheckedIOBaseStream<>((S)this);")));
        assertTrue(CompilerUtil.compile("1.8", new InMemoryJavaSourceFileObject(internalClassName, source)));
    }

    @Test
    public void testSubtypeArgumentDoesNotDegenerateToBoundCast() throws Exception {
        String internalClassName = VisitorCalls.class.getName().replace('.', '/');
        String source = decompileSuccess(new ClassPathLoader(), new PlainTextPrinter(), internalClassName);

        assertTrue(source.matches(PatternMaker.make("return visitFileTree(CountingPathVisitor.withLongCounters(), directory).getPathCounters();"))
                || source.matches(PatternMaker.make("return ((CountingPathVisitor)visitFileTree(CountingPathVisitor.withLongCounters(), directory)).getPathCounters();")));
        assertFalse(source.contains("(FileVisitorLike)CountingPathVisitor.withLongCounters()"));
    }

    @Test
    public void testNewSubtypeArgumentDoesNotDegenerateToBoundCast() throws Exception {
        String internalClassName = NewVisitorCalls.class.getName().replace('.', '/');
        String source = decompileSuccess(new ClassPathLoader(), new PlainTextPrinter(), internalClassName);

        assertTrue(source.matches(PatternMaker.make("return visitFileTree(new CountingPathVisitor(), directory).getPathCounters();"))
                || source.matches(PatternMaker.make("return ((CountingPathVisitor)visitFileTree(new CountingPathVisitor(), directory)).getPathCounters();")));
        assertFalse(source.contains("(FileVisitorLike)new CountingPathVisitor()"));
    }

    @Test
    public void testJdkFileVisitorHelperKeepsConcreteArgumentType() throws Exception {
        String internalClassName = JdkFileVisitorCalls.class.getName().replace('.', '/');
        String source = decompileSuccess(new ClassPathLoader(), new PlainTextPrinter(), internalClassName);

        assertTrue(source.matches(PatternMaker.make("return visitFileTree(new CountingPathVisitor(), directory).getPathCounters();"))
                || source.matches(PatternMaker.make("return ((CountingPathVisitor)visitFileTree(new CountingPathVisitor(), directory)).getPathCounters();"))
                || source.matches(PatternMaker.make("return ((CountingPathVisitor)visitFileTree((FileVisitor)new CountingPathVisitor(), directory)).getPathCounters();")));
    }

    @Test
    public void testOverloadedSubtypeArgumentDoesNotDegenerateToBoundCast() throws Exception {
        String internalClassName = OverloadedVisitorCalls.class.getName().replace('.', '/');
        String source = decompileSuccess(new ClassPathLoader(), new PlainTextPrinter(), internalClassName);

        assertTrue(source.matches(PatternMaker.make("return visitFileTree(CountingPathVisitor.withLongCounters(), directory).getPathCounters();"))
                || source.matches(PatternMaker.make("return ((CountingPathVisitor)visitFileTree(CountingPathVisitor.withLongCounters(), directory)).getPathCounters();")));
        assertFalse(source.contains("(OverloadedFileVisitorLike)CountingPathVisitor.withLongCounters()"));
    }

    @Test
    public void testInterfaceSubtypeParameterKeepsDirectArgumentType() throws Exception {
        String internalClassName = FunctionCalls.class.getName().replace('.', '/');
        String source = decompileSuccess(new ClassPathLoader(), new PlainTextPrinter(), internalClassName);

        assertTrue(source.matches(PatternMaker.make("return EraseLike.apply(accumulator, value, value);"))
                || source.matches(PatternMaker.make("return GenericInferenceRegressionTest.EraseLike.apply(accumulator, value, value);")));
        assertFalse(source.contains("(IOBiFunction"));
    }

    @Test
    public void testGenericHelperKeepsConcreteArrayArgumentType() throws Exception {
        String internalClassName = ArrayHelperCalls.class.getName().replace('.', '/');
        String source = decompileSuccess(new ClassPathLoader(), new PlainTextPrinter(), internalClassName);

        assertTrue(source.matches(PatternMaker.make("return UncheckLike.apply(ArrayHelperCalls::read0, cbuf, off, len);"))
                || source.matches(PatternMaker.make("return (Integer)UncheckLike.apply(ArrayHelperCalls::read0, cbuf, Integer.valueOf(off), Integer.valueOf(len));"))
                || source.matches(PatternMaker.make("return UncheckLike.apply(ArrayHelperCalls::read0, cbuf, Integer.valueOf(off), Integer.valueOf(len));"))
                || source.matches(PatternMaker.make("return GenericInferenceRegressionTest.ArrayHelperCalls.UncheckLike.apply(GenericInferenceRegressionTest.ArrayHelperCalls::read0, cbuf, off, len);"))
                || source.matches(PatternMaker.make("return (Integer)GenericInferenceRegressionTest.ArrayHelperCalls.UncheckLike.apply(GenericInferenceRegressionTest.ArrayHelperCalls::read0, cbuf, Integer.valueOf(off), Integer.valueOf(len));")));
        assertFalse(source.contains("(Object)cbuf"));
        assertTrue(CompilerUtil.compile("1.8", new InMemoryJavaSourceFileObject(internalClassName, source)));
    }

    @Test
    public void testGenericStreamReceiverKeepsParameterizedType() throws Exception {
        String internalClassName = "org/jd/core/v1/StreamReceiverStandalone";
        String source = decompileSuccess(new ClassPathLoader(), new PlainTextPrinter(), internalClassName);

        assertTrue(source.matches(PatternMaker.make("return unwrap().allMatch(t -> Erase.test(predicate, t));"))
                || source.matches(PatternMaker.make("return ((Stream<T>)unwrap()).allMatch(t -> Erase.test(predicate, t));")));
        assertFalse(source.contains("((Stream)unwrap())"));
        assertTrue(CompilerUtil.compile("1.8", new InMemoryJavaSourceFileObject(internalClassName, source)));
    }

    @Test
    public void testGenericReceiverKeepsConcreteToArrayType() throws Exception {
        String internalClassName = ArrayReceiverCalls.class.getName().replace('.', '/');
        String source = decompileSuccess(new ClassPathLoader(), new PlainTextPrinter(), internalClassName);

        assertTrue(source.matches(PatternMaker.make("return requireWildcards(wildcards).toArray(EMPTY_STRING_ARRAY);"))
                || source.matches(PatternMaker.make("return (String[])requireWildcards(wildcards).toArray(EMPTY_STRING_ARRAY);"))
                || source.matches(PatternMaker.make("return ArrayReceiverCalls.requireWildcards(wildcards).toArray(ArrayReceiverCalls.EMPTY_STRING_ARRAY);")));
        assertFalse(source.contains("(List)"));
        assertFalse(source.contains("(Object[])"));
        assertTrue(CompilerUtil.compile("1.8", new InMemoryJavaSourceFileObject(internalClassName, source)));
    }

    @Test
    public void testRequireNonNullGenericReceiverKeepsConcreteToArrayType() throws Exception {
        String internalClassName = RequireNonNullArrayReceiverCalls.class.getName().replace('.', '/');
        String source = decompileSuccess(new ClassPathLoader(), new PlainTextPrinter(), internalClassName);

        assertTrue(source.matches(PatternMaker.make("return requireWildcards(wildcards).toArray(EMPTY_STRING_ARRAY);"))
                || source.matches(PatternMaker.make("return (String[])requireWildcards(wildcards).toArray(EMPTY_STRING_ARRAY);"))
                || source.matches(PatternMaker.make("return RequireNonNullArrayReceiverCalls.requireWildcards(wildcards).toArray(RequireNonNullArrayReceiverCalls.EMPTY_STRING_ARRAY);")));
        assertFalse(source.contains("(List)"));
        assertFalse(source.contains("(Object[])"));
        assertTrue(CompilerUtil.compile("1.8", new InMemoryJavaSourceFileObject(internalClassName, source)));
    }

    @Test
    public void testRequireNonNullGenericReceiverKeepsConcreteToArrayTypeInDelegatingCall() throws Exception {
        String internalClassName = DelegatingArrayReceiverCalls.class.getName().replace('.', '/');
        String source = decompileSuccess(new ClassPathLoader(), new PlainTextPrinter(), internalClassName);

        assertTrue(source.matches(PatternMaker.make("setWildcards(requireWildcards(wildcards).toArray(EMPTY_STRING_ARRAY));"))
                || source.matches(PatternMaker.make("setWildcards((String[])requireWildcards(wildcards).toArray(EMPTY_STRING_ARRAY));"))
                || source.matches(PatternMaker.make("this.setWildcards(DelegatingArrayReceiverCalls.requireWildcards(wildcards).toArray(DelegatingArrayReceiverCalls.EMPTY_STRING_ARRAY));")));
        assertFalse(source.contains("(List)"));
        assertFalse(source.contains("(Object[])"));
        assertTrue(CompilerUtil.compile("1.8", new InMemoryJavaSourceFileObject(internalClassName, source)));
    }

    @Test
    public void testNestedRequireNonNullGenericReceiverKeepsConcreteToArrayTypeInDelegatingCall() throws Exception {
        String internalClassName = NestedDelegatingArrayReceiverCalls.class.getName().replace('.', '/');
        String source = decompileSuccess(new ClassPathLoader(), new PlainTextPrinter(), internalClassName);

        assertTrue(source.contains("requireWildcards(wildcards).toArray("));
        assertFalse(source.contains("(List)"));
        assertFalse(source.contains("(Object[])"));
        assertTrue(CompilerUtil.compile("1.8", new InMemoryJavaSourceFileObject(internalClassName, source)));
    }

    @Test
    public void testOuterGenericHelperReceiverKeepsConcreteToArrayTypeInDelegatingCall() throws Exception {
        String internalClassName = OuterHelperDelegatingArrayReceiverCalls.Builder.class.getName().replace('.', '/');
        String source = decompileSuccess(new ClassPathLoader(), new PlainTextPrinter(), internalClassName);

        assertTrue(source.contains("OuterHelperDelegatingArrayReceiverCalls.requireWildcards(wildcards).toArray("));
        assertFalse(source.contains("(List)"));
        assertFalse(source.contains("(Object[])"));
        assertTrue(CompilerUtil.compile("1.8", new InMemoryJavaSourceFileObject(internalClassName, source)));
    }

    @Test
    public void testOuterClassNestedBuilderKeepsConcreteToArrayType() throws Exception {
        String internalClassName = OuterHelperDelegatingArrayReceiverCalls.class.getName().replace('.', '/');
        String source = decompileSuccess(new ClassPathLoader(), new PlainTextPrinter(), internalClassName);

        assertTrue(source.contains("requireWildcards(wildcards).toArray("));
        assertFalse(source.contains("(List)"));
        assertFalse(source.contains("(Object[])"));
        assertTrue(CompilerUtil.compile("1.8", new InMemoryJavaSourceFileObject(internalClassName, source)));
    }

    @Test
    public void testOuterClassNestedBuilderOverloadKeepsConcreteToArrayType() throws Exception {
        String internalClassName = OuterRequireNonNullBuilderOverloadCalls.class.getName().replace('.', '/');
        String source = decompileSuccess(new ClassPathLoader(), new PlainTextPrinter(), internalClassName);

        assertTrue(source.contains("requireWildcards(wildcards).toArray("));
        assertFalse(source.contains("(List)"));
        assertFalse(source.contains("(Object[])"));
        assertTrue(CompilerUtil.compile("1.8", new InMemoryJavaSourceFileObject(internalClassName, source)));
    }

    @SuppressWarnings("unused")
    static final class GenericBoundReturnCalls {
        static class Constant {
        }

        @SuppressWarnings("unchecked")
        static <T extends Constant> T getConstant(int index) {
            return (T) getConstant(index, Constant.class);
        }

        static <T extends Constant> T getConstant(int index, Class<T> castTo) {
            return null;
        }
    }

    @SuppressWarnings("unused")
    interface BaseStreamLike<T, B extends BaseStreamLike<T, B>> {
    }

    @SuppressWarnings("unused")
    interface SelfTypedStreams<T, S extends SelfTypedStreams<T, S, B>, B extends BaseStreamLike<T, B>> {
        @SuppressWarnings("unchecked")
        default BaseStreamLike<T, B> asBaseStream() {
            return new UncheckedIOBaseStream<>((S) this);
        }
    }

    @SuppressWarnings("unused")
    static final class UncheckedIOBaseStream<T, S extends SelfTypedStreams<T, S, B>, B extends BaseStreamLike<T, B>>
            implements BaseStreamLike<T, B> {
        UncheckedIOBaseStream(S delegate) {
        }
    }

    @SuppressWarnings("unused")
    interface FileVisitorLike<P> {
    }

    @SuppressWarnings("unused")
    static final class VisitorCalls {
        static final class PathCounters {
        }

        static final class CountingPathVisitor implements FileVisitorLike<String> {
            static CountingPathVisitor withLongCounters() {
                return new CountingPathVisitor();
            }

            PathCounters getPathCounters() {
                return new PathCounters();
            }
        }

        static <T extends FileVisitorLike<String>> T visitFileTree(T visitor, String directory) {
            return visitor;
        }

        static PathCounters countDirectory(String directory) {
            return visitFileTree(CountingPathVisitor.withLongCounters(), directory).getPathCounters();
        }
    }

    @SuppressWarnings("unused")
    static final class NewVisitorCalls {
        static final class PathCounters {
        }

        static final class CountingPathVisitor implements FileVisitorLike<String> {
            PathCounters getPathCounters() {
                return new PathCounters();
            }
        }

        static <T extends FileVisitorLike<String>> T visitFileTree(T visitor, String directory) {
            return visitor;
        }

        static PathCounters countDirectory(String directory) {
            return visitFileTree(new CountingPathVisitor(), directory).getPathCounters();
        }
    }

    @SuppressWarnings("unused")
    static final class JdkFileVisitorCalls {
        static final class CountingPathVisitor extends SimpleFileVisitor<Path> {
            int getPathCounters() {
                return 1;
            }
        }

        static <T extends FileVisitor<? super Path>> T visitFileTree(T visitor, Path directory) {
            return visitor;
        }

        static <T extends FileVisitor<? super Path>> T visitFileTree(T visitor, URI directory) {
            return visitor;
        }

        static int countDirectory(Path directory) {
            return visitFileTree(new CountingPathVisitor(), directory).getPathCounters();
        }
    }

    @SuppressWarnings("unused")
    interface OverloadedFileVisitorLike<P> {
    }

    @SuppressWarnings("unused")
    static final class OverloadedVisitorCalls {
        static final class PathCounters {
        }

        static final class CountingPathVisitor implements OverloadedFileVisitorLike<String> {
            static CountingPathVisitor withLongCounters() {
                return new CountingPathVisitor();
            }

            PathCounters getPathCounters() {
                return new PathCounters();
            }
        }

        static <T extends OverloadedFileVisitorLike<String>> T visitFileTree(T visitor, String directory) {
            return visitor;
        }

        static <T extends OverloadedFileVisitorLike<String>> T visitFileTree(T visitor, CharSequence directory) {
            return visitor;
        }

        static PathCounters countDirectory(String directory) {
            return visitFileTree(CountingPathVisitor.withLongCounters(), directory).getPathCounters();
        }
    }

    @SuppressWarnings("unused")
    static final class FunctionCalls {
        @FunctionalInterface
        interface IOBiFunction<T, U, R> {
            R apply(T left, U right);
        }

        @FunctionalInterface
        interface IOBinaryOperator<T> extends IOBiFunction<T, T, T> {
        }

        static final class EraseLike {
            static <T, U, R> R apply(IOBiFunction<? super T, ? super U, ? extends R> mapper, T left, U right) {
                return mapper.apply(left, right);
            }
        }

        static <T> T reduce(T value, IOBinaryOperator<T> accumulator) {
            return EraseLike.apply(accumulator, value, value);
        }
    }

    @SuppressWarnings("unused")
    static final class ArrayHelperCalls {
        @FunctionalInterface
        interface IOTriFunction<T, U, V, R> {
            R apply(T left, U middle, V right);
        }

        static final class UncheckLike {
            static <T, U, V, R> R apply(IOTriFunction<? super T, ? super U, ? super V, ? extends R> mapper, T left, U middle, V right) {
                return mapper.apply(left, middle, right);
            }
        }

        static int read(char[] cbuf, int off, int len) {
            return UncheckLike.apply(ArrayHelperCalls::read0, cbuf, off, len);
        }

        static int read0(char[] cbuf, int off, int len) {
            return len;
        }
    }

    @SuppressWarnings("unused")
    interface StreamBaseLike<T, S extends StreamBaseLike<T, S, B>, B extends java.util.stream.BaseStream<T, B>> {
        B unwrap();
    }

    @SuppressWarnings("unused")
    interface StreamHelperCalls<T> extends StreamBaseLike<T, StreamHelperCalls<T>, Stream<T>> {
        @FunctionalInterface
        interface IOPredicate<T> {
            boolean test(T value);
        }

        final class Erase {
            static <T> boolean test(IOPredicate<? super T> predicate, T value) {
                return predicate.test(value);
            }
        }
        default boolean allMatch(IOPredicate<? super T> predicate) {
            return unwrap().allMatch(t -> Erase.test(predicate, t));
        }
    }

    @SuppressWarnings("unused")
    static final class ArrayReceiverCalls {
        static final String[] EMPTY_STRING_ARRAY = new String[0];

        private static <T> T requireWildcards(T wildcards) {
            return wildcards;
        }

        static String[] adapt(List<String> wildcards) {
            return requireWildcards(wildcards).toArray(EMPTY_STRING_ARRAY);
        }
    }

    @SuppressWarnings("unused")
    static final class RequireNonNullArrayReceiverCalls {
        static final String[] EMPTY_STRING_ARRAY = new String[0];

        static <T> T requireWildcards(T wildcards) {
            return Objects.requireNonNull(wildcards, "wildcards");
        }

        static String[] adapt(List<String> wildcards) {
            return requireWildcards(wildcards).toArray(EMPTY_STRING_ARRAY);
        }
    }

    @SuppressWarnings("unused")
    static final class DelegatingArrayReceiverCalls {
        static final String[] EMPTY_STRING_ARRAY = new String[0];

        static <T> T requireWildcards(T wildcards) {
            return Objects.requireNonNull(wildcards, "wildcards");
        }

        void setWildcards(String... wildcards) {
        }

        void adapt(List<String> wildcards) {
            setWildcards(requireWildcards(wildcards).toArray(EMPTY_STRING_ARRAY));
        }
    }

    @SuppressWarnings("unused")
    static final class NestedDelegatingArrayReceiverCalls {
        interface ArrayHolder {
            String[] EMPTY_STRING_ARRAY = new String[0];
        }

        static <T> T requireWildcards(T wildcards) {
            return Objects.requireNonNull(wildcards, "wildcards");
        }

        static final class Builder {
            void setWildcards(String... wildcards) {
            }

            void adapt(List<String> wildcards) {
                setWildcards(NestedDelegatingArrayReceiverCalls.requireWildcards(wildcards).toArray(ArrayHolder.EMPTY_STRING_ARRAY));
            }
        }
    }

    @SuppressWarnings("unused")
    static final class OuterHelperDelegatingArrayReceiverCalls {
        static <T> T requireWildcards(T wildcards) {
            return wildcards;
        }

        static final class Builder {
            static final String[] EMPTY_STRING_ARRAY = new String[0];

            void setWildcards(String... wildcards) {
            }

            void adapt(List<String> wildcards) {
                setWildcards(OuterHelperDelegatingArrayReceiverCalls.requireWildcards(wildcards).toArray(EMPTY_STRING_ARRAY));
            }
        }
    }

    @SuppressWarnings("unused")
    static final class OuterRequireNonNullBuilderOverloadCalls {
        interface IOFileFilterLike {
            String[] EMPTY_STRING_ARRAY = new String[0];
        }

        static <T> T requireWildcards(T wildcards) {
            return Objects.requireNonNull(wildcards, "wildcards");
        }

        static final class Builder {
            private String[] wildcards;

            Builder setWildcards(List<String> wildcards) {
                setWildcards(OuterRequireNonNullBuilderOverloadCalls.requireWildcards(wildcards).toArray(IOFileFilterLike.EMPTY_STRING_ARRAY));
                return this;
            }

            Builder setWildcards(String... wildcards) {
                this.wildcards = OuterRequireNonNullBuilderOverloadCalls.requireWildcards(wildcards);
                return this;
            }
        }
    }
}
