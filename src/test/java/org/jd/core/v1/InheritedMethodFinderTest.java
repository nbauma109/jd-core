package org.jd.core.v1;

import org.jd.core.v1.loader.ClassPathLoader;
import org.jd.core.v1.service.converter.classfiletojavasyntax.util.InheritedMethodFinder;
import org.jd.core.v1.stub.GuavaOuter;
import org.junit.Test;

import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class InheritedMethodFinderTest {
    private static final String OUTER = GuavaOuter.class.getName().replace('.', '/');
    private static final String PACKAGE = "org/jd/core/v1/stub";

    private final InheritedMethodFinder finder = new InheritedMethodFinder(new ClassPathLoader());

    @Test
    public void testPublicAndProtectedMethodsAreInherited() {
        assertEquals(Set.of("()"), finder.parameterDescriptors(OUTER, "delegate", "other/pkg"));
        assertEquals(Set.of("(Ljava/lang/Object;)"), finder.parameterDescriptors("java/util/ArrayList", "add", "other/pkg").stream()
                .filter(descriptor -> descriptor.equals("(Ljava/lang/Object;)")).collect(java.util.stream.Collectors.toSet()));
    }

    @Test
    public void testMethodOfAnotherPackageIsNotInheritedWhenItIsNotPublic() {
        assertEquals(Set.of(), finder.parameterDescriptors(OUTER, "hidden", "other/pkg"));
        assertEquals(Set.of("(Ljava/lang/String;)"), finder.parameterDescriptors(OUTER, "hidden", PACKAGE));
    }

    @Test
    public void testMethodsOfTheSuperclassesAndInterfaces() {
        assertEquals(Set.of("(Ljava/lang/Object;)"), finder.parameterDescriptors("java/util/AbstractList", "add", "other/pkg").stream()
                .filter(descriptor -> descriptor.equals("(Ljava/lang/Object;)")).collect(java.util.stream.Collectors.toSet()));
    }

    @Test
    public void testClassWhichCannotBeRead() {
        assertNull(finder.parameterDescriptors("does/not/Exist", "add", "other/pkg"));
    }

    @Test
    public void testStaticMethodsAreNotOverridden() {
        assertEquals(Set.of(), finder.parameterDescriptors(OUTER, "shared", "other/pkg"));
    }
}
