package org.jd.core.v1;

import java.net.MalformedURLException;
import java.net.URL;

/*
 * Copyright (c) 2008, 2025 Emmanuel Dupuy and others.
 * This project is distributed under the GPLv3 license.
 * This is a Copyleft license that gives the user the right to use,
 * copy and modify the code freely for non-commercial purposes.
 */
public class MavenHelper {

	/**
     * Builds the URL to the JAR file on Maven Central Repository
     * given the GAV coordinates.
     *
     * @param groupId    the group ID of the artifact (e.g., "org.apache.commons")
     * @param artifactId the artifact ID (e.g., "commons-lang3")
     * @param version    the version (e.g., "3.12.0")
     * @return the URL to the JAR file on Maven Central
     * @throws MalformedURLException 
     */
    public static URL buildJarUrl(String groupId, String artifactId, String version) throws MalformedURLException {
        if (groupId == null || artifactId == null || version == null) {
            throw new IllegalArgumentException("GroupId, ArtifactId, and Version must not be null");
        }

        // Replace dots in groupId with slashes to match the Maven repository structure
        String groupPath = groupId.replace('.', '/');

        // Construct the full URL
        String baseUrl = "https://repo1.maven.org/maven2/";
        String jarFileName = artifactId + "-" + version + ".jar";

        return new URL(baseUrl + groupPath + "/" + artifactId + "/" + version + "/" + jarFileName);
    }
}
