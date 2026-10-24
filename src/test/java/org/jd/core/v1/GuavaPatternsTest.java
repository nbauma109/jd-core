package org.jd.core.v1;

import org.jd.core.v1.compiler.CompilerUtil;
import org.jd.core.v1.compiler.InMemoryJavaSourceFileObject;
import org.jd.core.v1.api.loader.Loader;
import org.jd.core.v1.loader.ClassPathLoader;
import org.jd.core.v1.printer.PlainTextPrinter;
import org.jd.core.v1.stub.GuavaOuter;
import org.jd.core.v1.stub.GuavaPatterns;
import org.jd.core.v1.stub.BoundedBindings;
import org.jd.core.v1.stub.FinallyAroundALoop;
import org.jd.core.v1.stub.GuavaSub;
import org.jd.core.v1.stub.InferenceLimits;
import org.jd.core.v1.stub.InitializerLocals;
import org.jd.core.v1.stub.LocalClassPlacement;
import org.jd.core.v1.stub.LoopUpdates;
import org.jd.core.v1.stub.SynchronizedReturns;
import org.jd.core.v1.stub.TypeAnnotationPatterns;
import org.jd.core.v1.stub.TypeAnnotationRecord;
import org.junit.Test;

public class GuavaPatternsTest extends AbstractJdTest {
    @Test
    public void test() throws Exception {
        String internalClassName = GuavaPatterns.class.getName().replace('.', '/');
        String source = decompileSuccess(new ClassPathLoader(), new PlainTextPrinter(), internalClassName);

        assertEquals(-1, source.indexOf("ErrorThrower<RuntimeException>"));
        assertEquals(-1, source.indexOf("null."));
        assertEquals(-1, source.indexOf("throw null"));
        assertEquals(-1, source.indexOf("Decompilation failed"));
        assertEquals(-1, source.indexOf("new GuavaOuter.Wrapped(outer"));
        assertTrue(source.contains("for (GuavaPatterns.OneWayCollection<String> collection : collections)") || source.contains("for (OneWayCollection<String> collection : collections)"));
        assertTrue(CompilerUtil.compile("17", new InMemoryJavaSourceFileObject(internalClassName, source)));
    }

    @Test
    public void testInnerClassOfTheSuperclass() throws Exception {
        String internalClassName = GuavaSub.class.getName().replace('.', '/');
        String source = decompileSuccess(new ClassPathLoader(), new PlainTextPrinter(), internalClassName);

        assertEquals(-1, source.indexOf("(this,"));
        assertTrue(source.contains("new GuavaOuter.Wrapped(key, value)"));
        assertTrue(source.contains("new GuavaOuter.Marker()"));
    }

    @Test
    public void testTypeAnnotationsOfWholeTypes() throws Exception {
        String internalClassName = TypeAnnotationPatterns.class.getName().replace('.', '/');
        String printed = decompileSuccess(new ClassPathLoader(), new PlainTextPrinter(), internalClassName);
        String source = printed.replaceAll("/\\*[^*]*\\*/", "").replaceAll("\\s+", " ");

        assertTrue(source.contains("class TypeAnnotationPatterns<T extends @Maybe Object>"));
        assertTrue(source.contains("<U extends @Maybe Object> U first(U value)"));
        assertTrue(source.contains("@Maybe T generic;"));
        assertTrue(source.contains("public @Maybe T pick(@Maybe T value)"));
        assertTrue(source.contains("@Maybe Object object;"));
        // an annotation for declarations and types is written once
        assertTrue(source.contains("@Twice Object twice;"));
        assertTrue(source.contains("@Maybe Object several;") && source.contains("@Deprecated"));
        assertTrue(source.contains("<A extends @Maybe Object, B extends @Maybe Object>"));
        assertTrue(source.contains("<S extends @Maybe Object & Serializable> S sturdy(S value)") || source.contains("<S extends @Maybe Object & java.io.Serializable> S sturdy(S value)"));
        assertTrue(source.contains("<C extends @Maybe Closeable> C register(C closeable)") || source.contains("<C extends java.io.@Maybe Closeable> C register(C closeable)"));
        assertTrue(source.contains("Local(@Maybe Object value)"));
        assertTrue(source.contains("InInitializer(@Maybe String first, Object second)"));
        assertTrue(source.contains("OfTheOuterType(@Maybe TypeAnnotationPatterns first, String second)"));
        assertFalse(source.contains("@Twice @Twice"));
        // on every kind of type: a class, a parameterized class, a type variable
        assertTrue(source.contains("(@Maybe String first, int second, @Maybe Object third)"));
        assertTrue(source.contains("public @Maybe String find(@Maybe Object key, int count, String other, @Maybe List<String> values)"));
        assertTrue(source.contains("@Maybe List<String> elements;"));
        // a nested class is annotated after its outer class
        assertTrue(source.contains("Map.@Maybe Entry<String, String> nestedEntry;"));
        assertTrue(source.contains("Map.@Maybe Entry<String, String> entry"));
        // the annotation of the elements of an array is not the one of the array
        assertFalse(source.contains("@Maybe String[] annotatedElementsOfArray"));
        assertFalse(source.contains("@Maybe Object[]"));
        assertTrue(source.contains("String @Maybe [] arrayItself;"));
        assertTrue(source.contains("count(String @Maybe ... arguments)"));
        assertTrue(CompilerUtil.compile("17", new InMemoryJavaSourceFileObject(internalClassName, printed)));
    }

    /** The local class is declared before the block holding its only 'new', because what comes after the block needs it */
    @Test
    public void testLocalClassIsDeclaredBeforeWhatNeedsItAfterTheBlockOfItsOnlyInstance() throws Exception {
        String internalClassName = LocalClassPlacement.class.getName().replace('.', '/');
        String source = decompileSuccess(new ClassPathLoader(), new PlainTextPrinter(), internalClassName).replaceAll("/\\*[^*]*\\*/", "").replaceAll("\\s+", " ");

        // a field of the class in another local class, a bound, the declaration of a 'for' statement, an implements clause, a catch, type arguments, a constructor reference
        assertDeclaredBeforeTheBlock(source, "itemInTheSignatureOfAnotherLocalClass(", "Item");
        assertDeclaredBeforeTheBlock(source, "itemInTheBoundOfAnotherLocalClass(", "Item");
        assertDeclaredBeforeTheBlock(source, "itemInTheDeclarationOfAForStatement(", "Item");
        assertDeclaredBeforeTheBlock(source, "mentionedEverywhere(", "Marker");
        assertDeclaredBeforeTheBlock(source, "itemInTheTypeArgumentOfAnAnonymousClass(", "Item");
        assertDeclaredBeforeTheBlock(source, "itemInTheResourceOfATry(", "Item");
    }

    private static void assertDeclaredBeforeTheBlock(String source, String method, String className) {
        String body = source.substring(source.indexOf(method));

        assertTrue(body, body.indexOf("class " + className) != -1 && body.indexOf("class " + className) < body.indexOf("if (flag)"));
    }

    @Test
    public void testFieldOfTheSuperclassHiddenByAField() throws Exception {
        String internalClassName = GuavaPatterns.class.getName().replace('.', '/') + "$HiddenFieldDerived";
        String source = decompileSuccess(new ClassPathLoader(), new PlainTextPrinter(), internalClassName).replaceAll("/\\*[^*]*\\*/", "").replaceAll("\\s+", " ");

        assertTrue(source, source.contains("super.count"));
    }

    @Test
    public void testOuterInstanceOfAnInnerClassWhichCannotBeRead() throws Exception {
        String wrapped = (GuavaOuter.class.getName() + "$Wrapped").replace('.', '/');
        ClassPathLoader delegate = new ClassPathLoader();
        Loader loader = new Loader() {
            @Override
            public boolean canLoad(String internalName) {
                return delegate.canLoad(internalName);
            }

            @Override
            public byte[] load(String internalName) throws java.io.IOException {
                // the class file of the inner class is not a class file: nothing tells that it is an inner class
                return internalName.equals(wrapped) ? new byte[] {1, 2, 3} : delegate.load(internalName);
            }
        };
        String source = decompileSuccess(loader, new PlainTextPrinter(), GuavaSub.class.getName().replace('.', '/')).replaceAll("/\\*[^*]*\\*/", "").replaceAll("\\s+", " ");

        // the outer instance is written once, as the qualifier, or not at all
        assertFalse(source, source.contains(".new Wrapped(this,") || source.contains(".new GuavaOuter.Wrapped(this,"));
    }

    @Test
    public void testFieldTwoLevelsUpHiddenByTheFieldsBetween() throws Exception {
        String internalClassName = GuavaPatterns.class.getName().replace('.', '/') + "$FieldC";
        String source = decompileSuccess(new ClassPathLoader(), new PlainTextPrinter(), internalClassName).replaceAll("/\\*[^*]*\\*/", "").replaceAll("\\s+", " ");

        assertFalse(source, source.contains("super.x"));
        assertTrue(source, source.contains("(FieldA)this).x") || source.contains("(GuavaPatterns.FieldA)this).x"));
    }

    @Test
    public void testUpdateOfALoopWhoseInnerLoopEndsWithABreak() throws Exception {
        String internalClassName = LoopUpdates.class.getName().replace('.', '/');
        String source = decompileSuccess(new ClassPathLoader(), new PlainTextPrinter(), internalClassName).replaceAll("/\\*[^*]*\\*/", "").replaceAll("\\s+", " ");

        // each outer 'for' keeps its update, or its loop would never end
        assertFalse(source, source.contains("; )"));
        assertTrue(source, source.contains("++)"));
        // the position and the spread of CharMatcher.removeFrom: 'spread++' follows the inner loop, which the outer one goes on with
        String method = source.substring(source.indexOf("removeFrom("));
        assertTrue(method, method.indexOf("spread++") > method.indexOf("matcher.test"));
        assertFalse(method, method.contains("spread++; continue;"));
        assertEquals(method, 1, method.split("return new String", -1).length - 1);
    }

    @Test
    public void testSynchronizedBlockWhichReturnsInATryWithAFinally() throws Exception {
        String internalClassName = SynchronizedReturns.class.getName().replace('.', '/');
        String printed = decompileSuccess(new ClassPathLoader(), new PlainTextPrinter(), internalClassName);
        String source = printed.replaceAll("/\\*[^*]*\\*/", "").replaceAll("\\s+", " ");

        // the block is not taken for the copy of the 'finally' and removed with it
        assertTrue(source, source.contains("try { synchronized ("));
        assertEquals(source, 2, source.split("return load\\(", -1).length - 1);
        // the 'finally' which is a synchronized block is written once
        assertEquals(source, 1, source.split("misses--", -1).length - 1);
        assertTrue(source, source.contains("emptyUnderALock"));
        assertTrue(CompilerUtil.compile("17", new InMemoryJavaSourceFileObject(internalClassName, printed)));
    }

    @Test
    public void testTypeVariablesOutsideTheirBoundsAreBoundToTheirBounds() throws Exception {
        String internalClassName = BoundedBindings.class.getName().replace('.', '/');
        String printed = decompileSuccess(new ClassPathLoader(), new PlainTextPrinter(), internalClassName);

        assertEquals(printed, -1, printed.indexOf("throw null"));
        assertTrue(printed, CompilerUtil.compile("17", new InMemoryJavaSourceFileObject(internalClassName, printed)));
    }

    @Test
    public void testCallsWhoseTypeArgumentsTheBytecodeDoesNotKeep() throws Exception {
        String internalClassName = InferenceLimits.class.getName().replace('.', '/');
        String printed = decompileSuccess(new ClassPathLoader(), new PlainTextPrinter(), internalClassName);
        String source = printed.replaceAll("/\\*[^*]*\\*/", "").replaceAll("\\s+", " ");

        assertTrue(source, source.contains("((Value)values.get(\"key\")).answer()") || source.contains("((Value) values.get(\"key\")).answer()"));
        assertTrue(source, source.contains("((Value)pair.pick(value)).answer()") || source.contains("((Value) pair.pick(value)).answer()") || source.contains("pair.<Value>pick(value).answer()"));
        assertFalse(source, source.contains("::<String>create"));
        assertFalse(source, source.contains("<String>create"));
        assertFalse(source, source.contains("(List)box.lists()") || source.contains("(List) box.lists()"));
        assertFalse(source, source.contains("(Bounded<Number>)"));
        assertFalse(source, source.contains("first((List)") || source.contains("first((List) "));
        assertFalse(source, source.contains("(List strings)") || source.contains("(List xs)"));
        assertTrue(source, source.contains("applyFn(strings ->"));
        assertTrue(source, source.contains("applyListFn(strings ->"));
        assertTrue(printed, CompilerUtil.compile("17", new InMemoryJavaSourceFileObject(internalClassName, printed)));
    }

    @Test
    public void testLocalClassOfAnInitializerUsedOnlyByALambda() throws Exception {
        String internalClassName = InitializerLocals.class.getName().replace('.', '/');
        String printed = decompileSuccess(new ClassPathLoader(), new PlainTextPrinter(), internalClassName);

        // the lambda is written after the initializer which holds the class: it is the initializer which declares it
        assertTrue(printed, printed.replaceAll("\\s+", " ").contains("class OfTheStaticInitializer"));
        assertTrue(printed, CompilerUtil.compile("17", new InMemoryJavaSourceFileObject(internalClassName, printed)));
    }

    @Test
    public void testFinallyAroundALoopWhoseCatchSetsTheFlagOfTheFinally() throws Exception {
        String internalClassName = FinallyAroundALoop.class.getName().replace('.', '/');
        String printed = decompileSuccess(new ClassPathLoader(), new PlainTextPrinter(), internalClassName);
        String source = printed.replaceAll("/\\*[^*]*\\*/", "").replaceAll("\\s+", " ");

        // the flag set by the 'catch' is the one the 'finally' tests: the 'finally' is the one of the loop, not of one of its iterations
        assertTrue(source, source.contains("try { while (true) { try { return future.get(); } catch (InterruptedException e) { interrupted = true; } } } finally {"));
        // the 'finally' of the inner 'try' keeps what it does when the 'finally' of the outer one is copied after it
        assertTrue(source, source.contains("finally { if (!satisfied) lock.unlock(); }") || source.contains("finally { if (!satisfied) { lock.unlock(); } }"));
        assertTrue(printed, CompilerUtil.compile("17", new InMemoryJavaSourceFileObject(internalClassName, printed)));
    }

    @Test
    public void testTypeAnnotationOfARecordComponent() throws Exception {
        String internalClassName = TypeAnnotationRecord.class.getName().replace('.', '/');
        String source = decompileSuccess(new ClassPathLoader(), new PlainTextPrinter(), internalClassName).replaceAll("/\\*[^*]*\\*/", "").replaceAll("\\s+", " ");

        assertTrue(source, source.contains("@Maybe Object value"));
    }
}
