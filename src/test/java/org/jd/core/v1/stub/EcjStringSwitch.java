package org.jd.core.v1.stub;

/** The string switch of ECJ is a single switch on the hash code. */
public class EcjStringSwitch {
    public static int version(String name) {
        switch (name) {
            case "8.0":
                return 8;
            case "9.0":
                return 9;
            case "10.0":
                return 10;
            default:
                return -1;
        }
    }

    public static String describe(String key, StringBuilder out) {
        String result = null;
        switch (key) {
            case "and":
                out.append("a");
                result = "AND";
                break;
            case "not":
            case "nor":
                out.append("n");
                result = "NOT";
                break;
            case "Aa":
            case "BB":
                result = "COLLISION";
                break;
            default:
                out.append("?");
        }
        return result;
    }
}
