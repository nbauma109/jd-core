package org.jd.core.v1.stub;

/** Local classes whose declaration must stay before every class and statement that needs it */
public class LocalClassPlacement {
    /** The class used in the signature of another local class (a field here) stays visible to it */
    public static Object itemInTheSignatureOfAnotherLocalClass(boolean flag) {
        class Item {
        }
        Object first = null;

        if (flag) {
            first = new Item();
        }
        class Holder {
            Item value;

            Item get(Item other) throws IllegalStateException {
                return value == null ? other : value;
            }
        }
        return first == null ? new Holder().get(null) : first;
    }
}
