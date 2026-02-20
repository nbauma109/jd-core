package org.jd.core.v1.compiler;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;

import javax.tools.SimpleJavaFileObject;

public class InMemoryJavaSourceFileObject extends SimpleJavaFileObject {
    private final String sourceCode;
    private final String absClassName;

    public InMemoryJavaSourceFileObject(String absClassName, String sourceCode) {
        super(URI.create("memory:///" + normalizeClassName(absClassName).replace('.', '/') + ".java"), Kind.SOURCE);
        this.sourceCode = sourceCode;
        this.absClassName = normalizeClassName(absClassName);
    }

    public String getAbsClassName() {
        return absClassName;
    }

    public String getPackageName() {
        int lastDot = absClassName.lastIndexOf('.');
        return (lastDot < 0) ? "" : absClassName.substring(0, lastDot);
    }

    private static String normalizeClassName(String className) {
        return className.replace('/', '.');
    }

    @Override
    public CharSequence getCharContent(boolean ignoreEncodingErrors) throws IOException {
        return sourceCode;
    }

    @Override
    public InputStream openInputStream() throws IOException {
        return new ByteArrayInputStream(sourceCode.getBytes(StandardCharsets.UTF_8));
    }
}
