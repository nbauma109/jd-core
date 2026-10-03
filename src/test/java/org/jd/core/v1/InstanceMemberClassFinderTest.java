package org.jd.core.v1;

import org.jd.core.v1.api.loader.Loader;
import org.jd.core.v1.loader.ClassPathLoader;
import org.jd.core.v1.service.converter.classfiletojavasyntax.util.InstanceMemberClassFinder;
import org.jd.core.v1.stub.GuavaOuter;
import org.jd.core.v1.stub.GuavaPatterns;
import org.junit.Test;

import java.io.IOException;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class InstanceMemberClassFinderTest {
    private static final String OUTER = GuavaOuter.class.getName().replace('.', '/');
    private static final String PATTERNS = GuavaPatterns.class.getName().replace('.', '/');

    private static final class FaultyLoader implements Loader {
        private final ClassPathLoader delegate = new ClassPathLoader();
        private final String missing;
        private final String garbage;

        FaultyLoader(String missing, String garbage) {
            this.missing = missing;
            this.garbage = garbage;
        }

        @Override
        public boolean canLoad(String internalName) {
            return !internalName.equals(missing) && delegate.canLoad(internalName);
        }

        @Override
        public byte[] load(String internalName) throws IOException {
            return internalName.equals(garbage) ? new byte[] {1, 2, 3} : delegate.load(internalName);
        }
    }

    private static InstanceMemberClassFinder finder() {
        return new InstanceMemberClassFinder(new FaultyLoader(null, null));
    }

    @Test
    public void testInstanceMemberClass() {
        assertTrue(finder().isInstanceMemberClass(OUTER + "$Wrapped"));
        assertTrue(finder().isInstanceMemberClass(OUTER + "$Wrapped$Walker"));
    }

    @Test
    public void testStaticMemberClass() {
        assertFalse(finder().isInstanceMemberClass(PATTERNS + "$Cut"));
    }

    @Test
    public void testTopLevelClass() {
        assertFalse(finder().isInstanceMemberClass(OUTER));
        assertFalse(finder().isInstanceMemberClass("java/lang/String"));
    }

    @Test
    public void testLocalAndAnonymousClasses() {
        assertFalse(finder().isInstanceMemberClass(PATTERNS + "$1Local"));
        assertFalse(finder().isInstanceMemberClass(PATTERNS + "$1"));
    }

    @Test
    public void testClassWhichCannotBeRead() {
        assertFalse(finder().isInstanceMemberClass("does/not/Exist"));
        assertFalse(new InstanceMemberClassFinder(new FaultyLoader(OUTER + "$Wrapped", null)).isInstanceMemberClass(OUTER + "$Wrapped"));
        assertFalse(new InstanceMemberClassFinder(new FaultyLoader(null, OUTER + "$Wrapped")).isInstanceMemberClass(OUTER + "$Wrapped"));
    }

    @Test
    public void testResultIsCached() {
        InstanceMemberClassFinder finder = finder();

        assertTrue(finder.isInstanceMemberClass(OUTER + "$Wrapped"));
        assertTrue(finder.isInstanceMemberClass(OUTER + "$Wrapped"));
    }
}
