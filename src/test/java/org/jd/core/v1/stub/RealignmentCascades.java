package org.jd.core.v1.stub;

public class RealignmentCascades {
    /** An enum without constant: the ';' which ends the list of constants takes a line */
    enum Utils {
        ;

        public static final int MAX = read();

        private static int read() {
            return Integer.getInteger("x", 8);
        }
    }

    /** The line break between the ':' and the expression which follows the body of an anonymous class */
    public Object make(boolean anonymous) {
        Object info =
            anonymous ?
                new Object() {
                    @Override
                    public String toString() {
                        return "anonymous";
                    }
                } :
            new Object();
        info.hashCode();
        return info;
    }
}
