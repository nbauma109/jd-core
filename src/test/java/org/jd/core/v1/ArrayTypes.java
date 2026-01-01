package org.jd.core.v1;

import java.util.List;

public class ArrayTypes {

    @SuppressWarnings("unused")
    void arrays() {
        try {
            boolean[][] booleans = { { true, false }, { false, true } };
            short[][] shorts = { { 0, 1 }, { 1, 0 } };
            byte[][] bytes = { { 0, 1 }, { 1, 0 } };
            char[][] chars = { { 0, 1 }, { 1, 0 } };
            int[][] ints = { { 0, 1 }, { 1, 0 } };
            long[][] longs = { { 0, 1 }, { 1, 0 } };
            float[][] floats = { { 0, 1 }, { 1, 0 } };
            double[][] doubles = { { 0, 1 }, { 1, 0 } };
            ints = new int[0][0];
            List<String> value = null;
            System.out.println(value);
            System.out.println(Object.class);
            System.out.println(Float.MAX_VALUE);
            System.out.println(Double.MAX_VALUE);
            System.out.println(Integer.MAX_VALUE);
            System.out.println(Long.MAX_VALUE);
            System.out.println("\b\f\n\r\t\s");
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            System.out.println("finally");
        }
    }
}