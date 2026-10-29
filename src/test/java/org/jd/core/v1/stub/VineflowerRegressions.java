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

    public String uncheckedGenericCast(Object o) {
        if (o instanceof java.util.List) {
            @SuppressWarnings("unchecked")
            java.util.List<String> strings = (java.util.List<String>) o;
            return strings.get(0);
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    public String uncheckedCastOfAWildcard(java.util.Collection<?> o) {
        if (o instanceof java.util.List) {
            java.util.List<String> strings = (java.util.List<String>) o;
            return strings.get(0);
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    public String checkedCastOfTheSameArguments(java.util.Collection<String> o) {
        if (o instanceof java.util.List) {
            java.util.List<String> strings = (java.util.List<String>) o;
            return strings.get(0);
        }
        return null;
    }

    public String castAssignedInBothBranches(Object o) {
        String s;
        if (o instanceof String) {
            s = (String) o;
        } else {
            s = "fallback";
        }
        return s;
    }

    static class Holder {
        Putter target;
    }

    @SuppressWarnings({ "java:S1854", "java:S1172" }) // the field of the holder is not the variable which is assigned afterwards
    public String fieldNamedLikeTheVariable(Holder h) {
        Putter target = new Putter() {
            public String added() {
                return "added";
            }
        };
        String result = h.target.put("x");
        target = new Putter();
        return result + target.put("y");
    }

    public String castAssignedByTwoTests(Object a, Object b) {
        String s;
        if (a instanceof String) {
            s = (String) a;
        } else if (b instanceof String) {
            s = (String) b;
        } else {
            return "fallback";
        }
        return s;
    }

    @SuppressWarnings("unchecked")
    public String genericArrayPattern(java.util.Collection<String>[] o) {
        if (o instanceof java.util.List[]) {
            java.util.List<String>[] lists = (java.util.List<String>[]) o;
            return lists[0].get(0);
        }
        return null;
    }

    static class ProtectedBase {
        protected int widened() {
            return 1;
        }
    }

    @SuppressWarnings("java:S1854") // the variable is assigned again with the super type
    public int widenedOverrideCalledThroughTheSuperType() {
        ProtectedBase base = new ProtectedBase() {
            @Override
            public int widened() {
                return 2;
            }
        };
        int n = base.widened();
        base = new ProtectedBase();
        return n + base.widened();
    }

    public int anonymousVariableOfALoop() {
        int count = 0;

        for (var counter = new Object() { int limit() { return 3; } }; counter.limit() > count; count++) {
            System.out.println(count);
        }
        return count;
    }

    public int aliasOfAnAnonymousVariable() {
        var origin = new Object() {
            int added() {
                return 2;
            }
        };
        var alias = origin;
        return alias.added();
    }

    @SuppressWarnings({ "java:S1854", "java:S1196" }) // the alias is assigned again with another value: it keeps the type of Object
    public int explicitAliasKeepsItsType() {
        var holder = new Object() {
            int added() {
                return 2;
            }
        };
        Object copy = holder;
        copy = new Object();
        return holder.added() + copy.hashCode() * 0;
    }

    @SuppressWarnings("java:S1117") // the block declares the name of the captured variable on purpose
    public int blockWhichShadowsTheCapturedVariable() {
        var anon = new Object() {
            int added() {
                return 2;
            }
        };

        class Inner {
            int value(boolean flag) {
                if (flag) {
                    int anon = 1;
                    System.out.println(anon);
                }
                return anon.added();
            }
        }

        return new Inner().value(true);
    }

    @SuppressWarnings("java:S1659") // both variables are declared on the same line on purpose: the declarations are merged
    public int twoAnonymousVariablesOnOneLine() {
        var first = new Object() { int n = 1; }; var second = new Object() { int n = 2; };
        return first.n + second.n;
    }

    @SuppressWarnings("java:S1854") // the second block assigns the alias again with the super type
    public String blocksWhichReuseTheNameOfAVariable(boolean flag) {
        if (flag) {
            var shared = new Object() {
                String added() {
                    return "anonymous";
                }
            };
            return shared.added();
        } else {
            OtherSub shared = new OtherSub();
            Other alias = shared;
            alias = new Other();
            return alias.added();
        }
    }

    @SuppressWarnings("java:S1172") // the parameter of the nested class is not the one of the method
    public int parameterOfANestedClass() {
        var outer = new Object() {
            int added() {
                return 2;
            }
        };

        class Local {
            int value() {
                class Nested {
                    int get(int outer) {
                        return outer;
                    }
                }
                return outer.added() + new Nested().get(0);
            }
        }

        return new Local().value();
    }

    @SuppressWarnings({ "java:S1854", "java:S1172" }) // the field of the local class is not the variable which is assigned afterwards
    public String fieldOfALocalClassNamedLikeTheVariable() {
        Putter target = new Putter() {
            public String added() {
                return "added";
            }
        };

        class WithField {
            final Other target = new Other();

            String value() {
                return target.added();
            }
        }

        String result = new WithField().value();
        target = new Putter();
        return result + target.put("x");
    }

    public int aliasCapturedByALocalClass() {
        var base = new Object() {
            int added() {
                return 2;
            }
        };
        var copy = base;

        class Reading {
            int value() {
                return copy.added();
            }
        }

        return new Reading().value();
    }

    static class Reading {
        int read() throws java.io.IOException {
            return 1;
        }
    }

    public int overrideWhichNarrowsTheExceptions() {
        var reader = new Reading() {
            @Override
            int read() {
                return 2;
            }
        };
        return reader.read();
    }

    public int resourceOfAnAnonymousClass() {
        try (var resource = new AutoCloseable() {
            @Override
            public void close() {
                // nothing to release
            }

            int added() {
                return 2;
            }
        }) {
            return resource.added();
        }
    }

    public int resourceWhichIsClosedByTheTry() {
        try (var resource = new AutoCloseable() {
            @Override
            public void close() {
                // nothing to release
            }
        }) {
            return 1;
        }
    }

    static class CloneBase implements Cloneable {
        @Override
        public Object clone() throws CloneNotSupportedException {
            return super.clone();
        }
    }

    public Object overrideOfCloneWhichDeclaresNoException() {
        var copy = new CloneBase() {
            @Override
            public Object clone() {
                return new Object();
            }
        };
        return copy.clone();
    }

    @SuppressWarnings("java:S1659") // the aliases are declared on one line on purpose: the declarations are merged
    public int mergedAliases() {
        var origin = new Object() { int n = 1; };
        var first = origin; var second = origin;
        return first.n + second.n;
    }

    public int anonymousTypeThroughAGenericCall() {
        var value = java.util.Objects.requireNonNull(new Object() {
            int added() {
                return 2;
            }
        });
        return value.added();
    }

    static class Listing {
        public java.util.List<?> values() {
            return java.util.List.of();
        }
    }

    public java.util.List<String> overrideWhichRefinesTheTypeArguments() {
        var listing = new Listing() {
            @Override
            public java.util.List<String> values() {
                return java.util.List.of("x");
            }
        };
        return listing.values();
    }

    public int aliasInTheDeclarationOfALoop() {
        var origin = new Object() { int n = 2; };
        int count = 0;

        for (var counter = origin; counter.n > count; count++) { System.out.println(count); }
        return count;
    }

    public int aliasThroughAGenericCall() {
        var source = new Object() {
            int added() {
                return 2;
            }
        };
        var identity = java.util.Objects.requireNonNull(source);
        return identity.added();
    }

    @SuppressWarnings("java:S1172") // the argument only gives its type to nothing: the call returns another type
    static OtherSub make(Object argument) {
        return new OtherSub();
    }

    @SuppressWarnings("java:S1854") // the variable is assigned again with the super type
    public String anonymousArgumentOfACallWhichReturnsAnotherType() {
        Other holder = make(new Object() {
            String added() {
                return "argument";
            }
        });
        String result = holder.added();
        holder = new Other();
        return result + holder.added();
    }

    public int anonymousTypeOfAConditional(boolean flag) {
        var chosen = flag ? new Object() {
            int added() {
                return 2;
            }
        } : null;
        return chosen == null ? 0 : chosen.added();
    }

    @SuppressWarnings("java:S1172") // the argument gives its type to nothing: the call returns another type
    static Object plain(Object argument) {
        return new OtherSub();
    }

    @SuppressWarnings("java:S1854") // the variable is assigned again with the super type
    public String anonymousArgumentOfACallWhichReturnsObject() {
        Other kept = (OtherSub) plain(new Object() {
            String added() {
                return "argument";
            }
        });
        String result = kept.added();
        kept = new Other();
        return result + kept.added();
    }

    @SuppressWarnings("java:S2160") // the anonymous class only exposes a protected method of its super type
    public int protectedMethodOfAnotherPackage() {
        var list = new java.util.ArrayList<String>() {
            @Override
            protected void removeRange(int from, int to) {
                super.removeRange(from, to);
            }
        };
        list.add("x");
        list.removeRange(0, 1);
        return list.size();
    }

    static <T extends Other> T choose(T value, Object ignored) {
        return value;
    }

    @SuppressWarnings("java:S1854") // the variable is assigned again with the super type
    public String anonymousArgumentWhichIsNotTheReturnedOne() {
        Other selected = choose(new OtherSub(), new Object() {
            String added() {
                return "ignored";
            }
        });
        String result = selected.added();
        selected = new Other();
        return result + selected.added();
    }

    public int anonymousVariableCapturedByALocalClass() {
        var captured = new Object() {
            public int added() {
                return 2;
            }
        };

        class Capturing {
            int value() {
                return captured.added();
            }
        }

        return new Capturing().value();
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

    static class OtherSub extends Other {
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
