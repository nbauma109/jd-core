package org.jd.core.v1.stub;

@SuppressWarnings("all")
public class SuperOverload extends Base {

    interface Greeter {
        default String greet() {
            return "hello";
        }
    }

    static class Impl implements Greeter {
        String greet(String name) {
            return Greeter.super.greet() + " " + name;
        }
    }

    public long fit(long element) {
        return super.fit(Long.valueOf(element)).longValue();
    }
}
