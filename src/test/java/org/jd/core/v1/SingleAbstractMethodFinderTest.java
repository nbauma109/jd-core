package org.jd.core.v1;

import org.jd.core.v1.api.loader.Loader;
import org.jd.core.v1.loader.ClassPathLoader;
import org.jd.core.v1.service.converter.classfiletojavasyntax.util.SingleAbstractMethodFinder;
import org.jd.core.v1.stub.GsonFunctionalPatterns;
import org.junit.Test;

import java.util.Set;

import static org.junit.Assert.assertArrayEquals;

public class SingleAbstractMethodFinderTest {
    private static final String PATTERNS = GsonFunctionalPatterns.class.getName().replace('.', '/');

    /** Serves the classes of the class path, except for some which cannot be loaded or are not class files */
    private static final class FaultyLoader implements Loader {
        private final ClassPathLoader delegate = new ClassPathLoader();
        private final Set<String> missing;
        private final Set<String> garbage;

        FaultyLoader(Set<String> missing, Set<String> garbage) {
            this.missing = missing;
            this.garbage = garbage;
        }

        @Override
        public boolean canLoad(String internalName) {
            return !missing.contains(internalName) && delegate.canLoad(internalName);
        }

        @Override
        public byte[] load(String internalName) throws java.io.IOException {
            return garbage.contains(internalName) ? new byte[] {1, 2, 3} : delegate.load(internalName);
        }
    }

    private static SingleAbstractMethodFinder finder(Set<String> missing, Set<String> garbage) {
        return new SingleAbstractMethodFinder(new FaultyLoader(missing, garbage));
    }

    private static SingleAbstractMethodFinder finder() {
        return finder(Set.of(), Set.of());
    }

    @Test
    public void testDeclaredMethod() {
        assertArrayEquals(new String[] {"java/util/function/Supplier", "get", "()Ljava/lang/Object;"}, finder().find("java/util/function/Supplier"));
    }

    @Test
    public void testMethodsOfObjectAreNotAbstractMethods() {
        assertArrayEquals(new String[] {"java/util/Comparator", "compare", "(Ljava/lang/Object;Ljava/lang/Object;)I"}, finder().find("java/util/Comparator"));
        assertArrayEquals(new String[] {PATTERNS + "$Busy", "run", "()Ljava/lang/Object;"}, finder().find(PATTERNS + "$Busy"));
    }

    @Test
    public void testInheritedMethod() {
        assertArrayEquals(new String[] {"java/util/function/Function", "apply", "(Ljava/lang/Object;)Ljava/lang/Object;"},
                finder().find("java/util/function/UnaryOperator"));
        assertArrayEquals(new String[] {"java/util/function/Function", "apply", "(Ljava/lang/Object;)Ljava/lang/Object;"}, finder().find(PATTERNS + "$Fixed"));
    }

    @Test
    public void testDefaultMethodOverridesTheInheritedOne() {
        assertArrayEquals(new String[] {PATTERNS + "$Defaulted", "own", "()Ljava/lang/Object;"}, finder().find(PATTERNS + "$Defaulted"));
        // The default method is found in a sibling interface, after the abstract method it overrides
        assertArrayEquals(new String[] {PATTERNS + "$Mixed", "mixed", "()Ljava/lang/Object;"}, finder().find(PATTERNS + "$Mixed"));
    }

    @Test
    public void testSeveralAbstractMethods() {
        assertArrayEquals(new String[0], finder().find("java/util/Iterator"));
        assertArrayEquals(new String[0], finder().find("java/util/Map"));
    }

    @Test
    public void testNoAbstractMethod() {
        assertArrayEquals(new String[0], finder().find("java/util/RandomAccess"));
    }

    @Test
    public void testNotAnInterface() {
        assertArrayEquals(new String[0], finder().find("java/lang/String"));
    }

    @Test
    public void testClassWhichCannotBeLoaded() {
        assertArrayEquals(new String[0], finder().find("does/not/Exist"));
        assertArrayEquals(new String[0], finder(Set.of("java/util/function/Supplier"), Set.of()).find("java/util/function/Supplier"));
    }

    @Test
    public void testSuperInterfaceWhichCannotBeLoaded() {
        assertArrayEquals(new String[0], finder(Set.of("java/util/function/Function"), Set.of()).find("java/util/function/UnaryOperator"));
    }

    @Test
    public void testClassWhichIsNotAClassFile() {
        assertArrayEquals(new String[0], finder(Set.of(), Set.of("java/util/function/Supplier")).find("java/util/function/Supplier"));
    }

    @Test
    public void testResultIsCached() {
        SingleAbstractMethodFinder finder = finder();

        assertArrayEquals(finder.find("java/util/function/Supplier"), finder.find("java/util/function/Supplier"));
    }

    @Test
    public void testAbstractMethodWhichRedeclaresADefaultMethod() {
        assertArrayEquals(new String[] {PATTERNS + "$Reabstracted", "get", "()Ljava/lang/Object;"}, finder().find(PATTERNS + "$Reabstracted"));
    }

    @Test
    public void testCovariantDeclarationIsPreferred() {
        assertArrayEquals(new String[] {PATTERNS + "$TypedGet", "get", "()Ljava/lang/Object;"}, finder().find(PATTERNS + "$Covariant"));
    }

    @Test
    public void testSameDeclarationThroughTwoPaths() {
        assertArrayEquals(new String[] {PATTERNS + "$TypedGet", "get", "()Ljava/lang/Object;"}, finder().find(PATTERNS + "$Diamond"));
    }

    @Test
    public void testCovariantDeclarationFirst() {
        assertArrayEquals(new String[] {PATTERNS + "$TypedGet", "get", "()Ljava/lang/Object;"}, finder().find(PATTERNS + "$ReverseCovariant"));
    }

    @Test
    public void testUnrelatedDeclarationsKeepTheFirstOne() {
        assertArrayEquals(new String[] {PATTERNS + "$ObjectGet", "get", "()Ljava/lang/Object;"}, finder().find(PATTERNS + "$Unrelated"));
    }

    @Test
    public void testSubInterfaceFirst() {
        assertArrayEquals(new String[] {PATTERNS + "$MixedReverse", "mixed", "()Ljava/lang/Object;"}, finder().find(PATTERNS + "$MixedReverse"));
    }

    @Test
    public void testBothDeclarationsReturnATypeVariable() {
        assertArrayEquals(new String[] {PATTERNS + "$TypedGet", "get", "()Ljava/lang/Object;"}, finder().find(PATTERNS + "$BothTyped"));
    }

    @Test
    public void testCovariantReturnedTypesWithDifferentDescriptors() {
        assertArrayEquals(new String[] {PATTERNS + "$CovariantA", "get", "()Ljava/lang/Object;"}, finder().find(PATTERNS + "$CovariantC"));
    }

    @Test
    public void testBridgeMethodOfTheInterfaceIsNotTheAbstractMethod() {
        assertArrayEquals(new String[] {PATTERNS + "$BridgedGet", "get", "()Ljava/lang/CharSequence;"}, finder().find(PATTERNS + "$BridgedGet"));
    }

    @Test
    public void testParametersWhichAreTheSameOnceTheTypeArgumentsAreSubstituted() {
        assertArrayEquals(new String[] {PATTERNS + "$Merging", "apply", "(Ljava/lang/Object;)Ljava/lang/Object;"}, finder().find(PATTERNS + "$Combined"));
    }
}
