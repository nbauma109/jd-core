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
import org.junit.Test;

import java.io.IOException;
import java.net.SocketTimeoutException;

/**
 * Decompilation patterns distilled from the jsoup 1.22.2 upgrade: each fixture reproduces, from plain source,
 * a bytecode shape that used to decompile to broken Java (see the jsoup classes referenced on each test).
 */
public class JsoupPatternsTest extends AbstractJdTest {

    // --- org.jsoup.nodes.NodeIterator#findNextNode: shared null guard lost on the 'node = null' branch ---

    static class Node {
        int childNodeSize() { return 0; }
        Node childNode(int i) { return null; }
        Node nextSibling() { return null; }
        Node parent() { return null; }
    }

    static class NodeIter {
        Node root;
        Node current;
        Class<?> type;

        Node findNextNode() {
            Node node = current;
            while (true) {
                if (node.childNodeSize() > 0) {
                    node = node.childNode(0);
                } else if (root.equals(node)) {
                    node = null;
                } else if (node.nextSibling() != null) {
                    node = node.nextSibling();
                } else {
                    while (true) {
                        node = node.parent();
                        if (node == null || root.equals(node)) {
                            return null;
                        }
                        if (node.nextSibling() != null) {
                            node = node.nextSibling();
                            break;
                        }
                    }
                }
                if (node == null) {
                    return null;
                }
                if (type.isInstance(node)) {
                    return node;
                }
            }
        }
    }

    @Test
    public void testSharedNullGuardRestoredInLoop() throws Exception {
        String internalClassName = NodeIter.class.getName().replace('.', '/');
        String source = decompileSuccess(new ClassPathLoader(), new PlainTextPrinter(), internalClassName);

        // The 'node = null' branch must be followed by the null guard, not fall through to a dereference
        assertEquals(2, countOccurrences(source, "(node == null)"));
        assertTrue(CompilerUtil.compile("17", new InMemoryJavaSourceFileObject(internalClassName, source)));
    }

    // --- Legitimate 'var = null; continue;' source must NOT gain a spurious guard ---

    static class GenuineNullContinue {
        Object step() { return null; }

        Object find(Object cur, boolean restart) {
            Object node = cur;
            for (int i = 0; i < 100; i++) {
                if (restart) {
                    node = step();
                    node = null;
                    continue;
                }
                step();
                if (node == null) {
                    return null;
                }
                if (node instanceof String) {
                    return node;
                }
            }
            return node;
        }
    }

    @Test
    public void testGenuineNullContinueKeepsSingleGuard() throws Exception {
        String internalClassName = GenuineNullContinue.class.getName().replace('.', '/');
        String source = decompileSuccess(new ClassPathLoader(), new PlainTextPrinter(), internalClassName);

        assertEquals(1, countOccurrences(source, "(node == null)"));
        assertTrue(CompilerUtil.compile("17", new InMemoryJavaSourceFileObject(internalClassName, source)));
    }

    // --- org.jsoup.parser.HtmlTreeBuilderState$InColumnGroup: inner-switch default hoisted after the switch ---

    static class NestedSwitch {
        boolean process(String name, int type) {
            switch (type) {
                case 1:
                    say("comment");
                    break;
                case 2:
                    switch (name) {
                        case "html":
                            return other(name);
                        case "col":
                            say("col");
                            break;
                        case "template":
                            say("template");
                            break;
                        default:
                            return anythingElse(name);
                    }
                    break;
                case 3:
                    say("eof");
                    break;
                default:
                    return anythingElse(name);
            }
            return true;
        }

        boolean other(String s) { return false; }
        boolean anythingElse(String s) { return false; }
        void say(String s) { /* no-op */ }
    }

    @Test
    public void testNestedSwitchKeepsInnerDefaultCase() throws Exception {
        String internalClassName = NestedSwitch.class.getName().replace('.', '/');
        String source = decompileSuccess(new ClassPathLoader(), new PlainTextPrinter(), internalClassName);

        // Both the inner and the outer 'default' cases must survive; hoisting the inner one after its switch
        // would wrongly funnel the 'col'/'template' breaks into it
        assertEquals(2, countOccurrences(source, "default:"));
        assertTrue(CompilerUtil.compile("17", new InMemoryJavaSourceFileObject(internalClassName, source)));
    }

    // --- org.jsoup.parser.HtmlTreeBuilderState: loop exit from inside a switch needs a labeled break ---

    static class LoopExitFromSwitch {
        int firstMatch(int[] values) {
            int result = -1;
            loop:
            for (int i = 0; i < values.length; i++) {
                switch (values[i] % 3) {
                    case 0:
                        result = i;
                        break loop;
                    case 1:
                        result--;
                        break;
                    default:
                        result++;
                }
            }
            return result;
        }
    }

    @Test
    public void testLoopExitFromSwitchUsesLabeledBreak() throws Exception {
        String internalClassName = LoopExitFromSwitch.class.getName().replace('.', '/');
        String source = decompileSuccess(new ClassPathLoader(), new PlainTextPrinter(), internalClassName);

        // A bare 'break' would only exit the switch and change control flow
        assertTrue(source.contains("break label"));
        assertTrue(CompilerUtil.compile("17", new InMemoryJavaSourceFileObject(internalClassName, source)));
    }

    // --- org.jsoup.internal.ControllableInputStream#read: catch-scoped 'throw e' hoisted after the loop ---

    static class HoistedCatchThrow {
        long timeout;

        int read(boolean flag) throws IOException {
            while (true) {
                if (expired()) {
                    throw new SocketTimeoutException("Read timeout");
                }
                try {
                    return compute();
                } catch (SocketTimeoutException e) {
                    if (expired() || timeout == 0) {
                        throw e;
                    }
                }
            }
        }

        boolean expired() { return false; }
        int compute() throws IOException { return 0; }
    }

    @Test
    public void testThrowStaysInsideCatchScope() throws Exception {
        String internalClassName = HoistedCatchThrow.class.getName().replace('.', '/');
        String source = decompileSuccess(new ClassPathLoader(), new PlainTextPrinter(), internalClassName);

        // 'throw e;' must remain where 'e' is in scope, or the decompiled source cannot compile
        assertTrue(CompilerUtil.compile("17", new InMemoryJavaSourceFileObject(internalClassName, source)));
    }

    // --- org.jsoup.internal.ControllableInputStream#emitProgress: wildcard receiver needs a capture cast ---

    static class Progress<ProgressContext> {
        void onProgress(int read, int total, float percent, ProgressContext context) { /* no-op */ }
    }

    static class WildcardReceiver {
        Progress<?> progress;
        Object progressContext;
        int readPos;
        int contentLength;

        @SuppressWarnings("unchecked")
        void emitProgress() {
            if (progress == null) {
                return;
            }
            float percent = contentLength > 0 ? Math.min(100f, readPos * 100f / contentLength) : 0;
            ((Progress<Object>) progress).onProgress(readPos, contentLength, percent, progressContext);
        }
    }

    @Test
    public void testWildcardReceiverCaptureCast() throws Exception {
        String internalClassName = WildcardReceiver.class.getName().replace('.', '/');
        String source = decompileSuccess(new ClassPathLoader(), new PlainTextPrinter(), internalClassName);

        assertTrue(CompilerUtil.compile("17", new InMemoryJavaSourceFileObject(internalClassName, source)));
    }

    // --- Wildcard receiver whose type variable has a bound: the capture cast must respect the bound ---

    static class BoundedBox<T extends Number> {
        void set(T value) { /* no-op */ }
    }

    static class BoundedWildcardReceiver {
        BoundedBox<?> box;
        Integer number;

        @SuppressWarnings("unchecked")
        void store() {
            ((BoundedBox<Number>) box).set(number);
        }
    }

    @Test
    public void testBoundedWildcardReceiverCaptureCast() throws Exception {
        String internalClassName = BoundedWildcardReceiver.class.getName().replace('.', '/');
        String source = decompileSuccess(new ClassPathLoader(), new PlainTextPrinter(), internalClassName);

        // A cast to BoundedBox<Object> would violate the bound and not compile
        assertTrue(CompilerUtil.compile("17", new InMemoryJavaSourceFileObject(internalClassName, source)));
    }

    // --- org.jsoup.helper.HttpConnection: 'a.count = b.count + 1' across receivers must not fold to '++' ---

    static class Counted {
        int redirectCount;
    }

    static class FieldIncrementAcrossReceivers {
        void propagate(Counted target, Counted origin) {
            target.redirectCount = origin.redirectCount + 1;
        }
    }

    @Test
    public void testFieldIncrementAcrossReceiversNotFolded() throws Exception {
        String internalClassName = FieldIncrementAcrossReceivers.class.getName().replace('.', '/');
        String source = decompileSuccess(new ClassPathLoader(), new PlainTextPrinter(), internalClassName);

        assertTrue(source.contains("origin.redirectCount + 1"));
        assertFalse(source.contains("++"));
        assertTrue(CompilerUtil.compile("17", new InMemoryJavaSourceFileObject(internalClassName, source)));
    }

    private static int countOccurrences(String haystack, String needle) {
        int count = 0;
        int index = haystack.indexOf(needle);

        while (index != -1) {
            count++;
            index = haystack.indexOf(needle, index + needle.length());
        }

        return count;
    }
}
