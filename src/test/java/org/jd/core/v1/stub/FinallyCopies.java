package org.jd.core.v1.stub;

public class FinallyCopies {
    private boolean flag;
    private int count;
    private int index;
    private int[] tokens = new int[8];

    public boolean parse(int start) {
        boolean saved = this.flag;
        this.flag = true;
        try {
            boolean valid = true;
            boolean typeParam = false;
            int token;
            nextToken: while (true) {
                token = this.tokens[this.index++];
                switch (token) {
                    case 1:
                        if (valid) {
                            typeParam = true;
                            break nextToken;
                        }
                    default:
                        valid = false;
                        if (this.count > 3) {
                            break;
                        }
                    case 2:
                        this.count++;
                        if (valid) break;
                    case 0:
                        if (this.count > 0) {
                            this.count = start;
                            this.index = start;
                        }
                        this.flag = false;
                        return false;
                }
            }
            if (typeParam) {
                nextToken: while (true) {
                    token = this.tokens[this.index++];
                    switch (token) {
                        case 2:
                            if (valid) continue;
                        case 0:
                            this.count = 0;
                            if (this.count == 1) {
                                this.index = start;
                            }
                            return false;
                        case 3:
                            valid = true;
                            break nextToken;
                    }
                    valid = false;
                }
                boolean spaces = false;
                while (this.index < 10) {
                    token = this.tokens[this.index++];
                    switch (token) {
                        case 4:
                            spaces = true;
                            if (valid) continue;
                        case 0:
                            this.count = 2;
                            return false;
                        case 5:
                            if (valid) {
                                this.count = 5;
                                break;
                            }
                            continue;
                    }
                    valid = false;
                }
            }
            if (valid) {
                this.count = 6;
                this.index++;
                token = this.tokens[this.index];
                if (token == 7) {
                    this.count = 7;
                    return true;
                }
            }
            this.count = 8;
            this.index = start;
            return false;
        } finally {
            this.flag = saved;
        }
    }
}
