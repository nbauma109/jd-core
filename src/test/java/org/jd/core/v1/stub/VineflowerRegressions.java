/*
 * Copyright (c) 2026 Nicolas Baumann (@nbauma109), with Claude Sonnet 5.5.
 * This project is distributed under the GPLv3 license.
 * This is a Copyleft license that gives the user the right to use,
 * copy and modify the code freely for non-commercial purposes.
 */

package org.jd.core.v1.stub;

public class VineflowerRegressions {

    public double widenedFloat() {
        return 0.2F;
    }

    private double a;
    private double b;

    public void chainedWidenedFloat() {
        a = b = 0.2F;
    }

    public double widenedFloatFieldAsValue() {
        return a = 0.2F;
    }

    public Object boxedDouble() {
        return 0.20000000298023224D;
    }

    public String overloaded() {
        return which(0.20000000298023224D);
    }

    @SuppressWarnings({ "java:S1144", "java:S1172" }) // overloads which only exist to check the selected one
    private String which(float f) {
        return "float";
    }

    @SuppressWarnings({ "java:S1144", "java:S1172" }) // overloads which only exist to check the selected one
    private String which(double d) {
        return "double";
    }

    public String switchExpression(int i) {
        return switch (i) {
            case 1 -> "1";
            case 2 -> "2";
            default -> "Unknown";
        };
    }

    @SuppressWarnings("java:S9435") // the negated condition is the pattern under test
    public boolean negatedAnd(double n) {
        return !((n < 1.0 - n) && n > 3) ? n < 5.0 : n > 2;
    }

    public boolean or(double n) {
        return ((n < 1.0 - n) || n > 3) ? n < 5.0 : n > 2;
    }

    public boolean and(double n) {
        return ((n < 1.0 - n) && n > 3) ? n < 5.0 : n > 2;
    }

    public enum DoubleBraceEnum {
        A {{ System.out.println("A"); }},
        B {{ System.out.println("B"); }}
    }

    @SuppressWarnings("unused")
    public void localTypes(int i) {
        enum Type {
            VALID,
            INVALID
        }

        interface Named {
            String name();
        }

        record Point(int x, int y) {}

        Type type = i == 0 ? Type.VALID : Type.INVALID;
        Named named = type::name;
        System.out.println(named.name() + new Point(i, i).x());
    }

    public void namelessLocalClass() {
        var printer = new Object() {
            void println(String s) {
                System.out.println(s);
            }
        };
        printer.println("goodbye, world!");
    }

    public void overriddenMembersNeedNoVar() {
        Object object = new Object() {
            @Override
            public String toString() {
                return "overridden";
            }
        };
        System.out.println(object.toString());
    }

    void accept(Object o, boolean b) {
        System.out.println(o + " " + b);
    }

    public void patternInArgument(Object o) {
        accept(o, o instanceof String s && s.length() > 5);
    }

    public void patternInLocal(Object o) {
        boolean b = o instanceof String s && s.length() > 5;
        System.out.println(b);
    }

    public boolean patternInTernary(Object o) {
        return o instanceof String s ? s.isEmpty() : false;
    }

    public boolean patternBoundAfterTheTest(Object o) {
        if (!(o instanceof String s)) {
            return false;
        }
        return s.isEmpty();
    }

    @SuppressWarnings("unused")
    public void discardedBox(int n) {
        Boolean.valueOf(n < 5 && n > 0);
    }
}
