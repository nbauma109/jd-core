/*
 * Copyright (c) 2026 Nicolas Baumann (@nbauma109), with Claude Sonnet 5.5.
 * This project is distributed under the GPLv3 license.
 * This is a copyleft license that gives the user the right to use,
 * copy and modify the code freely for non-commercial purposes.
 */

package org.jd.core.v1.stub;

/**
 * 'AbstractCommentParser.parseReference' of the Eclipse compiler: a switch in a loop leaves the loop by a label, and a loop of another case comes
 * after that jump
 */
public class EcjLoopExitFromSwitch {
    int index;
    int count;

    int read() {
        return this.index++;
    }

    int parse(String[] tokens, Object reference) {
        int found = 0;
        nextToken: while (this.index < tokens.length) {
            int token = read();
            switch (token) {
                case 0:
                    if (reference != null) {
                        break nextToken;
                    }
                    found++;
                    String text = tokens[token];
                    if (text.length() > 2) {
                        int i = 1;
                        while (i < text.length() && Character.isLetter(text.charAt(i))) {
                            i++;
                        }
                        if (i < text.length() - 2 && text.charAt(i) == ':') {
                            found += 10;
                        }
                    }
                    break;
                case 1:
                    this.count++;
                    break;
                default:
                    found += 2;
                    break;
            }
        }
        return found;
    }
}
