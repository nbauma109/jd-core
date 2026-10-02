package org.jd.core.v1.stub;

import java.util.HashMap;
import java.util.Map;

/** The initializers of the fields are moved back to the fields even if the constructor starts with local variable declarations. */
public class FieldInitializersAndLocals {
    private final String name;
    private final Map<String, String> byName
            = new HashMap<>();
    private boolean debug = false;
    private final Map<String, String> byType = new HashMap<>();

    private FieldInitializersAndLocals(String name, Object source) {
        this.name = name;
        if (name != null) {
            this.debug = true;
        }
        Class<?> type;
        if (source != null) {
            type = source.getClass();
        } else {
            type = String.class;
        }
        byName.put(name, type.getName());
    }

    public static FieldInitializersAndLocals create(String name, Object source) {
        return new FieldInitializersAndLocals(name, source);
    }

    public String get(String key) {
        return byName.get(key) + byType.get(key) + debug + name;
    }
}
