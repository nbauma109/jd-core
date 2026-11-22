/*
 * Copyright (c) 2026 Nicolas Baumann (@nbauma109), with Claude Sonnet 5.5.
 * This project is distributed under the GPLv3 license.
 * This is a copyleft license that gives the user the right to use,
 * copy and modify the code freely for non-commercial purposes.
 */

package org.jd.core.v1.stub;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * 'ClasspathMultiReleaseJar.getModulesDeclaringPackage' of the Eclipse compiler: the variable of a for loop, then a try-with-resources whose exception
 * is kept in the slot of the variable of the loop
 */
public class EcjForDeclaration {
    Set<String> names = new HashSet<>();
    ZipFile zipFile;
    Path root;

    Set<String> plain() throws IOException {
        for (Enumeration<? extends ZipEntry> e = this.zipFile.entries(); e.hasMoreElements();) {
            this.names.add(e.nextElement().getName());
        }
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(this.root)) {
            for (final Path subdir : stream) {
                this.names.add(subdir.toString());
            }
        }
        return this.names;
    }
}
