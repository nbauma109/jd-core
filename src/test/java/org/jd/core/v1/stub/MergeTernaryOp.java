package org.jd.core.v1.stub;

import java.util.List;

public class MergeTernaryOp {

    void test(List<?> l1, List<?> l2) {
        for (int i = 0; i < l1.size(); i++) {
            Object o1 = l1.get(i);
            Object o2 = l2.get(i);
            if (o1.toString() != null && o2.toString() != null ? o1.toString().equals(o2.toString()) : o1.toString() == null && o2.toString() == null) {
                break;
            }
        }
    }
}
