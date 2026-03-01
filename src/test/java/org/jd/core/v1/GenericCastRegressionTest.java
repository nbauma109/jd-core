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

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.function.UnaryOperator;

public class GenericCastRegressionTest extends AbstractJdTest {
    @Test
    public void testReflectiveInvokeKeepsPrimitiveArrayAsSingleArgument() throws Exception {
        String internalClassName = ReflectiveInvokeCalls.class.getName().replace('.', '/');
        String source = decompileSuccess(new ClassPathLoader(), new PlainTextPrinter(), internalClassName);

        assertTrue(source.matches(PatternMaker.make("return (String)encodeMethod.invoke(encoder, data);"))
                || source.matches(PatternMaker.make("return (String)ReflectiveInvokeCalls.encodeMethod.invoke(ReflectiveInvokeCalls.encoder, data);")));
        assertFalse(source.contains("(Object[])data"));

        assertTrue(CompilerUtil.compile("1.8", new InMemoryJavaSourceFileObject(internalClassName, source)));
    }

    @Test
    public void testGenericClassCastIsPreservedForClassLoadingHelper() throws Exception {
        String internalClassName = ClassLoadingHelpers.class.getName().replace('.', '/');
        String source = decompileSuccess(new ClassPathLoader(), new PlainTextPrinter(), internalClassName);

        assertTrue(source.matches(PatternMaker.make("return ClassLoadingHelpers.<T>newInstanceOf((Class<T>)loadClass(className));"))
                || source.matches(PatternMaker.make("return newInstanceOf((Class<T>)loadClass(className));")));
        assertFalse(source.contains("return newInstanceOf(loadClass(className));"));

        assertTrue(CompilerUtil.compile("1.8", new InMemoryJavaSourceFileObject(internalClassName, source)));
    }

    @Test
    public void testEnumSetCopyOfDoesNotGainRawCollectionCast() throws Exception {
        String internalClassName = EnumSetCopyHelpers.class.getName().replace('.', '/');
        String source = decompileSuccess(new ClassPathLoader(), new PlainTextPrinter(), internalClassName);

        assertTrue(source.matches(PatternMaker.make("if (!EnumSet.copyOf(Arrays.asList(expected)).contains(this.scopes.pop()))"))
                || source.matches(PatternMaker.make("if (!EnumSet.copyOf(Arrays.asList(expected)).contains(scopes.pop()))")));
        assertFalse(source.contains("(Collection<Enum>)"));

        assertTrue(CompilerUtil.compile("1.8", new InMemoryJavaSourceFileObject(internalClassName, source)));
    }

    @Test
    public void testGenericNodeListMethodsDoNotDegenerateToBaseTypeCasts() throws Exception {
        String internalClassName = GenericNodeLists.class.getName().replace('.', '/');
        String source = decompileSuccess(new ClassPathLoader(), new PlainTextPrinter(), internalClassName);

        assertTrue(source.matches(PatternMaker.make("set(i, operator.apply(get(i)));")));
        assertFalse(source.contains("(GenericCastRegressionTest.Node)operator.apply"));

        assertTrue(CompilerUtil.compile("1.8", new InMemoryJavaSourceFileObject(internalClassName, source)));
    }

    @Test
    public void testSpecializedNodeListOverrideKeepsSpecializedReturnAndArgumentTypes() throws Exception {
        String internalClassName = ElementNodeList.class.getName().replace('.', '/');
        String source = decompileSuccess(new ClassPathLoader(), new PlainTextPrinter(), internalClassName);

        assertTrue(source.matches(PatternMaker.make("return super.set(index, element);"))
                || source.matches(PatternMaker.make("return (Element)super.set(index, element);"))
                || source.matches(PatternMaker.make("return (GenericCastRegressionTest.Element)super.set(index, element);"))
                || source.matches(PatternMaker.make("return (GenericCastRegressionTest.Element)super.set(index, (GenericCastRegressionTest.Node)element);")));
    }

    @Test
    public void testGenericMethodResultKeepsGenericArgumentCast() throws Exception {
        String internalClassName = NodeCloneLists.class.getName().replace('.', '/');
        String source = decompileSuccess(new ClassPathLoader(), new PlainTextPrinter(), internalClassName);

        assertTrue(source.matches(PatternMaker.make("clone.add((T)((Node)node).clone());"))
                || source.matches(PatternMaker.make("clone.add((T)((GenericCastRegressionTest.Node)node).clone());"))
                || source.matches(PatternMaker.make("clone.add((T)node.clone());")));
        assertTrue(CompilerUtil.compile("1.8", new InMemoryJavaSourceFileObject(internalClassName, source)));
    }

    @SuppressWarnings("unused")
    static final class ReflectiveInvokeCalls {
        private static final Method encodeMethod = null;
        private static final Object encoder = null;

        static String encode(String str) {
            if (str == null) {
                return null;
            }
            byte[] data = str.getBytes();
            try {
                return (String) encodeMethod.invoke(encoder, data);
            } catch (Exception ex) {
                throw new IllegalStateException(ex);
            }
        }
    }

    @SuppressWarnings("unused")
    static final class ClassLoadingHelpers {
        static Class<?> loadClass(String className) throws ClassNotFoundException {
            return String.class;
        }

        static <T> T newInstanceOf(Class<T> type)
                throws InvocationTargetException, InstantiationException, IllegalAccessException {
            return null;
        }

        static <T> T newInstanceOf(String className)
                throws ClassNotFoundException, InvocationTargetException, InstantiationException, IllegalAccessException {
            return newInstanceOf((Class<T>) loadClass(className));
        }
    }

    @SuppressWarnings("unused")
    static final class EnumSetCopyHelpers {
        private final ArrayDeque<Scope> scopes = new ArrayDeque<>();

        private void popScope(Scope... expected) {
            if (!EnumSet.copyOf(Arrays.asList(expected)).contains(scopes.pop())) {
                throw new IllegalStateException();
            }
        }

        private enum Scope {
            TYPE_DECLARATION,
            METHOD
        }
    }

    @SuppressWarnings("unused")
    static class Node {
        @Override
        public Node clone() {
            return this;
        }

        void replaceWith(Node node) {
        }
    }

    @SuppressWarnings("unused")
    static final class Element extends Node {
    }

    @SuppressWarnings("unused")
    static class GenericNodeLists<T extends Node> extends ArrayList<T> {
        public T set(int index, T node) {
            T old = super.set(index, node);
            old.replaceWith(node);
            return old;
        }

        @Override
        public void replaceAll(UnaryOperator<T> operator) {
            for (int i = 0; i < size(); i++) {
                set(i, operator.apply(get(i)));
            }
        }
    }

    @SuppressWarnings("unused")
    static final class ElementNodeList extends GenericNodeLists<Element> {
        @Override
        public Element set(int index, Element element) {
            return super.set(index, element);
        }
    }

    @SuppressWarnings("unused")
    static final class NodeCloneLists<T extends Node> extends ArrayList<T> {
        @Override
        public NodeCloneLists<T> clone() {
            NodeCloneLists<T> clone = new NodeCloneLists<>();
            for (T node : this) {
                clone.add((T) node.clone());
            }
            return clone;
        }
    }
}
