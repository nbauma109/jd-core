package org.jd.core.v1.stub;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.List;
import java.util.Map;

/** Annotations on types (like the '@Nullable' of JSpecify), which the class file keeps apart from the ones on declarations */
public class TypeAnnotationPatterns<T extends @Maybe Object> {
    @Target(ElementType.TYPE_USE)
    @Retention(RetentionPolicy.RUNTIME)
    public @interface Labeled {
        String value();
    }

    String name;
    @Maybe Object object;
    @Twice Object twice;
    @Deprecated @Twice @Maybe Object several;
    @Maybe T generic;
    @Maybe List<String> elements;
    @Maybe String[] annotatedElementsOfArray;
    Map.@Maybe Entry<String, String> nestedEntry;
    @Labeled("not a marker") String labeled;

    public TypeAnnotationPatterns(@Maybe String first, int second, @Maybe Object third) {
        this.name = first + second + third;
    }

    public @Maybe String find(@Maybe Object key, int count, String other, @Maybe List<String> values) {
        return key == null || count == 0 || other == null || values == null ? null : name;
    }

    public @Maybe T pick(@Maybe T value) {
        generic = value;
        return generic;
    }

    /** Two type parameters take an annotation on their bound */
    public static <A extends @Maybe Object, B extends @Maybe Object> int pair(A first, B second) {
        return java.util.Objects.hash(first, second);
    }

    public static <W extends @Maybe Comparable<W>> W max(W value) {
        return value;
    }

    public static <U extends @Maybe Object> U first(U value) {
        return value;
    }

    public void put(@Maybe Object[] keys, Map.@Maybe Entry<String, String> entry, @Labeled("x") Object value) {
        nestedEntry = entry;
        annotatedElementsOfArray = keys == null ? null : new String[0];
        labeled = String.valueOf(value);
    }
}
