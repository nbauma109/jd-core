package org.jd.core.v1;

public class NestedSealedExample {
    public sealed interface Branch permits Branch.Child, Branch.RecordChild, Branch.EnumChild {
        final class Child implements Branch {}

        record RecordChild(int value) implements Branch {}

        enum EnumChild implements Branch { INSTANCE }
    }
}
