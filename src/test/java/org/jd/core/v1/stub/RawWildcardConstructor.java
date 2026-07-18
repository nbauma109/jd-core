package org.jd.core.v1.stub;

import java.util.ArrayList;
import java.util.List;

public class RawWildcardConstructor {
    private List<? extends Number> values;

    @SuppressWarnings({"rawtypes", "unchecked"})
    public void create() {
        values = new ArrayList();
    }
}
