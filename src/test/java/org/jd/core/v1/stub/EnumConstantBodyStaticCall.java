package org.jd.core.v1.stub;

import java.lang.reflect.Field;

public enum EnumConstantBodyStaticCall {
    IDENTITY {
        @Override
        public String translate(Field f) {
            return f.getName();
        }
    },
    UPPER {
        @Override
        public String translate(Field f) {
            // javac qualifies these inherited static calls with the anonymous constant body class
            return upper(join(f.getName(), ' '));
        }
    };

    public abstract String translate(Field f);

    static String upper(String s) {
        return s.toUpperCase();
    }

    static String join(String s, char c) {
        return s + c;
    }
}
