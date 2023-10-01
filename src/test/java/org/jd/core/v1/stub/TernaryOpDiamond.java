package org.jd.core.v1.stub;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class TernaryOpDiamond {
    @SuppressWarnings("unused")
    void ternaryOp(boolean flag) {
        List<String> list = flag ? new ArrayList<String>() : Collections.<String>emptyList();
    }
}