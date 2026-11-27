/*
 * Copyright (c) 2008, 2019 Emmanuel Dupuy.
 * This project is distributed under the GPLv3 license.
 * This is a Copyleft license that gives the user the right to use,
 * copy and modify the code freely for non-commercial purposes.
 */

package org.jd.core.v1;

import org.apache.bcel.Const;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.jd.core.v1.compiler.CompilerUtil;
import org.jd.core.v1.compiler.InMemoryJavaSourceFileObject;
import org.jd.core.v1.loader.ZipLoader;
import org.jd.core.v1.model.message.DecompileContext;
import org.jd.core.v1.printer.PlainTextPrinter;
import org.jd.core.v1.util.DefaultList;
import org.jd.core.v1.util.StringConstants;
import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilderFactory;

import java.io.File;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static org.apache.bcel.Const.MAJOR_1_1;
import static org.apache.bcel.Const.MAJOR_1_5;
import static org.apache.bcel.Const.MAJOR_1_8;

import jd.core.ClassUtil;

public class JarFileToJavaSourceTest extends AbstractJdTest {

    private static final Pattern COMMIT_ID = Pattern.compile("[0-9a-f]{40}");

    private static final Pattern MODULE_INFO_CLASS = Pattern.compile("(?:META-INF/versions/(\\d+)/)?module-info\\.class");

    @Test
    public void testBCEL() throws Exception {
        test("https://github.com/apache/commons-bcel", "commons-bcel", "rel/commons-bcel-", "org.apache.bcel", "bcel", classpathVersion(Const.class));
    }

    @Test
    public void testCommonsIO() throws Exception {
        String version = pomVersion("commons-io", "commons-io");
        // The tests of URIOrigin which get a page of a web site (http://google.com/), which answers 429 to the runners of the CI when they ask too often
        ProjectLayout layout = new ProjectLayout(null, ProjectLayout.DEFAULT_SOURCE_DIRECTORY, false, "test", List.of("-Dtest=!URIOriginTest#testGetInputStrea*"));
        testAtTag("https://github.com/apache/commons-io", "commons-io", "rel/commons-io-" + version, "commons-io", "commons-io", version, true, null, layout);
    }

    @Test
    public void testCommonsCodec() throws Exception {
        test("https://github.com/apache/commons-codec", "commons-codec", "rel/commons-codec-", "commons-codec", "commons-codec", pomVersion("commons-codec", "commons-codec"));
    }

    @Test
    public void testCommonsCollections4() throws Exception {
        test("https://github.com/apache/commons-collections", "commons-collections", "rel/commons-collections-", "org.apache.commons", "commons-collections4", pomVersion("org.apache.commons", "commons-collections4"));
    }

    @Test
    public void testCommonsImaging() throws Exception {
        test("https://github.com/apache/commons-imaging", "commons-imaging", "rel/commons-imaging-", "org.apache.commons", "commons-imaging", pomVersion("org.apache.commons", "commons-imaging"));
    }

    @Test
    public void testCommonsLang3() throws Exception {
        String version = classpathVersion(StringUtils.class);
        // FastTimeZoneTest.testJavadocReflectsLenientBehavior searches the Javadoc in the source file, which the decompiler cannot restore
        ProjectLayout layout = new ProjectLayout(null, ProjectLayout.DEFAULT_SOURCE_DIRECTORY, false, "test",
                List.of("-Dtest=!FastTimeZoneTest#testJavadocReflectsLenientBehavior"));
        testAtTag("https://github.com/apache/commons-lang", "commons-lang", "rel/commons-lang-" + version, "org.apache.commons", "commons-lang3", version, true, null, layout);
    }

    @Test
    public void testCommonsCompress() throws Exception {
        String version = pomVersion("org.apache.commons", "commons-compress");
        testAtTag("https://github.com/apache/commons-compress", "commons-compress", "rel/commons-compress-" + version, "org.apache.commons", "commons-compress", version, true, null);
    }

    @Test
    public void testCommonsCsv() throws Exception {
        String version = pomVersion("org.apache.commons", "commons-csv");
        testAtTag("https://github.com/apache/commons-csv", "commons-csv", "rel/commons-csv-" + version, "org.apache.commons", "commons-csv", version, true, null);
    }

    @Test
    public void testCommonsMath3() throws Exception {
        String version = pomVersion("org.apache.commons", "commons-math3");

        testAtTag("https://github.com/apache/commons-math", "commons-math", "MATH_" + version.replace('.', '_'),
                "org.apache.commons", "commons-math3", version, true, findJava8Home());
    }

    @Test
    public void testCommonsNet() throws Exception {
        String version = pomVersion("commons-net", "commons-net");
        // The examples of the library are not in its jar, and its tests use them: their sources stay
        testAtTag("https://github.com/apache/commons-net", "commons-net", "rel/commons-net-" + version, "commons-net", "commons-net", version, true, null);
    }

    @Test
    public void testCommonsText() throws Exception {
        String version = pomVersion("org.apache.commons", "commons-text");
        // One test of the entity arrays parses the lines of the source file, which carry the line numbers of the decompiler,
        // and one test of the URL lookup gets a page of a web site, which the network of the runner does not always give
        ProjectLayout layout = new ProjectLayout(null, ProjectLayout.DEFAULT_SOURCE_DIRECTORY, false, "test",
                List.of("-Dtest=!EntityArraysTest#testForDuplicatedDeclaredMapKeys,!UrlStringLookupTest#testHttpScheme"));
        testAtTag("https://github.com/apache/commons-text", "commons-text", "rel/commons-text-" + version, "org.apache.commons", "commons-text", version, true, null, layout);
    }

    @Test
    public void testDiskLruCache() throws Exception {
        String version = pomVersion("com.jakewharton", "disklrucache");
        // One test expects ThreadPoolExecutor.getTaskCount, which is documented as approximate, to be exact: the released jar gives it 2 instead of 1 too
        ProjectLayout layout = new ProjectLayout(null, ProjectLayout.DEFAULT_SOURCE_DIRECTORY, false, "test", List.of("-Dtest=!DiskLruCacheTest#shrinkMaxSizeEvicts"));
        testAtTag("https://github.com/JakeWharton/DiskLruCache", "DiskLruCache", "disklrucache-" + version, "com.jakewharton", "disklrucache", version, true, null, layout);
    }

    @Test
    public void testJavaPoet() throws Exception {
        // Its tests need Java 8: its Mockito cannot define classes on a later JDK, whose compiler also reads parameter names which a test expects to be unknown
        String version = pomVersion("com.squareup", "javapoet");
        testAtTag("https://github.com/square/javapoet", "javapoet", "javapoet-" + version, "com.squareup", "javapoet", version, true, findJava8Home());
    }

    @Test
    public void testJavaWriter() throws Exception {
        test("https://github.com/square/javapoet", "javapoet", "javawriter-", "com.squareup", "javawriter", pomVersion("com.squareup", "javawriter"));
    }

    @Test
    public void testJacksonCore() throws Exception {
        String version = pomVersion("com.fasterxml.jackson.core", "jackson-core");
        testAtTag("https://github.com/FasterXML/jackson-core", "jackson-core", "jackson-core-" + version, "com.fasterxml.jackson.core", "jackson-core", version, true, null);
    }

    @Test
    public void testJacksonDatabind() throws Exception {
        String version = pomVersion("com.fasterxml.jackson.core", "jackson-databind");
        testAtTag("https://github.com/FasterXML/jackson-databind", "jackson-databind", "jackson-databind-" + version, "com.fasterxml.jackson.core", "jackson-databind", version, true, null);
    }

    @Test
    public void testGson() throws Exception {
        // Multi-module repository: the gson module lives in the gson/ sub-directory
        String version = pomVersion("com.google.code.gson", "gson");

        testAtTag("https://github.com/google/gson", "gson", "gson-parent-" + version,
                "com.google.code.gson", "gson", version, true, null, "gson");
    }

    @Test
    public void testJodaTime() throws Exception {
        String jodaVersion = pomVersion("joda-time", "joda-time");
        test("https://github.com/JodaOrg/joda-time", "joda-time", "v", "joda-time", "joda-time", jodaVersion);

        String source = Files.readString(Paths.get(
                "target/joda-time/joda-time-" + jodaVersion + "/src/main/java/org/joda/time/format/DateTimeFormatterBuilder.java"));
        int classStart = source.indexOf("static class TimeZoneOffset");
        int methodStart = source.indexOf("public int parseInto(", classStart);
        int methodEnd = source.indexOf("private int digitCount(", methodStart);
        Matcher labels = Pattern.compile("\\blabelMerge\\d+\\s*:").matcher(source.substring(methodStart, methodEnd));
        int labelCount = 0;

        while (labels.find()) {
            labelCount++;
        }

        assertEquals(1, labelCount);
    }

    @Test
    public void testJSoup() throws Exception {
        test("https://github.com/jhy/jsoup", "jsoup", "jsoup-", "org.jsoup", "jsoup", pomVersion("org.jsoup", "jsoup"));
    }

    @Test
    public void testJUnit4() throws Exception {
        test("https://github.com/junit-team/junit4", "junit4", "r", "junit", "junit", pomVersion("junit", "junit"));
    }

    @Test
    public void testMimecraft() throws Exception {
        test("https://github.com/square/mimecraft", "mimecraft", "mimecraft-", "com.squareup.mimecraft", "mimecraft", pomVersion("com.squareup.mimecraft", "mimecraft"));
    }

    @Test
    public void testScribe() throws Exception {
        // The library uses javax.xml.bind, which the JDK has no more after Java 8
        String version = pomVersion("org.scribe", "scribe");
        // One test expects an order of the entries of a HashMap which the released jar does not give either
        ProjectLayout layout = new ProjectLayout(null, ProjectLayout.DEFAULT_SOURCE_DIRECTORY, false, "test",
                List.of("-Dtest=!HeaderExtractorTest#shouldExtractStandardHeader"));
        testAtTag("https://github.com/scribejava/scribejava", "scribejava", version, "org.scribe", "scribe", version, true, findJava8Home(), layout);
    }

    @Test
    public void testSparkCore() throws Exception {
        String version = pomVersion("com.sparkjava", "spark-core");
        // The repository has no tag for 2.9.4: this is the commit whose sources are the ones of the released jar, with the same dependencies
        String revision = "2.9.4".equals(version) ? "1973e402f5d4c1442ad34a1d38ed0758079f7773" : version;
        // Its tests use PowerMock 1.7, which needs Java 8
        testAtTag("https://github.com/perwendel/spark", "spark", revision, "com.sparkjava", "spark-core", version, true, findJava8Home());
    }

    @Test
    public void testLog4jApi() throws Exception {
        test("org.apache.logging.log4j", "log4j-api", pomVersion("org.apache.logging.log4j", "log4j-core"));
    }

    @Test
    public void testLog4jCore() throws Exception {
        test("org.apache.logging.log4j", "log4j-core", pomVersion("org.apache.logging.log4j", "log4j-core"));
    }

    // The unit tests of Guava take about 8 minutes: they are run by three jobs of about the same duration, whose fixed part (the build of the modules) is about 2 minutes.
    // The classes are selected with the property which the pom of Guava gives to its surefire plugin ('test.include'), which keeps the exclusions of the pom
    // (the PackageSanityTest, the testers and the anonymous classes, which -Dtest would run), unlike the includes of the plugin.
    private static final String GUAVA_CONCURRENT = "util/concurrent/";
    private static final String GUAVA_ABSTRACT_FUTURE = "AbstractFuture";
    private static final String GUAVA_MATH = "math/";
    private static final String GUAVA_COLLECT_TESTING = "collect/testing/";
    private static final String GUAVA_CACHE = "cache/";
    private static final String GUAVA_GRAPH = "graph/";

    @Test
    public void testGuavaAbstractFutureAndTesting() throws Exception {
        testGuava("(" + GUAVA_CONCURRENT + GUAVA_ABSTRACT_FUTURE + "|" + GUAVA_COLLECT_TESTING + ")");
    }

    @Test
    public void testGuavaConcurrentMathCacheAndGraph() throws Exception {
        testGuava("(" + GUAVA_CONCURRENT + "(?!" + GUAVA_ABSTRACT_FUTURE + ")|" + GUAVA_MATH + "|" + GUAVA_CACHE + "|" + GUAVA_GRAPH + ")");
    }

    @Test
    public void testGuavaOthers() throws Exception {
        // Whatever the other packages are, including the ones which a new version of Guava adds
        testGuava("(?!" + String.join("|", GUAVA_CONCURRENT, GUAVA_MATH, GUAVA_COLLECT_TESTING, GUAVA_CACHE, GUAVA_GRAPH) + ")");
    }

    /** @param classesToRun the regular expression of the path of the classes of the tests to run, after 'com/google/common/' */
    private void testGuava(String classesToRun) throws Exception {
        // The sources of Guava are in guava/src (not src/main/java) and its unit tests in the sibling module guava-tests (the others have no test to run)
        String version = pomVersion("com.google.guava", "guava");

        testAtTag("https://github.com/google/guava", "guava", "v" + version.replace("-jre", ""), "com.google.guava", "guava", version, true, null,
                new ProjectLayout("guava", "src", true, "package",
                        List.of("-pl", "guava-tests", "-am", "-Dtest.include=%regex[.*com/google/common/" + classesToRun + ".*\\.class]")));
    }

    /**
     * The repository at the tag has source files which are not in the released jar (the jar is built from only a part of the sources). This method
     * deletes the ones which are, and keeps the others.
     * <p>
     * The source files of the classes which are in the jar are deleted: the decompiler writes them again, and the original ones would make duplicate
     * classes (src/main/java/a/b/Foo.java is deleted if the jar has a/b/Foo.class). The source files of the classes which are in the repository but
     * not in the jar are kept: nothing replaces them, and the tests of the library may use them. For example, the sources of the package
     * org.apache.commons.net.examples are in the repository of Commons Net but not in its jar, and its tests use Main and SimpleNTPServer of this package:
     * if their sources were deleted too, the tests would not compile.
     * <p>
     * The module descriptors (module-info.java) are kept too: the decompiler does not write them (module-info.class is skipped), and the build of the
     * modules which require the library (the test library of Guava) needs them.
     */
    private static void deleteSourcesOfTheClassesOfTheJar(Path sourceDirectory, Set<String> jarEntries) throws IOException {
        try (Stream<Path> files = Files.walk(sourceDirectory)) {
            for (Path path : files.filter(file -> file.toString().endsWith(".java") && !file.endsWith("module-info.java")).toList()) {
                String relativePath = sourceDirectory.relativize(path).toString().replace(File.separatorChar, '/');

                if (jarEntries.contains(relativePath.substring(0, relativePath.length() - ".java".length()) + StringConstants.CLASS_FILE_SUFFIX)) {
                    Files.delete(path);
                }
            }
        }
    }

    /**
     * Opens the released jar of the library. The libraries tested here are test dependencies of the pom, so that Maven has already put their jar in its local
     * repository (which the CI caches): it is read from there, and downloaded from Maven Central, with some more attempts, only if it is not there.
     */
    private static InputStream openJar(String groupId, String artifactId, String version) throws IOException, InterruptedException, ExecutionException {
        String localRepository = System.getProperty("maven.repo.local", Paths.get(System.getProperty("user.home"), ".m2", "repository").toString());
        Path local = Paths.get(localRepository, groupId.replace('.', '/'), artifactId, version, artifactId + "-" + version + ".jar");

        if (Files.isRegularFile(local)) {
            return Files.newInputStream(local);
        }
        return openWithRetries(MavenHelper.buildJarUrl(groupId, artifactId, version));
    }

    private static final int DOWNLOAD_ATTEMPTS = 6;

    /**
     * Maven Central answers 429 or even 404 to the jobs of the CI which all download their jar at the same time: the download is tried again, with a longer
     * wait each time, before it fails the test.
     */
    private static InputStream openWithRetries(URL url) throws IOException, InterruptedException, ExecutionException {
        // (each attempt is scheduled after a longer delay than the previous one, instead of the test waiting)
        ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

        try {
            IOException failure = null;

            for (int attempt = 0; attempt < DOWNLOAD_ATTEMPTS; attempt++) {
                try {
                    return scheduler.schedule(url::openStream, attempt * 5L, TimeUnit.SECONDS).get();
                } catch (ExecutionException e) {
                    if (!(e.getCause() instanceof IOException ioException)) {
                        throw e;
                    }
                    failure = ioException;
                }
            }
            throw failure;
        } finally {
            scheduler.shutdown();
        }
    }

    /**
     * Version of a dependency declared in pom.xml (properties resolved), so that the library tested here is always the one
     * Dependabot bumps, instead of a copy hardcoded in this class.
     */
    private static String pomVersion(String groupId, String artifactId) {
        try {
            Document pom = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(new File("pom.xml"));
            NodeList dependencies = pom.getElementsByTagName("dependency");
            for (int i = 0; i < dependencies.getLength(); i++) {
                Element dependency = (Element) dependencies.item(i);
                if (groupId.equals(childText(dependency, "groupId")) && artifactId.equals(childText(dependency, "artifactId"))) {
                    String version = childText(dependency, "version");
                    Matcher property = Pattern.compile("\\$\\{(.+)}").matcher(version);
                    if (property.matches()) {
                        version = childText((Element) pom.getElementsByTagName("properties").item(0), property.group(1));
                    }
                    return version;
                }
            }
        } catch (Exception e) {
            throw new IllegalStateException("Cannot read pom.xml", e);
        }
        throw new IllegalArgumentException("No version of " + groupId + ":" + artifactId + " in pom.xml");
    }

    /**
     * Version of a transitive dependency (bcel and commons-lang3 come with jd-util), read from the manifest of the jar
     * which provides the class on the test classpath.
     */
    private static String classpathVersion(Class<?> libraryClass) {
        String version = libraryClass.getPackage().getImplementationVersion();
        if (version == null) {
            throw new IllegalStateException("No implementation version in the jar of " + libraryClass.getName());
        }
        return version;
    }

    private static String childText(Element parent, String name) {
        for (Node child = parent.getFirstChild(); child != null; child = child.getNextSibling()) {
            if (child instanceof Element element && name.equals(element.getTagName())) {
                return element.getTextContent().trim();
            }
        }
        return null;
    }

    protected void test(String groupId, String artifactId, String version) throws Exception {
    	test(null, null, null, groupId, artifactId, version, false);
    }

    protected void test(String repo, String repoName, String tagPrefix, String groupId, String artifactId, String version) throws Exception {
    	test(repo, repoName, tagPrefix, groupId, artifactId, version, true);
    }

    protected void test(String repo, String repoName, String tagPrefix, String groupId, String artifactId, String version, boolean runUnitTests) throws Exception {
        testAtTag(repo, repoName, tagPrefix + version, groupId, artifactId, version, runUnitTests);
    }

    protected void testAtTag(String repo, String repoName, String tag, String groupId, String artifactId, String version, boolean runUnitTests) throws Exception {
		testAtTag(repo, repoName, tag, groupId, artifactId, version, runUnitTests, null);
    }

    protected void testAtTag(String repo, String repoName, String tag, String groupId, String artifactId,
            String version, boolean runUnitTests, Path testJavaHome) throws Exception {
        testAtTag(repo, repoName, tag, groupId, artifactId, version, runUnitTests, testJavaHome, ProjectLayout.root());
    }

    /**
     * Where the sources of the library live in its repository, and how its unit tests are run.
     *
     * @param moduleDir sub-directory of the extracted repository holding the module to rebuild (for multi-module
     *                  repositories whose parent pom is at the root); {@code null} when the module is the root
     * @param sourceDirectory directory of the sources of the module, relative to the module: Maven's default is
     *                        {@code src/main/java}, but some projects (Guava) use {@code src}
     * @param runFromRepositoryRoot whether Maven runs at the root of the repository rather than in the module: the unit
     *                              tests of some projects (Guava) are in sibling modules of the one which is rebuilt
     * @param mavenPhase the last phase of the Maven build: {@code test} is enough unless a module needs the jar of another
     *                   one of the build ({@code package} for Guava, whose test library requires the module descriptor
     *                   that only the multi-release jar of Guava has)
     * @param mavenArguments more arguments of the Maven command (e.g. {@code -pl guava-tests -am})
     */
    protected record ProjectLayout(String moduleDir, String sourceDirectory, boolean runFromRepositoryRoot, String mavenPhase, List<String> mavenArguments) {
        static final String DEFAULT_SOURCE_DIRECTORY = "src/main/java";

        /** The module is the repository itself */
        static ProjectLayout root() {
            return new ProjectLayout(null, DEFAULT_SOURCE_DIRECTORY, false, "test", List.of());
        }

        /** A module of a multi-module repository, built and tested on its own */
        static ProjectLayout module(String moduleDir) {
            return new ProjectLayout(moduleDir, DEFAULT_SOURCE_DIRECTORY, false, "test", List.of());
        }
    }

    protected void testAtTag(String repo, String repoName, String tag, String groupId, String artifactId,
            String version, boolean runUnitTests, Path testJavaHome, String moduleDir) throws Exception {
        testAtTag(repo, repoName, tag, groupId, artifactId, version, runUnitTests, testJavaHome,
                moduleDir == null ? ProjectLayout.root() : ProjectLayout.module(moduleDir));
    }

    protected void testAtTag(String repo, String repoName, String tag, String groupId, String artifactId,
            String version, boolean runUnitTests, Path testJavaHome, ProjectLayout layout) throws Exception {
    	if (runUnitTests) {
    		System.out.println("====== Decompiling, recompiling and running unit tests for " + repoName + " tag " + tag + " ======");
    	} else {
    		System.out.println("====== Decompiling and recompiling " + String.join(":", groupId, artifactId, version) + " ======");
    	}
        File projectDir = null;
        File repositoryDir = null;
        if (repoName != null && runUnitTests) {
            File repoDir = new File("target/" + repoName);  // Directory for extracted project files

            // If repoDir exists, delete it
            if (repoDir.exists()) {
                try {
                    FileUtils.deleteDirectory(repoDir);
                } catch (IOException e) {
                    throw new RuntimeException("Error deleting directory " + repoDir, e);
                }
            }

            // Prepare URL of the .zip file for the tag, or for the commit of a release which has no tag
            String repoZipURL = repo + (COMMIT_ID.matcher(tag).matches() ? "/archive/" : "/archive/refs/tags/") + tag + ".zip";

            // Download and extract .zip file for the tag. The root folder name inside the archive is derived
            // from the first entry rather than guessed as "repoName-tag": GitHub strips a leading 'v' from
            // the tag for the folder name when the tag looks like a bare version (e.g. joda-time's "v2.14.2"
            // tag extracts to "joda-time-2.14.2", not "joda-time-v2.14.2").
            try (InputStream in = new URL(repoZipURL).openStream();
                 ZipInputStream zin = new ZipInputStream(in)) {

                ZipEntry entry;
                while ((entry = zin.getNextEntry()) != null) {
                    File file = new File(repoDir, entry.getName());
                    if (projectDir == null) {
                        projectDir = new File(repoDir, entry.getName().split("/")[0]);
                    }
                    if (entry.isDirectory()) {
                        file.mkdirs();
                    } else {
                        File parent = file.getParentFile();
                        if (parent != null) {
                            parent.mkdirs();
                        }
                        Files.copy(zin, file.toPath(), StandardCopyOption.REPLACE_EXISTING);
                    }
                }
            }

            repositoryDir = projectDir;
            if (layout.moduleDir() != null) {
                // Maven resolves the parent pom through the default relativePath (../pom.xml)
                projectDir = new File(projectDir, layout.moduleDir());
            }

            // Some projects generate part of their sources from templates (e.g. gson's GsonBuildConfig): the
            // decompiled class replaces the generated one, which would otherwise be a duplicate class
            File templatesDir = new File(projectDir, "src/main/java-templates");
            if (templatesDir.exists()) {
                FileUtils.deleteDirectory(templatesDir);
            }
        }

        long fileCounter = 0;
        long exceptionCounter = 0;
        long assertFailedCounter = 0;
        long recompilationFailedCounter = 0;

        try (InputStream is = openJar(groupId, artifactId, version)) {
            ZipLoader loader = new ZipLoader(is);

            if (projectDir != null && runUnitTests) {
                deleteSourcesOfTheClassesOfTheJar(Paths.get(projectDir.getPath(), layout.sourceDirectory()), loader.getMap().keySet());
            }

            CounterPrinter printer = new CounterPrinter();
            Map<String, Integer> statistics = new HashMap<>();
            Map<String, Object> configuration = new HashMap<>();

            configuration.put("realignLineNumbers", Boolean.TRUE);

            long time0 = System.currentTimeMillis();

            for (String path : loader.getMap().keySet()) {
                if (path.endsWith(StringConstants.CLASS_FILE_SUFFIX) && path.indexOf('$') == -1) {
                    String internalTypeName = ClassUtil.getInternalName(path);

                    // TODO DEBUG if (!internalTypeName.endsWith("/Debug")) continue;
                    //if (!internalTypeName.endsWith("/MapUtils")) continue;

                    if (projectDir != null && runUnitTests && path.startsWith("META-INF/versions/")) {
                        // Multi-release JAR overrides can't be recompiled correctly once flattened into a
                        // single src/main/java tree: the rebuilt project's own base-release compiler
                        // execution would choke on APIs (e.g. java.net.http) that only exist in the JDK
                        // version the override targets. Skip them before decompiling so an overlay entry
                        // can't skew the exception counters either; the base classes still get rebuilt and
                        // exercised, same as running on a JDK/consumer unaware of multi-release JARs.
                        continue;
                    }

                    if (projectDir != null && runUnitTests && Files.exists(Paths.get(projectDir.getPath(), layout.sourceDirectory(), internalTypeName + ".java.in"))) {
                        // The build generates this class from the template which stays next to it (e.g. the PackageVersion of
                        // Jackson): the decompiled class would be a duplicate of the generated one
                        continue;
                    }

                    printer.init();

                    fileCounter++;

                    DecompileContext ctx = null;
                    try {
                        // Decompile class
                        ClassFileToJavaSourceDecompiler classFileToJavaSourceDecompiler = new ClassFileToJavaSourceDecompiler();
                        ctx = classFileToJavaSourceDecompiler.decompile(loader, printer, internalTypeName, configuration);
                    } catch (AssertionError e) {
                        String msg = e.getMessage() == null ? "<?>" : e.getMessage();
                        statistics.merge(msg, 1, Integer::sum);
                        assertFailedCounter++;
                    } catch (Throwable t) {
                        System.err.println("Decompilation failed for " + internalTypeName);
                        t.printStackTrace();
                        String msg = t.getMessage() == null ? t.getClass().toString() : t.getMessage();
                        statistics.merge(msg, 1, Integer::sum);
                        exceptionCounter++;
                    }

                    String source = printer.toString();
                    StringBuilder jdkVersion = new StringBuilder();
                    Matcher m = MODULE_INFO_CLASS.matcher(path);
                    if (m.matches()) {
                        continue;
                    }
                    int majorVersion = ctx == null ? MAJOR_1_8 : ctx.getMajorVersion();
                    if (majorVersion >= MAJOR_1_1) {
                        if (majorVersion >= MAJOR_1_5) {
                            jdkVersion.append(majorVersion - (MAJOR_1_5 - 5));
                        } else {
                            jdkVersion.append(majorVersion - (MAJOR_1_1 - 1));
                        }
                    }

                    if (projectDir != null && runUnitTests) {
                        // Write source file to the source directory of the module
                        Path destinationPath = Paths.get(projectDir.getPath(), layout.sourceDirectory(), internalTypeName + ".java");
                        if (!Files.exists(destinationPath.getParent())) {
                        	destinationPath.getParent().toFile().mkdirs();
                        }
						Files.writeString(destinationPath, source);
                    } else if (!CompilerUtil.compile(jdkVersion.toString(), new InMemoryJavaSourceFileObject(internalTypeName, source))) {
                        recompilationFailedCounter++;
                    }
                }
            }

            if (projectDir != null && runUnitTests) {
                // Disable OSGi bundle manifest generation to avoid build failures in some parent POMs.
                disableBundlePlugin(Paths.get(projectDir.getPath(), "pom.xml"));
                // Some older POMs (e.g. joda-time) still target a pre-8 source/target release that modern
                // javac refuses outright ("Source option 5 is no longer supported"); bump those up to 8,
                // which every such library's own pre-8 syntax already compiles under unchanged.
                raiseObsoleteCompilerLevel(Paths.get(projectDir.getPath(), "pom.xml"));
                useStandardCompiler(Paths.get(projectDir.getPath(), "pom.xml"));
                if (layout.moduleDir() != null) {
                    relaxInheritedCompilerChecks(Paths.get(projectDir.getPath(), "pom.xml"));
                }

                // Compile and run tests
                String mvnCommand = System.getProperty("os.name").toLowerCase().contains("win") ? "mvn.cmd" : "mvn";
                String mavenRepoLocal = Paths.get("target", "m2-local").toAbsolutePath().normalize().toString();
                List<String> command = new ArrayList<>();
                command.add(mvnCommand);
                command.add("--batch-mode");
                command.add(layout.mavenPhase());
                command.add("--no-transfer-progress");
                if (testJavaHome == null) {
                    command.add("-DargLine=--add-opens java.base/java.lang=ALL-UNNAMED --add-opens java.base/java.util=ALL-UNNAMED"
                            + " --add-opens java.base/java.time=ALL-UNNAMED --add-opens java.base/java.time.chrono=ALL-UNNAMED"
                            + " --add-opens java.base/java.time.format=ALL-UNNAMED --add-opens java.base/java.time.temporal=ALL-UNNAMED"
                            + " --add-opens java.base/java.time.zone=ALL-UNNAMED --add-opens java.base/java.util.concurrent.atomic=ALL-UNNAMED");
                }
                command.add("-Danimal.sniffer.skip=true");
                // The decompiled sources have no license header: the Apache RAT check of the Commons projects would fail the build
                command.add("-Drat.skip=true");
                // The old FindBugs of some poms (Scribe), which they run when they compile, cannot read classes of Java 8
                command.add("-Dfindbugs.skip=true");
                command.add("-Dmaven.repo.local=" + mavenRepoLocal);
                // Maven Central answers 429 to the jobs of the CI which all resolve the artifacts of the library at the same time: Maven tries again, later
                command.add("-Daether.connector.http.retryHandler.count=10");
                command.add("-Daether.connector.http.retryHandler.interval=10000");
                command.add("-Daether.connector.http.retryHandler.serviceUnavailable=429,503");
                command.addAll(layout.mavenArguments());
                ProcessBuilder pbTest = new ProcessBuilder(command);
                pbTest.environment().remove("JAVA_TOOL_OPTIONS");
                if (testJavaHome != null) {
                    pbTest.environment().put("JAVA_HOME", testJavaHome.toString());
                    String path = pbTest.environment().getOrDefault("PATH", "");
                    pbTest.environment().put("PATH", testJavaHome.resolve("bin") + File.pathSeparator + path);
                }
                pbTest.directory(layout.runFromRepositoryRoot() ? repositoryDir : projectDir);
                pbTest.redirectErrorStream(true);
                Process pTest = pbTest.start();
                AtomicReference<IOException> outputForwardError = new AtomicReference<>();
                Thread outputForwarder = new Thread(() -> forwardProcessOutput(pTest, outputForwardError), "maven-output-forwarder");
                outputForwarder.start();
                int exitCode = pTest.waitFor();
                outputForwarder.join();
                if (outputForwardError.get() != null) {
                    throw outputForwardError.get();
                }

                // Check result
                assertEquals(0, exitCode);
            }

            long time9 = System.currentTimeMillis();

            System.out.println("Time: " + (time9-time0) + " ms");

            System.out.println("Counters:");
            System.out.println("  fileCounter             =" + fileCounter);
            System.out.println("  class+innerClassCounter =" + printer.classCounter);
            System.out.println("  methodCounter           =" + printer.methodCounter);
            System.out.println("  exceptionCounter        =" + exceptionCounter);
            System.out.println("  assertFailedCounter     =" + assertFailedCounter);
            System.out.println("  errorInMethodCounter    =" + printer.errorInMethodCounter);
            System.out.println("  accessCounter           =" + printer.accessCounter);
            System.out.println("  recompilationFailed     =" + recompilationFailedCounter);
            System.out.println("Percentages:");
            System.out.println("  % exception             =" + exceptionCounter * 100F / fileCounter);
            System.out.println("  % assert failed         =" + assertFailedCounter * 100F / fileCounter);
            System.out.println("  % error in method       =" + printer.errorInMethodCounter * 100F / printer.methodCounter);
            System.out.println("  % recompilation failed  =" + recompilationFailedCounter * 100F / fileCounter);

            System.out.println("Errors:");
            DefaultList<String> stats = new DefaultList<>();
            for (Map.Entry<String, Integer> stat : statistics.entrySet()) {
                stats.add("  " + stat.getValue() + " \t: " + stat.getKey());
            }
            stats.sort(Comparator.comparing(this::getCount).reversed());
            for (String stat : stats) {
                System.out.println(stat);
            }

            assertEquals(0, exceptionCounter);
            assertEquals(0, assertFailedCounter);
            assertEquals(0, printer.errorInMethodCounter);
            assertEquals(0, recompilationFailedCounter);
        }
    }

    private int getCount(String stat) {
        return Integer.parseInt(stat.substring(0, 5).trim());
    }

    private static Path findJava8Home() throws IOException {
        for (String configured : new String[] {
                System.getProperty("jd.test.java8.home"),
                System.getenv("JAVA8_HOME"),
                System.getenv("JAVA_HOME_8_X64")
        }) {
            if (configured != null && !configured.isBlank()) {
                Path home = javaHome(Paths.get(configured));
                if (home != null) {
                    return home;
                }
            }
        }

        Path candidates = Paths.get(System.getProperty("user.home"), ".sdkman", "candidates", "java");
        if (Files.isDirectory(candidates)) {
            try (var homes = Files.list(candidates)) {
                Path home = homes
                        .filter(path -> path.getFileName().toString().startsWith("8."))
                        .map(JarFileToJavaSourceTest::javaHome)
                        .filter(javaHome -> javaHome != null)
                        .sorted(Comparator.reverseOrder())
                        .findFirst()
                        .orElse(null);
                if (home != null) {
                    return home;
                }
            }
        }

        throw new IOException("Commons Math 3.6.1 tests require Java 8; set -Djd.test.java8.home or JAVA8_HOME");
    }

    private static Path javaHome(Path configured) {
        Path fileName = configured.getFileName();
        if (fileName != null && ("java".equals(fileName.toString()) || "java.exe".equals(fileName.toString()))
                && Files.isRegularFile(configured)) {
            Path bin = configured.getParent();
            return bin == null ? null : bin.getParent();
        }
        return Files.isRegularFile(configured.resolve("bin/java"))
                || Files.isRegularFile(configured.resolve("bin/java.exe")) ? configured : null;
    }

    private static void forwardProcessOutput(Process process, AtomicReference<IOException> outputForwardError) {
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                System.out.println(line);
            }
        } catch (IOException e) {
            outputForwardError.compareAndSet(null, e);
        }
    }

    /** The properties and the settings of the compiler plugin by which the poms give the compiler level */
    private static final String COMPILER_LEVEL_TAGS = "maven\\.compiler\\.(?:source|target|compilerVersion|release)|jdkVersion|java\\.version|source|target";

    private static final Pattern OBSOLETE_COMPILER_LEVEL = Pattern.compile(
            "(?<open><(?:" + COMPILER_LEVEL_TAGS + ")>)\\s*(?:1\\.[0-7]|[0-7])\\s*(?<close></(?:" + COMPILER_LEVEL_TAGS + ")>)");

    private static final Pattern ERROR_PRONE_DEPENDENCIES = Pattern.compile(
            "<dependencies>\\s*<dependency>\\s*<groupId>org\\.codehaus\\.plexus</groupId>\\s*<artifactId>plexus-compiler-javac-errorprone</artifactId>.*?</dependencies>",
            Pattern.DOTALL);

    /** The compiler of old versions of Error Prone, which some poms (JavaPoet) set, cannot read the classes of the JDK which runs the tests */
    private static void useStandardCompiler(Path pomPath) throws IOException {
        if (!Files.exists(pomPath)) {
            return;
        }

        String pom = Files.readString(pomPath);
        // (the dependencies of the compiler plugin which add it have the compiler too)
        String patched = ERROR_PRONE_DEPENDENCIES.matcher(pom.replace("<compilerId>javac-with-errorprone</compilerId>", "")).replaceFirst("");

        if (!patched.equals(pom)) {
            Files.writeString(pomPath, patched);
        }
    }

    private static void raiseObsoleteCompilerLevel(Path pomPath) throws IOException {
        if (!Files.exists(pomPath)) {
            return;
        }

        String pom = Files.readString(pomPath);
        String patched = OBSOLETE_COMPILER_LEVEL.matcher(pom).replaceAll("${open}8${close}");

        if (!patched.equals(pom)) {
            Files.writeString(pomPath, patched);
        }
    }

    /**
     * A module inheriting its compiler configuration from a parent pom may fail the build on warnings and run extra
     * static analysis (e.g. Error Prone). Decompiled sources lose source-retention annotations such as
     * {@code @SuppressWarnings}, so those checks would reject them before the unit tests run: keep plain javac
     * lint, but do not fail on warnings and drop the annotation processors.
     */
    private static void relaxInheritedCompilerChecks(Path pomPath) throws IOException {
        if (!Files.exists(pomPath)) {
            return;
        }

        String pom = Files.readString(pomPath);
        String compilerPlugin = "<artifactId>maven-compiler-plugin</artifactId>";
        int index = pom.indexOf(compilerPlugin);

        if (index < 0) {
            return;
        }

        String configuration = """

                <configuration>
                  <failOnWarning>false</failOnWarning>
                  <compilerArgs combine.self="override">
                    <arg>-Xlint:all,-options</arg>
                  </compilerArgs>
                  <annotationProcessorPaths combine.self="override" />
                </configuration>""";
        int end = index + compilerPlugin.length();

        Files.writeString(pomPath, pom.substring(0, end) + configuration + pom.substring(end));
    }

    private static void disableBundlePlugin(Path pomPath) throws IOException {
        if (!Files.exists(pomPath)) {
            return;
        }
        String pom = Files.readString(pomPath);
        if (pom.contains("maven-bundle-plugin")) {
            return;
        }

        String pluginBlock =
                """
                  <plugin>
                    <groupId>org.apache.felix</groupId>
                    <artifactId>maven-bundle-plugin</artifactId>
                    <executions>
                      <execution>
                        <id>bundle-manifest</id>
                        <phase>none</phase>
                      </execution>
                    </executions>
                  </plugin>
            """;

        int buildIndex = pom.indexOf("<build>");
        if (buildIndex >= 0) {
            int pluginsIndex = pom.indexOf("<plugins>", buildIndex);
            if (pluginsIndex >= 0) {
                int insertPos = pluginsIndex + "<plugins>".length();
                pom = pom.substring(0, insertPos) + "\n" + pluginBlock + pom.substring(insertPos);
                Files.writeString(pomPath, pom);
                return;
            }
            int buildEnd = pom.indexOf("</build>", buildIndex);
            if (buildEnd >= 0) {
                String buildBlock =
                        "  <build>\n" +
                        "    <plugins>\n" +
                        pluginBlock +
                        "    </plugins>\n" +
                        "  </build>\n";
                pom = pom.substring(0, buildEnd) + buildBlock + pom.substring(buildEnd);
                Files.writeString(pomPath, pom);
                return;
            }
        }

        int projectEnd = pom.indexOf("</project>");
        if (projectEnd >= 0) {
            String buildBlock =
                    "  <build>\n" +
                    "    <plugins>\n" +
                    pluginBlock +
                    "    </plugins>\n" +
                    "  </build>\n";
            pom = pom.substring(0, projectEnd) + buildBlock + pom.substring(projectEnd);
            Files.writeString(pomPath, pom);
        }
    }

    protected static class CounterPrinter extends PlainTextPrinter {
        public long classCounter = 0;
        public long methodCounter = 0;
        public long errorInMethodCounter = 0;
        public long accessCounter = 0;

        public CounterPrinter() {
            super(true);
        }

        @Override
        public void printText(String text) {
            if (text != null) {
                if ("// Byte code:".equals(text) || text.startsWith("/* monitor enter ") || text.startsWith("/* monitor exit ")) {
                    errorInMethodCounter++;
                }
                super.printText(text);
            }
        }

        @Override
        public void printDeclaration(int type, String internalTypeName, String name, String descriptor) {
            if (type == TYPE) classCounter++;
            if (type == METHOD || type == CONSTRUCTOR) methodCounter++;
            super.printDeclaration(type, internalTypeName, name, descriptor);
        }

        @Override
        public void printReference(int type, String internalTypeName, String name, String descriptor, String ownerInternalName) {
            if (name != null && name.startsWith("access$")) {
                accessCounter++;
            }
            super.printReference(type, internalTypeName, name, descriptor, ownerInternalName);
        }
    }
}
