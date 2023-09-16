package org.jd.core.v1.util;

import java.io.File;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

public final class ClasspathUtil {

    private ClasspathUtil() {
    }

    public static List<String> getJDKClasspath() {
        List<String> cpEntries = new ArrayList<>();
        String javaHome = System.getenv("JAVA_HOME");
        if (javaHome != null && !javaHome.isBlank()) {
            File jmods = new File(javaHome, "jmods");
            if (jmods.exists() && jmods.isDirectory()) {
                File[] files = jmods.listFiles();
                for (File file : files) {
                    cpEntries.add(file.getAbsolutePath());
                }
            }
            File rt = new File(javaHome, "jre/lib/rt.jar");
            if (rt.exists() && rt.isFile()) {
                cpEntries.add(rt.getAbsolutePath());
            }
        }
        return cpEntries;
    }

    public static String[] createClasspathEntries(URI jarURI, List<String> jdkClasspath) {
        List<String> cpEntries = new ArrayList<>(jdkClasspath);
        File parentFile = new File(jarURI).getParentFile();
        if (parentFile.isDirectory()) {
            File[] files = parentFile.listFiles((dir, name) -> new File(dir, name).isFile() && name.matches(".*(?<!-sources)\\.jar$"));
            if (files != null) {
                for (File file : files) {
                    cpEntries.add(file.getAbsolutePath());
                }
            }
        }
        return cpEntries.toArray(String[]::new);
    }

}
