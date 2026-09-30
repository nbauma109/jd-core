package org.jd.core.v1.stub;

public class OuterParameterEdgeCases {

    static {
        class InInitializer {
            final Object outer;

            @SuppressWarnings("java:S117") // deliberately named like a synthetic outer-instance parameter
            InInitializer(OuterParameterEdgeCases this$0) {
                this.outer = this$0;
            }
        }
        System.out.println(new InInitializer(new OuterParameterEdgeCases()).outer);
    }

    final Runnable instanceInitializerAnonymous = new Runnable() {
        @Override
        public void run() {
            System.out.println("run");
        }
    };

    public static Object staticContextLocal() {
        class Local {
            final int value;

            @SuppressWarnings("java:S117") // deliberately named like a synthetic outer-instance parameter
            Local(int this$value) {
                this.value = this$value;
            }
        }
        class Enclosing {
            final Object outer;

            @SuppressWarnings("java:S117") // deliberately named like a synthetic outer-instance parameter
            Enclosing(OuterParameterEdgeCases this$0) {
                this.outer = this$0;
            }
        }
        return new Local(1).value + "" + new Enclosing(new OuterParameterEdgeCases()).outer;
    }

    public Object shadowedCapture(int x) {
        class Local {
            int captured = x;
            int shadowing;

            Local(int x) {
                this.shadowing = x;
            }
        }
        Local local = new Local(x + 1);
        return local.captured + local.shadowing;
    }

    public class Inner {
        @SuppressWarnings("java:S116") // deliberately named like a synthetic outer-instance field
        OuterParameterEdgeCases this$0 = new OuterParameterEdgeCases();

        public Inner() {
            System.out.println(this$0);
        }
    }
}
