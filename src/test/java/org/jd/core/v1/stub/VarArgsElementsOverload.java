/*
 * Copyright (c) 2026 Nicolas Baumann (@nbauma109), with Claude Sonnet 5.5.
 * This project is distributed under the GPLv3 license.
 * This is a copyleft license that gives the user the right to use,
 * copy and modify the code freely for non-commercial purposes.
 */

package org.jd.core.v1.stub;

import java.io.Serializable;
import java.util.function.Supplier;

/**
 * 'AbstractFilter' of Log4j: the overloads which take the parameters one by one pass them to the variable arity method
 */
public class VarArgsElementsOverload {
    String filter(String message, Object... params) {
        return message + params.length;
    }

    String filter(String message, Object first) {
        return filter(message, new Object[] {first});
    }

    String filter(String message, Object first, Object second) {
        return filter(message, new Object[] {first, second});
    }

    /** no overload takes three parameters: the array is not needed */
    String three(String message, Object first, Object second, Object third) {
        return filter(message, new Object[] {first, second, third});
    }

    static String of(String format, Object... values) {
        return String.format(format, values);
    }

    static String of(Integer width, String text) {
        return width + text;
    }

    /** the overload of the same arity cannot take these arguments: the array is not needed */
    String unrelated(String first, String second) {
        return of("%s %s", new Object[] {second}) + of(1, first);
    }

    String join(String separator, Object... parts) {
        return separator + parts.length;
    }

    String join(String separator, Number count) {
        return separator + count;
    }

    String join(String separator, Supplier<String> first, Supplier<String> second) {
        return separator + first.get() + second.get();
    }

    String join(String separator, Object first, Object second, Object third) {
        return join(separator, new Object[] {first, second, third});
    }

    /** 'null' can be passed as a 'Number', not as an 'int' */
    String ofNull() {
        return join("-", new Object[] {null});
    }

    /** an 'Integer' is a 'Number' */
    String ofSubtype(Integer count) {
        return join("-", new Object[] {count});
    }

    /** a primitive value is boxed to a 'Number' */
    String ofPrimitive(int count) {
        return join("-", new Object[] {count});
    }

    /** a 'String' is no 'Number': the array is not needed */
    String ofText(String text) {
        return join("-", new Object[] {text});
    }

    static String lazy(String separator, Supplier<?>... parts) {
        return separator + parts.length;
    }

    static String lazy(String separator, Object first, Object second, Object third) {
        return separator + first + second + third;
    }

    /** a method reference cannot be passed as an 'Object': the array is not needed */
    String ofReferences(String text) {
        return lazy("-", new Supplier<?>[] {text::trim, text::strip, text::intern});
    }

    static void number(Object... values) {
        // only the choice of the overload matters
    }

    static void number(int value) {
        // only the choice of the overload matters
    }

    static void serial(Object... values) {
        // only the choice of the overload matters
    }

    static void serial(Serializable value) {
        // only the choice of the overload matters
    }

    static void names(Object... values) {
        // only the choice of the overload matters
    }

    static void names(String[] value) {
        // only the choice of the overload matters
    }

    static void text(Object... values) {
        // only the choice of the overload matters
    }

    static void text(CharSequence value) {
        // only the choice of the overload matters
    }

    /** a primitive value can be passed as a primitive, or as 'Serializable' once boxed */
    void primitives(int value, char letter) {
        number(new Object[] {value});
        number(new Object[] {letter});
        serial(new Object[] {value});
    }

    /** an array of the type of the parameter can be passed to it, another one cannot */
    void arrays(String[] strings, int[] ints) {
        names(new Object[] {strings});
        names(new Object[] {ints});
        text(new Object[] {ints});
    }

    /** a sub type of the parameter can be passed to it, another type cannot */
    void types(String string, Integer integer, Object object) {
        text(new Object[] {string});
        text(new Object[] {integer});
        number(new Object[] {string});
        names(new Object[] {object});
    }

    static void sum(int... values) {
        // only the choice of the overload matters
    }

    static void sum(long value) {
        // only the choice of the overload matters
    }

    static void listed(int... values) {
        // only the choice of the overload matters
    }

    static void listed(java.util.List<?> value) {
        // only the choice of the overload matters
    }

    static void serialized(int... values) {
        // only the choice of the overload matters
    }

    static void serialized(Serializable value) {
        // only the choice of the overload matters
    }

    /** a value of a primitive varargs method can be passed to a wider primitive and to 'Serializable', not to a 'List' */
    void numbers(int value) {
        sum(new int[] {value});
        listed(new int[] {value});
        serialized(new int[] {value});
    }

    /** a value of a type variable can only be passed to 'Object' as far as the descriptors tell: the array is not needed */
    <T> void generic(T value) {
        text(new Object[] {value});
    }

    static class Child extends VarArgsElementsOverload {
        /** the overload is inherited */
        String inherited(String message, Object value) {
            return filter(message, new Object[] {value});
        }
    }
}
