package org.jd.core.v1.stub;

import java.io.Closeable;
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
    String @Maybe [] arrayItself;
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

    /** The annotated class bound is followed by an interface bound */
    public static <S extends @Maybe Object & java.io.Serializable> S sturdy(S value) {
        return value;
    }

    /** The annotation is on the array of the arguments, not on its elements */
    public static int count(String @Maybe ... arguments) {
        return arguments.length;
    }

    public static <U extends @Maybe Object> U first(U value) {
        return value;
    }

    public void put(@Maybe Object[] keys, Map.@Maybe Entry<String, String> entry, @Labeled("x") Object value) {
        nestedEntry = entry;
        annotatedElementsOfArray = keys == null ? null : new String[0];
        labeled = String.valueOf(value);
    }

    /** The first bound is an interface: the annotation has the index 1 of the bounds, the index 0 being the one of the class bound */
    public static <C extends @Maybe Closeable> C register(C closeable) {
        return closeable;
    }

    /** A local class of a static method has no outer instance: the index of the parameter of its constructor is the one of the descriptor */
    public static Object localClassOfAStaticMethod() {
        class Local {
            Local(@Maybe Object value) {
            }
        }
        return new Local(null);
    }

    static {
        class InInitializer {
            InInitializer(@Maybe String first, Object second) {
            }
        }
        Object created = new InInitializer(null, null);
        created.hashCode();
    }
}
