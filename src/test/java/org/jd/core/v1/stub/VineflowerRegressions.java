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

    @SuppressWarnings("java:S1172") // the second argument gives its type to nothing: it is not the one of the returned type variable
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

    public int receiverWhichIsAGenericCall() {
        var target = new Object() {
            int added() {
                return 2;
            }
        };
        return java.util.Objects.requireNonNull(target).added();
    }

    @SuppressWarnings({ "unchecked", "java:S1130" }) // the type variable of the exception is what the generic signature of the test needs
    static <T, E extends Exception> T identity(T value) throws E {
        return value;
    }

    public int identityWhichThrowsATypeVariable() {
        var picked = identity(new Object() {
            int added() {
                return 2;
            }
        });
        return picked.added();
    }

    @SuppressWarnings({ "java:S1854", "java:S1659" }) // the declarations are merged on one line on purpose
    public int mergedGenericInitializers() {
        var one = java.util.Objects.requireNonNull(new Object() { int added() { return 1; } }); var two = java.util.Objects.requireNonNull(new Object() { int added() { return 2; } });
        return one.added() + two.added();
    }

    @SuppressWarnings({ "java:S1854", "java:S1172" }) // the parameter of the local class has the name of the alias
    public String parameterWhichHidesAnAlias() {
        Putter origin = new Putter() {
            public String added() {
                return "added";
            }
        };
        Object alias = origin;
        alias.hashCode();

        class Reading {
            String value(Other alias) {
                return alias.added();
            }
        }

        String result = new Reading().value(new Other());
        origin = new Putter();
        return result + origin.put("x");
    }

    @SuppressWarnings("java:S1854") // the variable is assigned again with the super type
    public int anonymousMethodWhichHidesAPrivateMethod() {
        PrivateBase hiding = new PrivateBase() {
            int value() {
                return 2;
            }
        };
        int first = hiding.value();
        hiding = new PrivateBase();
        return first + hiding.value();
    }

    private static <T> T privateIdentity(T value) {
        return value;
    }

    public int privateGenericCall() {
        var retained = privateIdentity(new Object() {
            int added() {
                return 2;
            }
        });
        return retained.added();
    }

    public int anonymousTypeOfASwitchExpression(int k) {
        var outcome = switch (k) {
            case 0 -> new Object() {
                int added() {
                    return 2;
                }
            };
            default -> null;
        };
        return outcome == null ? 0 : outcome.added();
    }

    static class ThrowingOther extends RuntimeException {
        String added() {
            return "thrown";
        }
    }

    @SuppressWarnings({ "java:S1854", "java:S1172" }) // the variable of the handler has the name of the alias
    public String catchVariableWhichHidesAnAlias() {
        Putter source = new Putter() {
            public String added() {
                return "added";
            }
        };
        Object copy = source;
        copy.hashCode();

        class Handling {
            String value() {
                try {
                    throw new ThrowingOther();
                } catch (ThrowingOther copy) {
                    return copy.added();
                }
            }
        }

        String result = new Handling().value();
        source = new Putter();
        return result + source.put("x");
    }

    @SuppressWarnings({ "java:S1854", "java:S1172" }) // the local of the nested method has the name of the alias
    public String localWhichHidesAnAlias() {
        Putter holder = new Putter() {
            public String added() {
                return "added";
            }
        };
        Object duplicate = holder;
        duplicate.hashCode();

        class Declaring {
            String value() {
                Other duplicate = new Other();
                return duplicate.added();
            }
        }

        String result = new Declaring().value();
        holder = new Putter();
        return result + holder.put("x");
    }

    public int typeThroughTheReceiverOfAGenericCall() {
        var element = java.util.List.of(new Object() {
            int added() {
                return 2;
            }
        }).get(0);
        return element.added();
    }

    static class Wrapper<T> extends Other {
        T content;
    }

    @SuppressWarnings({ "java:S1172", "java:S1854" }) // the argument gives its type to the type argument of the result only
    static <T> Wrapper<T> wrap(T ignored) {
        return new Wrapper<>();
    }

    @SuppressWarnings("java:S1854") // the variable is assigned again with the super type
    public String containerOfAnAnonymousArgument() {
        Other wrapped = wrap(new Object() {
            String added() {
                return "inside";
            }
        });
        String result = wrapped.added();
        wrapped = new Other();
        return result + wrapped.added();
    }

    static class Pair<K, V extends Other> {
        V value;

        V get() {
            return value;
        }
    }

    @SuppressWarnings({ "java:S1172", "java:S1854" }) // the key gives its type to the first type argument only
    static <K, V extends Other> Pair<K, V> pair(K key, V value) {
        return new Pair<>();
    }

    @SuppressWarnings("java:S1854") // the variable is assigned again with the super type
    public String secondTypeArgumentOfAPair() {
        Other pairValue = pair(new Object() {
            String added() {
                return "key";
            }
        }, new OtherSub()).get();
        String result = pairValue.added();
        pairValue = new Other();
        return result + pairValue.added();
    }

    public int closeableWithAHandler() {
        try (java.io.Closeable resource = new java.io.Closeable() {
            @Override
            public void close() {
                // nothing to release
            }
        }) {
            return 1;
        } catch (java.io.IOException e) {
            return 2;
        }
    }

    @SafeVarargs
    static <T> T first(T... values) {
        return values[0];
    }

    public int anonymousTypeThroughVarargs() {
        var firstValue = first(new Object() {
            int added() {
                return 2;
            }
        });
        return firstValue.added();
    }

    public int closeableWithAnUncheckedHandler() {
        try (var closing = new AutoCloseable() {
            @Override
            public void close() {
                // nothing to release
            }
        }) {
            return 1;
        } catch (RuntimeException e) {
            return 2;
        }
    }

    static <T> int apply(T value, java.util.function.ToIntFunction<T> function) {
        return function.applyAsInt(value);
    }

    public int lambdaWhichNeedsTheAnonymousType() {
        var inferred = new Object() {
            int added() {
                return 2;
            }
        };
        return apply(inferred, x -> x.added());
    }

    public int closeableWithACheckedHandlerWhichTheBodyThrows(boolean fail) {
        try (var thrower = new AutoCloseable() {
            @Override
            public void close() {
                // nothing to release
            }
        }) {
            if (fail) {
                throw new java.io.IOException();
            }
            return 1;
        } catch (java.io.IOException e) {
            return 2;
        }
    }

    @SuppressWarnings({ "java:S2159", "java:S2178" }) // the instance is converted to a string (the call of toString can throw), and both operands are evaluated
    public String stringOfAnObjectBeforeTheIncrement(Object object, int i) {
        while (i > 0) {
            i++;
            if (("" + object).isEmpty() | i > 3) {
                break;
            }
        }
        return String.valueOf(i);
    }

    @SuppressWarnings("java:S1172") // the first argument gives its type to nothing: the lambda is independent of it
    static int consume(Object ignored, java.util.function.ToIntFunction<Other> function) {
        return function.applyAsInt(new Other());
    }

    @SuppressWarnings("java:S1854") // the variable is assigned again with the super type
    public int lambdaWhichIsIndependentOfTheArgument() {
        Putter standalone = new Putter() {
            public String added() {
                return "added";
            }
        };
        int length = consume(standalone, x -> x.added().length());
        standalone = new Putter();
        return length + standalone.put("x").length();
    }

    public int lambdaWhichReadsAField() {
        var fielded = new Object() {
            int added = 2;
        };
        return apply(fielded, x -> x.added);
    }

    public static class FieldHolder<T> {
        public T held;

        public T get() {
            return held;
        }
    }

    private static <T> FieldHolder<T> holderOf(T value) {
        FieldHolder<T> holder = new FieldHolder<>();
        holder.held = value;
        return holder;
    }

    /** The field read is cast to the anonymous class, a check which only a 'var' keeps */
    public String fieldReadWhichKeepsTheAnonymousCheck() {
        var anonymous = new Object() {
            String label() {
                return "anonymous";
            }
        };
        var result = holderOf(anonymous).held;
        return result.label();
    }

    public String callWhichKeepsTheAnonymousCheck() {
        var anonymous = new Object() {
            String label() {
                return "anonymous";
            }
        };
        var result = holderOf(anonymous).get();
        return result.label();
    }

    static class Box<T> {
        final int value;

        Box(T content, java.util.function.ToIntFunction<T> function) {
            this.value = function.applyAsInt(content);
        }
    }

    public int constructorWhichInfersTheLambda() {
        var content = new Object() {
            int added() {
                return 2;
            }
        };
        return new Box<>(content, x -> x.added()).value;
    }

    @SuppressWarnings("java:S1854") // the variable is assigned again with the super type
    public int lambdaWhichCallsTheMemberOfAnotherObject() {
        Putter unrelated = new Putter() {
            public String added() {
                return "added";
            }
        };
        int length = apply(unrelated, x -> new Other().added().length());
        unrelated = new Putter();
        return length + unrelated.put("x").length();
    }

    @SuppressWarnings({ "java:S108", "java:S1166" }) // the inner handler consumes the exception which the body throws
    public int closeableWhichTheInnerHandlerCovers() {
        try (java.io.Closeable inner = new java.io.Closeable() {
            @Override
            public void close() {
                // nothing to release
            }
        }) {
            try {
                throw new java.io.IOException();
            } catch (java.io.IOException consumed) {
            }
            return 1;
        } catch (java.io.IOException outer) {
            return 2;
        }
    }

    static <T> int applyTwice(T value, java.util.function.ToIntBiFunction<T, Other> function) {
        return function.applyAsInt(value, new Other());
    }

    @SuppressWarnings("java:S1854") // the variable is assigned again with the super type
    public int lambdaWhoseSecondParameterIsOfAnotherType() {
        Putter first = new Putter() {
            public String added() {
                return "added";
            }
        };
        int length = applyTwice(first, (x, y) -> y.added().length());
        first = new Putter();
        return length + first.put("x").length();
    }

    interface Visitor<T> {
        int visit(Other other);
    }

    @SuppressWarnings("java:S1172") // the type variable of the interface is not used by its method
    static <T> int visitWith(T value, Visitor<T> visitor) {
        return visitor.visit(new Other());
    }

    @SuppressWarnings("java:S1854") // the variable is assigned again with the super type
    public int lambdaOfAFunctionalMethodWhichDoesNotUseTheTypeVariable() {
        Putter visited = new Putter() {
            public String added() {
                return "added";
            }
        };
        int length = visitWith(visited, x -> x.added().length());
        visited = new Putter();
        return length + visited.put("x").length();
    }

    @SuppressWarnings({ "java:S108", "java:S1166" }) // the lambda is run later, if ever
    public int closeableWhoseBodyDefinesALambdaWhichThrows() {
        try (java.io.Closeable deferred = new java.io.Closeable() {
            @Override
            public void close() {
                // nothing to release
            }
        }) {
            java.util.concurrent.Callable<Void> later = () -> {
                throw new java.io.IOException();
            };
            return later == null ? 0 : 1;
        } catch (java.io.IOException e) {
            return 2;
        }
    }

    @SuppressWarnings("java:S1172") // the lambda parameter has the type of the argument by a wildcard
    static <T> int applyWithABound(T value, java.util.function.ToIntFunction<? super T> function) {
        return function.applyAsInt(value);
    }

    public int lambdaWhoseTargetHasAWildcardBound() {
        var bounded = new Object() {
            int added() {
                return 2;
            }
        };
        return applyWithABound(bounded, x -> x.added());
    }

    @SuppressWarnings("java:S1130") // the declared exception is what the handler of the callers catches
    private static void io() throws java.io.IOException {
        // declares what the handler catches
    }

    public int closeableWhoseBodyCallsAMethodWhichThrows() {
        try (var caller = new AutoCloseable() {
            @Override
            public void close() {
                // nothing to release
            }
        }) {
            io();
            return 1;
        } catch (java.io.IOException e) {
            return 2;
        }
    }

    public int closeableWhoseCloseDeclaresTheCaughtException() {
        try (var declaring = new AutoCloseable() {
            @Override
            public void close() throws java.io.IOException {
                // declares what the handler catches
            }
        }) {
            return 1;
        } catch (java.io.IOException e) {
            return 2;
        }
    }

    @SuppressWarnings({ "java:S108", "java:S1166" }) // the nested try has no handler: what the body throws leaves it
    public int closeableWhoseBodyHasANestedTryWithoutHandler(boolean fail) {
        try (var nested = new AutoCloseable() {
            @Override
            public void close() {
                // nothing to release
            }
        }) {
            try {
                if (fail) {
                    throw new java.io.IOException();
                }
            } finally {
                fail = false;
            }
            return 1;
        } catch (java.io.IOException e) {
            return 2;
        }
    }

    public int closeableNextToAResourceWhichThrows() throws java.io.IOException {
        try (var anonymous = new AutoCloseable() {
            @Override
            public void close() {
                // nothing to release
            }
        }; java.io.Closeable other = () -> { }) {
            return other == null ? 0 : 1;
        } catch (java.io.IOException e) {
            return 2;
        }
    }

    public java.util.function.IntSupplier methodReferenceOfALambdaParameter() {
        var referenced = new Object() {
            int added() {
                return 2;
            }
        };
        return applyToReference(referenced, x -> x::added);
    }

    static <T> java.util.function.IntSupplier applyToReference(T value, java.util.function.Function<T, java.util.function.IntSupplier> function) {
        return function.apply(value);
    }

    static <T> int applyIndexed(T value, Indexed<T> function) {
        return function.apply(1, value, 2);
    }

    interface Indexed<T> {
        int apply(int index, T value, int count);
    }

    public int lambdaWithPrimitiveParametersAndAnAlias() {
        var indexed = new Object() {
            int added() {
                return 2;
            }
        };
        return applyIndexed(indexed, (index, x, count) -> {
            var y = x;
            return y.added() + index + count;
        });
    }

    @SuppressWarnings({ "unchecked", "java:S1172" }) // the unchecked cast of the generic result is a runtime check
    static <T> T unchecked(T ignored) {
        return (T) new Object();
    }

    public String castToTheAnonymousClassIsARuntimeCheck() {
        var guarded = unchecked(new Object() { });
        return guarded.toString();
    }

    @SuppressWarnings({ "unchecked", "java:S1172" })
    static <T> T unrelated(T ignored) {
        return (T) new Object();
    }

    public String noCastToTheAnonymousClassMeansNoVar() {
        Object plain = unrelated(new Object() { });
        return plain.toString();
    }

    public String castThroughALocalIsARuntimeCheck() {
        var origin = new Object() { };
        var value = unchecked(origin);
        return value.toString();
    }

    interface Extra {
        int extra();
    }

    interface InheritedVisitor<T> extends java.util.function.ToIntFunction<Extra> {
    }

    static <T> int consumeInherited(T first, InheritedVisitor<T> visitor) {
        return visitor.applyAsInt(() -> 1) + first.hashCode();
    }

    public int inheritedFunctionalMethodMapsItsParameters() {
        Object tracked = new Object() {
            int extra() {
                return 2;
            }
        };
        int result = consumeInherited(tracked, x -> x.extra());
        tracked = new Object();
        return result;
    }

    static class $1 {
    }

    public boolean namedLikeAnonymous(Object value) {
        return (($1) value) == null;
    }

    static <T> int applyTo(T value, java.util.function.ToIntFunction<T> function) {
        return function.applyAsInt(value);
    }

    public int aliasEndsWithItsBlock(boolean flag) {
        Object sibling = new Object() {
            int extra() {
                return 1;
            }
        };
        int total = applyTo(sibling, x -> {
            if (flag) {
                Object y = x;
                return y.hashCode();
            }
            if (flag) {
                Extra y = () -> 3;
                return y.extra();
            }
            return 0;
        });
        sibling = new Object();
        return total + sibling.hashCode();
    }

    interface Overloaded<T> {
        int equals(Extra other);
    }

    static <T> int overloadedConsume(T value, Overloaded<T> function) {
        return function.equals(() -> 4) + value.hashCode();
    }

    public int declaredOverloadOfAnObjectMethodIsTheFunctionalMethod() {
        Object overloaded = new Object() {
            int extra() {
                return 2;
            }
        };
        int result = overloadedConsume(overloaded, x -> x.extra());
        overloaded = new Object();
        return result + overloaded.hashCode();
    }

    static final class Boxed<T> {
        final int value;

        Boxed(T first, java.util.function.ToIntFunction<T> function) {
            value = function.applyAsInt(first);
        }
    }

    public int aliasInAConstructorLambda() {
        var constructed = new Object() {
            int added() {
                return 2;
            }
        };
        return new Boxed<>(constructed, x -> {
            var y = x;
            return y.added();
        }).value;
    }

    static <T> int applyToArray(T value, java.util.function.ToIntFunction<T[]> function) {
        return function.applyAsInt(null) + value.hashCode();
    }

    public int arrayOfTheTypeVariableGivesItsType() {
        var arrayed = new Object() {
            int added() {
                return 2;
            }
        };
        return applyToArray(arrayed, xs -> xs[0].added());
    }

    @SuppressWarnings("java:S1172")
    static <T> T second(T value, String ignored) {
        return value;
    }

    @SuppressWarnings({ "unchecked", "java:S1172" })
    static <T> T badSecond(T value, String ignored) {
        return (T) new Object();
    }

    public String castOfAnUnrelatedArgumentIsNoEvidence() {
        Object unrelatedCast = badSecond(new Object() { }, second(new Object() { }, "").toString());
        return unrelatedCast.toString();
    }

    interface ArrayFunction<E> {
        int apply(E[] values);
    }

    static <T> int applyArrayFunction(T value, ArrayFunction<T> function) {
        return function.apply(null) + value.hashCode();
    }

    public int arrayOfTheInterfaceTypeParameterGivesItsType() {
        var inArray = new Object() {
            int added() {
                return 2;
            }
        };
        return applyArrayFunction(inArray, xs -> xs[0].added());
    }

    public String reassignedWithARuntimeCast() {
        var reassigned = new Object() { };
        reassigned = unchecked(reassigned);
        return reassigned.toString();
    }

    public int lambdaWithAnAnonymousClassWhichCapturesALocal(int captured) {
        var carrier = new Object() {
            int added() {
                return 1;
            }
        };
        return applyTo(carrier, x -> new java.util.function.IntSupplier() {
            @Override
            public int getAsInt() {
                return captured;
            }
        }.getAsInt());
    }

    public String castInANestedBlock(boolean flag) {
        var nestedOrigin = new Object() { };
        if (flag) {
            var nestedValue = unchecked(nestedOrigin);
            return nestedValue.toString();
        }
        return "";
    }

    interface DefaultedFunction<E> extends java.util.function.Consumer<String>, java.util.function.ToIntFunction<E> {
        @Override
        default void accept(String text) {
            // implemented here: the functional method is applyAsInt
        }
    }

    static <T> int applyDefaulted(T value, DefaultedFunction<T> function) {
        return function.applyAsInt(value);
    }

    public int defaultMethodImplementsTheFirstInheritedOne() {
        var defaulted = new Object() {
            int added() {
                return 2;
            }
        };
        return applyDefaulted(defaulted, x -> x.added());
    }

    public String castOfAnInlineCall() {
        var inlineOrigin = new Object() { };
        return unchecked(inlineOrigin).toString();
    }

    interface OverloadingFunction<E> extends java.util.function.ToIntFunction<Extra> {
        default int applyAsInt(String text) {
            return 0;
        }
    }

    static <T> int applyOverloading(T value, OverloadingFunction<T> function) {
        return function.applyAsInt(() -> 5) + value.hashCode();
    }

    public int defaultOverloadDoesNotImplementTheInheritedMethod() {
        Object overloading = new Object() {
            int extra() {
                return 2;
            }
        };
        int result = applyOverloading(overloading, x -> x.extra());
        overloading = new Object();
        return result + overloading.hashCode();
    }

    public String targetTypedReassignment() {
        var targetTyped = new Object() { };
        targetTyped = unchecked(null);
        return targetTyped.toString();
    }

    public int nestedHandlerOfAnotherException(boolean fail) {
        try (var other = new AutoCloseable() {
            @Override
            public void close() {
                // closes nothing
            }
        }) {
            try {
                if (fail) {
                    throw new java.io.IOException();
                }
            } catch (IllegalArgumentException e) {
                return 3;
            }
            return 1;
        } catch (java.io.IOException e) {
            return 2;
        }
    }

    public String aliasOfTheOriginFeedsACast() {
        var aliasOrigin = new Object() { };
        var alias = aliasOrigin;
        return unchecked(alias).toString();
    }

    public int arrayTypeArgumentOfThePatternOperand(java.util.Collection<String[]> values) {
        if (values instanceof java.util.List<String[]> list) {
            return list.size();
        }
        return 0;
    }

    static <T> int applyToList(T value, java.util.function.ToIntFunction<java.util.List<T>> function) {
        return function.applyAsInt(null) + value.hashCode();
    }

    public int containerOfTheTypeVariableGivesItsType() {
        var listed = new Object() {
            int added() {
                return 2;
            }
        };
        return applyToList(listed, xs -> xs.get(0).added());
    }

    static <T> T identityOf(T value) {
        return value;
    }

    public String castOfAnotherLocalWhichHasTheSameName() {
        Object shadowed = new Object() { };
        Object holder = new Object() {
            @Override
            public String toString() {
                var shadowed = new Object() { };
                return identityOf(shadowed).toString();
            }
        };
        shadowed = "ok";
        return shadowed + holder.toString();
    }

    interface ListFunction<E> {
        int apply(java.util.List<E> values);
    }

    static <T> int applyListFunction(T value, ListFunction<T> function) {
        return function.apply(null) + value.hashCode();
    }

    public int containerDeclaredInTheFunctionalMethod() {
        var contained = new Object() {
            int added() {
                return 2;
            }
        };
        return applyListFunction(contained, xs -> xs.get(0).added());
    }

    public int nestedResourceWhichClosesWithAnException(boolean flag) {
        try (var nestedOuter = new AutoCloseable() {
            @Override
            public void close() {
                // closes nothing
            }
        }) {
            if (flag) {
                try (java.io.Closeable inner = () -> { }) {
                    return 1;
                }
            }
            return 0;
        } catch (java.io.IOException e) {
            return 2;
        }
    }

    public int elementOfTheContainerStoredInALocal() {
        var element = new Object() {
            int added() {
                return 2;
            }
        };
        return applyToList(element, xs -> {
            var stored = xs.get(0);
            return stored.added();
        });
    }

    static class Source implements AutoCloseable {
        Source() throws java.io.IOException {
            // opens the source
        }

        @Override
        public void close() throws Exception {
            // closes the source
        }
    }

    public int constructorOfTheResourceThrows() {
        try (var source = new Source() {
            @Override
            public void close() {
                // closes nothing
            }
        }) {
            return source.hashCode();
        } catch (java.io.IOException e) {
            return 2;
        }
    }

    interface Extra2 {
        int extra();
    }

    static <T> int applyToMap(T value, java.util.function.ToIntFunction<java.util.Map<T, Extra2>> function) {
        return function.applyAsInt(null) + value.hashCode();
    }

    public int accessorOfAnotherTypeArgument() {
        Object keyed = new Object() {
            int extra() {
                return 1;
            }
        };
        int result = applyToMap(keyed, xs -> xs.get("key").extra());
        keyed = new Object();
        return result + keyed.hashCode();
    }

    public int arrayElementStoredInALocal() {
        var elemental = new Object() {
            int added() {
                return 2;
            }
        };
        return applyToArray(elemental, xs -> {
            var stored = xs[0];
            return stored.added();
        });
    }

    static <E extends Exception> void raise(E exception) throws E {
        // throws what it receives
    }

    public int genericThrowsAreInferred() {
        try (AutoCloseable generic = new AutoCloseable() {
            @Override
            public void close() {
                // closes nothing
            }
        }) {
            raise(new IllegalArgumentException());
            return 1;
        } catch (java.io.IOException e) {
            return 2;
        } catch (Exception e) {
            return 3;
        }
    }

    public int containerAliasAfterANestedBlock(boolean flag) {
        var nestedBlock = new Object() {
            int added() {
                return 2;
            }
        };
        return applyToList(nestedBlock, xs -> {
            if (flag) {
                ping();
                ping();
            }
            var stored = xs.get(0);
            return stored.added();
        });
    }

    static void ping() {
        // does nothing
    }

    @SuppressWarnings({ "unchecked", "java:S1172" })
    static <T> T castingIdentity(T ignored) {
        AutoCloseable closer = () -> { };

        return (T) closer;
    }

    public String resourceWhichIsNarrowedWithoutAHandler() throws Exception {
        try (AutoCloseable unhandled = new AutoCloseable() {
            @Override
            public void close() {
                // closes nothing
            }
        }) {
            return castingIdentity(unhandled).toString();
        }
    }

    @SuppressWarnings({ "unchecked", "java:S1172" })
    static <T> T closeableIdentity(T ignored) {
        java.io.Closeable closer = () -> { };

        return (T) closer;
    }

    @SuppressWarnings("java:S1172")
    public String resourceWhichTheThrowsClauseCovers() throws java.io.IOException {
        try (java.io.Closeable covered = new java.io.Closeable() {
            @Override
            public void close() {
                // closes nothing
            }
        }) {
            return closeableIdentity(covered).toString();
        }
    }

    interface Reversed<K, V> extends java.util.Map<V, K> {
    }

    static <T> int applyToReversed(T value, java.util.function.ToIntFunction<Reversed<Extra2, T>> function) {
        return function.applyAsInt(null) + value.hashCode();
    }

    public int inheritedAccessorWithPermutedArguments() {
        Object reversed = new Object() {
            int extra() {
                return 1;
            }
        };
        int result = applyToReversed(reversed, xs -> xs.get("key").extra());
        reversed = new Object();
        return result + reversed.hashCode();
    }

    @SuppressWarnings("java:S1172")
    public String resourceWhichAnEnclosingHandlerCovers() {
        try {
            try (AutoCloseable enclosed = new AutoCloseable() {
                @Override
                public void close() {
                    // closes nothing
                }
            }) {
                return castingIdentity(enclosed).toString();
            }
        } catch (Exception e) {
            return e.toString();
        }
    }

    interface Thrower<E extends Exception> {
        void run() throws E;
    }

    public int genericThrowsOfTheReceiver(Thrower<java.io.IOException> thrower) {
        try (var thrown = new AutoCloseable() {
            @Override
            public void close() {
                // closes nothing
            }
        }) {
            thrower.run();
            return 1;
        } catch (java.io.IOException e) {
            return 2;
        }
    }

    static <E extends Exception> void raiseOneOf(E first, E second) throws E {
        // throws what it receives
    }

    @SuppressWarnings("java:S2486")
    public int genericThrowsOfSeveralArguments() throws java.io.IOException {
        try (var shared = new AutoCloseable() {
            @Override
            public void close() {
                // closes nothing
            }
        }) {
            raiseOneOf(new java.io.FileNotFoundException(), new java.io.EOFException());
        } catch (java.io.EOFException e) {
            return 1;
        }
        return 0;
    }

    public int resourceInTheFinallyOfAHandledTry() {
        try {
            ping();
        } catch (Exception e) {
            ping();
        } finally {
            try (var closing2 = new AutoCloseable() {
                @Override
                public void close() {
                    // closes nothing
                }
            }) {
                ping();
            }
        }
        return 0;
    }

    @SafeVarargs
    static <E extends Exception> void raiseAll(E... exceptions) throws E {
        // throws what it receives
    }

    public int genericThrowsOfVarargs() {
        try (var spread = new AutoCloseable() {
            @Override
            public void close() {
                // closes nothing
            }
        }) {
            raiseAll(new java.io.IOException());
            return 1;
        } catch (java.io.IOException e) {
            return 2;
        }
    }

    public Runnable executableInsideAHandledTry() {
        try {
            return new Runnable() {
                @Override
                public void run() {
                    try (var nestedRun = new AutoCloseable() {
                        @Override
                        public void close() {
                            // closes nothing
                        }
                    }) {
                        ping();
                    }
                }
            };
        } catch (Exception e) {
            return null;
        }
    }

    interface InputThrower extends Thrower<java.io.IOException> {
    }

    public int genericThrowsOfTheInheritedReceiver(InputThrower thrower) {
        try (var inherited = new AutoCloseable() {
            @Override
            public void close() {
                // closes nothing
            }
        }) {
            thrower.run();
            return 1;
        } catch (java.io.IOException e) {
            return 2;
        }
    }

    public java.util.concurrent.Callable<String> lambdaWhichMayThrow() {
        return () -> {
            try (AutoCloseable lambdaResource = new AutoCloseable() {
                @Override
                public void close() {
                    // closes nothing
                }
            }) {
                return castingIdentity(lambdaResource).toString();
            }
        };
    }

    @SuppressWarnings("java:S1172")
    public String existingVariableResource() throws Exception {
        AutoCloseable existing = new AutoCloseable() {
            @Override
            public void close() {
                // closes nothing
            }
        };

        try (existing) {
            return castingIdentity(existing).toString();
        }
    }

    @SuppressWarnings("java:S1130")
    public int broaderThrownTypeReachesTheHandler() throws java.io.IOException {
        try (var broader = new AutoCloseable() {
            @Override
            public void close() {
                // closes nothing
            }
        }) {
            if (hashCode() > 0) {
                throw new java.io.IOException();
            }
            return 1;
        } catch (java.io.FileNotFoundException e) {
            return 2;
        }
    }

    interface Task<E extends Exception> {
        void run() throws E;
    }

    public Task<java.io.IOException> lambdaWithAGenericThrows() {
        return () -> {
            try (var taskResource = new AutoCloseable() {
                @Override
                public void close() {
                    // closes nothing
                }
            }) {
                ping();
            }
        };
    }

    public String castInAForLoop(boolean run) {
        String result = "";
        boolean again = run;

        for (var looped = new Object() { }; again; again = false) {
            result = unchecked(looped).toString();
        }
        return result;
    }

    @SuppressWarnings("java:S1130")
    public <E extends java.io.IOException> int thrownTypeVariable(E failure) {
        try (var bounded = new AutoCloseable() {
            @Override
            public void close() {
                // closes nothing
            }
        }) {
            throw failure;
        } catch (java.io.IOException e) {
            return 2;
        }
    }

    public int alternateEntryIntoTheCast(Object first, Object second) {
        if (first instanceof String || second instanceof String) {
            String entered = (String) second;

            return entered.length();
        }
        return 0;
    }

    static <E extends Exception> void raiseFrom(java.util.List<E> failures) throws E {
        // throws one of them
    }

    public int genericThrowsOfAContainer(java.util.List<java.io.IOException> failures) {
        try (var contained2 = new AutoCloseable() {
            @Override
            public void close() {
                // closes nothing
            }
        }) {
            raiseFrom(failures);
            return 1;
        } catch (java.io.IOException e) {
            return 2;
        }
    }

    static <E extends Exception> void raiseFromNested(java.util.List<java.util.List<E>> failures) throws E {
        // throws one of them
    }

    public int genericThrowsOfNestedContainers(java.util.List<java.util.List<java.io.IOException>> failures) {
        try (var nestedContained = new AutoCloseable() {
            @Override
            public void close() {
                // closes nothing
            }
        }) {
            raiseFromNested(failures);
            return 1;
        } catch (java.io.IOException e) {
            return 2;
        }
    }

    static <E extends Exception> void raiseFromBounded(java.util.List<? extends E> failures) throws E {
        // throws one of them
    }

    public <E extends java.io.IOException> int genericThrowsOfAVariableContainer(java.util.List<E> failures) {
        try (var contained = new AutoCloseable() {
            @Override
            public void close() {
                // closes nothing
            }
        }) {
            raiseFrom(failures);
            return 1;
        } catch (java.io.IOException e) {
            return 2;
        }
    }

    public int genericThrowsOfAWildcardContainer(java.util.List<? extends java.io.IOException> failures) {
        try (var wildcarded = new AutoCloseable() {
            @Override
            public void close() {
                // closes nothing
            }
        }) {
            raiseFromBounded(failures);
            return 1;
        } catch (java.io.IOException e) {
            return 2;
        }
    }

    interface BroadRunner {
        void run() throws Exception;
    }

    interface NarrowRunner {
        void run() throws java.io.IOException;
    }

    interface BothRunner extends BroadRunner, NarrowRunner {
    }

    public BothRunner lambdaWithTwoInheritedThrows() {
        return () -> {
            try (var intersected = new AutoCloseable() {
                @Override
                public void close() {
                    // closes nothing
                }
            }) {
                new java.io.StringReader("").read();
            }
        };
    }

    public <X extends java.io.IOException, E extends X> int throwOfAChainedVariable(E failure) {
        try (var chained = new AutoCloseable() {
            @Override
            public void close() {
                // closes nothing
            }
        }) {
            if (failure != null) {
                throw failure;
            }
            return 1;
        } catch (java.io.IOException e) {
            return 2;
        }
    }

    static class NestedFailures extends java.util.ArrayList<java.util.List<java.io.IOException>> {
        private static final long serialVersionUID = 1L;
    }

    static <E extends Exception> void raiseFromListOfLists(java.util.List<java.util.List<E>> failures) throws E {
        // throws one of them
    }

    static class MixedArguments extends java.util.HashMap<String, Integer> {
        private static final long serialVersionUID = 1L;
    }

    static <T> int sameArguments(java.util.Map<T, T> value) {
        return value.size();
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    public int inheritedArgumentsWhichDisagree(MixedArguments value) {
        return sameArguments((java.util.Map) value);
    }

    static class StringList extends java.util.ArrayList<String> {
        private static final long serialVersionUID = 1L;
    }

    static <T extends Number> int boundedArguments(java.util.List<T> value) {
        return value.size();
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    public int inheritedArgumentsOutsideTheBound(StringList value) {
        return boundedArguments((java.util.List) value);
    }

    static class IntegerList extends java.util.ArrayList<Integer> {
        private static final long serialVersionUID = 1L;
    }

    static <T extends Number & Runnable> int intersectionBounded(java.util.List<T> value) {
        return value.size();
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    public int inheritedArgumentsOutsideAnIntersectionBound(IntegerList value) {
        return intersectionBounded((java.util.List) value);
    }

    static <T extends Comparable<String>> int parameterizedBound(java.util.List<T> value) {
        return value.size();
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    public int inheritedArgumentsOutsideAParameterizedBound(IntegerList value) {
        return parameterizedBound((java.util.List) value);
    }

    static <T extends Comparable<? extends CharSequence>> int wildcardBound(java.util.List<T> value) {
        return value.size();
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    public int inheritedArgumentsOutsideAWildcardBound(IntegerList value) {
        return wildcardBound((java.util.List) value);
    }

    public int genericThrowsOfAnInheritedNestedContainer(NestedFailures failures) {
        try (var inheritedNested = new AutoCloseable() {
            @Override
            public void close() {
                // closes nothing
            }
        }) {
            raiseFromListOfLists(failures);
            return 1;
        } catch (java.io.IOException e) {
            return 2;
        }
    }

    public <X extends java.io.IOException, E extends X> String resourceWhoseVariableIsBoundedByAPermittedOne() throws X {
        try (Closer<E> boundedVariable = new Closer<E>() {
            @Override
            public void close() {
                // closes nothing
            }
        }) {
            return passThrough(boundedVariable).toString();
        }
    }

    static <T> T passThrough(T value) {
        return value;
    }

    interface IoClosing extends AutoCloseable {
        @Override
        void close() throws java.io.IOException;
    }

    interface SqlClosing extends AutoCloseable {
        @Override
        void close() throws java.sql.SQLException;
    }

    interface InheritedClosing extends IoClosing, SqlClosing {
        // inherits two closes
    }

    interface GenericClosing<E extends Exception> extends AutoCloseable {
        @Override
        void close() throws E;
    }

    interface GenericInheritedClosing<E extends Exception> extends GenericClosing<E>, IoClosing {
        // inherits two closes
    }

    static <T extends GenericInheritedClosing<java.sql.SQLException>> T sameGeneric(T value) {
        return value;
    }

    public int resourceWhoseGenericInheritedClosesAreIntersected() {
        try (GenericInheritedClosing<java.sql.SQLException> substitutedClose = new GenericInheritedClosing<java.sql.SQLException>() {
            @Override
            public void close() {
                // closes nothing
            }
        }) {
            return sameGeneric(substitutedClose).hashCode();
        }
    }

    static <T extends InheritedClosing> T same(T value) {
        return value;
    }

    public int resourceWhoseInheritedClosesAreIntersected() {
        try (InheritedClosing intersectedClose = new InheritedClosing() {
            @Override
            public void close() {
                // closes nothing
            }
        }) {
            return same(intersectedClose).hashCode();
        }
    }

    static class SqlCloser implements AutoCloseable {
        @Override
        public void close() throws java.sql.SQLException {
            // closes nothing
        }
    }

    interface FirstRunner {
        void run() throws java.io.IOException, java.sql.SQLException;
    }

    interface SecondRunner {
        void run() throws java.io.IOException, ReflectiveOperationException;
    }

    interface OverlappingRunner extends FirstRunner, SecondRunner {
    }

    interface GenericRunner<E extends Exception> {
        void run() throws E;
    }

    interface InputRunner {
        void run() throws java.io.IOException;
    }

    interface UnrelatedRunner extends GenericRunner<java.sql.SQLException>, InputRunner {
    }

    static class InputCloser implements AutoCloseable {
        @Override
        public void close() throws java.io.IOException {
            // closes nothing
        }
    }

    public UnrelatedRunner lambdaWithUnrelatedInheritedThrows() {
        return () -> {
            try (var unrelated = new InputCloser() {
                @Override
                public void close() {
                    // closes nothing
                }
            }) {
                unrelated.toString();
            }
        };
    }

    public OverlappingRunner lambdaWithOverlappingInheritedThrows() {
        return () -> {
            try (var overlapped = new SqlCloser() {
                @Override
                public void close() {
                    // closes nothing
                }
            }) {
                overlapped.toString();
            }
        };
    }

    static String pick(Exception exception) {
        return "exception" + exception.getMessage();
    }

    static String pick(java.io.IOException exception) {
        return "io" + exception.getMessage();
    }

    static <E extends java.io.IOException> String castToException(E failure) {
        return pick((Exception) failure);
    }

    interface BroadSupplier {
        Object get() throws Exception;
    }

    interface NarrowSupplier {
        String get() throws java.io.IOException;
    }

    interface CovariantSupplier extends BroadSupplier, NarrowSupplier {
    }

    public CovariantSupplier lambdaWithCovariantInheritedThrows() {
        return () -> {
            try (var covariant = new AutoCloseable() {
                @Override
                public void close() {
                    // closes nothing
                }
            }) {
                return "ok";
            }
        };
    }

    interface BroadAction {
        void run() throws Exception;
    }

    interface DefaultedAction extends BroadAction {
        @Override
        default void run() {
            // does nothing
        }
    }

    interface ActualAction {
        void act() throws java.io.IOException;
    }

    interface DefaultingAction extends DefaultedAction, ActualAction {
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    static <T> String adaptAndApply(T ignored, java.util.function.Function<T, String> function) {
        java.util.Objects.requireNonNull(ignored);
        return (String) ((java.util.function.Function) function).apply(new Object());
    }

    public String lambdaAdaptedToAMemberlessType() {
        var adapted = new Object() {
        };
        return adaptAndApply(adapted, x -> "ok");
    }

    public String lambdaAdaptedToATypeWithAnUnusedMember() {
        var adapted = new Object() {
            int unused;
        };
        return adaptAndApply(adapted, x -> "ok");
    }

    public String lambdaAdaptedThroughAnAlias() {
        var origin = new Object() {
        };
        var value = origin;
        return adaptAndApply(value, x -> "ok");
    }

    public String methodReferenceAdaptedToAnAnonymousType() {
        var adapted = new Object() {
        };
        return adaptAndApply(adapted, Object::toString);
    }

    static class AnonymousArgumentTarget {
        AnonymousArgumentTarget(Object value) {
            // holds nothing
        }
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    static <T> String adaptAndBuild(T ignored, java.util.function.Function<T, AnonymousArgumentTarget> factory) {
        java.util.Objects.requireNonNull(ignored);
        return String.valueOf(((java.util.function.Function) factory).apply(new Object()));
    }

    public String constructorReferenceAdaptedToAnAnonymousType() {
        var adapted = new Object() {
        };
        return adaptAndBuild(adapted, AnonymousArgumentTarget::new);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    static <T> String adaptToContainer(java.util.List<T> values, java.util.function.Function<T, String> function) {
        java.util.Objects.requireNonNull(values);
        return (String) ((java.util.function.Function) function).apply(new Object());
    }

    public String lambdaAdaptedThroughAContainer() {
        var adapted = new Object() {
        };
        return adaptToContainer(java.util.List.of(adapted), x -> "ok");
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    static <K, V> String adaptToMap(java.util.Map<K, V> values, java.util.function.Function<V, String> function) {
        java.util.Objects.requireNonNull(values);
        return (String) ((java.util.function.Function) function).apply(new Object());
    }

    public String lambdaAdaptedThroughTheSecondTypeArgument() {
        var adapted = new Object() {
        };
        return adaptToMap(java.util.Map.of("key", adapted), x -> "ok");
    }

    public String lambdaAdaptedThroughACopiedContainer() {
        var adapted = new Object() {
        };
        return adaptToContainer(new java.util.ArrayList<>(java.util.List.of(adapted)), x -> "ok");
    }

    public String lambdaAdaptedThroughAResourceAlias() {
        var origin = new AutoCloseable() {
            @Override
            public void close() {
                java.util.Objects.requireNonNull(this);
            }

            void added() {
                java.util.Objects.requireNonNull(this);
            }
        };
        origin.added();
        try (var value = origin) {
            return adaptToContainer(java.util.List.of(value), x -> "ok");
        }
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    static <T> String adaptToNestedContainer(java.util.List<java.util.List<T>> values, java.util.function.Function<T, String> function) {
        java.util.Objects.requireNonNull(values);
        return (String) ((java.util.function.Function) function).apply(new Object());
    }

    public String lambdaAdaptedThroughANestedContainer() {
        var adapted = new Object() {
        };
        return adaptToNestedContainer(java.util.List.of(java.util.List.of(adapted)), x -> "ok");
    }

    static final class KeyedValue<K, V> {
        final K key;
        final V value;

        KeyedValue(K key, V value) {
            this.key = key;
            this.value = value;
        }
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    static <K, V> String adaptToKeyedValue(KeyedValue<K, V> keyed, java.util.function.Function<V, String> function) {
        java.util.Objects.requireNonNull(keyed);
        return (String) ((java.util.function.Function) function).apply(new Object());
    }

    public String lambdaAdaptedThroughAConstructorArgument() {
        var adapted = new Object() {
        };
        return adaptToKeyedValue(new KeyedValue<>("key", adapted), x -> "ok");
    }

    public String lambdaAdaptedThroughBothTypeArguments() {
        var adapted = new Object() {
        };
        return adaptToMap(java.util.Map.of(adapted, adapted), x -> "ok");
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    static <T, U> String adaptToBoth(T first, U second, java.util.function.BiFunction<T, U, String> function) {
        java.util.Objects.requireNonNull(first);
        return (String) ((java.util.function.BiFunction) function).apply(new Object(), second);
    }

    public String lambdaAdaptedOnlyForTheSecondParameter() {
        var origin = new Object() {
        };
        Object value = origin;
        return adaptToBoth(value, origin, (x, y) -> "ok");
    }

    static String combine(Object first, Object second) {
        return String.valueOf(first) + second;
    }

    public String methodReferenceAdaptedOnlyForTheSecondParameter() {
        var origin = new Object() {
        };
        Object value = origin;
        return adaptToBoth(value, origin, VineflowerRegressions::combine);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    static <T> String adaptToArray(T ignored, java.util.function.Function<T[], String> function) {
        java.util.Objects.requireNonNull(ignored);
        return (String) ((java.util.function.Function) function).apply(new Object[0]);
    }

    public String methodReferenceAdaptedToAnArrayOfTheAnonymousType() {
        var adapted = new Object() {
        };
        return adaptToArray(adapted, Object::toString);
    }

    @SafeVarargs
    @SuppressWarnings({"unchecked", "rawtypes"})
    static <T> String adaptToVarargs(java.util.function.Function<T, String> function, T... values) {
        java.util.Objects.requireNonNull(values);
        return (String) ((java.util.function.Function) function).apply(new Object());
    }

    public String lambdaAdaptedThroughVarargs() {
        var adapted = new Object() {
        };
        return adaptToVarargs(x -> "ok", adapted);
    }

    interface GenericBroadAction {
        <T> void run(T value) throws Exception;
    }

    interface ErasedNarrowAction {
        void run(Object value) throws java.io.IOException;
    }

    interface ErasedBothActions extends GenericBroadAction, ErasedNarrowAction {
    }

    public ErasedBothActions lambdaWhoseGenericAncestorErasesToTheNarrowOne() {
        return value -> {
            try (var erased = new AutoCloseable() {
                @Override
                public void close() {
                    // closes nothing
                }
            }) {
                erased.toString();
            }
        };
    }

    interface RelatedClosingA<T extends Exception> extends AutoCloseable {
        @Override
        void close() throws T;
    }

    interface RelatedClosingB<T extends Exception> extends AutoCloseable {
        @Override
        void close() throws T;
    }

    interface RelatedClosing<X extends java.io.IOException, E extends X> extends RelatedClosingA<E>, RelatedClosingB<X> {
        // inherits two closes, one throws a variable which the other one bounds
    }

    public <X extends java.io.IOException, E extends X> void resourceWhoseInheritedVariablesBoundEachOther() {
        try (var related = new RelatedClosing<X, E>() {
            @Override
            public void close() {
                // closes nothing
            }
        }) {
            related.toString();
        }
    }

    interface ShadowedGenericAction<T> {
        <T> void run(T value) throws Exception;
    }

    interface ErasedShadowAction {
        void run(Object value) throws java.io.IOException;
    }

    interface ErasedShadowActions extends ShadowedGenericAction<String>, ErasedShadowAction {
    }

    public ErasedShadowActions lambdaWhoseGenericAncestorShadowsItsVariable() {
        return value -> {
            try (var shadowed = new AutoCloseable() {
                @Override
                public void close() {
                    // closes nothing
                }
            }) {
                shadowed.toString();
            }
        };
    }

    abstract static class InheritingClosingClass implements IoClosing, SqlClosing {
        // inherits two closes
    }

    static <T extends InheritingClosingClass> T sameClosingClass(T value) {
        return value;
    }

    public int resourceWhoseAbstractClassInheritsTwoCloses() {
        try (InheritingClosingClass abstractClose = new InheritingClosingClass() {
            @Override
            public void close() {
                // closes nothing
            }
        }) {
            return sameClosingClass(abstractClose).hashCode();
        }
    }

    interface BoundedGenericAction<U> {
        <T extends U> void run(T value) throws Exception;
    }

    interface StringAction {
        void run(String value) throws java.io.IOException;
    }

    interface BoundedBothActions extends BoundedGenericAction<String>, StringAction {
    }

    public BoundedBothActions lambdaWhoseMethodVariableIsBoundedByAnInterfaceVariable() {
        return value -> {
            try (var bounded = new AutoCloseable() {
                @Override
                public void close() {
                    // closes nothing
                }
            }) {
                bounded.toString();
            }
        };
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    static <T> String applyToElements(T ignored, java.util.function.Function<java.util.List<T>, String> function) {
        java.util.Objects.requireNonNull(ignored);
        return (String) ((java.util.function.Function) function).apply(plainElements());
    }

    static Object plainElements() {
        return java.util.List.of(new Object());
    }

    public String lambdaWhoseContainerElementIsChecked() {
        var checked = new Object() {
        };
        return applyToElements(checked, xs -> xs.get(0).toString());
    }

    abstract static class FileClosingBase implements AutoCloseable {
        @Override
        public void close() throws java.io.FileNotFoundException {
            // closes nothing
        }
    }

    interface IoOnlyClosing extends AutoCloseable {
        @Override
        void close() throws java.io.IOException;
    }

    interface AnyClosing extends AutoCloseable {
        @Override
        void close() throws Exception;
    }

    abstract static class ImplementedClosingClass extends FileClosingBase implements IoOnlyClosing, AnyClosing {
        // the close of the superclass is the one of the class
    }

    static <T extends ImplementedClosingClass> T sameImplementedClosing(T value) {
        return value;
    }

    public int resourceWhoseSuperclassImplementsTheClose() throws java.io.FileNotFoundException {
        try (ImplementedClosingClass implemented = new ImplementedClosingClass() {
            @Override
            public void close() {
                // closes nothing
            }
        }) {
            return sameImplementedClosing(implemented).hashCode();
        }
    }

    interface ChainedClosingA<T extends Exception> extends AutoCloseable {
        @Override
        void close() throws T;
    }

    interface ChainedClosingB extends AutoCloseable {
        @Override
        void close() throws java.io.IOException;
    }

    interface ChainedClosing<T extends java.io.IOException> extends ChainedClosingA<T>, ChainedClosingB {
        // inherits two closes
    }

    public <X extends java.io.IOException, E extends X> void resourceWhoseArgumentIsBoundedByAnotherVariable() {
        try (var chainedArgument = new ChainedClosing<E>() {
            @Override
            public void close() {
                // closes nothing
            }
        }) {
            chainedArgument.toString();
        }
    }

    static <E extends Exception> void raiseWildcard(java.util.List<? extends E> errors) throws E {
        if (!errors.isEmpty()) {
            throw errors.get(0);
        }
    }

    public void genericThrowsOfAWildcardFormal(java.util.List<java.io.IOException> errors) {
        try (var wildcard = new AutoCloseable() {
            @Override
            public void close() {
                // closes nothing
            }
        }) {
            raiseWildcard(errors);
        } catch (java.io.IOException e) {
            e.printStackTrace();
        }
    }

    public <E extends java.io.IOException> void genericThrowsOfAWildcardOfAVariable(java.util.List<? extends E> errors) {
        try (var wildcardVariable = new AutoCloseable() {
            @Override
            public void close() {
                // closes nothing
            }
        }) {
            raiseWildcard(errors);
        } catch (java.io.IOException e) {
            e.printStackTrace();
        }
    }

    public String lambdaWhoseCheckedAnonymousTypeIsAnotherOne() {
        Object broadValue = new Object() {
        };
        var other = new Object() {
        };
        return adaptAndApply(broadValue, x -> java.util.Objects.requireNonNull(other).toString());
    }

    abstract static class AbstractIoClosingBase implements AutoCloseable {
        @Override
        public abstract void close() throws java.io.IOException;
    }

    abstract static class AbstractInheritingClass extends AbstractIoClosingBase implements SqlClosing {
        // inherits the abstract close of its superclass and the one of its interface
    }

    static <T extends AbstractInheritingClass> T sameAbstractInheriting(T value) {
        return value;
    }

    public int resourceWhoseSuperclassDeclaresAnAbstractClose() {
        try (AbstractInheritingClass abstractInherited = new AbstractInheritingClass() {
            @Override
            public void close() {
                // closes nothing
            }
        }) {
            return sameAbstractInheriting(abstractInherited).hashCode();
        }
    }

    static class PrivateClosingBase {
        @SuppressWarnings("unused")
        private void close() throws java.io.IOException {
            // is not inherited
            if (hashCode() == 0) {
                throw new java.io.IOException("never");
            }
        }
    }

    abstract static class PrivateCloseInheriting extends PrivateClosingBase implements IoClosing, SqlClosing {
        // inherits the closes of its interfaces only
    }

    static <T extends PrivateCloseInheriting> T samePrivateCloseInheriting(T value) {
        return value;
    }

    public int resourceWhoseSuperclassHasAPrivateClose() {
        try (PrivateCloseInheriting privateClose = new PrivateCloseInheriting() {
            @Override
            public void close() {
                // closes nothing
            }
        }) {
            return samePrivateCloseInheriting(privateClose).hashCode();
        }
    }

    interface ShadowedThrowsAction<T> {
        <T extends java.sql.SQLException> void run(Object value) throws T;
    }

    interface IoThrowsAction {
        void run(Object value) throws java.io.IOException;
    }

    interface ShadowedThrowsActions extends ShadowedThrowsAction<Exception>, IoThrowsAction {
    }

    interface IoClosingResource extends AutoCloseable {
        @Override
        void close() throws java.io.IOException;
    }

    public ShadowedThrowsActions lambdaWhoseThrownVariableIsShadowed() {
        return value -> {
            try (var shadowedThrow = new IoClosingResource() {
                @Override
                public void close() {
                    // closes nothing
                }
            }) {
                shadowedThrow.toString();
            }
        };
    }

    public String lambdaAdaptedThroughAnAliasAfterACheckedCall() {
        var checkedOrigin = new Object() {
        };
        java.util.Objects.requireNonNull(checkedOrigin).toString();
        var checkedAlias = checkedOrigin;
        return adaptAndApply(checkedAlias, x -> "ok");
    }

    public String lambdaAdaptedToObject() {
        Object broad = new Object() {
        };
        return adaptAndApply(broad, x -> "ok");
    }

    interface RedundantDefaultingAction extends BroadAction, DefaultedAction, ActualAction {
    }

    public RedundantDefaultingAction lambdaWithARedundantAncestorBeforeTheDefault() {
        return () -> {
            try (var redundant = new AutoCloseable() {
                @Override
                public void close() {
                    // closes nothing
                }
            }) {
                redundant.toString();
            }
        };
    }

    public DefaultingAction lambdaWithADefaultedInheritedMethod() {
        return () -> {
            try (var defaulted = new AutoCloseable() {
                @Override
                public void close() {
                    // closes nothing
                }
            }) {
                defaulted.toString();
            }
        };
    }

    static <E extends java.io.IOException> void raiseBound() throws E {
        // throws its bound
    }

    public int thrownVariableWithoutConstraint() {
        try (var unconstrained = new AutoCloseable() {
            @Override
            public void close() {
                // closes nothing
            }
        }) {
            raiseBound();
            return 1;
        } catch (java.io.IOException e) {
            return 2;
        }
    }

    interface GenericParameterRunner<T> {
        void run(T value) throws Exception;
    }

    interface StringRunner {
        void run(String value) throws java.io.IOException;
    }

    interface SubstitutedRunner extends GenericParameterRunner<String>, StringRunner {
    }

    public SubstitutedRunner lambdaWithSubstitutedInheritedParameters() {
        return value -> {
            try (var substituted = new SqlCloser() {
                @Override
                public void close() {
                    // closes nothing
                }
            }) {
                value.length();
            }
        };
    }

    interface InputTask extends Task<java.io.IOException> {
    }

    public InputTask lambdaWithAnInheritedGenericThrows() {
        return () -> {
            try (var inheritedTask = new AutoCloseable() {
                @Override
                public void close() {
                    // closes nothing
                }
            }) {
                ping();
            }
        };
    }

    @SuppressWarnings("java:S1172")
    public String existingResourceInANestedHandler(boolean flag) {
        AutoCloseable nestedExisting = new AutoCloseable() {
            @Override
            public void close() {
                // closes nothing
            }
        };

        try {
            if (flag) {
                try (nestedExisting) {
                    return castingIdentity(nestedExisting).toString();
                }
            }
        } catch (Exception e) {
            return e.toString();
        }
        return "";
    }

    public int charSequenceOperand(String text) {
        if (text instanceof CharSequence) {
            CharSequence sequence = (CharSequence) (Object) text;

            return sequence.length();
        }
        return 0;
    }

    public String castOfAnAliasInAForLoop(boolean run) {
        String result = "";
        boolean again = run;
        var loopOrigin = new Object() { };

        for (var aliased = loopOrigin; again; again = false) {
            result = unchecked(aliased).toString();
        }
        return result;
    }

    interface Closer<E extends Exception> extends AutoCloseable {
        @Override
        void close() throws E;
    }

    @SuppressWarnings({ "unchecked", "java:S1172" })
    static <T> T closerIdentity(T ignored) {
        Closer<java.io.IOException> closer = () -> { };

        return (T) closer;
    }

    public String resourceWhoseCloseThrowsATypeArgument() throws java.io.IOException {
        try (Closer<java.io.IOException> generic = new Closer<java.io.IOException>() {
            @Override
            public void close() {
                // closes nothing
            }
        }) {
            return closerIdentity(generic).toString();
        }
    }

    public <E extends java.io.IOException> String resourceWhoseCloseThrowsATypeVariable() throws java.io.IOException {
        try (Closer<E> genericVariable = new Closer<E>() {
            @Override
            public void close() {
                // closes nothing
            }
        }) {
            return closerIdentity(genericVariable).toString();
        }
    }

    public <E extends java.io.IOException> String methodWhichThrowsATypeVariable() throws E {
        try (Closer<E> declaredVariable = new Closer<E>() {
            @Override
            public void close() {
                // closes nothing
            }
        }) {
            return closerIdentity(declaredVariable).toString();
        }
    }

    public <E extends java.io.IOException> String boundNotPermittedByItsVariable(E failure) throws E {
        try (var bound = new Closer<java.io.IOException>() {
            @Override
            public void close() {
                // closes nothing
            }
        }) {
            raise(failure);
            return bound.toString();
        }
    }

    public <E extends java.io.IOException> Task<E> lambdaWhoseTargetHasATypeVariable() {
        return () -> {
            try (var variableTarget = new AutoCloseable() {
                @Override
                public void close() {
                    // closes nothing
                }
            }) {
                ping();
            }
        };
    }

    public <E extends java.io.IOException> int typeVariablePassedToAGenericCall(E failure) {
        try (var passed = new AutoCloseable() {
            @Override
            public void close() {
                // closes nothing
            }
        }) {
            raise(failure);
            return 1;
        } catch (java.io.IOException e) {
            return 2;
        }
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
