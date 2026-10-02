package org.jd.core.v1.stub;

/** The update of a 'for' header is on the line of the header, whatever the body is. */
public class ForLoopUpdates {
    private final String fExpected;
    private final String fActual;
    private final int fPrefix;
    private int fSuffix;

    public ForLoopUpdates(String expected, String actual, int prefix) {
        this.fExpected = expected;
        this.fActual = actual;
        this.fPrefix = prefix;
    }

    public void findCommonSuffix() {
        int expectedSuffix = fExpected.length() - 1;
        int actualSuffix = fActual.length() - 1;
        for (; actualSuffix >= fPrefix && expectedSuffix >= fPrefix; actualSuffix--, expectedSuffix--) {
            if (fExpected.charAt(expectedSuffix) != fActual.charAt(actualSuffix)) {
                break;
            }
        }
        fSuffix = fExpected.length() - expectedSuffix;
    }

    public int suffix() {
        return fSuffix;
    }
}
