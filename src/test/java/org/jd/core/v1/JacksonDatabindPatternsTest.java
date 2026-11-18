/*
 * Copyright (c) 2026 Nicolas Baumann (@nbauma109), with Claude Sonnet 5.5.
 * This project is distributed under the GPLv3 license.
 * This is a copyleft license that gives the user the right to use,
 * copy and modify the code freely for non-commercial purposes.
 */

package org.jd.core.v1;

import org.jd.core.v1.compiler.CompilerUtil;
import org.jd.core.v1.compiler.InMemoryJavaSourceFileObject;
import org.jd.core.v1.printer.PlainTextPrinter;
import org.junit.Test;

import java.util.Map;

/** Patterns found by decompiling and recompiling Jackson Databind: each fixture is the shape of a method of the library */
public class JacksonDatabindPatternsTest extends AbstractJdTest {

    private String decompile(String internalClassName, String source) throws Exception {
        String decompiled = decompileSuccess(RestoreMethodReferenceTest.compileForJava8("-g", Map.of(internalClassName, source)), new PlainTextPrinter(), internalClassName);

        assertTrue(decompiled, CompilerUtil.compile("17", new InMemoryJavaSourceFileObject(internalClassName, decompiled)));
        return decompiled;
    }

    @Test
    public void testVariableAssignedInTheBranchesOfAnIfIsDeclaredBeforeIt() throws Exception {
        // 'BeanDeserializer.vanillaDeserialize': the name is assigned in the two branches, then a block, then it is assigned again by the condition of the loop
        String source = decompile("jackson/DeclaredBeforeTheBranches", String.join("\n",
                "package jackson;",
                "public class DeclaredBeforeTheBranches {",
                "    interface Parser { boolean isStart(); boolean hasId(int id); String nextFieldName(); String currentName(); void nextToken(); }",
                "    static Object find(String name) { return name.isEmpty() ? null : name; }",
                "    static void handleUnknown(Parser p, Object bean, String name) { p.nextToken(); }",
                "    static Object deserialize(Parser p, Object bean, boolean needView) {",
                "        String propName;",
                "        if (p.isStart()) {",
                "            propName = p.nextFieldName();",
                "            if (propName == null) { return bean; }",
                "        } else {",
                "            if (p.hasId(5)) { propName = p.currentName(); } else { return bean; }",
                "        }",
                "        if (needView) {",
                "            Object view = new Object();",
                "            if (view != null) { return view; }",
                "        }",
                "        do {",
                "            p.nextToken();",
                "            Object prop = find(propName);",
                "            if (prop != null) { continue; }",
                "            handleUnknown(p, bean, propName);",
                "        } while ((propName = p.nextFieldName()) != null);",
                "        return bean;",
                "    }",
                "}"));
        String normalized = source.replaceAll("/\\*[^*]*\\*/", "").replaceAll("\\s+", " ");

        // the declaration is before the first assignment, not before the loop
        assertTrue(source, normalized.indexOf("String propName;") < normalized.indexOf("propName = p.nextFieldName();"));
        assertEquals(source, 1, normalized.split("String propName;", -1).length - 1);
    }

    @Test
    public void testResultOfAGenericMethodGivenAWildcardClassKeepsItsCast() throws Exception {
        // 'DatabindContext.objectIdGeneratorInstance': 'createInstance(Class<T>)' with a 'Class<?>': T is the capture, the result is cast
        String source = decompile("jackson/CastOfTheResult", String.join("\n",
                "package jackson;",
                "public class CastOfTheResult {",
                "    interface Gen<T> { Gen<T> forScope(Class<?> scope); }",
                "    static <T> T create(Class<T> type, boolean fixAccess) { return null; }",
                "    static Gen<?> instance(Class<?> implClass, boolean fix, Class<?> scope) {",
                "        Gen<?> gen = fix ? null : null;",
                "        if (gen == null) {",
                "            gen = (Gen<?>) create(implClass, fix);",
                "        }",
                "        return gen.forScope(scope);",
                "    }",
                "}"));
        String normalized = source.replaceAll("/\\*[^*]*\\*/", "").replaceAll("\\s+", " ");

        // the class is not cast to a raw type to make T the type of the variable: the result is cast instead
        assertFalse(source, normalized.contains("(Class)"));
        assertTrue(source, normalized.contains("(Gen)create(implClass, fix)") || normalized.contains("(Gen<?>)create(implClass, fix)"));
    }

    @Test
    public void testCatchWhichContinuesAForLoopKeepsItsContinue() throws Exception {
        // 'BeanDeserializer.deserializeUsingPropertyBasedWithExternalTypeId': the catch is left by a 'continue' to the update of the loop: without it
        // the code which follows the 'try' would read the variable the 'try' did not assign
        String source = decompile("jackson/CatchContinue", String.join("\n",
                "package jackson;",
                "public class CatchContinue {",
                "    interface Parser { int nextToken(); String currentName(); }",
                "    interface Builder { Object build() throws Exception; Object find(String name); }",
                "    static void wrapAndThrow(Exception e, String name) { throw new IllegalStateException(name, e); }",
                "    static boolean handle(String name) { return name.isEmpty(); }",
                "    static Object report(Object bean) { return bean; }",
                "    static Object deserialize(Parser p, Builder creator, boolean view) {",
                "        int t = p.nextToken();",
                "        for (; t == 5; t = p.nextToken()) {",
                "            String propName = p.currentName();",
                "            p.nextToken();",
                "            Object creatorProp = creator.find(propName);",
                "            if (creatorProp != null) {",
                "                if (view) {",
                "                    report(creatorProp);",
                "                } else if (creatorProp == propName) {",
                "                    p.nextToken();",
                "                } else if (!handle(propName)) {",
                "                    if (creator.find(propName) != null) {",
                "                        t = p.nextToken();",
                "                        Object bean;",
                "                        try {",
                "                            bean = creator.build();",
                "                        } catch (Exception e) {",
                "                            wrapAndThrow(e, propName);",
                "                            continue;",
                "                        }",
                "                        if (bean.getClass() != String.class) { return report(bean); }",
                "                        return bean;",
                "                    }",
                "                }",
                "                continue;",
                "            }",
                "            report(propName);",
                "        }",
                "        return null;",
                "    }",
                "}"));
        String normalized = source.replaceAll("/\\*[^*]*\\*/", "").replaceAll("\\s+", " ");

        assertTrue(source, normalized.contains("wrapAndThrow(e, propName); continue; }"));
        // the update of the loop is the one of its header: it is not copied before the 'continue'
        assertEquals(source, 1, normalized.split("t = p.nextToken\\(\\); Object bean;", -1).length - 1);
        assertFalse(source, normalized.contains("wrapAndThrow(e, propName); t = p.nextToken();"));
    }

    @Test
    public void testSwitchOfAForLoopWithAContinueKeepsTheCodeWhichFollowsIt() throws Exception {
        // 'JsonNodeDeserializer._deserializeContainerNoRecursion': a case goes on with the update of the loop, the others with the code after the switch
        String source = decompile("jackson/SwitchContinue", String.join("\n",
                "package jackson;",
                "import java.util.*;",
                "public class SwitchContinue {",
                "    static String next(int p) { return p < 5 ? \"p\" : null; }",
                "    static Object rare(int t) { return \"r\" + t; }",
                "    static void parse(int[] tokens, Map<String, Object> target) {",
                "        int pos = 0;",
                "        for (String name = \"p\"; name != null; name = next(pos)) {",
                "            Object value;",
                "            int t = tokens[pos++];",
                "            switch (t) {",
                "            case 1:",
                "                target = new HashMap<>();",
                "                continue;",
                "            case 6:",
                "                value = \"s\";",
                "                break;",
                "            case 7:",
                "                value = 7;",
                "                break;",
                "            default:",
                "                value = rare(t);",
                "            }",
                "            target.put(name, value);",
                "        }",
                "    }",
                "}"));
        String normalized = source.replaceAll("/\\*[^*]*\\*/", "").replaceAll("\\s+", " ");

        assertTrue(source, normalized.contains("target.put(name, value);"));
        assertTrue(source, normalized.contains("name = next(pos))"));
    }

    @Test
    public void testLoopLeftByASwitchBreakEndsBeforeTheCodeOfTheDoWhile() throws Exception {
        // 'JsonNodeDeserializer._deserializeContainerNoRecursion': 'continue outer' goes to the condition of the 'do', 'break' of the loop to the code before it
        String source = decompile("jackson/DoWhileBreak", String.join("\n",
                "package jackson;",
                "import java.util.*;",
                "public class DoWhileBreak {",
                "    static void parse(int[] tokens, List<Object> root, Deque<List<Object>> stack) {",
                "        List<Object> curr = root;",
                "        int pos = 0;",
                "        outer:",
                "        do {",
                "            if (pos > 100) {",
                "                curr.add(\"x\");",
                "            } else {",
                "                loop:",
                "                while (true) {",
                "                    int t = tokens[pos++];",
                "                    switch (t) {",
                "                    case 1:",
                "                        stack.push(curr);",
                "                        curr = new ArrayList<Object>();",
                "                        continue outer;",
                "                    case 4:",
                "                        break loop;",
                "                    default:",
                "                        curr.add(t);",
                "                    }",
                "                }",
                "            }",
                "            curr = stack.poll();",
                "        } while (curr != null);",
                "    }",
                "}"));
        String normalized = source.replaceAll("/\\*[^*]*\\*/", "").replaceAll("\\s+", " ");

        // 'break' of the loop is followed by the poll, 'continue' of the 'do' skips it
        int continued = normalized.indexOf("case 1: ");
        int broken = normalized.indexOf("case 4: break label");

        assertTrue(source, continued >= 0 && broken > continued && normalized.substring(continued, broken).contains("continue label"));
    }

    @Test
    public void testUncheckedCastsOfGenericTypesAreKept() throws Exception {
        // 'JacksonAnnotationIntrospector', 'DateDeserializers', 'SimpleDeserializers', 'BeanSerializerFactory', 'EnumValues', 'NumberSerializer': the bytecode has none of these casts
        String source = decompile("jackson/UncheckedCasts", String.join("\n",
                "package jackson;",
                "import java.lang.annotation.Annotation;",
                "import java.lang.reflect.Constructor;",
                "import java.math.BigDecimal;",
                "import java.util.*;",
                "public class UncheckedCasts<N extends Number> {",
                "    interface Ser<T> {}",
                "    static abstract class Std<T> implements Ser<T> {}",
                "    static class EntrySer extends Std<Map.Entry<?, ?>> { static EntrySer create(String s) { return null; } }",
                "    @SuppressWarnings(\"unchecked\")",
                "    static final Class<? extends Annotation>[] ANN = (Class<? extends Annotation>[]) new Class<?>[] { Deprecated.class, FunctionalInterface.class };",
                "    static <T> Constructor<T> find(Class<T> cls) { return null; }",
                "    static class Cal { final Constructor<Calendar> ctor;",
                "        @SuppressWarnings(\"unchecked\")",
                "        Cal(Class<? extends Calendar> cc) { ctor = (Constructor<Calendar>) find(cc); } }",
                "    static class Reg {",
                "        <T> void add(Class<T> forClass, List<? extends T> deser) {}",
                "        @SuppressWarnings(\"unchecked\")",
                "        void addAll(Class<?> cls, List<?> values) { List<Object> deser = (List<Object>) values; add((Class<Object>) cls, deser); }",
                "    }",
                "    @SuppressWarnings(\"unchecked\")",
                "    static Ser<Object> entry(String s) { return (Ser<Object>) (Ser<?>) EntrySer.create(s); }",
                "    @SuppressWarnings({\"unchecked\", \"rawtypes\"})",
                "    static Object enumMap(Map<Enum<?>, String> map) { return new EnumMap(map); }",
                "    Class<N> handled() { return null; }",
                "    boolean isBig() { return ((Class<?>) handled()) == BigDecimal.class; }",
                "}"));
        String normalized = source.replaceAll("/\\*[^*]*\\*/", "").replaceAll("\\s+", " ");

        assertTrue(source, normalized.contains("(Class<? extends Annotation>[])new Class<?>[]"));
        assertTrue(source, normalized.contains("(Constructor<Calendar>)"));
        assertTrue(source, normalized.contains("(Class<Object>)cls"));
        assertTrue(source, normalized.contains("(Ser<Object>)(Ser<?>)EntrySer.create(s)"));
        assertTrue(source, normalized.contains("new EnumMap(map)"));
        assertTrue(source, normalized.contains("(Class<?>)handled()"));
    }

    @Test
    public void testWildcardParameterizedValueGivenToARawGenericParameterKeepsItsCast() throws Exception {
        // 'StaticListSerializerBase.createContextual': a 'JsonSerializer<Object>' parameter is no different from a raw one for the types, a 'JsonSerializer<?>' is no 'JsonSerializer<Object>'
        String source = decompile("jackson/RawParameter", String.join("\n",
                "package jackson;",
                "import java.util.*;",
                "public class RawParameter<T extends Collection<?>> {",
                "    interface Ser<X> {}",
                "    static class Coll { Coll(boolean flag, Ser<Object> serializer) {} }",
                "    @SuppressWarnings(\"unchecked\")",
                "    Object create(Ser<?> ser) { return new Coll(true, (Ser<Object>) ser); }",
                "}"));
        String normalized = source.replaceAll("/\\*[^*]*\\*/", "").replaceAll("\\s+", " ");

        assertTrue(source, normalized.contains("new Coll(true, (Ser)ser)") || normalized.contains("new Coll(true, (Ser<Object>)ser)"));
    }

    @Test
    public void testVariableAssignedInATryAndUsedAfterItIsDeclaredBeforeTheTry() throws Exception {
        // 'T4AndT6Compression.decompressT4_1D' (Commons Imaging): the 'for' of the 'try' initializes the variable which the code after the 'try' reads
        String source = decompile("jackson/AssignedInTry", String.join("\n",
                "package jackson;",
                "import java.io.*;",
                "public class AssignedInTry {",
                "    static int next(int c) throws IOException { return c + 1; }",
                "    static void flush() {}",
                "    public static void f(int width, int height) throws Exception {",
                "        for (int y = 0; y < height; y++) {",
                "            int rowLength;",
                "            try {",
                "                int color = 0;",
                "                for (rowLength = 0; rowLength < width; ) {",
                "                    int runLength = next(color);",
                "                    color = 1 - color;",
                "                    rowLength += runLength;",
                "                }",
                "            } catch (IOException e) {",
                "                throw new Exception(\"Decompression error\", e);",
                "            }",
                "            if (rowLength == width) {",
                "                flush();",
                "            }",
                "        }",
                "    }",
                "}"));
        String normalized = source.replaceAll("/\\*[^*]*\\*/", "").replaceAll("\\s+", " ");

        assertTrue(source, normalized.indexOf("int rowLength;") < normalized.indexOf("try {"));
    }

    @Test
    public void testCastsAreNotAddedWhereTheTypesAgree() throws Exception {
        String source = decompile("jackson/NoCasts", String.join("\n",
                "package jackson;",
                "import java.lang.reflect.Constructor;",
                "import java.math.BigDecimal;",
                "import java.util.*;",
                "public class NoCasts<N extends Number> {",
                "    interface Ser<T> {}",
                "    static final Class<?>[] PLAIN = new Class<?>[] { String.class, Integer.class };",
                "    static final List<String>[] LISTS = new List[0];",
                "    static <T> Constructor<T> find(Class<T> cls) { return null; }",
                "    static <T> void add(Class<T> forClass, List<? extends T> values) {}",
                "    Constructor<String> exact(Class<String> cls) { Constructor<String> c = find(cls); return c; }",
                "    Constructor<?> wild(Class<? extends Calendar> cls) { Constructor<?> c = find(cls); return c; }",
                "    void same(Class<String> cls, List<String> values, Class<?> any) { add(cls, values); }",
                "    boolean same(Class<String> cls) { return cls == String.class; }",
                "    boolean differ(Class<?> cls) { return cls == BigDecimal.class; }",
                "    boolean other(NoCasts<N> n) { return n.handled() != null && n.handled() == n.handled(); }",
                "    Class<N> handled() { return null; }",
                "    static Ser<?> wildcard(Ser<?> ser) { return ser; }",
                "}"));
        String normalized = source.replaceAll("/\\*[^*]*\\*/", "").replaceAll("\\s+", " ");

        assertFalse(source, normalized.contains("(Class<Object>)"));
        assertFalse(source, normalized.contains("(Constructor<"));
        assertFalse(source, normalized.contains("(Class<?>)"));
    }

    @Test
    public void testSwitchWhoseCasesAllContinueAForLoopKeepsItsLoopUpdate() throws Exception {
        String source = decompile("jackson/AllContinue", String.join("\n",
                "package jackson;",
                "public class AllContinue {",
                "    static void use(int v) {}",
                "    static void run(int[] tokens) {",
                "        for (int pos = 0; pos < tokens.length; pos++) {",
                "            switch (tokens[pos]) {",
                "            case 1: continue;",
                "            case 2: use(2); continue;",
                "            default: use(3);",
                "            }",
                "            use(4);",
                "        }",
                "    }",
                "}"));
        String normalized = source.replaceAll("/\\*[^*]*\\*/", "").replaceAll("\\s+", " ");

        assertTrue(source, normalized.contains("pos++"));
        assertTrue(source, normalized.contains("use(4);"));
    }

    @Test
    public void testSwitchLoopsInADoWhileWithAnIfElseKeepTheirCode() throws Exception {
        String source = decompile("jackson/DoWhileSwitch", String.join("\n",
                "package jackson;",
                "import java.util.*;",
                "public class DoWhileSwitch {",
                "    static void run(int[] tokens, List<Object> root, Deque<List<Object>> stack) {",
                "        List<Object> curr = root;",
                "        int pos = 0;",
                "        outer:",
                "        do {",
                "            if (pos > 100) {",
                "                for (; pos < 200; pos++) {",
                "                    switch (tokens[pos]) {",
                "                    case 3: stack.push(curr); curr = new ArrayList<Object>(); continue outer;",
                "                    case 6: curr.add(\"s\"); break;",
                "                    default: curr.add(tokens[pos]);",
                "                    }",
                "                    curr.add(\"x\");",
                "                }",
                "            } else {",
                "                while (true) {",
                "                    int t = tokens[pos++];",
                "                    if (t == 4) { break; }",
                "                    curr.add(t);",
                "                }",
                "            }",
                "            curr = stack.poll();",
                "        } while (curr != null);",
                "    }",
                "}"));

        assertTrue(source, source.contains("stack.poll()"));
    }
}
