/*
 * Copyright (c) 2026 Nicolas Baumann (@nbauma109), with Claude Sonnet 5.5.
 * This project is distributed under the GPLv3 license.
 * This is a Copyleft license that gives the user the right to use,
 * copy and modify the code freely for non-commercial purposes.
 */

package org.jd.core.v1;

import org.jd.core.v1.api.loader.Loader;
import org.jd.core.v1.compiler.CompilerUtil;
import org.jd.core.v1.compiler.InMemoryJavaSourceFileObject;
import org.jd.core.v1.model.javasyntax.expression.Expression;
import org.jd.core.v1.model.javasyntax.statement.Statements;
import org.jd.core.v1.model.javasyntax.type.ObjectType;
import org.jd.core.v1.printer.PlainTextPrinter;
import org.jd.core.v1.service.converter.classfiletojavasyntax.model.javasyntax.expression.ClassFileMethodReferenceLambdaExpression;
import org.junit.Test;

import javax.tools.JavaCompiler;
import javax.tools.SimpleJavaFileObject;
import javax.tools.ToolProvider;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * Before the nests of Java 11, javac compiles a method reference to a private member of another class into a synthetic lambda
 * 'lambda$m$0(Bag rec$, Object x$0) { Bag.access$000(rec$, x$0); }': the decompiler turns it back into the method reference.
 */
public class RestoreMethodReferenceTest extends AbstractJdTest {

    private static final String INTERNAL_CLASS_NAME = "restore/Fixture";

    private static final String SOURCE = String.join("\n",
            "package restore;",
            "import java.util.List;",
            "import java.util.function.BiConsumer;",
            "import java.util.function.Consumer;",
            "public class Fixture extends restore.base.Base {",
            "    private static class Bag {",
            "        private Object add(Object o) { return o; }",
            "        private static void remember(Object o) { }",
            "        private void two(Object a, Object b) { }",
            "        private void run() { }",
            "        private void addAll(Object... items) { }",
            "        private static void all(Object... items) { }",
            "        private void two(Object a, Object b, Object c) { }",
            "        private static Bag make() { return new Bag(); }",
            "    }",
            "    void bound(List<Object> list) {",
            "        Bag bag = new Bag();",
            "        list.forEach(bag::add);",
            "    }",
            "    void staticMethod(List<Object> list) {",
            "        list.forEach(Bag::remember);",
            "    }",
            "    void receiverIsACall(List<Object> list) {",
            "        list.forEach(Bag.make()::add);",
            "    }",
            "    long returnsAValue(List<Object> list) {",
            "        Bag bag = new Bag();",
            "        return list.stream().map(bag::add).count();",
            "    }",
            "    void withoutArgument() {",
            "        Runnable runnable = Bag.make()::run;",
            "        runnable.run();",
            "    }",
            "    Consumer<Object[]> varargs() {",
            "        return Bag.make()::addAll;",
            "    }",
            "    Consumer<Object> lambdaCapturingAVariableNamedLikeJavac(Bag rec$) {",
            "        return o -> rec$.add(o);",
            "    }",
            "    Consumer<Object> lambdaWithTwoStatementsCapturingAVariableNamedLikeJavac(Bag rec$) {",
            "        return o -> { rec$.add(o); rec$.add(o); };",
            "    }",
            "    java.util.function.Supplier<Object> lambdaWhichIsNoCallCapturingAVariableNamedLikeJavac(Bag rec$) {",
            "        return () -> rec$;",
            "    }",
            "    Consumer<Object> lambdaWhichCallsWithMoreThanItsParameters(Bag rec$) {",
            "        return o -> rec$.two(o, o, o);",
            "    }",
            "    Consumer<Object> lambdaWhichCallsWithAConstant(Bag rec$) {",
            "        return o -> rec$.add(1);",
            "    }",
            "    void lambdaOfTheSourceWhichCallsWithAnExpression(List<Object> list) {",
            "        list.forEach(x$0 -> Bag.remember(x$0.toString()));",
            "    }",
            "    void lambdaOfTheSourceWhichDropsAnArgument(BiConsumer<Object, Object> consumer) {",
            "        BiConsumer<Object, Object> dropping = (x$0, x$1) -> Bag.remember(x$0);",
            "        dropping.andThen(consumer).accept(1, 2);",
            "    }",
            "    void lambdaOfTheSourceWithSeveralStatementsNamedLikeJavac(List<Object> list) {",
            "        list.forEach(x$0 -> { Bag.remember(x$0); Bag.remember(x$0); });",
            "    }",
            "    Consumer<Object[]> lambdaOfTheSourceWhichCallsAVarargsMethod() {",
            "        return x$0 -> Bag.all(x$0);",
            "    }",
            "    Runnable lambdaOfTheSourceWithoutParameter() {",
            "        return () -> Bag.make();",
            "    }",
            "    void greet(Object o) { }",
            "    void protectedMethodOfAnotherPackage(List<Object> list, Fixture other) {",
            "        list.forEach(other::protectedMethod);",
            "    }",
            "    Runnable protectedMethodWithoutArgument(Fixture other) {",
            "        return other::protectedRun;",
            "    }",
            "    void lambdaOfTheSourceWhichCallsThis(List<Object> list) {",
            "        list.forEach(x$0 -> this.greet(x$0));",
            "    }",
            "    void lambdaOfTheSourceNamedLikeJavac(List<Object> list) {",
            "        list.forEach(x$0 -> Bag.make().add(x$0));",
            "    }",
            "    void lambdaOfTheSource(List<Object> list) {",
            "        Bag bag = new Bag();",
            "        list.forEach(o -> bag.add(o));",
            "    }",
            "    void lambdaWhichSwapsItsArguments(Bag bag) {",
            "        BiConsumer<Object, Object> consumer = (first, second) -> bag.two(second, first);",
            "        consumer.accept(1, 2);",
            "    }",
            "    void lambdaWithSeveralStatements(List<Object> list) {",
            "        Bag bag = new Bag();",
            "        list.forEach(o -> { bag.add(o); bag.add(o); });",
            "    }",
            "}");

    /** A member which javac cannot reach from the call site, as the private ones of another class: the call is direct in the lambda, as 'rec$.protectedMethod(x$0)' */
    private static final String BASE_SOURCE = String.join("\n",
            "package restore.base;",
            "public class Base {",
            "    protected void protectedMethod(Object o) { }",
            "    protected void protectedRun() { }",
            "}");

    /** Without the table of the local variables, the variables which a lambda captures have no name when the lambda is made */
    private static final String SOURCE_WITHOUT_DEBUG_INFORMATION = String.join("\n",
            "package restore;",
            "import java.util.List;",
            "public class WithoutDebugInformation {",
            "    void print(List<String> list, String prefix) {",
            "        String suffix = prefix.trim();",
            "        list.forEach(s -> System.out.println(prefix + s + suffix));",
            "    }",
            "}");

    @Test
    public void testLambdaCapturingVariablesWithoutName() throws Exception {
        String internalClassName = "restore/WithoutDebugInformation";
        String source = decompileSuccess(compileForJava8("-g:none", Map.of(internalClassName, SOURCE_WITHOUT_DEBUG_INFORMATION)), new PlainTextPrinter(), internalClassName);

        assertTrue(CompilerUtil.compile("17", new InMemoryJavaSourceFileObject(internalClassName, source)));
    }

    @Test
    public void testMarkOfTheLambdaIsKeptByCopy() {
        ClassFileMethodReferenceLambdaExpression lambda = new ClassFileMethodReferenceLambdaExpression(1, ObjectType.TYPE_OBJECT, ObjectType.TYPE_OBJECT, List.of("x$0"), new Statements());
        lambda.setParameterTypes(List.of(ObjectType.TYPE_OBJECT));
        lambda.setExplicitParameterTypes(true);

        Expression copy = lambda.copyTo(2);

        assertTrue(copy instanceof ClassFileMethodReferenceLambdaExpression);
        assertEquals(2, copy.getLineNumber());
        assertEquals(List.of("x$0"), ((ClassFileMethodReferenceLambdaExpression) copy).getParameterNames());
        assertEquals(List.of(ObjectType.TYPE_OBJECT), ((ClassFileMethodReferenceLambdaExpression) copy).getParameterTypes());
        assertTrue(((ClassFileMethodReferenceLambdaExpression) copy).isExplicitParameterTypes());
    }

    @Test
    public void testMethodReferencesCompiledToLambdas() throws Exception {
        String source = decompileSuccess(compileForJava8("-g", Map.of(INTERNAL_CLASS_NAME, SOURCE, "restore/base/Base", BASE_SOURCE)), new PlainTextPrinter(), INTERNAL_CLASS_NAME);

        // The synthetic parameters of javac are gone, and the variable which holds the receiver is folded in the method reference
        // (the 'rec$' left are the ones of the variables of the source)
        assertEquals(-1, source.indexOf("rec$::"));
        assertTrue(source, source.contains("list.forEach(bag::add);"));
        assertTrue(source, source.contains("list.forEach(Bag::remember);"));
        assertTrue(source, source.contains("list.forEach(Bag.make()::add);"));
        assertTrue(source, source.contains("list.stream().map(bag::add).count()"));
        // The receiver is made once, where the method reference is, even when there is no argument to tell the lambda from one of the source
        assertTrue(source, source.contains("Runnable runnable = Bag.make()::run;"));
        // ... and for a method with a variable number of arguments, which the lambda receives as an array
        assertTrue(source, source.contains("return Bag.make()::addAll;"));
        // The lambdas of the source keep their own parameters and bodies, whatever the names of the parameters: 'Bag.make()' is made at each call
        assertTrue(source, source.contains("x$0 -> Bag.make().add(x$0)"));
        // 'rec$' is a legal name of a variable: javac does not null-check it, as it does the receiver of a method reference
        assertTrue(source, source.contains("o -> rec$.add(o)"));
        // ... or one of the lambdas which are not a call with the parameters of the lambda, and only them
        assertTrue(source, source.contains("rec$.add(Integer.valueOf(1))"));
        assertTrue(source, source.contains("rec$.two(o, o, o)"));
        assertTrue(source, source.contains("() -> Bag.make()"));
        assertTrue(source, source.contains("x$0 -> Bag.remember(x$0.toString())"));
        assertTrue(source, source.contains("Bag.remember(x$0)"));
        assertTrue(source, source.contains("x$0 -> Bag.all(x$0)"));
        // 'this' has no receiver to evaluate once either: the lambda is the method reference
        assertTrue(source, source.contains("list.forEach(this::greet);"));
        // The call is direct in the lambda when the member can be called, as the protected members of a class of another package
        assertTrue(source, source.contains("list.forEach(other::protectedMethod);"));
        assertTrue(source, source.contains("return other::protectedRun;"));
        assertTrue(source, source.contains("o -> bag.add(o)"));
        assertTrue(source, source.contains("(first, second) -> bag.two(second, first)"));
        assertTrue(source, source.contains("bag.add(o);"));

        assertTrue(CompilerUtil.compile("17", new InMemoryJavaSourceFileObject(INTERNAL_CLASS_NAME, source), new InMemoryJavaSourceFileObject("restore/base/Base", BASE_SOURCE)));
    }

    /** The class files of the JDK's javac for Java 8: the compiler of the other tests may be the one of Eclipse, which names the synthetic members differently */
    static Loader compileForJava8(String debugOption, Map<String, String> sources) throws IOException {
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        Path directory = Files.createTempDirectory("restore-method-reference");
        List<SimpleJavaFileObject> sourceFiles = new ArrayList<>();

        sources.forEach((internalClassName, source) -> sourceFiles.add(new SimpleJavaFileObject(URI.create("string:///" + internalClassName + ".java"), SimpleJavaFileObject.Kind.SOURCE) {
            @Override
            public CharSequence getCharContent(boolean ignoreEncodingErrors) {
                return source;
            }
        }));
        List<String> options = Arrays.asList("--release", "8", debugOption, "-proc:none", "-Xlint:-options", "-d", directory.toString());

        assertTrue(compiler.getTask(null, null, null, options, null, sourceFiles).call());

        return new Loader() {
            @Override
            public boolean canLoad(String internalName) {
                return Files.exists(directory.resolve(internalName + ".class"));
            }

            @Override
            public byte[] load(String internalName) throws IOException {
                return Files.readAllBytes(directory.resolve(internalName + ".class"));
            }
        };
    }
}
