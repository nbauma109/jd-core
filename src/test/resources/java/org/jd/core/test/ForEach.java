package org.jd.core.test;

import java.util.Objects;

public class ForEach {

    <T> void iterate(T[] arr) {
        for (T elem : Objects.requireNonNull(arr)) {
            System.out.println(elem);
        }
    }
}
