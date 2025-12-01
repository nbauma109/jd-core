package org.jd.core.v1;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

public class Entries {
    Map<Integer, String[]> cCache = new HashMap<>();

    Map<String, Entry<String, String>> entries = new HashMap<>();

    void test() {
        for (Entry<String, String> entry : new ArrayList<>(entries.values())) {
            System.out.println(entry);
        }
    }
}