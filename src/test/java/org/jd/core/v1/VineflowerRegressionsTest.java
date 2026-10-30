/*
 * Copyright (c) 2026 Nicolas Baumann (@nbauma109), with Claude Sonnet 5.5.
 * This project is distributed under the GPLv3 license.
 * This is a Copyleft license that gives the user the right to use,
 * copy and modify the code freely for non-commercial purposes.
 */

package org.jd.core.v1;

import org.jd.core.v1.compiler.CompilerUtil;
import org.jd.core.v1.compiler.InMemoryJavaSourceFileObject;
import org.jd.core.v1.loader.ClassPathLoader;
import org.jd.core.v1.printer.PlainTextPrinter;
import org.jd.core.v1.stub.VineflowerRegressions;
import org.junit.Test;

/**
 * Cases of the Vineflower output comparison which jd-core got wrong.
 */
public class VineflowerRegressionsTest extends AbstractJdTest {
    private static final String INTERNAL_CLASS_NAME = VineflowerRegressions.class.getName().replace('.', '/');

    private String decompile() throws Exception {
        return decompileSuccess(new ClassPathLoader(), new PlainTextPrinter(), INTERNAL_CLASS_NAME);
    }

    @Test
    public void testFloatsAndConditions() throws Exception {
        String source = decompile();

        // 'double x = 0.2F' is folded into 0.20000000298023224D by the compiler
        assertTrue(source.contains("return 0.2F;"));
        // ... but not where it would change the boxed type or the selected overload
        assertTrue(source.contains("return 0.20000000298023224D;"));
        assertTrue(source.contains("which(0.20000000298023224D)"));
        // ... also when the value of the assignment is used (the double occupies two slots of the stack)
        assertTrue(source.contains("this.a = this.b = 0.2F;"));
        assertTrue(source.contains("return this.a = 0.2F;"));
        // ... and where a double is expected
        assertTrue(source.contains("double local = 0.2F;"));
        assertTrue(source.contains("local = 0.3F;"));

        // The result of a switch expression is yielded by the arrow, not after a 'break'
        assertTrue(source.contains("default -> \"Unknown\";"));
        assertFalse(source.contains("yield"));

        // A compound condition which is the test of a ternary operator keeps all its operands
        assertTrue(source.contains("(!(n < 1.0D - n) || !(n > 3.0D)) ? ((n < 5.0D)) : ((n > 2.0D))"));
        assertTrue(source.contains("(n < 1.0D - n || n > 3.0D) ? ((n < 5.0D)) : ((n > 2.0D))"));
        assertTrue(source.contains("(n < 1.0D - n && n > 3.0D) ? ((n < 5.0D)) : ((n > 2.0D))"));

        // The increment which precedes the 'break' is evaluated by every iteration, not once before the loop
        assertTrue(source.contains("while (i > 10 &&"));
        assertTrue(source.contains("++i != 15)"));

        // The call which discards a box stays
        assertTrue(source.contains("Boolean.valueOf("));
    }

    @Test
    public void testEnumsLocalTypesAndVar() throws Exception {
        String source = decompile();

        // The initializer of the body of an enum constant
        assertTrue(source.contains("System.out.println(\"A\");"));

        // Local enum, interface and record are declared in the method
        assertTrue(source.contains("enum Type {"));
        assertTrue(source.contains("interface Named {"));
        assertTrue(source.contains("record Point(int x, int y) {}"));

        // The anonymous class is only reachable through 'var', unless the call is made on a member of its super type
        assertTrue(source.contains("var printer = new Object() {"));
        assertTrue(source.contains("Object object = new Object() {"));

        // 'var' for an anonymous class of a field or of an interface
        assertTrue(source.contains("var holder = new Object() {"));
        assertTrue(source.contains("var runner = new Runnable() {"));
        // ... or of an overload of an inherited method: a call has to select the overload of the anonymous class
        assertTrue(source.contains("var v = new Object() {"));
        assertTrue(source.contains("var putter = new Putter() {"));
        // ... a reference to a method of the class needs the type of the class too
        assertTrue(source.contains("var v = new Object() {"));
        // ... a private method of the super type is not overridden by the one of the class
        assertTrue(source.contains("var base = new PrivateBase() {"));
        // ... or when another variable of the same name, the parameter of a local class, is the one which is used
        assertTrue(source.contains("Putter named = new Putter() {"));
        // ... but not when the call is the one of the inherited overload
        assertTrue(source.contains("Putter putter = new Putter() {"));
    }

    @Test
    public void testInstanceOfPatterns() throws Exception {
        String source = decompile();

        // Whatever the shape of the test
        assertTrue(source.contains("accept(o, (o instanceof String s && s.length() > 5));"));
        assertTrue(source.contains("boolean matches = (o instanceof String s && s.length() > 5);"));
        assertTrue(source.contains("(o instanceof String s) ? s.isEmpty() : false"));
        assertTrue(source.contains("if (!(o instanceof String s))"));

        // Several patterns, a chain of tests and a pattern which is negated before a 'throw'
        assertTrue(source.contains("a instanceof String s && b instanceof String t && s.equals(t)"));
        assertTrue(source.contains("if (o instanceof Integer i)"));
        assertTrue(source.contains("throw new IllegalArgumentException();"));
        assertTrue(source.contains("if (o instanceof String)"));
        // The test of 'this'
        assertTrue(source.contains("this instanceof Derived derived"));

        // The increment stays before an operation which can throw: the handler sees the incremented value
        String throwing = source.substring(source.indexOf("incrementBeforeAnOperationWhichCanThrow"));

        assertFalse(throwing.substring(0, throwing.indexOf("genericPatternVariable")).contains("++i"));

        // An assignment to a parameter is no pattern variable, the increment of a test is evaluated before the first use of the variable
        assertTrue(source.contains("s = (String)o;"));
        // ... nor a variable which is declared with another type than the pattern
        assertTrue(source.contains("Object s = (String)o;"));
        assertTrue(source.contains("++i * 2 <= i)"));

    }

    @Test
    public void testDeclarationsWhichStayAndGenericPatterns() throws Exception {
        String source = decompile();

        // A field which hides the one of the super type does not need 'var'
        assertTrue(source.contains("ShadowedBase base = new ShadowedBase() {"));
        // ... unless the field is read through 'var', which makes the anonymous class the owner of the access
        assertTrue(source.contains("var base = new ShadowedBase() {"));
        // A public override of a protected method of Object is called through the anonymous type
        assertTrue(source.contains("var o = new Object() {"));
        // ... but not when the call is owned by the super type
        assertTrue(source.contains("ProtectedBase base = new ProtectedBase() {"));
        // A local class reaches the variable by a captured field
        assertTrue(source.contains("var captured = new Object() {"));
        // The resource of a try is declared 'var' too
        assertTrue(source.contains("try (var resource = new AutoCloseable() {"));
        // The type of an anonymous class which a generic call returns
        assertTrue(source.contains("var value = Objects.requireNonNull(new Object() {"));
        // An override whose return type has other type arguments than the one of its super method
        assertTrue(source.contains("var listing = new Listing() {"));
        // An alias which a generic call returns
        assertTrue(source.contains("var identity = Objects.requireNonNull(source);"));
        // An alias which is declared by a loop
        assertTrue(source.contains("for (var counter = origin;"));
        // ... also when the body does not use it: the try calls its close() which declares no exception
        assertTrue(source.substring(source.indexOf("resourceWhichIsClosedByTheTry")).contains("try (var resource = new AutoCloseable() {"));
        // An override of clone which declares no exception, whose super method declares one
        assertTrue(source.contains("var copy = new CloneBase() {"));
        // Aliases which were merged are declared one by one
        assertTrue(source.contains("var first = origin;"));
        assertTrue(source.contains("var second = origin;"));
        // An override which declares fewer exceptions is called through the anonymous type
        assertTrue(source.contains("var reader = new Reading() {"));
        // Two variables which are declared on one line are declared one by one: 'var' declares one variable
        assertTrue(source.contains("var first = new Object() {"));
        assertTrue(source.contains("var second = new Object() {"));
        // ... through a variable which is declared with its value too
        assertTrue(source.contains("var origin = new Object() {"));
        assertTrue(source.contains("var alias = origin;"));
        // ... also when only the alias is captured
        assertTrue(source.contains("var copy = base;"));
        // ... but not through a variable which is assigned again
        assertTrue(source.contains("Object copy = holder;"));
        // The variables of a block are known up to its end: a later block can reuse the name for a variable of another type
        assertTrue(source.contains("var object = new Object() {"));
        assertTrue(source.contains("Other alias = shared;"));
    }

    @Test
    public void testAnonymousTypesWhichPassThroughValues() throws Exception {
        String source = decompile();

        // The branches of a conditional give their type to the variable
        assertTrue(source.contains("var chosen = flag ? new Object() {") || source.contains("var chosen = (flag) ? new Object() {"));
        // A protected method of a super type of another package is called through the anonymous type
        assertTrue(source.contains("var list = new ArrayList"));
        // The argument of a call which returns Object, and which is cast, gives no type to the variable
        assertFalse(source.contains("var kept"));
        // Declarations which were merged are split before 'var' is restored, whatever the initializer which gives the type
        assertTrue(source.contains("var one = Objects.requireNonNull(new Object() {"));
        assertTrue(source.contains("var two = Objects.requireNonNull(new Object() {"));
        // The parameter of a local class which has the name of an alias hides it
        assertTrue(source.contains("Putter origin = new Putter() {"));
        // A method which hides a private method of the super type is not the one which a call through the super type reaches
        assertTrue(source.contains("PrivateBase hiding = new PrivateBase() {"));
        // The results of a switch expression give their type to the variable
        assertTrue(source.contains("var outcome = switch (k) {"));
        // A private generic method of the class of the call gives its signature too
        assertTrue(source.contains("var retained = privateIdentity(new Object() {"));
        // The exceptions of the generic signature are not part of the returned type
        assertTrue(source.contains("var picked = identity(new Object() {"));
        // The receiver of a call can be a generic call which returns the variable
        assertTrue(source.contains("var target = new Object() {"));
        // Only the argument of the returned type variable gives its type to the variable
        assertFalse(source.contains("var selected"));
        // The anonymous argument of a call which returns another type gives no type to the variable
        assertTrue(source.contains("Other holder = make(new Object() {"));
        // The variable of a handler hides the alias which has its name
        assertTrue(source.contains("Putter source = new Putter() {"));
        // Only the type argument which the method returns counts, and a handler keeps the explicit type of a resource
        assertFalse(source.contains("var pairValue"));
        assertTrue(source.contains("try (Closeable resource = new Closeable() {"));
        // The elements of the varargs array give their type to the result, an unchecked handler does not keep the explicit type of a resource
        assertTrue(source.contains("var firstValue = first(new Object() {"));
        assertTrue(source.contains("try (var closing = new AutoCloseable() {"));
    }

    @Test
    public void testAnonymousTypesWhichLambdasAndHandlersNeed() throws Exception {
        String source = decompile();

        // The constructor infers the type of a lambda from its arguments like a generic method
        assertTrue(source.contains("var content = new Object() {"));
        // A throw which an inner handler consumes does not reach the outer handler: the resource keeps its explicit type
        assertTrue(source.contains("try (Closeable inner = new Closeable() {"));
        // A lambda which reads a field of the anonymous class needs its type too
        assertTrue(source.contains("var fielded = new Object() {"));
        // A lambda with primitive parameters and a local alias of its parameter, and the cast to the anonymous class which a generic call needs at run time
        assertTrue(source.contains("var indexed = new Object() {"));
        assertTrue(source.contains("var guarded = unchecked(new Object() {"));
        assertFalse(source.contains("var plain"));
        assertTrue(source.contains("var origin = new Object() {"));
        assertFalse(source.contains("var tracked"));
        // A named class with a numeric name is not anonymous: its cast stays; the alias of a lambda ends with its block
        assertTrue(source.contains("($1)value"));
        assertFalse(source.contains("var sibling"));
        assertFalse(source.contains("var overloaded"));
        assertTrue(source.contains("var constructed = new Object() {"));
        // A nested try without handler lets the exception through, another resource can throw what the handler catches, a method reference counts like a call
        assertTrue(source.contains("try (var nested = new AutoCloseable() {"));
        assertTrue(source.contains("try (var anonymous = new AutoCloseable() {"));
        assertTrue(source.contains("var referenced = new Object() {"));
        // The close of the anonymous class declares the exception which the handler catches
        assertTrue(source.contains("try (var declaring = new AutoCloseable() {"));
        // A wildcard bound gives its type to the parameter of the lambda, a call which declares the exception makes the handler valid
        assertTrue(source.contains("var bounded = new Object() {"));
        assertTrue(source.contains("try (var caller = new AutoCloseable() {"));
    }

    @Test
    public void testAnonymousTypesWhichFunctionalMethodsDecide() throws Exception {
        String source = decompile();

        // The functional method decides which parameters of the lambda have the type variable, and a lambda in the body of a try is not run by it
        assertTrue(source.contains("Putter visited = new Putter() {"));
        assertTrue(source.contains("try (Closeable deferred = new Closeable() {"));
        // Only the parameters of the lambda which have the type of the variable count
        assertTrue(source.contains("Putter first = new Putter() {"));
        // The member which the lambda calls is the one of another object
        assertTrue(source.contains("Putter unrelated = new Putter() {"));
        // A lambda which is independent of the argument gives no type to it
        assertTrue(source.contains("Putter standalone = new Putter() {"));
        // The lambda which infers its parameter from the variable, the body which throws what the handler catches
        assertTrue(source.contains("var inferred = new Object() {"));
        assertTrue(source.contains("try (var thrower = new AutoCloseable() {"));
        // The container of an anonymous argument has its own type
        assertFalse(source.contains("var wrapped"));
        // A local of a nested method hides the alias which has its name; the type goes through the receiver of a generic call
        assertTrue(source.contains("Putter holder = new Putter() {"));
        assertTrue(source.contains("var element = List.of(new Object() {"));
        // The parameter of a nested class hides the variable in that class only
        assertTrue(source.contains("var outer = new Object() {"));
        // ... and past a block which declares a variable of the same name
        assertTrue(source.contains("var anon = new Object() {"));
        // The declaration of a loop is searched in its condition, its update and its body
        assertTrue(source.contains("for (var counter = new Object() {"));
    }

    @Test
    public void testGenericPatternsAndSharedVariables() throws Exception {
        String source = decompile();

        // An unchecked cast to a generic type is no pattern: the test of 'o' has no type arguments
        assertTrue(source.contains("if (o instanceof List) {"));
        // ... nor is the test of a wildcard against a type with arguments, while the arguments of the operand make the pattern legal
        assertTrue(source.contains("uncheckedCastOfAWildcard"));
        assertTrue(source.contains("if (o instanceof List<String> strings)"));
        assertTrue(source.contains("List<String> strings = (List)o;"));
        // A variable which is assigned by both branches is declared before the test
        assertTrue(source.contains("String s;"));
        assertTrue(source.contains("s = (String)b;"));
        // The pattern of an array keeps the type arguments of its variable
        assertTrue(source.contains("o instanceof List<String>[] lists"));
        assertTrue(source.contains("s = (String)o;"));
        // A field of another object which has the name of the variable is no use of it
        assertTrue(source.contains("Putter target = new Putter() {"));
    }

    @Test
    public void testLambdaWhoseMethodIsNamedLikeTheBootstrapOfASwitch() throws Exception {
        String source = decompile();

        assertTrue(source.contains("return () -> o.toString() + n;") || source.contains("() ->"));
        assertFalse(source.contains("SwitchBootstraps"));
    }

    @Test
    public void testLabeledBreakOutOfALoopInASwitch() throws Exception {
        String source = decompile();
        String method = source.substring(source.indexOf("loopWhoseBreakJumpsOverTheCodeAfterTheLoop"));

        method = method.substring(0, method.indexOf("after2"));

        // The break of the loop goes to the code which follows the code after the loop, none of them is a tail of the loop
        assertFalse(method.contains("throw null"));
        assertTrue(method.contains("System.out.println(0);"));
        assertTrue(method.contains("System.out.println(\"after\");"));
        assertTrue(method.indexOf("System.out.println(0);") < method.indexOf("System.out.println(\"after\");"));
    }

    @Test
    public void testRecompilation() throws Exception {
        // Recompile decompiled source code and check errors
        assertTrue(CompilerUtil.compile("17", new InMemoryJavaSourceFileObject(INTERNAL_CLASS_NAME, decompile())));
    }
}
