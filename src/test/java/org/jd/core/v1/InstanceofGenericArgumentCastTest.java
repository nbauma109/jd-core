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

import java.util.ArrayList;
import java.util.List;

public class InstanceofGenericArgumentCastTest extends AbstractJdTest {
    @Test
    public void testInstanceofNarrowingKeepsGenericComparatorCast() throws Exception {
        String internalClassName = GenericComparatorGuards.class.getName().replace('.', '/');
        String source = decompileSuccess(new ClassPathLoader(), new PlainTextPrinter(), internalClassName);

        assertTrue(source.contains("COMPARATOR.same(this.sample, (Sample)obj)"));

        assertTrue(CompilerUtil.compile("1.8", new InMemoryJavaSourceFileObject(internalClassName, source)));
    }

    @Test
    public void testInstanceofNarrowingKeepsCollectionElementCast() throws Exception {
        String internalClassName = FormElementCollectors.class.getName().replace('.', '/');
        String source = decompileSuccess(new ClassPathLoader(), new PlainTextPrinter(), internalClassName);

        assertTrue(source.matches(PatternMaker.make("forms.add((FormElement)el);")));

        assertTrue(CompilerUtil.compile("1.8", new InMemoryJavaSourceFileObject(internalClassName, source)));
    }

    @SuppressWarnings("unused")
    static final class GenericComparatorGuards {
        interface TypedComparator<T> {
            boolean same(T left, T right);
        }

        static final class Sample {
        }

        static final TypedComparator<Sample> COMPARATOR = (left, right) -> left == right;

        private final Sample sample = new Sample();

        boolean matches(Object obj) {
            return obj instanceof Sample && COMPARATOR.same(sample, (Sample) obj);
        }
    }

    @SuppressWarnings("unused")
    static final class FormElementCollectors {
        static class Element {
        }

        static final class FormElement extends Element {
        }

        private final Iterable<Element> elements = new ArrayList<>();

        List<FormElement> forms() {
            ArrayList<FormElement> forms = new ArrayList<>();
            for (Element el : elements) {
                if (el instanceof FormElement) {
                    forms.add((FormElement) el);
                }
            }
            return forms;
        }
    }
}
