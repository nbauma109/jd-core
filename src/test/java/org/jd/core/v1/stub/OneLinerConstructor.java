package org.jd.core.v1.stub;

public class OneLinerConstructor {
    private final String name = String.valueOf(System.nanoTime());
    private final int value;

    public OneLinerConstructor(int v) { this.value = v; }

    public int value() { return this.value; }
}
