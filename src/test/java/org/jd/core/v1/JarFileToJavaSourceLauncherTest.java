/*
 * Copyright (c) 2008, 2019 Emmanuel Dupuy.
 * This project is distributed under the GPLv3 license.
 * This is a Copyleft license that gives the user the right to use,
 * copy and modify the code freely for non-commercial purposes.
 */

package org.jd.core.v1;

import org.junit.Test;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class JarFileToJavaSourceLauncherTest {
    @Test
    public void testNestedMavenLaunchPassesNestedJvmOpensViaArgLine() {
        ProcessBuilder processBuilder = JarFileToJavaSourceTest.newProjectTestProcessBuilder(
                "mvn",
                "/tmp/m2-local",
                new File("."));

        assertTrue(processBuilder.command().stream().anyMatch(argument ->
                argument.startsWith("-DargLine=")
                        && argument.contains("java.base/java.time=ALL-UNNAMED")));
        assertFalse(processBuilder.environment().containsKey("JDK_JAVA_OPTIONS"));
        assertEquals(new File("."), processBuilder.directory());
    }

    @Test
    public void testAppendJvmOptionsAppendsWithoutDroppingExistingOptions() {
        Map<String, String> environment = new HashMap<>();
        environment.put("JDK_JAVA_OPTIONS", "-Xmx512m");

        JarFileToJavaSourceTest.appendJvmOptions(environment, "JDK_JAVA_OPTIONS", JarFileToJavaSourceTest.NESTED_TEST_JAVA_OPTIONS);

        assertTrue(environment.get("JDK_JAVA_OPTIONS").startsWith("-Xmx512m "));
        assertTrue(environment.get("JDK_JAVA_OPTIONS").contains("java.base/java.lang=ALL-UNNAMED"));
    }
}
