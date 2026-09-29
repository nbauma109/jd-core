package org.jd.core.v1.stub;

public class UnusedOuterInstance {

    public class Inner {
        public Inner() {
            System.out.println("default");
        }

        public Inner(int value) {
            System.out.println(value);
        }

        public Inner(String text) {
            this(text.length());
        }
    }

    public Inner createDefault() {
        return new Inner();
    }

    public Inner createWithValue() {
        return new Inner(1);
    }

    public Inner createWithText() {
        return new Inner("text");
    }
}
