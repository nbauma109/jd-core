package org.jd.core.v1.compiler;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import javax.tools.FileObject;
import javax.tools.ForwardingJavaFileManager;
import javax.tools.JavaFileManager;
import javax.tools.JavaFileObject;
import javax.tools.JavaFileObject.Kind;
import javax.tools.StandardLocation;

public class InMemoryJavaFileManager extends ForwardingJavaFileManager<JavaFileManager> {
    private final List<InMemoryJavaSourceFileObject> sources;
    private final InMemoryClassLoader classLoader;

    protected InMemoryJavaFileManager(JavaFileManager fileManager, List<InMemoryJavaSourceFileObject> sources, InMemoryClassLoader classLoader) {
        super(fileManager);
        this.sources = sources;
        this.classLoader = classLoader;
    }

    public boolean hasLocation(Location location) {
        if (location == StandardLocation.SOURCE_PATH) {
            return true;
        }
        return super.hasLocation(location);
    }

    public boolean contains(Location location, FileObject fo) throws IOException {
        if (location == StandardLocation.SOURCE_PATH) {
            return sources.contains(fo);
        }
        return super.contains(location, fo);
    }

    public Iterable<JavaFileObject> list(Location location, String packageName, Set<Kind> kinds, boolean recurse) throws IOException {
        List<JavaFileObject> result = new ArrayList<>();
        if (location == StandardLocation.SOURCE_PATH && kinds.contains(Kind.SOURCE)) {
            for (InMemoryJavaSourceFileObject source : sources) {
                if (isSourceInPackage(source, packageName, recurse)) {
                    result.add(source);
                }
            }
        }
        if (super.hasLocation(location)) {
            Iterable<JavaFileObject> superResult = super.list(location, packageName, kinds, recurse);
            for (JavaFileObject fileObject : superResult) {
                result.add(fileObject);
            }
        }
        return result;
    }

    @Override
    public JavaFileObject getJavaFileForInput(Location location, String className, Kind kind) throws IOException {
        if (location == StandardLocation.SOURCE_PATH && kind == Kind.SOURCE) {
            String normalizedClassName = normalizeClassName(className);
            for (InMemoryJavaSourceFileObject source : sources) {
                if (source.getAbsClassName().equals(normalizedClassName)) {
                    return source;
                }
            }
        }
        if (super.hasLocation(location)) {
            return super.getJavaFileForInput(location, className, kind);
        }
        return null;
    }

    @Override
    public String inferBinaryName(Location location, JavaFileObject file) {
        if (file instanceof InMemoryJavaSourceFileObject source) {
            return source.getAbsClassName();
        }
        return super.inferBinaryName(location, file);
    }

    public JavaFileObject getJavaFileForOutput(Location location, String name, JavaFileObject.Kind kind, FileObject sibling) throws IOException {
        InMemoryJavaClassFileObject fileObject = new InMemoryJavaClassFileObject(name);
        classLoader.add(name, fileObject);
        return fileObject;
    }

    private static boolean isSourceInPackage(InMemoryJavaSourceFileObject source, String packageName, boolean recurse) {
        String sourcePackage = source.getPackageName();
        String normalizedPackage = (packageName == null) ? "" : packageName;

        if (normalizedPackage.isEmpty()) {
            return recurse || sourcePackage.isEmpty();
        }
        if (sourcePackage.equals(normalizedPackage)) {
            return true;
        }
        return recurse && sourcePackage.startsWith(normalizedPackage + ".");
    }

    private static String normalizeClassName(String className) {
        return className.replace('/', '.');
    }
}
