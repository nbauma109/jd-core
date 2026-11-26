package sweepup;

public class SweepUpFixture {
    private int calls;

    public Object value() {
        return this;
    }

    public SweepUpFixture next() {
        calls++;
        return this;
    }

    public static Object combine(SweepUpFixture receiver, Object value) {
        return value;
    }

    public class First {
        public class Second {
            public class Third {
                public Object stable() {
                    SweepUpFixture receiver = SweepUpFixture.this;
                    Object result = null;
                    Object value = receiver.value();
                    result = combine(receiver, value);
                    return result;
                }

                public Object effectful() {
                    SweepUpFixture receiver = SweepUpFixture.this.next();
                    Object result = null;
                    Object value = receiver.value();
                    result = combine(receiver, value);
                    return result;
                }
            }
        }
    }
}
