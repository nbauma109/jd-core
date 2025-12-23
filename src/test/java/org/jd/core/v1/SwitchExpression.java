/*
 * Copyright (c) 2025 GPLv3.
 * This project is distributed under the GPLv3 license.
 * This is a Copyleft license that gives the user the right to use,
 * copy and modify the code freely for non-commercial purposes.
 */

package org.jd.core.v1;

public class SwitchExpression {

    public String simpleSwitch(int i) {
        String s = switch (i) {
        case 0 -> "Test.method4 case 0";
        case 1 -> "Test.method4 case 1";
        case 2 -> "Test.method4 case 2";
        case 3 -> "Test.method4 case 3";
        default -> throw new IllegalArgumentException();
        };
        return "return:" + s;
    }

    public String returnSwitch(int i) {
        return switch (i) {
        case 0 -> "Test.method4 case 0";
        case 1 -> "Test.method4 case 1";
        case 2 -> "Test.method4 case 2";
        case 3 -> "Test.method4 case 3";
        default -> throw new IllegalArgumentException();
        };
    }
    
    @SuppressWarnings("all")
    public String yieldSwitch(int i) {
        String s = switch (i) {
        case 0 -> {
            System.out.println("Test.method4 case 0");
            yield "0";
        }
        case 1 -> {
            System.out.println("Test.method4 case 1");
            yield "1";
        }
        case 2 -> {
            System.out.println("Test.method4 case 2");
            yield "2";
        }
        case 3 -> {
            System.out.println("Test.method4 case 3");
            yield "3";
        }
        default -> throw new IllegalArgumentException();
        };
        return "return:" + s;
    }
}
