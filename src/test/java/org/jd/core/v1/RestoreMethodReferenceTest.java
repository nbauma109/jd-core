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
import org.jd.core.v1.printer.PlainTextPrinter;
import org.junit.Test;

import javax.tools.JavaCompiler;
import javax.tools.SimpleJavaFileObject;
import javax.tools.ToolProvider;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;

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
            "public class Fixture {",
            "    private static class Bag {",
            "        private Object add(Object o) { return o; }",
            "        private static void remember(Object o) { }",
            "        private void two(Object a, Object b) { }",
            "        private void run() { }",
            "        private void addAll(Object... items) { }",
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

    @Test
    public void testMethodReferencesCompiledToLambdas() throws Exception {
        String source = decompileSuccess(compileForJava8(), new PlainTextPrinter(), INTERNAL_CLASS_NAME);

        // The synthetic parameters of javac are gone, and the variable which holds the receiver is folded in the method reference
        // (the only 'rec$' left is the variable of the source: the parameter, and its use)
        assertEquals(2, source.split("rec\\$", -1).length - 1);
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
        assertTrue(source, source.contains("o -> bag.add(o)"));
        assertTrue(source, source.contains("(first, second) -> bag.two(second, first)"));
        assertTrue(source, source.contains("bag.add(o);"));

        assertTrue(CompilerUtil.compile("17", new InMemoryJavaSourceFileObject(INTERNAL_CLASS_NAME, source)));
    }

    /** The class files of the JDK's javac for Java 8: the compiler of the other tests may be the one of Eclipse, which names the synthetic members differently */
    private static Loader compileForJava8() throws IOException {
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        Path directory = Files.createTempDirectory("restore-method-reference");
        SimpleJavaFileObject sourceFile = new SimpleJavaFileObject(URI.create("string:///" + INTERNAL_CLASS_NAME + ".java"), SimpleJavaFileObject.Kind.SOURCE) {
            @Override
            public CharSequence getCharContent(boolean ignoreEncodingErrors) {
                return SOURCE;
            }
        };
        List<String> options = Arrays.asList("--release", "8", "-g", "-proc:none", "-Xlint:-options", "-d", directory.toString());

        assertTrue(compiler.getTask(null, null, null, options, null, List.of(sourceFile)).call());

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
