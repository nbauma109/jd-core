package org.jd.core.v1;

public class GenericParameterMethod {
    static void use(Integer i) {
        System.out.println("use(Integer)");
    }

    static <T> void use(T t) {
        System.out.println("use(T)");
    }

    public static void main(String... args) {
        use(1);
        use((Object) 1); // Calls use(T)
    }
}