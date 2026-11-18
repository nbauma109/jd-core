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
}
