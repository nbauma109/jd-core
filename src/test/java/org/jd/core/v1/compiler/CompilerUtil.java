/*
 * Copyright (c) 2008, 2019 Emmanuel Dupuy.
 * This project is distributed under the GPLv3 license.
 * This is a Copyleft license that gives the user the right to use,
 * copy and modify the code freely for non-commercial purposes.
 */

package org.jd.core.v1.compiler;

import org.eclipse.jdt.internal.compiler.tool.EclipseCompiler;

import java.io.File;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import javax.tools.Diagnostic;
import javax.tools.DiagnosticCollector;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;

public class CompilerUtil {
    protected static final File DESTINATION_DIRECTORY = new File("target/test-recompiled");
    protected static final String DESTINATION_DIRECTORY_PATH = DESTINATION_DIRECTORY.getAbsolutePath();

    private CompilerUtil() {
    }

    public static boolean compile(String preferredJavaVersion, InMemoryJavaSourceFileObject... javaFileObjects) throws Exception {
        InMemoryClassLoader classLoader = new InMemoryClassLoader();
        return compile(preferredJavaVersion, classLoader, javaFileObjects);
    }
    
    public static boolean compile(String preferredJavaVersion, InMemoryClassLoader classLoader, InMemoryJavaSourceFileObject... javaFileObjects) throws Exception {
        String javaVersion = getJavaVersion(preferredJavaVersion);

        DESTINATION_DIRECTORY.mkdirs();

        List<String> javacOptions = Arrays.asList("-g", "-source", javaVersion, "-target", javaVersion, "-d", DESTINATION_DIRECTORY_PATH, "-cp", System.getProperty("java.class.path"));
        List<String> ecjOptions = Arrays.asList(
                "-g",
                "-source", javaVersion,
                "-target", javaVersion,
                toEcjComplianceOption(javaVersion),
                "-d", DESTINATION_DIRECTORY_PATH,
                "-cp", System.getProperty("java.class.path"));
        List<InMemoryJavaSourceFileObject> compilationUnits = Arrays.asList(javaFileObjects);

        CompilationResult result = compileWithCompiler(new EclipseCompiler(), ecjOptions, compilationUnits, classLoader);

        if (!result.compilationSuccess) {
            JavaCompiler javacCompiler = ToolProvider.getSystemJavaCompiler();
            if (javacCompiler != null) {
                classLoader.classes.clear();
                result = compileWithCompiler(javacCompiler, javacOptions, compilationUnits, classLoader);
            }
        }

        printDiagnostics(result.diagnostics, javaFileObjects, compilationUnits);
        return result.compilationSuccess;
    }

    private static CompilationResult compileWithCompiler(
            JavaCompiler compiler,
            List<String> options,
            List<InMemoryJavaSourceFileObject> compilationUnits,
            InMemoryClassLoader classLoader) throws Exception {
        StringWriter writer = new StringWriter();
        DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<>();
        boolean compilationSuccess;
        try (StandardJavaFileManager standardFileManager = compiler.getStandardFileManager(diagnostics, Locale.US, StandardCharsets.UTF_8)) {
            try (InMemoryJavaFileManager fileManager = new InMemoryJavaFileManager(standardFileManager, compilationUnits, classLoader)) {
                compilationSuccess = compiler.getTask(writer, fileManager, diagnostics, options, null, compilationUnits).call();
            }
        }
        return new CompilationResult(compilationSuccess, diagnostics);
    }

    private static void printDiagnostics(
            DiagnosticCollector<JavaFileObject> diagnostics,
            InMemoryJavaSourceFileObject[] javaFileObjects,
            List<InMemoryJavaSourceFileObject> compilationUnits) throws Exception {
        if (diagnostics.getDiagnostics().isEmpty()) {
            return;
        }
        StringBuilder sb = new StringBuilder();
        Set<Long> lineNumbers = new HashSet<>();
        for (Diagnostic<? extends JavaFileObject> d : diagnostics.getDiagnostics()) {
            switch (d.getKind()) {
                case NOTE:
                case WARNING:
                case OTHER:
                    break;
                default:
                    if (d.getLineNumber() > 0) {
                        sb.append(String.format("%-7s - line %-4d- %s%n", d.getKind(), d.getLineNumber(), d.getMessage(null)));
                        if (!lineNumbers.contains(d.getLineNumber())) {
                            System.out.println(javaFileObjects[0].getCharContent(true).toString().split("\n")[(int) (d.getLineNumber() - 1)]);
                            lineNumbers.add(d.getLineNumber());
                        }
                    } else {
                        sb.append(String.format("%-7s -          - %s%n", d.getKind(), d.getMessage(null)));
                    }
                    break;
            }
        }

        if (sb.length() > 0) {
            System.err.println(compilationUnits.get(0).getName());
            System.err.print(sb.toString());
        }
    }

    private record CompilationResult(boolean compilationSuccess, DiagnosticCollector<JavaFileObject> diagnostics) {
    }

    private static String getJavaVersion(String preferredJavaVersion) {
        int numericSystemJavaVersion = parseJavaVersion(System.getProperty("java.version"));

        if (numericSystemJavaVersion <= 8) {
            return preferredJavaVersion;
        }
        int numericPreferredJavaVersion = parseJavaVersion(preferredJavaVersion);

        if (numericPreferredJavaVersion < 8) {
            return "1.8";
        }
        return preferredJavaVersion;
    }

    private static int parseJavaVersion(String javaVersion) {
        if(javaVersion.startsWith("1.")) {
            javaVersion = javaVersion.substring(2, 3);
        } else {
            int index = javaVersion.indexOf(".");

            if(index != -1) {
                javaVersion = javaVersion.substring(0, index);
            }
        }

        return Integer.parseInt(javaVersion);
    }

    private static String toEcjComplianceOption(String javaVersion) {
        int numericJavaVersion = parseJavaVersion(javaVersion);
        if (numericJavaVersion <= 8) {
            return "-1." + numericJavaVersion;
        }
        return "-" + numericJavaVersion;
    }
}
