package org.jd.core.v1.stub;

import java.util.List;

@SuppressWarnings("all")
public class ReturnOnlyTypeVariable {

    interface IndexedMap {
        <V> V getValueAt(int index);
    }

    private IndexedMap map;

    public boolean isMatch(int i, String toMatch) {
        return toMatch != null && ((List<String>) map.getValueAt(i)).contains(toMatch);
    }
}
