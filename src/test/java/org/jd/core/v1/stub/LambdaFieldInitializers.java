package org.jd.core.v1.stub;

import java.util.function.BiFunction;
import java.util.function.Function;

/** The parameters of the lambdas which initialize fields are not local variables of the constructor. */
public class LambdaFieldInitializers {
    private final BiFunction<Integer, Integer, Integer> adder = (a, b) -> a + b;
    private final Function<String, String> echo = text -> text;
    private final String name;

    public LambdaFieldInitializers(String name) {
        this.name = name;
    }

    public int add(int a, int b) {
        return adder.apply(a, b);
    }

    public String echo() {
        return echo.apply(name);
    }
}
