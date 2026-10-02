package org.jd.core.v1.stub;

/** An inner class which uses the private static members of its outer class through the synthetic accessors of javac 8. */
public class AccessorStatics {
    private static int counter;
    private static int total;
    private static String name = "x";

    private static int twice(int value) {
        return value * 2;
    }

    public static class Inner {
        public int use(int value) {
            counter++;
            ++total;
            total += value;
            name = name + counter;
            return twice(counter) + total + name.length();
        }
    }
}
