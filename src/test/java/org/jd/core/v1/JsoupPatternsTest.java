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

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import java.io.IOException;
import java.net.SocketTimeoutException;

/**
 * Decompilation patterns distilled from the jsoup 1.22.2 upgrade: each fixture reproduces, from plain source,
 * a bytecode shape that used to decompile to broken Java (see the jsoup classes referenced on each test).
 */
@SuppressWarnings("all")
@SuppressFBWarnings
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

    // --- Legitimate restart patterns that must stay untouched: each first decisive use is safe ---

    static class GenuineRestartNullCheckFirst {
        Object step() { return null; }

        Object find(Object cur, boolean restart) {
            Object node = cur;
            while (step() != null) {
                if (restart) {
                    node = step();
                    node = null;
                    continue;
                }
                if (node == null) {
                    return null;
                }
                step();
            }
            return node;
        }
    }

    static class GenuineRestartReassignedFirst {
        Object step() { return null; }

        Object find(Object cur, boolean restart) {
            Object node = cur;
            while (step() != null) {
                if (restart) {
                    node = step();
                    node = null;
                    continue;
                }
                node = step();
                if (node == null) {
                    return null;
                }
            }
            return node;
        }
    }

    static class GenuineRestartTernaryNullCheck {
        Object step() { return null; }

        int find(Object cur, boolean restart, int[] counts) {
            Object node = cur;
            int total = 0;
            while (step() != null) {
                total += node == null ? counts[0] : counts[1];
                if (restart) {
                    node = step();
                    node = null;
                    continue;
                }
                if (node == null) {
                    return total;
                }
            }
            return total;
        }
    }

    @Test
    public void testGenuineRestartPatternsKeepSingleGuard() throws Exception {
        Object[][] fixtures = {
            { GenuineRestartNullCheckFirst.class, 1 },
            { GenuineRestartReassignedFirst.class, 1 },
            { GenuineRestartTernaryNullCheck.class, 2 }, // its ternary condition is a second, legitimate occurrence
        };
        for (Object[] fixture : fixtures) {
            String internalClassName = ((Class<?>) fixture[0]).getName().replace('.', '/');
            String source = decompileSuccess(new ClassPathLoader(), new PlainTextPrinter(), internalClassName);

            // The loop already handles null on its continuation path: no guard may be added
            assertEquals(internalClassName, ((Integer) fixture[1]).intValue(), countOccurrences(source, "(node == null)"));
            assertTrue(CompilerUtil.compile("17", new InMemoryJavaSourceFileObject(internalClassName, source)));
        }
    }

    // --- Same guard loss as NodeIterator, but the dereference is a field access and the guard sits in a try ---

    static class Chain {
        Chain next;
        int value;
    }

    static class FieldDerefAfterLostGuard {
        Chain root;

        int sum(Chain start, boolean skip) {
            Chain node = start;
            int total = 0;
            while (true) {
                if (node.value > 0) {
                    node = node.next;
                } else if (root.equals(node)) {
                    node = null;
                } else {
                    while (true) {
                        node = node.next;
                        if (node == null || root.equals(node)) {
                            return total;
                        }
                        if (node.value > 0) {
                            node = node.next;
                            break;
                        }
                    }
                }
                if (node == null) {
                    return total;
                }
                total += node.value;
            }
        }
    }

    @Test
    public void testFieldDereferenceGetsRestoredGuard() throws Exception {
        String internalClassName = FieldDerefAfterLostGuard.class.getName().replace('.', '/');
        String source = decompileSuccess(new ClassPathLoader(), new PlainTextPrinter(), internalClassName);

        assertEquals(2, countOccurrences(source, "(node == null)"));
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

    // --- Wildcard receiver whose type variable has multiple bounds: no denotable argument, cast must go raw ---

    static class MultiBoundBox<T extends Number & Comparable<T>> {
        void set(T value) { /* no-op */ }
    }

    static class MultiBoundWildcardReceiver {
        MultiBoundBox<?> box;
        Integer number;

        @SuppressWarnings({ "unchecked", "rawtypes" })
        void store() {
            ((MultiBoundBox) box).set(number);
        }
    }

    @Test
    public void testMultiBoundWildcardReceiverCaptureCast() throws Exception {
        String internalClassName = MultiBoundWildcardReceiver.class.getName().replace('.', '/');
        String source = decompileSuccess(new ClassPathLoader(), new PlainTextPrinter(), internalClassName);

        // No parameterization satisfies 'T extends Number & Comparable<T>': a cast to MultiBoundBox<Number>
        // would not compile, only the raw type works
        assertTrue(CompilerUtil.compile("17", new InMemoryJavaSourceFileObject(internalClassName, source)));
    }

    // --- Receiver with several wildcard type arguments: the capture cast must cover each parameter ---

    static class BiConsumerLike<K, V extends Number> {
        void put(K key, V value) { /* no-op */ }
    }

    static class MultiWildcardReceiver {
        BiConsumerLike<?, ?> sink;
        String key;
        Integer number;

        @SuppressWarnings("unchecked")
        void store() {
            ((BiConsumerLike<Object, Number>) sink).put(key, number);
        }
    }

    @Test
    public void testMultiWildcardReceiverCaptureCast() throws Exception {
        String internalClassName = MultiWildcardReceiver.class.getName().replace('.', '/');
        String source = decompileSuccess(new ClassPathLoader(), new PlainTextPrinter(), internalClassName);

        assertTrue(CompilerUtil.compile("17", new InMemoryJavaSourceFileObject(internalClassName, source)));
    }

    // --- 'break outer;' from a switch nested in an inner loop must reach the outer loop's label ---

    static class LabeledBreakThroughNestedLoops {
        int scan(int[][] m) {
            int total = 0;
            outer:
            for (int i = 0; i < m.length; i++) {
                for (int j = 0; j < m[i].length; j++) {
                    switch (m[i][j] % 3) {
                        case 0:
                            total++;
                            break;
                        case 1:
                            break outer;
                        default:
                            total--;
                    }
                }
                total += 10;
            }
            return total;
        }

        int scanDeep(int[] v, boolean flag) {
            int total = 0;
            outer:
            while (flag) {
                switch (v[total % v.length]) {
                    case 0:
                        for (int j = 0; j < v.length; j++) {
                            switch (v[j]) {
                                case 7: break outer;
                                case 8: total += 2; break;
                                default: total++;
                            }
                        }
                        break;
                    default:
                        total--;
                }
                total += 100;
            }
            return total;
        }
    }

    @Test
    public void testLabeledBreakThroughNestedLoops() throws Exception {
        String internalClassName = LabeledBreakThroughNestedLoops.class.getName().replace('.', '/');
        String source = decompileSuccess(new ClassPathLoader(), new PlainTextPrinter(), internalClassName);

        // Both outer loops must be labeled and both 'break outer' must use those labels: a bare break
        // (or a break to the inner loop's label) would keep running the outer loop
        assertEquals(2, countOccurrences(source, "break label"));
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

    // --- Genuine restart in a for loop: update and condition are scanned before the body ---

    static class GenuineRestartInForLoop {
        Object step() { return null; }
        int advance(int i) { return i + 1; }

        Object find(Object cur, boolean restart) {
            Object node = cur;
            for (int i = 0; i < 100; i = advance(i)) {
                if (restart) {
                    node = step();
                    node = null;
                    continue;
                }
                node = step();
                if (node == null) {
                    return null;
                }
            }
            return node;
        }
    }

    // --- Genuine restart where the next use of the variable is a null-safe multi-argument call ---

    static class GenuineRestartMultiArgUse {
        Object step() { return null; }
        void use(int a, int b) { /* no-op */ }

        Object find(Object cur, boolean restart) {
            Object node = cur;
            while (step() != null) {
                use(1, node == null ? 0 : 1);
                if (restart) {
                    node = step();
                    node = null;
                    continue;
                }
                if (node == null) {
                    return null;
                }
            }
            return node;
        }
    }

    // --- Genuine restart where the loop continues into a try block: the scan must stop safely ---

    static class GenuineRestartIntoTry {
        Object step() { return null; }

        Object find(Object cur, boolean restart) {
            Object node = cur;
            while (step() != null) {
                try {
                    if (node == null) {
                        return null;
                    }
                    step();
                } catch (RuntimeException e) {
                    step();
                }
                if (restart) {
                    node = step();
                    node = null;
                    continue;
                }
            }
            return node;
        }
    }

    @Test
    public void testGenuineRestartVariantsKeepSingleGuard() throws Exception {
        Object[][] fixtures = {
            { GenuineRestartInForLoop.class, 1 },
            { GenuineRestartMultiArgUse.class, 2 }, // its ternary condition is a second, legitimate occurrence
            { GenuineRestartIntoTry.class, 1 },
        };
        for (Object[] fixture : fixtures) {
            String internalClassName = ((Class<?>) fixture[0]).getName().replace('.', '/');
            String source = decompileSuccess(new ClassPathLoader(), new PlainTextPrinter(), internalClassName);

            assertEquals(internalClassName, ((Integer) fixture[1]).intValue(), countOccurrences(source, "(node == null)"));
            assertTrue(CompilerUtil.compile("17", new InMemoryJavaSourceFileObject(internalClassName, source)));
        }
    }

    // --- Hoisted catch-throw variants: labeled breaks, if/else and finally inside the loop ---

    static class HoistedCatchThrowWithFinally {
        long timeout;

        int read() throws IOException {
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
                } finally {
                    cleanup();
                }
            }
        }

        boolean expired() { return false; }
        int compute() throws IOException { return 0; }
        void cleanup() { /* no-op */ }
    }

    static class HoistedCatchThrowWithLabeledBreak {
        long timeout;

        int read(int[] values, boolean running) throws IOException {
            int total = 0;
            outer:
            while (running) {
                if (expired()) {
                    throw new SocketTimeoutException("Read timeout");
                }
                for (int value : values) {
                    if (value < 0) {
                        break outer;
                    }
                    total += value;
                }
                try {
                    return compute() + total;
                } catch (SocketTimeoutException e) {
                    if (expired() || timeout == 0) {
                        throw e;
                    }
                }
            }
            return total;
        }

        boolean expired() { return false; }
        int compute() throws IOException { return 0; }
    }

    @Test
    public void testHoistedCatchThrowVariantsCompile() throws Exception {
        for (Class<?> clazz : new Class<?>[] { HoistedCatchThrowWithFinally.class, HoistedCatchThrowWithLabeledBreak.class }) {
            String internalClassName = clazz.getName().replace('.', '/');
            String source = decompileSuccess(new ClassPathLoader(), new PlainTextPrinter(), internalClassName);

            assertTrue(CompilerUtil.compile("17", new InMemoryJavaSourceFileObject(internalClassName, source)));
        }
    }

    // --- Same-receiver folding must still work: 'this.count = this.count + 1' is really '++' ---

    static class SameReceiverIncrements {
        int count;
        static int total;
        Counted[] cells = { new Counted(), new Counted() };
        Counted inner = new Counted();

        void bump() {
            count = count + 1;
            total = total + 1;
            cells[1].redirectCount = cells[1].redirectCount + 1;
            inner.redirectCount = inner.redirectCount + 1;
        }
    }

    @Test
    public void testSameReceiverIncrementsStayFolded() throws Exception {
        String internalClassName = SameReceiverIncrements.class.getName().replace('.', '/');
        String source = decompileSuccess(new ClassPathLoader(), new PlainTextPrinter(), internalClassName);

        assertEquals(4, countOccurrences(source, "++"));
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
