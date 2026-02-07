/*
 * Copyright (c) 2008, 2019 Emmanuel Dupuy.
 * This project is distributed under the GPLv3 license.
 * This is a Copyleft license that gives the user the right to use,
 * copy and modify the code freely for non-commercial purposes.
 */

package org.jd.core.v1;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.IOFileFilter;
import org.apache.logging.log4j.core.appender.HttpURLConnectionManager;
import org.jd.core.test.TryResourcesImaging;
import org.jd.core.v1.api.loader.Loader;
import org.jd.core.v1.compiler.CompilerUtil;
import org.jd.core.v1.compiler.InMemoryClassLoader;
import org.jd.core.v1.compiler.InMemoryJavaSourceFileObject;
import org.jd.core.v1.loader.ClassPathLoader;
import org.jd.core.v1.loader.CompositeLoader;
import org.jd.core.v1.printer.ClassFilePrinter;
import org.jd.core.v1.printer.PlainTextPrinter;
import org.jd.core.v1.regex.PatternMaker;
import org.jd.core.v1.stub.NumericConstants;
import org.jd.core.v1.stub.TernaryOpDiamond;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.invoke.MethodHandle;
import java.lang.reflect.Type;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.security.PrivilegedActionException;
import java.sql.Time;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.Deque;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.ServiceLoader;
import java.util.Set;
import java.util.Vector;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import net.sourceforge.plantuml.klimt.drawing.LimitFinder;

public class MiscTest extends AbstractJdTest {

//    TODO: in progress
//    @Test
//    public void testLabel() throws Exception {
//        String internalClassName = Label.class.getName().replace('.', '/');
//        String source = decompileSuccess(new ClassPathLoader(), new PlainTextPrinter(), internalClassName);
//
//        // Recompile decompiled source code and check errors
//        assertTrue(CompilerUtil.compile("1.8", new InMemoryJavaSourceFileObject(internalClassName, source)));
//    }

    @Test
    public void testFREM() throws Exception {
        class FREM {
            @SuppressWarnings("unused")
            float frem(float a, float b) {
                return a % b;
            }
        }
        String internalClassName = FREM.class.getName().replace('.', '/');
        String source = decompileSuccess(new ClassPathLoader(), new PlainTextPrinter(), internalClassName);

        // Check decompiled source code
        assertTrue(source.matches(PatternMaker.make("float frem(float a, float b) {")));
        assertTrue(source.matches(PatternMaker.make("return a % b;")));

        // Recompile decompiled source code and check errors
        assertTrue(CompilerUtil.compile("1.4", new InMemoryJavaSourceFileObject(internalClassName, source)));
    }

    @Test
    public void testFileFilterUtils() throws Exception {
        abstract class FileFilterUtils {
            abstract <R, A> R filterFiles(IOFileFilter filter, Stream<File> stream, Collector<? super File, A, R> collector);
            @SuppressWarnings("unused")
            File[] filter(IOFileFilter filter, File... files) {
                return filterFiles(filter, Stream.of(files), Collectors.toList()).toArray(FileUtils.EMPTY_FILE_ARRAY);
            }
        }
        String internalClassName = FileFilterUtils.class.getName().replace('.', '/');
        String source = decompileSuccess(new ClassPathLoader(), new ClassFilePrinter(), internalClassName);

        // Recompile decompiled source code and check errors
        assertTrue(CompilerUtil.compile("1.8", new InMemoryJavaSourceFileObject(internalClassName, source)));
    }

    @Test
    public void testCastPrimitive() throws Exception {
        abstract class CastPrimitive {
            abstract void testByte(byte... b);
            abstract void testShort(short... s);
            @SuppressWarnings("unused")
            void test(int i) {
                testByte((byte)Short.MAX_VALUE);
                testShort((short)Integer.MAX_VALUE);
            }
        }
        String internalClassName = CastPrimitive.class.getName().replace('.', '/');
        String source = decompileSuccess(new ClassPathLoader(), new PlainTextPrinter(), internalClassName);

        // Recompile decompiled source code and check errors
        assertTrue(CompilerUtil.compile("1.8", new InMemoryJavaSourceFileObject(internalClassName, source)));
    }

    @Test
    public void testInitTable() throws Exception {
        class InitTable {
            @SuppressWarnings("unused")
            void initTable(int[][] table) {
                for (int i = 0; i < table.length; i++) {
                    table[i] = new int[] { i, i + 1, i + 2 };
                }
                for (int j = 0; j < table.length; j++) {
                    table[j] = new int[] { j, j - 1, j - 2 };
                }
            }
        }
        String internalClassName = InitTable.class.getName().replace('.', '/');
        String source = decompileSuccess(new ClassPathLoader(), new PlainTextPrinter(), internalClassName);

        // Check decompiled source code
        assertTrue(source.matches(PatternMaker.make("table[i] = new int[] { i, i + 1, i + 2 };")));
        assertTrue(source.matches(PatternMaker.make("table[j] = new int[] { j, j - 1, j - 2 };")));

        // Recompile decompiled source code and check errors
        assertTrue(CompilerUtil.compile("1.4", new InMemoryJavaSourceFileObject(internalClassName, source)));
    }

    @Test
    public void testTryResourcesImaging() throws Exception {
        String internalClassName = TryResourcesImaging.class.getName().replace('.', '/');
        try (InputStream is = this.getClass().getResourceAsStream("/jar/try-resources-imaging-jdk-11.0.12.jar")) {
            Loader loader = new CompositeLoader(is);
            String source = decompileSuccess(loader, new PlainTextPrinter(), internalClassName);

            // Recompile decompiled source code and check errors
            assertTrue(CompilerUtil.compile("11", new InMemoryJavaSourceFileObject(internalClassName, source)));
        }
    }

    private void test(String jarPath, String internalClassName, String expectedOutput, String compilerVersion) throws Exception {
        try (InputStream is = this.getClass().getResourceAsStream(jarPath)) {
            Loader loader = new CompositeLoader(is);
            String source = decompileSuccess(loader, new PlainTextPrinter(), internalClassName);
            
            // Check decompiled source code
            String expected = Files.readString(Paths.get(getClass().getResource(expectedOutput).toURI()));
            assertEqualsIgnoreEOL(expected, source);
            
            // Recompile decompiled source code and check errors
            assertTrue(CompilerUtil.compile(compilerVersion, new InMemoryJavaSourceFileObject(internalClassName, source)));
        }
    }
    
    @Test
    public void testTryResourcesGeneric() throws Exception {
        test("/jar/try-resources-generic-jdk-1.8.0_331.jar", "org/jd/core/v1/TryResources", "/txt/TryResourcesGeneric.txt", "1.8");
    }

    @Test
    public void testTryResources2JDK8() throws Exception {
        test("/jar/try-resources-2-jdk-1.8.0_331.jar", "org/jd/core/v1/TryResources2", "/txt/TryResources2.txt", "1.8");
    }

    @Test
    public void testTryResources2JDK17() throws Exception {
        test("/jar/try-resources-2-jdk-17.0.11.jar", "org/jd/core/v1/TryResources2", "/txt/TryResources2.txt", "1.8");
    }
    
    @Test
    public void testTryResources2ECJ8() throws Exception {
        test("/jar/try-resources-2-ecj-8.jar", "org/jd/core/v1/TryResources2", "/txt/TryResources2.txt", "1.8");
    }
    
    @Test
    public void testTryResources2ECJ17() throws Exception {
        test("/jar/try-resources-2-ecj-17.jar", "org/jd/core/v1/TryResources2", "/txt/TryResources2.txt", "1.8");
    }
    
    @Test
    public void testTryResourcesNewPatternJDK() throws Exception {
        test("/jar/try-resources-new-pattern-jdk-17.0.11.jar", "org/jd/core/v1/TryResourcesNewPattern", "/txt/TryResourcesNewPattern.txt", "11");
    }

    @Test
    public void testTryResourcesNewPatternECJ() throws Exception {
        test("/jar/try-resources-new-pattern-ecj-17.jar", "org/jd/core/v1/TryResourcesNewPattern", "/txt/TryResourcesNewPattern.txt", "11");
    }
    
    @Test
    public void testTryResourcesMixedExpression() throws Exception {
        String internalClassName = TryResourcesMixedExpression.class.getName().replace('.', '/');
        String source = decompileSuccess(new ClassPathLoader(), new PlainTextPrinter(), internalClassName);

        // Recompile decompiled source code and check errors
        assertTrue(CompilerUtil.compile("9", new InMemoryJavaSourceFileObject(internalClassName, source)));
    }

//    @Test
//    public void testTryResourcesThrowableMultiDeclarator() throws Exception {
//        String internalClassName = TryResourcesThrowableMultiDeclarator.class.getName().replace('.', '/');
//        String source = decompileSuccess(new ClassPathLoader(), new PlainTextPrinter(), internalClassName);
//        
//        // Recompile decompiled source code and check errors
//        assertTrue(CompilerUtil.compile("1.7", new InMemoryJavaSourceFileObject(internalClassName, source)));
//    }
    
    @Test
    public void testTryResourcesThrowableNullDecl() throws Exception {
        String internalClassName = TryResourcesThrowableNullDecl.class.getName().replace('.', '/');
        String source = decompileSuccess(new ClassPathLoader(), new PlainTextPrinter(), internalClassName);
        
        // Recompile decompiled source code and check errors
        assertTrue(CompilerUtil.compile("1.7", new InMemoryJavaSourceFileObject(internalClassName, source)));
    }
    
//    @Test
//    public void testTryResourcesThrowableNullDeclUsed() throws Exception {
//        String internalClassName = TryResourcesThrowableNullDeclUsed.class.getName().replace('.', '/');
//        String source = decompileSuccess(new ClassPathLoader(), new PlainTextPrinter(), internalClassName);
//        
//        // Recompile decompiled source code and check errors
//        assertTrue(CompilerUtil.compile("1.7", new InMemoryJavaSourceFileObject(internalClassName, source)));
//    }
    
    @Test
    public void testCreateDirs() throws Exception {
        class CreateDirs {
            @SuppressWarnings("unused")
            void createDirs(Path outputDirectory) throws IOException {
                Files.createDirectories(outputDirectory);
            }
        }
        String internalClassName = CreateDirs.class.getName().replace('.', '/');
        String source = decompileSuccess(new ClassPathLoader(), new PlainTextPrinter(), internalClassName);

        // Check decompiled source code
        assertTrue(source.matches(PatternMaker.make("Files.createDirectories(outputDirectory);")));

        // Recompile decompiled source code and check errors
        assertTrue(CompilerUtil.compile("1.8", new InMemoryJavaSourceFileObject(internalClassName, source)));
    }

    @Test
    public void testArraysCopyOf() throws Exception {
        class ParameterizedTypeImpl {
            @SuppressWarnings("unused")
            Type[] typeArguments;

            @SuppressWarnings("unused")
            ParameterizedTypeImpl(Type[] typeArguments) {
                this.typeArguments = Arrays.copyOf(typeArguments, typeArguments.length, Type[].class);
            }
        }
        String internalClassName = ParameterizedTypeImpl.class.getName().replace('.', '/');
        String source = decompileSuccess(new ClassPathLoader(), new PlainTextPrinter(), internalClassName);

        // Recompile decompiled source code and check errors
        assertTrue(CompilerUtil.compile("1.8", new InMemoryJavaSourceFileObject(internalClassName, source)));
    }

    @Test
    public void testServiceLoader() throws Exception {
        class SvcLoader {
            @SuppressWarnings("unused")
            static <T> Iterable<T> callServiceLoader(MethodHandle handle, Class<T> serviceType, ClassLoader classLoader) throws Throwable {
                ServiceLoader<T> serviceLoader = (ServiceLoader<T>) handle.invokeExact(serviceType, classLoader);
                return serviceLoader;
            }
        }
        String internalClassName = SvcLoader.class.getName().replace('.', '/');
        String source = decompileSuccess(new ClassPathLoader(), new PlainTextPrinter(), internalClassName);

        // Recompile decompiled source code and check errors
        assertTrue(CompilerUtil.compile("1.8", new InMemoryJavaSourceFileObject(internalClassName, source)));
    }

    @Test
    public void testExpectedExceptionMatcherBuilder() throws Exception {
        String internalClassName = ExpectedExceptionMatcherBuilder.class.getName().replace('.', '/');
        try (InputStream is = this.getClass().getResourceAsStream("/jar/expected-exception-matcher-builder-jdk1.6.0u119.jar")) {
            Loader loader = new CompositeLoader(is);
            String source = decompileSuccess(loader, new PlainTextPrinter(), internalClassName);

            // Recompile decompiled source code and check errors
            assertTrue(CompilerUtil.compile("1.6", new InMemoryJavaSourceFileObject(internalClassName, source)));
        }
    }

    @Test
    public void testMap2() throws Exception {
        class MapIter2 {
            @SuppressWarnings("unused")
            void iter(Map<String, String> headers) throws Exception {
                for (Map.Entry<String, String> header : headers.entrySet()) {
                    System.out.println(header.getKey().getBytes("UTF-8"));
                }
            }
        }
        String internalClassName = MapIter2.class.getName().replace('.', '/');
        String source = decompileSuccess(new ClassPathLoader(), new PlainTextPrinter(), internalClassName);

        // Recompile decompiled source code and check errors
        assertTrue(CompilerUtil.compile("1.8", new InMemoryJavaSourceFileObject(internalClassName, source)));
    }

    @Test
    public void testMap3() throws Exception {
        class MapTest {
            @SuppressWarnings("unused")
            private static TriConsumer<String, String, Map<String, String>> PUT_ALL;
            static {
                PUT_ALL = (key, value, stringStringMap) -> stringStringMap.put(key, value);
            }
        }
        String internalClassName = MapTest.class.getName().replace('.', '/');
        String source = decompileSuccess(new ClassPathLoader(), new PlainTextPrinter(), internalClassName);

        // Recompile decompiled source code and check errors
        assertTrue(CompilerUtil.compile("1.8", new InMemoryJavaSourceFileObject(internalClassName, source)));
    }

    @Test
    public void testMapLambda() throws Exception {
        String internalClassName = MapLambda.class.getName().replace('.', '/');
        try (InputStream is = this.getClass().getResourceAsStream("/jar/map-lambda-jdk8u331.jar")) {
            Loader loader = new CompositeLoader(is);
            String source = decompileSuccess(loader, new PlainTextPrinter(), internalClassName);

            // Check decompiled source code
            assertTrue(source.matches(PatternMaker.make("this.map.forEach((key, value) -> result.put(key, value));")));

            // Recompile decompiled source code and check errors
            InMemoryClassLoader classLoader = new InMemoryClassLoader();
            assertTrue(CompilerUtil.compile("1.8", classLoader, new InMemoryJavaSourceFileObject(internalClassName, source)));

            Class<?> recompiledClass = classLoader.findClassByInternalName(internalClassName);
            assertNotNull(recompiledClass);
            assertTrue(classLoader.canLoad(internalClassName));
            assertNotNull(classLoader.load(internalClassName));

            String eclipseSource = decompileSuccess(classLoader, new PlainTextPrinter(), internalClassName);

            // Check decompiled source code
            assertEquals(source, eclipseSource);
        }
    }

    @Test
    public void testIndexedStringMapLambda() throws Exception {
        String internalClassName = IndexedStringMapLambda.class.getName().replace('.', '/');
        String source = decompileSuccess(new ClassPathLoader(), new PlainTextPrinter(), internalClassName);

        // Check decompiled source code
        assertTrue(source.matches(PatternMaker.make("this.map.forEach((key, value) -> result.put(key, (List)value));")));

        // Recompile decompiled source code and check errors
        assertTrue(CompilerUtil.compile("1.8", new InMemoryJavaSourceFileObject(internalClassName, source)));
    }

    @Test
    public void testPrivilegedAction() throws Exception {
        class PrivilegedActionClass {
            @SuppressWarnings("all")
            <T> void doPrivileged() throws PrivilegedActionException {
                AccessController.doPrivileged((PrivilegedAction<T>) () -> null);
            }
        }
        String internalClassName = PrivilegedActionClass.class.getName().replace('.', '/');
        String source = decompileSuccess(new ClassPathLoader(), new PlainTextPrinter(), internalClassName);

        // Recompile decompiled source code and check errors
        assertTrue(CompilerUtil.compile("1.8", new InMemoryJavaSourceFileObject(internalClassName, source)));
    }

    /*
     * TODO FIXME Eclipse compiler incompatibility
     */
    @Test
    public void testConsumer() throws Exception {
        try (InputStream is = this.getClass().getResourceAsStream("/jar/misc-oracle-jdk1.8.0_331.jar")) {
            Loader loader = new CompositeLoader(is);
            String internalClassName = MiscOracleJDK8.Consumer.class.getName().replace('.', '/');
            String source = decompileSuccess(loader, new PlainTextPrinter(), internalClassName);
    
            // Recompile decompiled source code and check errors
            assertTrue(CompilerUtil.compile("1.8", new InMemoryJavaSourceFileObject(internalClassName, source)));
        }
    }

    @Test
    public void testIterable() throws Exception {
        class Iter {
            @SuppressWarnings({ "unused", "unchecked" })
            static List<Object> allParameters(Object parameters) throws Throwable {
                if (parameters instanceof Iterable) {
                    List<Object> result = new ArrayList<Object>();
                    for (Object entry : ((Iterable<Object>) parameters)) {
                        result.add(entry);
                    }
                    return result;
                }
                return null;
            }
        }
        String internalClassName = Iter.class.getName().replace('.', '/');
        String source = decompileSuccess(new ClassPathLoader(), new PlainTextPrinter(), internalClassName);

        // Recompile decompiled source code and check errors
        assertTrue(CompilerUtil.compile("1.8", new InMemoryJavaSourceFileObject(internalClassName, source)));
    }

    @Test
    public void testEnumMap() throws Exception {
        try (InputStream is = this.getClass().getResourceAsStream("/jar/enum-map-jdk8u331.jar")) {
            Loader loader = new CompositeLoader(is);
            String internalClassName = EnumMapUtil.class.getName().replace('.', '/');
            String source = decompileSuccess(loader, new PlainTextPrinter(), internalClassName);

            // Recompile decompiled source code and check errors
            assertTrue(CompilerUtil.compile("1.8", new InMemoryJavaSourceFileObject(internalClassName, source)));
        }
    }

    @Test
    public void testSafeList1() throws Exception {
        class SafeList1 {
            Map<String, Set<String>> attributes;

            @SuppressWarnings("unused")
            SafeList1() {
                for (Map.Entry<String, Set<String>> copyTagAttributes : attributes.entrySet()) {
                    attributes.put(copyTagAttributes.getKey(), new HashSet<>(copyTagAttributes.getValue()));
                }
            }
        }
        String internalClassName = SafeList1.class.getName().replace('.', '/');
        String source = decompileSuccess(new ClassPathLoader(), new PlainTextPrinter(), internalClassName);

        // Check decompiled source code
        assertTrue(source.matches(PatternMaker.make("attributes.put(copyTagAttributes.getKey(), new HashSet<>(copyTagAttributes.getValue()));")));

        // Recompile decompiled source code and check errors
        assertTrue(CompilerUtil.compile("1.8", new InMemoryJavaSourceFileObject(internalClassName, source)));
    }

    @Test
    public void testSafeList2() throws Exception {
        class SafeList2 {
            Map<String, Map<String, String>> enforcedAttributes;

            @SuppressWarnings("unused")
            void test(String attrKey, String attrVal, String tagName) {
                enforcedAttributes.get(tagName).put(attrKey, attrVal);
            }
        }
        String internalClassName = SafeList2.class.getName().replace('.', '/');
        String source = decompileSuccess(new ClassPathLoader(), new PlainTextPrinter(), internalClassName);

        // Check decompiled source code
        assertTrue(source.matches(PatternMaker.make("enforcedAttributes.get(tagName).put(attrKey, attrVal);")));

        // Recompile decompiled source code and check errors
        assertTrue(CompilerUtil.compile("1.8", new InMemoryJavaSourceFileObject(internalClassName, source)));
    }

    @Test
    public void testNewInstance() throws Exception {
        class NewInstance {
            @SuppressWarnings("unused")
            NewInstance newCastTests(Class<? extends NewInstance> clazz) throws Exception {
                return newInstanceOf(clazz);
            }

            @SuppressWarnings({ "unchecked", "unused" })
            <T> T newInstanceOf(String className) throws Exception {
                return newInstanceOf((Class<T>) Class.forName(className));
            }

            <T> T newInstanceOf(Class<T> clazz) throws Exception {
                return clazz.getConstructor().newInstance();
            }
        }
        String internalClassName = NewInstance.class.getName().replace('.', '/');
        String source = decompileSuccess(new ClassPathLoader(), new PlainTextPrinter(), internalClassName);

        // Check decompiled source code
        assertTrue(source.matches(PatternMaker.make("return newInstanceOf(clazz);")));
        assertTrue(source.matches(PatternMaker.make("return newInstanceOf((Class<T>)Class.forName(className));")));
        assertTrue(source.matches(PatternMaker.make("return clazz.getConstructor().newInstance();")));

        // Recompile decompiled source code and check errors
        assertTrue(CompilerUtil.compile("1.8", new InMemoryJavaSourceFileObject(internalClassName, source)));
    }

    @Test
    public void testToArray() throws Exception {
        class ToArray {
            @SuppressWarnings("unused")
            List<String> toArray(List<String> list) {
                return Arrays.asList(list.<String>toArray(new String[0]));
            }
        }
        String internalClassName = ToArray.class.getName().replace('.', '/');
        String source = decompileSuccess(new ClassPathLoader(), new PlainTextPrinter(), internalClassName);

        // Check decompiled source code
        assertTrue(source.matches(PatternMaker.make("return Arrays.asList(list.<String>toArray(new String[0]));")));

        // Recompile decompiled source code and check errors
        assertTrue(CompilerUtil.compile("1.8", new InMemoryJavaSourceFileObject(internalClassName, source)));
    }

    @Test
    public void testClassArray1() throws Exception {
        class ClassArray1 {
            static {
                List<Class<? extends Date>> dateClassList = Arrays.asList(Timestamp.class, Date.class, java.sql.Date.class, Time.class);
                for (Class<? extends Date> dateClass : dateClassList) {
                    System.out.println(dateClass);
                }
            }
        }
        String internalClassName = ClassArray1.class.getName().replace('.', '/');
        String source = decompileSuccess(new ClassPathLoader(), new PlainTextPrinter(), internalClassName);

        // Check decompiled source code
        assertTrue(source.matches(PatternMaker.make("List<Class<? extends Date>> dateClassList = Arrays.asList(Timestamp.class, Date.class, java.sql.Date.class, Time.class);")));

        // Recompile decompiled source code and check errors
        assertTrue(CompilerUtil.compile("1.7", new InMemoryJavaSourceFileObject(internalClassName, source)));
    }

    @Test
    public void testClassArray2() throws Exception {
        class ClassArray2 {
            static {
                for (Class<? extends Date> dateClass : Arrays.asList(Timestamp.class, Date.class, java.sql.Date.class, Time.class)) {
                    System.out.println(dateClass);
                }
            }
        }
        String internalClassName = ClassArray2.class.getName().replace('.', '/');
        String source = decompileSuccess(new ClassPathLoader(), new PlainTextPrinter(), internalClassName);

        // Check decompiled source code
        assertTrue(source.matches(PatternMaker.make("for (Class<? extends Date> dateClass : Arrays.asList(Timestamp.class, Date.class, java.sql.Date.class, Time.class))")));

        // Recompile decompiled source code and check errors
        assertTrue(CompilerUtil.compile("1.7", new InMemoryJavaSourceFileObject(internalClassName, source)));
    }

    @Test
    public void testListArray() throws Exception {
        class ListArray {
            @SuppressWarnings("unused")
            List<List<?>> lists = Arrays.asList(new LinkedList<>(), new ArrayList<>(), new Vector<>());
        }
        String internalClassName = ListArray.class.getName().replace('.', '/');
        String source = decompileSuccess(new ClassPathLoader(), new PlainTextPrinter(), internalClassName);

        // Recompile decompiled source code and check errors
        assertTrue(CompilerUtil.compile("1.8", new InMemoryJavaSourceFileObject(internalClassName, source)));
    }

    @Test
    public void testJoin() throws Exception {
        class Join {
            @SuppressWarnings("unused")
            public String join() {
                return String.join(",", "a", "b", "c");
            }
        }
        String internalClassName = Join.class.getName().replace('.', '/');
        String source = decompileSuccess(new ClassPathLoader(), new PlainTextPrinter(), internalClassName);

        // Recompile decompiled source code and check errors
        assertTrue(CompilerUtil.compile("1.8", new InMemoryJavaSourceFileObject(internalClassName, source)));
    }

    @Test
    public void testInnerClassConstructorInvocation() throws Exception {
        try (InputStream is = this.getClass().getResourceAsStream("/jar/inner-class-constructor-call-jdk8u331.jar")) {
            Loader loader = new CompositeLoader(is);
            String internalClassName = Parameterized.class.getName().replace('.', '/');
            String source = decompileSuccess(loader, new PlainTextPrinter(), internalClassName);

            // Recompile decompiled source code and check errors
            assertTrue(CompilerUtil.compile("1.8", new InMemoryJavaSourceFileObject(internalClassName, source)));
        }
    }

    @Test
    public void testBoundsAnonymous() throws Exception {
        try (InputStream is = this.getClass().getResourceAsStream("/jar/bounds-anonymous-jdk8u331.jar")) {
            Loader loader = new CompositeLoader(is);
            String internalClassName = TestBoundsAnonymous.class.getName().replace('.', '/');
            String source = decompileSuccess(loader, new PlainTextPrinter(), internalClassName);

            // Check decompiled source code
            assertTrue(source.matches(PatternMaker.make("Iterator<Class<?>> wrapped = Collections.<Class<?>>emptySet().iterator();")));
            assertTrue(source.matches(PatternMaker.make("Iterator<Class<?>> interfaces = Collections.<Class<?>>emptySet().iterator();")));
            assertTrue(source.matches(PatternMaker.make("Class<?> nextInterface = this.interfaces.next();")));
            assertTrue(source.matches(PatternMaker.make("Class<?> nextSuperclass = TestBoundsAnonymous.wrapped.next();")));

            // Recompile decompiled source code and check errors
            assertTrue(CompilerUtil.compile("1.8", new InMemoryJavaSourceFileObject(internalClassName, source)));
        }
    }

    @Test
    public void testBoundsLambda() throws Exception {
        try (InputStream is = this.getClass().getResourceAsStream("/jar/bounds-lambda-jdk8u331.jar")) {
            Loader loader = new CompositeLoader(is);
            String internalClassName = TestBoundsLambda.class.getName().replace('.', '/');
            String source = decompileSuccess(loader, new PlainTextPrinter(), internalClassName);

            // Check decompiled source code
            assertTrue(source.matches(PatternMaker.make("Iterator<Class<?>> wrapped = Collections.<Class<?>>emptySet().iterator();")));
            assertTrue(source.matches(PatternMaker.make("Iterator interfaces = Collections.emptySet().iterator();")));
            assertTrue(source.matches(PatternMaker.make("Class<?> nextInterface = (Class)this.interfaces.next();")));
            assertTrue(source.matches(PatternMaker.make("Class<?> nextSuperclass = TestBoundsLambda.wrapped.next();")));

            // Recompile decompiled source code and check errors
            assertTrue(CompilerUtil.compile("1.8", new InMemoryJavaSourceFileObject(internalClassName, source)));
        }
    }

    @Test
    public void testBounds3() throws Exception {
        enum Bounds3 {
            ;
            Deque<Bounds3> scopes;
            @SuppressWarnings("unused")
            void popScope(List<Bounds3> expected) {
                if (!EnumSet.<Bounds3>copyOf((Collection<Bounds3>)Collections.synchronizedList(expected)).contains(this.scopes.pop())) {
                    throw new IllegalStateException();
                }
            }
        }
        String internalClassName = Bounds3.class.getName().replace('.', '/');
        String source = decompileSuccess(new ClassPathLoader(), new PlainTextPrinter(), internalClassName);

        // Check decompiled source code
        assertTrue(source.matches(PatternMaker.make("if (!EnumSet.copyOf((Collection)Collections.synchronizedList(expected)).contains(this.scopes.pop()))")));

        // Recompile decompiled source code and check errors
        assertTrue(CompilerUtil.compile("1.8", new InMemoryJavaSourceFileObject(internalClassName, source)));
    }

    @Test
    public void testHandleExact() throws Exception {
        class HandleExact {
            @SuppressWarnings("unused")
            <T> void handleExact(MethodHandle handle, Class<T> serviceType, ClassLoader classLoader) throws Throwable {
                ServiceLoader<T> serviceLoader = (ServiceLoader<T>) handle.invokeExact(serviceType, classLoader);
            }
        }
        String internalClassName = HandleExact.class.getName().replace('.', '/');
        String source = decompileSuccess(new ClassPathLoader(), new PlainTextPrinter(), internalClassName);

        // Check decompiled source code
        assertTrue(source.matches(PatternMaker.make("ServiceLoader<T> serviceLoader = (ServiceLoader<T>)handle.invokeExact(serviceType, classLoader);")));

        // Recompile decompiled source code and check errors
        assertTrue(CompilerUtil.compile("1.8", new InMemoryJavaSourceFileObject(internalClassName, source)));
    }

    @Test
    public void testOverload1() throws Exception {
        interface ILogger {
            boolean isEnabled(CharSequence message, Throwable t);

            boolean isEnabled(Object message, Throwable t);

            @SuppressWarnings("unused")
            abstract class TestOverload implements ILogger {
                public boolean isEnabled() {
                    return isEnabled((Object) null, null);
                }
            }
        }
        String internalClassName = ILogger.class.getName().replace('.', '/');
        String source = decompileSuccess(new ClassPathLoader(), new PlainTextPrinter(), internalClassName);

        // Check decompiled source code
        assertTrue(source.matches(PatternMaker.make("return isEnabled((Object)null, (Throwable)null);")));

        // Recompile decompiled source code and check errors
        assertTrue(CompilerUtil.compile("1.8", new InMemoryJavaSourceFileObject(internalClassName, source)));
    }

    @Test
    public void testSuperDefault() throws Exception {
        abstract class SuperDefault implements IDefault {
            public void test(Object... o) {
                IDefault.super.test(o);
            }
        }
        String internalClassName = SuperDefault.class.getName().replace('.', '/');
        String source = decompileSuccess(new ClassPathLoader(), new PlainTextPrinter(), internalClassName);

        // Check decompiled source code
        assertTrue(source.matches(PatternMaker.make("IDefault.super.test(o);")));

        // Recompile decompiled source code and check errors
        assertTrue(CompilerUtil.compile("1.8", new InMemoryJavaSourceFileObject(internalClassName, source)));
    }

    @Test
    public void testVarArg1() throws Exception {
        abstract class VarArgTest1 implements IDefault {
            @SuppressWarnings("unused")
            void test1() {
                test(new Object[0], new Object[0]);
            }
        }
        String internalClassName = VarArgTest1.class.getName().replace('.', '/');
        String source = decompileSuccess(new ClassPathLoader(), new PlainTextPrinter(), internalClassName);

        // Check decompiled source code
        assertTrue(source.matches(PatternMaker.make("test(new Object[0]);")));

        // Recompile decompiled source code and check errors
        assertTrue(CompilerUtil.compile("1.8", new InMemoryJavaSourceFileObject(internalClassName, source)));
    }

    @Test
    public void testVarArg2() throws Exception {
        abstract class VarArgTest2 implements IDefault {
            @SuppressWarnings("unused")
            void test2() {
                test(new Object[] { 0, 1 }, new Object[] { 0, 1 });
            }
        }
        String internalClassName = VarArgTest2.class.getName().replace('.', '/');
        String source = decompileSuccess(new ClassPathLoader(), new PlainTextPrinter(), internalClassName);

        // Check decompiled source code
        assertTrue(source.matches(PatternMaker.make("test(new Object[] { 0, 1 }, 0, 1);")));

        // Recompile decompiled source code and check errors
        assertTrue(CompilerUtil.compile("1.8", new InMemoryJavaSourceFileObject(internalClassName, source)));
    }

    @Test
    public void testVarArgDefault() throws Exception {
        String internalClassName = IDefault.class.getName().replace('.', '/');
        String source = decompileSuccess(new ClassPathLoader(), new PlainTextPrinter(), internalClassName);

        // Check decompiled source code
        assertTrue(source.matches(PatternMaker.make("default void test(Object... o) {}")));

        // Recompile decompiled source code and check errors
        assertTrue(CompilerUtil.compile("1.8", new InMemoryJavaSourceFileObject(internalClassName, source)));
    }

    @Test
    public void testLambdaStackWalker1() throws Exception {
        try (InputStream is = this.getClass().getResourceAsStream("/jar/lambda-stackwalker-jdk17.0.1.jar")) {
            Loader loader = new CompositeLoader(is);
            String internalClassName = LambdaStackWalker1.class.getName().replace('.', '/');
            String source = decompileSuccess(loader, new PlainTextPrinter(), internalClassName);

            // Check decompiled source code
            assertTrue(source.matches(PatternMaker.make("return StackWalker.getInstance().walk(s -> s.map(StackWalker.StackFrame::getDeclaringClass).dropWhile(clazz -> !sentinelClass.equals(clazz)).findFirst().orElse(null));")));

            // Recompile decompiled source code and check errors
            assertTrue(CompilerUtil.compile("1.8", new InMemoryJavaSourceFileObject(internalClassName, source)));
        }
    }

    @Test
    public void testLambdaStackWalker2() throws Exception {
        try (InputStream is = this.getClass().getResourceAsStream("/jar/lambda-stackwalker2-jdk17.0.1.jar")) {
            Loader loader = new CompositeLoader(is);
            String internalClassName = LambdaStackWalker2.class.getName().replace('.', '/');
            String source = decompileSuccess(loader, new PlainTextPrinter(), internalClassName);

            // Check decompiled source code
            assertTrue(source.matches(PatternMaker.make("return StackWalker.getInstance().walk(s -> s.findFirst()).map(s -> s.getDeclaringClass()).orElse(null);")));
            assertTrue(source.matches(PatternMaker.make("return StackWalker.getInstance().walk(s -> s.findFirst()).map(StackWalker.StackFrame::getDeclaringClass).orElse(null);")));

            // Recompile decompiled source code and check errors
            assertTrue(CompilerUtil.compile("1.8", new InMemoryJavaSourceFileObject(internalClassName, source)));
        }
    }

    public void testNoDiamondJDK6() throws Exception {
        try (InputStream is = this.getClass().getResourceAsStream("/jar/entries-test-jdk6u119.jar")) {
            Loader loader = new CompositeLoader(is);
            String internalClassName = Entries.class.getName().replace('.', '/');
            String source = decompileSuccess(loader, new PlainTextPrinter(), internalClassName);

            // Check decompiled source code
            assertTrue(source.matches(PatternMaker.make("Map<Integer, String[]> cCache = new HashMap<Integer, String[]>();")));
            assertTrue(source.matches(PatternMaker.make("for (Map.Entry<String, String> entry : new ArrayList<Map.Entry<String, String>>(this.entries.values()))")));

            // Recompile decompiled source code and check errors
            assertTrue(CompilerUtil.compile("1.8", new InMemoryJavaSourceFileObject(internalClassName, source)));
        }
    }

    public void testNoDiamondJDK7() throws Exception {
        try (InputStream is = this.getClass().getResourceAsStream("/jar/ternary-op-diamond-jdk7u80.jar")) {
            Loader loader = new CompositeLoader(is);
            String internalClassName = TernaryOpDiamond.class.getName().replace('.', '/');
            String source = decompileSuccess(loader, new PlainTextPrinter(), internalClassName);
            
            // Check decompiled source code
            assertTrue(source.matches(PatternMaker.make("List<String> list = flag ? new ArrayList<String>() : Collections.<String>emptyList();")));
            assertTrue(source.matches(PatternMaker.make("List<String> list2 = flag ? Collections.<String>emptyList() : new ArrayList<String>();")));
            
            // Recompile decompiled source code and check errors
            assertTrue(CompilerUtil.compile("1.7", new InMemoryJavaSourceFileObject(internalClassName, source)));
        }
    }
    
    @Test
    public void testDiamond() throws Exception {
        class Entries {
            Map<String, Entry<String, String>> entries = new HashMap<>();

            @SuppressWarnings("unused")
            void test() {
                for (Map.Entry<String, String> entry : new ArrayList<>(this.entries.values())) {
                    System.out.println(entry);
                }
            }
        }
        String internalClassName = Entries.class.getName().replace('.', '/');
        String source = decompileSuccess(new ClassPathLoader(), new PlainTextPrinter(), internalClassName);

        // Check decompiled source code
        assertTrue(source.matches(PatternMaker.make("for (Map.Entry<String, String> entry : new ArrayList<>(this.entries.values()))")));

        // Recompile decompiled source code and check errors
        assertTrue(CompilerUtil.compile("1.8", new InMemoryJavaSourceFileObject(internalClassName, source)));
    }

    /*
     * TODO FIXME Eclipse compiler incompatibility
     */
    @Test
    public void testLambdaVariables() throws Exception {
        try (InputStream is = this.getClass().getResourceAsStream("/jar/misc-oracle-jdk1.8.0_331.jar")) {
            Loader loader = new CompositeLoader(is);
            String internalClassName = MiscOracleJDK8.LambdaVariables.class.getName().replace('.', '/');
            String source = decompileSuccess(loader, new PlainTextPrinter(), internalClassName);
    
            // Check decompiled source code
            assertTrue(source.matches(PatternMaker.make("void test(String str, int intger) {")));
            assertTrue(source.matches(PatternMaker.make("  char chrctr = Character.MAX_VALUE;")));
            assertTrue(source.matches(PatternMaker.make("  CharSequence chrsq = null;")));
            assertTrue(source.matches(PatternMaker.make("  List<Integer> lst = null;")));
            assertTrue(source.matches(PatternMaker.make("  Runnable r = () -> Collections.sort(lst, (a, b) -> {")));
            assertTrue(source.matches(PatternMaker.make("        System.out.print(intger);")));
            assertTrue(source.matches(PatternMaker.make("        System.out.print(chrsq);")));
            assertTrue(source.matches(PatternMaker.make("        System.out.print(str);")));
            assertTrue(source.matches(PatternMaker.make("        System.out.print(lst);")));
            assertTrue(source.matches(PatternMaker.make("        System.out.print(chrctr);")));
            assertTrue(source.matches(PatternMaker.make("        return Integer.compare(a, b);")));
    
    
            // Recompile decompiled source code and check errors
            assertTrue(CompilerUtil.compile("1.8", new InMemoryJavaSourceFileObject(internalClassName, source)));
        }
    }

    @Test
    public void testEmptyEnum() throws Exception {
        enum EmptyEnum {
            ;
            @SuppressWarnings("unused")
            static final int A = 0;
        }
        String internalClassName = EmptyEnum.class.getName().replace('.', '/');
        String source = decompileSuccess(new ClassPathLoader(), new PlainTextPrinter(), internalClassName);

        // Recompile decompiled source code and check errors
        assertTrue(CompilerUtil.compile("1.8", new InMemoryJavaSourceFileObject(internalClassName, source)));
    }

    @Test
    public void testBuilder() throws Exception {
        String internalClassName = FileAppender.class.getName().replace('.', '/');
        String source = decompileSuccess(new ClassPathLoader(), new PlainTextPrinter(), internalClassName);

        // Recompile decompiled source code and check errors
        assertTrue(CompilerUtil.compile("1.8", new InMemoryJavaSourceFileObject(internalClassName, source)));
    }

    @Test
    public void testJSONUtils() throws Exception {
        String internalClassName = JSONUtils.class.getName().replace('.', '/');
        String source = decompileSuccess(new ClassPathLoader(), new PlainTextPrinter(), internalClassName);

        // Recompile decompiled source code and check errors
        assertTrue(CompilerUtil.compile("1.8", new InMemoryJavaSourceFileObject(internalClassName, source)));
    }

    @Test
    public void testLambdaRenameVariables() throws Exception {
        class LambdaRenameVariables {
            @SuppressWarnings({ "unused", "unchecked", "rawtypes" })
            private void test(Object ref, Map map, Object key) {
                if (ref == null) {
                    Object ctx = new Object();
                    map.computeIfAbsent(key, k -> ctx);
                }
                Object ctx = new Object();
            }
        }
        String internalClassName = LambdaRenameVariables.class.getName().replace('.', '/');
        String source = decompileSuccess(new ClassPathLoader(), new PlainTextPrinter(), internalClassName);

        // Recompile decompiled source code and check errors
        assertTrue(CompilerUtil.compile("1.8", new InMemoryJavaSourceFileObject(internalClassName, source)));
    }

    @Test
    public void testCSS() throws Exception {
        String internalClassName = javax.swing.text.html.CSS.class.getName().replace('.', '/');
        String source = decompileSuccess(new ClassPathLoader(), new PlainTextPrinter(), internalClassName);

        // Recompile decompiled source code and check errors
        assertTrue(CompilerUtil.compile("1.8", new InMemoryJavaSourceFileObject(internalClassName, source)));
    }

    @Test
    public void testLimitFinder() throws Exception {
        String internalClassName = LimitFinder.class.getName().replace('.', '/');
        String source = decompileSuccess(new ClassPathLoader(), new PlainTextPrinter(), internalClassName);
        
        // Recompile decompiled source code and check errors
        assertTrue(CompilerUtil.compile("1.8", new InMemoryJavaSourceFileObject(internalClassName, source)));
    }

    @Test
    public void testHttpURLConnectionManager() throws Exception {
        String internalClassName = HttpURLConnectionManager.class.getName().replace('.', '/');
        String source = decompileSuccess(new ClassPathLoader(), new PlainTextPrinter(), internalClassName);
        
        // Recompile decompiled source code and check errors
        assertTrue(CompilerUtil.compile("1.8", new InMemoryJavaSourceFileObject(internalClassName, source)));
    }
    
    @Test
    public void testLockableFileWriter() throws Exception {
        class LockableFileWriter {
            @SuppressWarnings("unused")
            public LockableFileWriter(File file, Charset charset, boolean append, String lockDir) throws IOException {
                // init file to create/append
                file = file.getAbsoluteFile();
                if (file.getParentFile() != null) {
                    FileUtils.forceMkdir(file.getParentFile());
                }
                if (file.isDirectory()) {
                    throw new IOException("File specified is a directory");
                }
                // init lock file
                if (lockDir == null) {
                    lockDir = System.getProperty("java.io.tmpdir");
                }
                File lockDirFile = new File(lockDir);
                FileUtils.forceMkdir(lockDirFile);
            }
        }
        String internalClassName = LockableFileWriter.class.getName().replace('.', '/');
        String source = decompileSuccess(new ClassPathLoader(), new PlainTextPrinter(), internalClassName);

        // Recompile decompiled source code and check errors
        assertTrue(CompilerUtil.compile("1.8", new InMemoryJavaSourceFileObject(internalClassName, source)));
    }

    @Test
    public void testNumericConstants() throws Exception {
        String internalClassName = NumericConstants.class.getName().replace('.', '/');
        String source = decompileSuccess(new ClassPathLoader(), new PlainTextPrinter(), internalClassName);
        
        // Check decompiled source code
        assertTrue(source.matches(PatternMaker.make(":  5 */", "static final Long LONG_INT_MAX_VALUE = Long.valueOf(Integer.MAX_VALUE);")));
        assertTrue(source.matches(PatternMaker.make(":  6 */", "static final Long LONG_INT_MIN_VALUE = Long.valueOf(Integer.MIN_VALUE);")));
        assertTrue(source.matches(PatternMaker.make(":  7 */", "static final Double DOUBLE_FLOAT_MIN_VALUE = Double.valueOf(Float.MIN_VALUE);")));
        assertTrue(source.matches(PatternMaker.make(":  8 */", "static final Double DOUBLE_FLOAT_MAX_VALUE = Double.valueOf(Float.MAX_VALUE);")));
        assertTrue(source.matches(PatternMaker.make(":  9 */", "static final Float FLOAT_MIN_VALUE = Float.MIN_VALUE;")));
        assertTrue(source.matches(PatternMaker.make(": 10 */", "static final Double DOUBLE_MIN_VALUE = Double.MIN_VALUE;")));
        assertTrue(source.matches(PatternMaker.make(": 13 */", "return (d >= Integer.MIN_VALUE && d <= Integer.MAX_VALUE);")));
        assertTrue(source.matches(PatternMaker.make(": 17 */", "return (l >= Integer.MIN_VALUE && l <= Integer.MAX_VALUE);")));
        assertTrue(source.matches(PatternMaker.make(": 21 */", "return (f >= Integer.MIN_VALUE && f <= Integer.MAX_VALUE);")));
        
        // Recompile decompiled source code and check errors
        assertTrue(CompilerUtil.compile("1.8", new InMemoryJavaSourceFileObject(internalClassName, source)));
    }

    @Test
    public void testStringBuxxxer() throws Exception {
        class StringBuxxxer {
            @SuppressWarnings("unused")
            StringBuilder createStringBuilder(boolean flag) {
                return new StringBuilder().append('+').append(flag ? '+' : '-');
            }

            @SuppressWarnings("unused")
            StringBuffer createStringBuffer(boolean flag) {
                return new StringBuffer().append('+').append(flag ? '+' : '-');
            }
        }
        String internalClassName = StringBuxxxer.class.getName().replace('.', '/');
        String source = decompileSuccess(new ClassPathLoader(), new PlainTextPrinter(), internalClassName);
        
        // Check decompiled source code
        assertTrue(source.matches(PatternMaker.make("return new StringBuilder().append('+').append(flag ? '+' : '-');")));
        assertTrue(source.matches(PatternMaker.make("return new StringBuffer().append('+').append(flag ? '+' : '-');")));
        
        // Recompile decompiled source code and check errors
        assertTrue(CompilerUtil.compile("1.8", new InMemoryJavaSourceFileObject(internalClassName, source)));
        
    }

    @Test
    public void testConsumeCastExpressionLL1() throws Exception {
        String internalClassName = ConsumeCastExpressionLL1.class.getName().replace('.', '/');
        String source = decompileSuccess(new ClassPathLoader(), new PlainTextPrinter(), internalClassName);

        // Check decompiled source code
        String expected = Files.readString(Paths.get(getClass().getResource("/txt/ConsumeCastExpressionLL1.txt").toURI()));
        assertEqualsIgnoreEOL(expected, source);

        // Recompile decompiled source code and check errors
        assertTrue(CompilerUtil.compile("21", new InMemoryJavaSourceFileObject(internalClassName, source)));
    }

    @Test
    public void testCopyInputStreamToFile() throws Exception {
        class CopyInputStreamToFile {
            @SuppressWarnings("unused")
            void copyInputStreamToFile(InputStream source, File destination) throws IOException {
                try (InputStream inputStream = source) {
                    FileUtils.copyToFile(inputStream, destination);
                }
            }
        }
        String internalClassName = CopyInputStreamToFile.class.getName().replace('.', '/');
        String source = decompileSuccess(new ClassPathLoader(), new PlainTextPrinter(), internalClassName);
        
        // Check decompiled source code
        assertFalse(source.matches(PatternMaker.make("null = null;")));
        assertTrue(source.matches(PatternMaker.make("try (InputStream inputStream = source) {")));
        
        // Recompile decompiled source code and check errors
        assertTrue(CompilerUtil.compile("1.8", new InMemoryJavaSourceFileObject(internalClassName, source)));
        
    }

}
