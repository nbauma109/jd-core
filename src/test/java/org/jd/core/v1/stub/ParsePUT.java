package org.jd.core.v1.stub;

public class ParsePUT {

    int x;
    int y;

    public ParsePUT(int x, int y) {
        this.x %= x;
        this.y /= y;
        this.x += -1;
        this.y -= -1;
    }
}
