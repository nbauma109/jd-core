package org.jd.core.v1;

import org.jd.core.v1.compiler.CompilerUtil;
import org.jd.core.v1.compiler.InMemoryJavaSourceFileObject;
import org.jd.core.v1.api.loader.Loader;
import org.jd.core.v1.loader.ClassPathLoader;
import org.jd.core.v1.printer.PlainTextPrinter;
import org.jd.core.v1.stub.GuavaOuter;
import org.jd.core.v1.stub.GuavaPatterns;
import org.jd.core.v1.stub.GuavaSub;
import org.jd.core.v1.stub.LocalClassPlacement;
import org.jd.core.v1.stub.LoopUpdates;
import org.jd.core.v1.stub.SynchronizedReturns;
import org.jd.core.v1.stub.TypeAnnotationPatterns;
import org.junit.Test;

public class GuavaPatternsTest extends AbstractJdTest {
    @Test
    public void test() throws Exception {
        String internalClassName = GuavaPatterns.class.getName().replace('.', '/');
        String source = decompileSuccess(new ClassPathLoader(), new PlainTextPrinter(), internalClassName);

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
        assertTrue(source.contains("@Maybe public T pick(@Maybe T value)"));
        assertTrue(source.contains("@Maybe Object object;"));
        // an annotation for declarations and types is written once
        assertTrue(source.contains("@Twice Object twice;"));
        assertTrue(source.contains("@Maybe Object several;") && source.contains("@Deprecated"));
        assertTrue(source.contains("<A extends @Maybe Object, B extends @Maybe Object>"));
        assertFalse(source.contains("@Twice @Twice"));
        assertTrue(source.contains("(String first, int second, @Maybe Object third)"));
        assertTrue(source.contains("public String find(@Maybe Object key, int count, String other, List<String> values)"));
        // written in front of a declaration, they would not annotate the same type
        assertFalse(source.contains("@Maybe String[] annotatedElementsOfArray"));
        assertFalse(source.contains("@Maybe Map.Entry"));
        assertFalse(source.contains("@Maybe Entry"));
        assertFalse(source.contains("@Maybe Object[]"));
        assertTrue(CompilerUtil.compile("17", new InMemoryJavaSourceFileObject(internalClassName, printed)));
    }

    @Test
    public void testLocalClassUsedInTheSignatureOfAnotherLocalClass() throws Exception {
        String internalClassName = LocalClassPlacement.class.getName().replace('.', '/');
        String source = decompileSuccess(new ClassPathLoader(), new PlainTextPrinter(), internalClassName).replaceAll("/\\*[^*]*\\*/", "").replaceAll("\\s+", " ");

        // Item is declared before the block holding its only 'new Item()', because Holder, declared after the block, has a field of that type
        assertTrue(source, source.indexOf("class Item") != -1 && source.indexOf("class Item") < source.indexOf("if (flag)"));
    }

    @Test
    public void testLocalClassUsedInTheBoundOfAnotherLocalClass() throws Exception {
        String internalClassName = LocalClassPlacement.class.getName().replace('.', '/');
        String source = decompileSuccess(new ClassPathLoader(), new PlainTextPrinter(), internalClassName).replaceAll("/\\*[^*]*\\*/", "").replaceAll("\\s+", " ");
        String method = source.substring(source.indexOf("itemInTheBoundOfAnotherLocalClass("));

        // Item is declared before the block holding its only 'new Item()', because Bounded, declared after the block, has a type parameter bounded by it
        assertTrue(method, method.indexOf("class Item") != -1 && method.indexOf("class Item") < method.indexOf("if (flag)"));
    }

    @Test
    public void testLocalClassesMentionedEverywhere() throws Exception {
        String internalClassName = LocalClassPlacement.class.getName().replace('.', '/');
        String source = decompileSuccess(new ClassPathLoader(), new PlainTextPrinter(), internalClassName).replaceAll("/\\*[^*]*\\*/", "").replaceAll("\\s+", " ");
        String method = source.substring(source.indexOf("mentionedEverywhere("));

        // Marker is used by Tagged, in a catch, in type arguments and in a constructor reference: its declaration stays before the block
        assertTrue(method, method.indexOf("class Marker") != -1 && method.indexOf("class Marker") < method.indexOf("if (flag)"));
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
    }

    @Test
    public void testSynchronizedBlockWhichReturnsInATryWithAFinally() throws Exception {
        String internalClassName = SynchronizedReturns.class.getName().replace('.', '/');
        String printed = decompileSuccess(new ClassPathLoader(), new PlainTextPrinter(), internalClassName);
        String source = printed.replaceAll("/\\*[^*]*\\*/", "").replaceAll("\\s+", " ");

        // the block is not taken for the copy of the 'finally' and removed with it
        assertTrue(source, source.contains("try { synchronized ("));
        assertEquals(source, 2, source.split("return load\\(", -1).length - 1);
        assertTrue(CompilerUtil.compile("17", new InMemoryJavaSourceFileObject(internalClassName, printed)));
    }
}
