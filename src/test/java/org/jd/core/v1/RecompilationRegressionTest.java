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
import org.jd.core.v1.stub.AmbiguousMethodReference;
import org.jd.core.v1.stub.ConflictingWildcardCapture;
import org.jd.core.v1.stub.DiamondWithFunctionalArguments;
import org.jd.core.v1.stub.ErasedMethodReference;
import org.jd.core.v1.stub.ExceptionBoundViolation;
import org.jd.core.v1.stub.ExceptionWitness;
import org.jd.core.v1.stub.ForwardStaticReference;
import org.jd.core.v1.stub.MultiWitness;
import org.jd.core.v1.stub.NullArgumentWitness;
import org.jd.core.v1.stub.RawDeclaredOverloads;
import org.jd.core.v1.stub.RawWildcardConstructor;
import org.jd.core.v1.stub.SneakyThrow;
import org.jd.core.v1.stub.WildcardExtendsBound;
import org.jd.core.v1.stub.WildcardExtendsThrows;
import org.jd.core.v1.stub.WildcardUnboundedVariable;
import org.jd.core.v1.stub.WildcardObjectBound;
import org.jd.core.v1.stub.RawDeclaredMethodReference;
import org.jd.core.v1.stub.ReturnOnlyTypeVariable;
import org.jd.core.v1.stub.SelfOverloadBoxing;
import org.jd.core.v1.stub.SuperOverload;
import org.jd.core.v1.stub.WildcardCapture;
import org.jd.core.v1.stub.WildcardCaptureBound;
import org.junit.Test;

public class RecompilationRegressionTest extends AbstractJdTest {

    private String decompile(Class<?> clazz) throws Exception {
        return decompileSuccess(new ClassPathLoader(), new PlainTextPrinter(), clazz.getName().replace('.', '/'));
    }

    private void assertRecompiles(Class<?> clazz, String source) throws Exception {
        assertTrue(CompilerUtil.compile("17", new InMemoryJavaSourceFileObject(clazz.getName().replace('.', '/'), source)));
    }

    @Test
    public void testAmbiguousMethodReferenceCast() throws Exception {
        String source = decompile(AmbiguousMethodReference.class);
        assertTrue(source.matches(PatternMaker.make("return find((Predicate<Thread>)predicate::test);")));
        assertRecompiles(AmbiguousMethodReference.class, source);
    }

    @Test
    public void testRawDeclaredMethodReferenceCast() throws Exception {
        String source = decompile(RawDeclaredMethodReference.class);
        assertTrue(source.matches(PatternMaker.make("use((F1<List>)this::foo);")));
        assertRecompiles(RawDeclaredMethodReference.class, source);
    }

    @Test
    public void testRawConstructorInWildcardContext() throws Exception {
        String source = decompile(RawWildcardConstructor.class);
        assertRecompiles(RawWildcardConstructor.class, source);
    }

    @Test
    public void testErasedMethodReferenceWithoutCast() throws Exception {
        String source = decompile(ErasedMethodReference.class);
        assertTrue(source.matches(PatternMaker.make("Uncheck.accept(this.delegate::forEachRemaining, action::accept);")));
        assertRecompiles(ErasedMethodReference.class, source);
    }

    @Test
    public void testConflictingWildcardCaptureKeepsCast() throws Exception {
        String source = decompile(ConflictingWildcardCapture.class);
        assertTrue(source.matches(PatternMaker.make("m((Holder)x, y);")));
        assertRecompiles(ConflictingWildcardCapture.class, source);
    }

    @Test
    public void testWildcardCaptureWithoutCast() throws Exception {
        String source = decompile(WildcardCapture.class);
        assertTrue(source.matches(PatternMaker.make("return app(f, \"x\");")));
        assertRecompiles(WildcardCapture.class, source);
    }

    @Test
    public void testWildcardCaptureBoundRequiresCast() throws Exception {
        String source = decompile(WildcardCaptureBound.class);
        assertTrue(source.matches(PatternMaker.make("return instantiate(name, (Class)pluginType.getPluginClass());")));
        assertRecompiles(WildcardCaptureBound.class, source);
    }

    @Test
    public void testReturnOnlyTypeVariableKeepsWitness() throws Exception {
        String source = decompile(ReturnOnlyTypeVariable.class);
        assertTrue(source.matches(PatternMaker.make("this.map.<List>getValueAt(i)")));
        assertRecompiles(ReturnOnlyTypeVariable.class, source);
    }

    @Test
    public void testExceptionWitnessBinding() throws Exception {
        String source = decompile(ExceptionWitness.class);
        assertTrue(source.matches(PatternMaker.make("return of((FailableConsumer<Instant, E>)start -> runnable.run());")));
        assertRecompiles(ExceptionWitness.class, source);
    }

    @Test
    public void testExceptionBoundViolationSkipsWitness() throws Exception {
        String source = decompile(ExceptionBoundViolation.class);
        assertFalse(source.matches(PatternMaker.make("<E>multiBound")));
        assertRecompiles(ExceptionBoundViolation.class, source);
    }

    @Test
    public void testSuperOverload() throws Exception {
        String source = decompile(SuperOverload.class);
        assertTrue(source.matches(PatternMaker.make("Greeter.super.greet()")));
        assertTrue(source.matches(PatternMaker.make("return super.fit(element);")));
        assertRecompiles(SuperOverload.class, source);
    }

    @Test
    public void testSelfOverloadBoxing() throws Exception {
        String source = decompile(SelfOverloadBoxing.class);
        assertTrue(source.matches(PatternMaker.make("toString(ch.charValue())")));
        assertTrue(source.matches(PatternMaker.make("return LONG_TO_INT_RANGE.fit(Long.valueOf(millis)).intValue();")));
        assertRecompiles(SelfOverloadBoxing.class, source);
    }

    @Test
    public void testForwardStaticReference() throws Exception {
        String source = decompile(ForwardStaticReference.class);
        assertRecompiles(ForwardStaticReference.class, source);
    }

    @Test
    public void testNullArgumentKeepsWitness() throws Exception {
        String source = decompile(NullArgumentWitness.class);
        assertTrue(source.matches(PatternMaker.make("<List>id(null)")));
        assertRecompiles(NullArgumentWitness.class, source);
    }

    @Test
    public void testSneakyThrow() throws Exception {
        String source = decompile(SneakyThrow.class);
        assertTrue(source.matches(PatternMaker.make("throw (E)t;")));
        assertRecompiles(SneakyThrow.class, source);
    }

    @Test
    public void testRawDeclaredOverloads() throws Exception {
        String source = decompile(RawDeclaredOverloads.class);
        assertTrue(source.matches(PatternMaker.make("use((F1)this::foo);")));
        assertRecompiles(RawDeclaredOverloads.class, source);
    }

    @Test
    public void testWildcardObjectBound() throws Exception {
        String source = decompile(WildcardObjectBound.class);
        assertTrue(source.matches(PatternMaker.make("return first(holder);")));
        assertRecompiles(WildcardObjectBound.class, source);
    }

    @Test
    public void testWildcardExtendsBound() throws Exception {
        String source = decompile(WildcardExtendsBound.class);
        assertTrue(source.matches(PatternMaker.make("return pick(holder);")));
        assertRecompiles(WildcardExtendsBound.class, source);
    }

    @Test
    public void testWildcardExtendsThrows() throws Exception {
        String source = decompile(WildcardExtendsThrows.class);
        assertTrue(source.matches(PatternMaker.make("return app(f, \"x\");")));
        assertRecompiles(WildcardExtendsThrows.class, source);
    }

    @Test
    public void testWildcardUnboundedVariable() throws Exception {
        String source = decompile(WildcardUnboundedVariable.class);
        assertTrue(source.matches(PatternMaker.make("return app(f, \"x\");")));
        assertRecompiles(WildcardUnboundedVariable.class, source);
    }

    @Test
    public void testMultiWitness() throws Exception {
        String source = decompile(MultiWitness.class);
        // The witness is inferable from the return target type, so it may be omitted
        assertTrue(source.matches(PatternMaker.make("pair(null, null)")));
        assertRecompiles(MultiWitness.class, source);
    }

    @Test
    public void testDiamondWithFunctionalArguments() throws Exception {
        String source = decompile(DiamondWithFunctionalArguments.class);
        assertTrue(source.matches(PatternMaker.make("return new SimpleCollector<>(StringBuilder::new, StringBuilder::append, StringBuilder::append, StringBuilder::toString);")));
        assertRecompiles(DiamondWithFunctionalArguments.class, source);
    }
}
