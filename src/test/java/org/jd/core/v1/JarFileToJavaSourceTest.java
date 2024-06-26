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
import org.jd.core.v1.service.converter.classfiletojavasyntax.util.ExceptionUtil;
import org.jd.core.v1.util.DefaultList;
import org.jd.core.v1.util.LicenseExtractor;
import org.jd.core.v1.util.StringConstants;
import org.junit.Test;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
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
        test(org.apache.bcel.Const.class, "https://github.com/apache/commons-bcel", "commons-bcel", "rel/commons-bcel-");
    }
    
    @Test
    public void testCommonsIO() throws Exception {
        test(org.apache.commons.io.IOUtils.class, "https://github.com/apache/commons-io", "commons-io", "rel/commons-io-");
    }

    @Test
    public void testCommonsCodec() throws Exception {
        test(org.apache.commons.codec.Charsets.class, "https://github.com/apache/commons-codec", "commons-codec", "rel/commons-codec-");
    }

    @Test
    public void testCommonsCollections4() throws Exception {
        test(org.apache.commons.collections4.CollectionUtils.class);
    }

    @Test
    public void testCommonsImaging() throws Exception {
        test(org.apache.commons.imaging.Imaging.class, "https://github.com/apache/commons-imaging", "commons-imaging", "rel/commons-imaging-");
    }

    @Test
    public void testCommonsLang3() throws Exception {
        test(org.apache.commons.lang3.JavaVersion.class, "https://github.com/apache/commons-lang", "commons-lang", "rel/commons-lang-");
    }
    
//    @Test
//    public void testCommonsMath3() throws Exception {
//        test(org.apache.commons.math3.Field.class);
//    }

    @Test
    public void testDiskLruCache() throws Exception {
        test(com.jakewharton.disklrucache.DiskLruCache.class);
    }

    @Test
    public void testJavaPoet() throws Exception {
        test(com.squareup.javapoet.JavaFile.class);
    }

    @Test
    public void testJavaWriter() throws Exception {
        test(com.squareup.javawriter.JavaWriter.class);
    }

//    TODO: in progress
//    @Test
//    public void testJodaTime() throws Exception {
//        test(org.joda.time.DateTime.class);
//    }

    @Test
    public void testJSoup() throws Exception {
        test(org.jsoup.Jsoup.class);
    }

    @Test
    public void testJUnit4() throws Exception {
        test(org.junit.Test.class);
    }

    @Test
    public void testMimecraft() throws Exception {
        test(com.squareup.mimecraft.Part.class);
    }

    @Test
    public void testScribe() throws Exception {
        test(org.scribe.oauth.OAuthService.class);
    }

    @Test
    public void testSparkCore() throws Exception {
        test(spark.Spark.class);
    }

    @Test
    public void testLog4jApi() throws Exception {
        test(org.apache.logging.log4j.Logger.class);
    }

    @Test
    public void testLog4jCore() throws Exception {
        test(org.apache.logging.log4j.core.Logger.class);
    }
    
//    @Test
//    public void testGuava() throws Exception {
//        test(com.google.common.collect.Collections2.class);
//    }

    private static Properties getPomProperties(File file) {
        // Search 'META-INF/maven/*/*/pom.properties'
        try (JarFile jarFile = new JarFile(file)) {
            Enumeration<JarEntry> entries = jarFile.entries();
            while (entries.hasMoreElements()) {
                JarEntry nextEntry = entries.nextElement();
                String entryName = nextEntry.getName();
                if (entryName.startsWith("META-INF/maven/") && entryName.endsWith("/pom.properties")) {
                    try (InputStream is = jarFile.getInputStream(nextEntry)) {
                        Properties properties = new Properties();
                        properties.load(is);
                        return properties;
                    }
                }
            }
        } catch (IOException e) {
            assert ExceptionUtil.printStackTrace(e);
        }
        return null;
    }
    
    protected void test(Class<?> clazz) throws Exception {
        test(clazz, null, null, null);
    }

    protected void test(Class<?> clazz, String repo, String repoName, String tag) throws Exception {
        File file = Paths.get(clazz.getProtectionDomain().getCodeSource().getLocation().toURI()).toFile();
        Properties pomProperties = getPomProperties(file);
        String implementationVersion = pomProperties == null ? clazz.getPackage().getImplementationVersion() : pomProperties.getProperty("version");
        System.out.println("====== Decompiling and recompiling " + file.getName() + " ======");
        try (FileInputStream inputStream = new FileInputStream(file)) {
            test(inputStream, repo, repoName, tag + implementationVersion);
        }
    }
    
    protected void test(InputStream inputStream, String repo, String repoName, String tag) throws Exception {
        String license = "";
        File projectDir = null;
        if (repoName != null) {
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

        try (InputStream is = inputStream) {
            ZipLoader loader = new ZipLoader(is);
            CounterPrinter printer = new CounterPrinter();
            Map<String, Integer> statistics = new HashMap<>();
            Map<String, Object> configuration = new HashMap<>();

            configuration.put("realignLineNumbers", Boolean.TRUE);

            long time0 = System.currentTimeMillis();

            for (String path : loader.getMap().keySet()) {
                if (path.endsWith(StringConstants.CLASS_FILE_SUFFIX) && (path.indexOf('$') == -1)) {
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
                        String msg = (e.getMessage() == null) ? "<?>" : e.getMessage();
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

                    if (projectDir != null) {
                        // Write source file to source directory src/main/java
                        Files.writeString(Paths.get(projectDir.getPath() + "/src/main/java/" + internalTypeName + ".java"), source);
                    } else if (!CompilerUtil.compile(jdkVersion.toString(), new InMemoryJavaSourceFileObject(internalTypeName, source))) {
                        recompilationFailedCounter++;
                    }
                }
            }

            if (projectDir != null) {
                // Compile and run tests
                String mvnCommand = System.getProperty("os.name").toLowerCase().contains("win") ? "mvn.cmd" : "mvn";
                ProcessBuilder pbTest = new ProcessBuilder(mvnCommand, "test", "--no-transfer-progress", "-DargLine=--add-opens java.base/java.lang=ALL-UNNAMED --add-opens java.base/java.util=ALL-UNNAMED");
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
            System.out.println("  % exception             =" + (exceptionCounter * 100F / fileCounter));
            System.out.println("  % assert failed         =" + (assertFailedCounter * 100F / fileCounter));
            System.out.println("  % error in method       =" + (printer.errorInMethodCounter * 100F / printer.methodCounter));
            System.out.println("  % recompilation failed  =" + (recompilationFailedCounter * 100F / fileCounter));

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
            if ((type == METHOD) || (type == CONSTRUCTOR)) methodCounter++;
            super.printDeclaration(type, internalTypeName, name, descriptor);
        }

        @Override
        public void printReference(int type, String internalTypeName, String name, String descriptor, String ownerInternalName) {
            if ((name != null) && name.startsWith("access$")) {
                accessCounter++;
            }
            super.printReference(type, internalTypeName, name, descriptor, ownerInternalName);
        }
    }
}
