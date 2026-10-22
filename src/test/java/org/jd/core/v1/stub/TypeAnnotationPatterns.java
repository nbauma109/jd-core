package org.jd.core.v1.stub;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.List;
import java.util.Map;

/** Annotations on types (like the '@Nullable' of JSpecify), which the class file keeps apart from the ones on declarations */
public class TypeAnnotationPatterns {
    @Target(ElementType.TYPE_USE)
    @Retention(RetentionPolicy.RUNTIME)
    public @interface Maybe {
    }

    @Target(ElementType.TYPE_USE)
    @Retention(RetentionPolicy.RUNTIME)
    public @interface Labeled {
        String value();
    }

    @Maybe String name;
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

    public void put(@Maybe Object[] keys, Map.@Maybe Entry<String, String> entry, @Labeled("x") Object value) {
        nestedEntry = entry;
        annotatedElementsOfArray = keys == null ? null : new String[0];
        labeled = String.valueOf(value);
    }
}
