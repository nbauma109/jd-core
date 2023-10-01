package org.jd.core.v1.stub;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class TernaryOpDiamond {
    @SuppressWarnings("unused")
    void ternaryOp(boolean flag) {
        List<String> list = flag ? new ArrayList<String>() : Collections.<String>emptyList();
    }

    @SuppressWarnings("unused")
    void ternaryOp2(boolean flag) {
        List<String> list2 = flag ? Collections.<String>emptyList() : new ArrayList<String>();
    }
}