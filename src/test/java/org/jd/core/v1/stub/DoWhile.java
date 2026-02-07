package org.jd.core.v1.stub;

public class DoWhile {

    void doWhileLoop(String[] arguments) {
        int index = 0;
        do {
            log(arguments[index++]);
        } while (index < arguments.length);
        index = 0;
        do {
            log(arguments[index++]);
        } while (index < arguments.length);
    }




    void whileLoop(String[] arguments) {
        int index = 0;
        while (true) {
            log(arguments[index++]);
            if (index >= arguments.length) {
                index = 0;
                do {
                    log(arguments[index++]);
                } while (index < arguments.length);
                return;
            }
        }
    }

    native void log(String string);
}