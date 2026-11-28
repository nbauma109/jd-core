/*
 * Copyright (c) 2026 Nicolas Baumann (@nbauma109), with Claude Sonnet 5.5.
 * This project is distributed under the GPLv3 license.
 * This is a copyleft license that gives the user the right to use,
 * copy and modify the code freely for non-commercial purposes.
 */

package org.jd.core.v1.compiler;

import org.jd.core.v1.ClassFileToJavaSourceDecompiler;
import org.jd.core.v1.api.loader.Loader;
import org.jd.core.v1.printer.PlainTextPrinter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.List;
import java.util.Map;

/**
 * The round trip of a decompiled source: the Eclipse compiler compiles it, its classes are decompiled, and that source compiles again. The code which the
 * Eclipse compiler generates reveals other bugs than the one of the compiler of the class which the test decompiles.
 * <p>
 * A source which the Eclipse compiler rejects makes no round trip. The failures of the next steps are reported in {@code target/round-trip-report.txt}; the
 * system property {@code jd.roundTrip.strict} makes them fail the test.
 */
public final class RoundTrip {
    private static final Path REPORT = Path.of("target", "round-trip-report.txt");

    /** The major version of the class files of Java 8 */
    private static final int JAVA_8 = 52;

    /** A class file of Java N has the major version N + 44 */
    private static final int MAJOR_VERSION_OFFSET = 44;

    private RoundTrip() {
    }

    /**
     * @param loader the loader of the decompiled class, which has the classes which the source refers to
     * @param majorVersion the major version of the decompiled class file
     * @return the description of the failure of the round trip, null if it succeeds or cannot be made
     */
    public static String check(Loader loader, String internalName, String source, int majorVersion, Map<String, Object> configuration) throws Exception {
        int release = Math.max(majorVersion, JAVA_8) - MAJOR_VERSION_OFFSET;

        if (release > Runtime.version().feature()) {
            return null;
        }
        String version = release == 8 ? "1.8" : Integer.toString(release);
        InMemoryClassLoader classes = new InMemoryClassLoader();

        if (!CompilerUtil.compileWithEclipse(version, classes, new InMemoryJavaSourceFileObject(internalName, source)).isEmpty()
                || !classes.canLoad(internalName)) {
            return null;
        }
        PlainTextPrinter printer = new PlainTextPrinter();

        try {
            new ClassFileToJavaSourceDecompiler().decompile(new FallbackLoader(classes, loader), printer, internalName, configuration);
        } catch (Exception | AssertionError | StackOverflowError e) {
            return "decompilation: " + e;
        }
        String second = printer.toString();

        if (second.contains("// Byte code:") || second.contains("Decompilation failed at line #")) {
            return "decompilation: a method is not decompiled";
        }
        List<String> errors = CompilerUtil.compileWithEclipse(version, new InMemoryClassLoader(), new InMemoryJavaSourceFileObject(internalName, second));

        return errors.isEmpty() ? null : "compilation: " + errors.get(0);
    }

    public static void report(String test, String internalName, String failure) throws IOException {
        String line = "[round-trip] " + test + " " + internalName + " " + failure.replace('\n', ' ');

        System.out.println(line);
        Files.createDirectories(REPORT.getParent());
        Files.writeString(REPORT, line + System.lineSeparator(), StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
    }

    /** The recompiled classes, then the ones of the loader of the test */
    private record FallbackLoader(Loader first, Loader second) implements Loader {
        @Override
        public boolean canLoad(String internalName) {
            return first.canLoad(internalName) || second.canLoad(internalName);
        }

        @Override
        public byte[] load(String internalName) throws IOException {
            return first.canLoad(internalName) ? first.load(internalName) : second.load(internalName);
        }
    }
}
