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
        boolean matches = o instanceof String s && s.length() > 5;
        System.out.println(matches);
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

    public void whileWithBreak(int i) {
        while (i > 10) {
            i++;

            if (i == 15) {
                break;
            }

            System.out.println(0);
        }
    }

    private double widenedField = 0.2F;

    @SuppressWarnings("unused")
    public void floatsInContexts() {
        double local = 0.2F;
        local = 0.3F;
        java.util.function.DoubleSupplier supplier = () -> 0.2F;
        System.out.println(local + supplier.getAsDouble() + widenedField);
    }

    public void varWithAField() {
        var holder = new Object() {
            int count = 1;
        };
        holder.count++;
        System.out.println(holder.count);
    }

    public void varOfAnInterface() {
        var runner = new Runnable() {
            @Override
            public void run() {
                extra();
            }

            void extra() {
                System.out.println("extra");
            }
        };
        runner.run();
        runner.extra();
    }

    public int chainOfPatterns(Object o) {
        if (o instanceof String s) {
            return s.length();
        } else if (o instanceof Integer i) {
            return i;
        }
        return -1;
    }

    public boolean twoPatterns(Object a, Object b) {
        return a instanceof String s && b instanceof String t && s.equals(t);
    }

    public boolean negatedPatternWithThrow(Object o) {
        if (!(o instanceof String s)) {
            throw new IllegalArgumentException();
        }
        return s.isEmpty();
    }

    public String notAPattern(Object o) {
        if (o instanceof String) {
            return o.toString();
        }
        return null;
    }

    @SuppressWarnings("java:S107") // many parameters: the pattern variable is stored with a wide index
    public boolean manyLocals(int a, int b, int c, int d, Object o) {
        if (o instanceof String s) {
            return s.length() > a + b + c + d;
        }
        return false;
    }

    public void anonymousClassNeedsNoVar() {
        Object unused = new Object() {
            int field;

            void method() {
                field++;
            }
        };
        System.out.println(unused);
    }

    public void tryWhichIsNotAMatchException(Object o) {
        if (o instanceof String) {
            try {
                System.out.println(o);
            } catch (Exception e) {
                throw new IllegalStateException(e);
            }
        }
    }

    public void tryWithFinally(Object o) {
        if (o instanceof String) {
            try {
                System.out.println(o);
            } finally {
                System.out.println("finally");
            }
        }
    }

    public void tryWhichRethrowsAThrowable(Object o) {
        if (o instanceof String) {
            try {
                System.out.println(o);
            } catch (Throwable t) {
                throw new IllegalStateException(t.toString(), t);
            }
        }
    }

    public boolean negatedTernaryOfPatterns(Object o) {
        return !(o instanceof String s) || s.isEmpty();
    }

    public String assignmentToAParameter(Object o, String s) {
        if (o instanceof String) {
            s = (String) o;
        }
        return s;
    }

    public void incrementIsEvaluatedBeforeTheFirstUse(int i) {
        while (i > 0) {
            i++;

            if (i * 2 > i) {
                break;
            }

            System.out.println(i);
        }
    }

    static class ShadowedBase {
        public int n = 1;
    }

    public int fieldWhichHidesTheOneOfTheSuperType() {
        ShadowedBase base = new ShadowedBase() {
            public int n = 2;
        };
        return base.n;
    }

    public int hiddenFieldReadThroughVar() {
        var base = new ShadowedBase() {
            public int n = 2;
        };
        return base.n;
    }

    @SuppressWarnings({ "java:S2975", "java:S1182" }) // the public override of the protected 'clone' is what the call below needs
    public Object publicCloneCalledThroughVar() {
        var o = new Object() {
            @Override
            public Object clone() {
                return "ok";
            }
        };
        return o.clone();
    }

    @SuppressWarnings({ "java:S1172", "java:S1481", "java:S1854" }) // the variables only shadow the one of the enclosing method
    public int variablesOfAnInnerClassWhichShadowTheOneOfTheMethod() {
        var shadowed = new Object() {
            int added() {
                return 1;
            }
        };
        class Catching {
            void run() {
                try {
                    Integer.parseInt("x");
                } catch (NumberFormatException shadowed) {
                    System.out.println(shadowed.getMessage());
                }
            }
        }

        class Iterating {
            void run() {
                for (String shadowed : new String[] { "a" }) {
                    System.out.println(shadowed.length());
                }
            }
        }

        new Catching().run();
        new Iterating().run();
        return shadowed.added();
    }

    static class Putter {
        public String put(Object o) {
            return String.valueOf(o);
        }
    }

    @SuppressWarnings({ "java:S1172", "java:S1206", "java:S2162" }) // the overload of 'equals' is what the call below needs
    public boolean overloadOfEquals() {
        var v = new Object() {
            public boolean equals(String s) {
                return true;
            }
        };
        return v.equals("x");
    }

    @SuppressWarnings("java:S1172") // the overload has the arity of the inherited method
    public String overloadWithTheArityOfAnInheritedMethod() {
        var putter = new Putter() {
            public String put(String s) {
                return "string";
            }
        };
        return putter.put("x");
    }

    @SuppressWarnings("java:S1172") // the overload has the arity of the inherited method
    public String callOfTheInheritedOverload() {
        Putter putter = new Putter() {
            public String put(String s) {
                return "string";
            }
        };
        return putter.put((Object) "x");
    }

    @SuppressWarnings("java:S1854") // the first value of 's' is the cast which has to stay
    public Object declaredTypeIsNotThePatternType(Object o) {
        if (o instanceof String) {
            Object s = (String) o;
            s = new Object();
            return s;
        }
        return null;
    }

    static class Other {
        String added() {
            return "other";
        }
    }

    @SuppressWarnings({ "java:S1172", "java:S1854" }) // the parameter of the local class has the name of the variable, and the first value is replaced
    public String nameShadowedByALocalClass() {
        Putter named = new Putter() {
            public String added() {
                return "added";
            }
        };

        class Holder {
            String of(Other named) {
                return named.added();
            }
        }

        named = new Putter();
        return named.put("x") + new Holder().of(new Other());
    }

    @SuppressWarnings("java:S128") // the case which falls through is what the method is about
    public void loopWhoseBreakJumpsOverTheCodeAfterTheLoop(int i) {
        switch (i) {
            case 0:
                label: {
                    for (int j = 0; j < 10; j++) {
                        if (j == 3) {
                            break label;
                        }
                    }

                    System.out.println(0);
                }
                System.out.println("after");
            case 1:
                System.out.println(1);
                break;
            default:
                break;
        }

        System.out.println("after2");
    }

    static class Derived extends VineflowerRegressions {
        int n;
    }

    public int patternOfThis() {
        if (this instanceof Derived) {
            Derived derived = (Derived) this;
            return derived.n;
        }
        return -1;
    }

    interface TypeSwitchLike {
        String typeSwitch();
    }

    public TypeSwitchLike lambdaWhoseMethodIsNamedLikeTheBootstrap(Object o, int n) {
        return () -> o.toString() + n;
    }

    static class PrivateBase {
        @SuppressWarnings("unused")
        private int value() {
            return 1;
        }
    }

    public int methodWhichIsNotOverriddenBecauseItIsPrivate() {
        var base = new PrivateBase() {
            public int value() {
                return 2;
            }
        };
        return base.value();
    }

    public java.util.function.IntSupplier methodReferenceToAMethodOfTheClass() {
        var v = new Object() {
            public int added() {
                return 2;
            }
        };
        return v::added;
    }

    @SuppressWarnings("java:S1854") // the handler sees the value of 'i'
    public int incrementBeforeAnOperationWhichCanThrow(int i, int[] a) {
        try {
            while (i > 0) {
                i++;

                if (a[0] > i) {
                    break;
                }

                System.out.println(i);
            }
        } catch (NullPointerException e) {
            return i;
        }
        return -i;
    }

    public String genericPatternVariable(Object o) {
        if (o instanceof java.util.ArrayList<?> list) {
            return String.valueOf(list.size());
        }
        return null;
    }
}
