/*
 * Copyright (c) 2008, 2019 Emmanuel Dupuy.
 * This project is distributed under the GPLv3 license.
 * This is a Copyleft license that gives the user the right to use,
 * copy and modify the code freely for non-commercial purposes.
 */

package org.jd.core.v1;

import org.apache.commons.io.FileUtils;
import org.jd.core.v1.compiler.CompilerUtil;
import org.jd.core.v1.compiler.InMemoryJavaSourceFileObject;
import org.jd.core.v1.loader.ZipLoader;
import org.jd.core.v1.model.message.DecompileContext;
import org.jd.core.v1.printer.PlainTextPrinter;
import org.jd.core.v1.util.DefaultList;
import org.jd.core.v1.util.LicenseExtractor;
import org.jd.core.v1.util.StringConstants;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static org.apache.bcel.Const.MAJOR_1_1;
import static org.apache.bcel.Const.MAJOR_1_5;
import static org.apache.bcel.Const.MAJOR_1_8;

import jd.core.ClassUtil;

public class JarFileToJavaSourceTest extends AbstractJdTest {

    private static final Pattern MODULE_INFO_CLASS = Pattern.compile("META-INF/versions/(\\d+)/module-info\\.class");

    @Test
    public void testBCEL() throws Exception {
        test("https://github.com/apache/commons-bcel", "commons-bcel", "rel/commons-bcel-", "org.apache.bcel", "bcel", "6.7.0");
    }

    @Test
    public void testCommonsIO() throws Exception {
        test("https://github.com/apache/commons-io", "commons-io", "rel/commons-io-", "commons-io", "commons-io", "2.15.0");
    }

    @Test
    public void testCommonsCodec() throws Exception {
        test("https://github.com/apache/commons-codec", "commons-codec", "rel/commons-codec-", "commons-codec", "commons-codec", "1.18.0");
    }

    @Test
    public void testCommonsCollections4() throws Exception {
        test("https://github.com/apache/commons-collections", "commons-collections", "commons-commons-collections-", "org.apache.commons", "commons-collections4", "4.4");
    }

    @Test
    public void testCommonsImaging() throws Exception {
        test("https://github.com/apache/commons-imaging", "commons-imaging", "rel/commons-imaging-", "org.apache.commons", "commons-imaging", "1.0-alpha3");
    }

    @Test
    public void testCommonsLang3() throws Exception {
        test("https://github.com/apache/commons-lang", "commons-lang", "rel/commons-lang-", "org.apache.commons", "commons-lang3", "3.12.0");
    }

//    @Test
//    public void testCommonsMath3() throws Exception {
//        test(org.apache.commons.math3.Field.class);
//    }

    @Test
    public void testDiskLruCache() throws Exception {
        test("https://github.com/JakeWharton/DiskLruCache", "DiskLruCache", "disklrucache-", "com.jakewharton", "disklrucache", "2.0.2", false);
    }

    @Test
    public void testJavaPoet() throws Exception {
        test("https://github.com/square/javapoet", "javapoet", "javapoet-", "com.squareup", "javapoet", "1.13.0", false);
    }

    @Test
    public void testJavaWriter() throws Exception {
        test("https://github.com/square/javapoet", "javapoet", "javawriter-", "com.squareup", "javawriter", "2.5.1", false);
    }

//    TODO: in progress
//    @Test
//    public void testJodaTime() throws Exception {
//        test(org.joda.time.DateTime.class);
//    }

    @Test
    public void testJSoup() throws Exception {
        test("https://github.com/jhy/jsoup", "jsoup", "jsoup-", "org.jsoup", "jsoup", "1.16.2", false);
    }

    @Test
    public void testJUnit4() throws Exception {
        test("https://github.com/junit-team/junit4", "junit4", "r", "junit", "junit", "4.13.2", false);
    }

    @Test
    public void testMimecraft() throws Exception {
        test("https://github.com/square/mimecraft", "mimecraft", "mimecraft-", "com.squareup.mimecraft", "mimecraft", "1.1.1", false);
    }

    @Test
    public void testScribe() throws Exception {
        test("https://github.com/scribejava/scribejava", "scribejava", "", "org.scribe", "scribe", "1.3.7", false);
    }

    @Test
    public void testSparkCore() throws Exception {
        test("https://github.com/perwendel/spark", "spark", "", "com.sparkjava", "spark-core", "2.9.4", false);
    }

    @Test
    public void testLog4jApi() throws Exception {
        test("org.apache.logging.log4j", "log4j-api", "2.20.0");
    }

    @Test
    public void testLog4jCore() throws Exception {
        test("org.apache.logging.log4j", "log4j-core", "2.20.0");
    }

//    @Test
//    public void testGuava() throws Exception {
//        test(com.google.common.collect.Collections2.class);
//    }

    protected void test(String groupId, String artifactId, String version) throws Exception {
    	test(null, null, null, groupId, artifactId, version, false);
    }

    protected void test(String repo, String repoName, String tagPrefix, String groupId, String artifactId, String version) throws Exception {
    	test(repo, repoName, tagPrefix, groupId, artifactId, version, true);
    }

    protected void test(String repo, String repoName, String tagPrefix, String groupId, String artifactId, String version, boolean runUnitTests) throws Exception {
    	String tag = tagPrefix + version;
    	if (runUnitTests) {
    		System.out.println("====== Decompiling, recompiling and running unit tests for " + repoName + " tag " + tag + " ======");
    	} else {
    		System.out.println("====== Decompiling and recompiling " + String.join(":", groupId, artifactId, version) + " ======");
    	}
        String license = "";
        File projectDir = null;
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

            // Prepare URL of the .zip file for the tag
            String repoZipURL = repo + "/archive/refs/tags/" + tag + ".zip";

            // Directory where the project files are extracted
            projectDir = new File(repoDir, repoName + "-" + tag.replace("/", "-"));

            // Download and extract .zip file for the tag
            try (InputStream in = new URL(repoZipURL).openStream();
                 ZipInputStream zin = new ZipInputStream(in)) {

                ZipEntry entry;
                while ((entry = zin.getNextEntry()) != null) {
                    File file = new File(repoDir, entry.getName());
                    if (entry.isDirectory()) {
                        file.mkdirs();
                    } else {
                        File parent = file.getParentFile();
                        if (parent != null) {
                            parent.mkdirs();
                        }
                        Files.copy(zin, file.toPath(), StandardCopyOption.REPLACE_EXISTING);
                        if (license.isEmpty() && file.getName().endsWith(".java")) {
                            license = LicenseExtractor.extractLicense(file.toPath());
                        }
                    }
                }
            }

            // Delete all .java files in src/main/java
            Files.walk(Paths.get(projectDir.getPath() + "/src/main/java"))
                    .filter(path -> path.toString().endsWith(".java"))
                    .forEach(path -> {
                        try {
                            Files.delete(path);
                        } catch (IOException e) {
                            throw new RuntimeException("Error deleting .java files", e);
                        }
                    });
        }

        long fileCounter = 0;
        long exceptionCounter = 0;
        long assertFailedCounter = 0;
        long recompilationFailedCounter = 0;

        try (InputStream is = MavenHelper.buildJarUrl(groupId, artifactId, version).openStream()) {
            ZipLoader loader = new ZipLoader(is);
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

                    printer.init();
                    if (!license.isEmpty()) {
                        printer.printText(license);
                        printer.endLine();
                    }

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
                        // Write source file to source directory src/main/java
                        Path destinationPath = Paths.get(projectDir.getPath() + "/src/main/java/" + internalTypeName + ".java");
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

                // Compile and run tests
                String mvnCommand = System.getProperty("os.name").toLowerCase().contains("win") ? "mvn.cmd" : "mvn";
                ProcessBuilder pbTest = new ProcessBuilder(
                        mvnCommand,
                        "--batch-mode",
                        "test",
                        "--no-transfer-progress",
                        "-DargLine=--add-opens java.base/java.lang=ALL-UNNAMED --add-opens java.base/java.util=ALL-UNNAMED",
                        "-Danimal.sniffer.skip=true",
                        "-Dmaven.repo.local=" + Paths.get(projectDir.getPath(), "target", "m2").toString()
                );
                pbTest.environment().remove("JAVA_TOOL_OPTIONS");
                pbTest.directory(projectDir);
                pbTest.redirectOutput(ProcessBuilder.Redirect.INHERIT);
                pbTest.redirectError(ProcessBuilder.Redirect.INHERIT);
                Process pTest = pbTest.start();
                int exitCode = pTest.waitFor();

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
