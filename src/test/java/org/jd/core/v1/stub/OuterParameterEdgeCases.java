package org.jd.core.v1.stub;

public class OuterParameterEdgeCases {

    public static Object staticContextLocal() {
        class Local {
            final int value;

            @SuppressWarnings("java:S117") // deliberately named like a synthetic outer-instance parameter
            Local(int this$value) {
                this.value = this$value;
            }
        }
        return new Local(1).value;
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
