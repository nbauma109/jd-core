/*
 * Copyright (c) 2026 Nicolas Baumann (@nbauma109), with Claude Sonnet 5.5.
 * This project is distributed under the GPLv3 license.
 * This is a copyleft license that gives the user the right to use,
 * copy and modify the code freely for non-commercial purposes.
 */

package org.jd.core.v1.stub;

/**
 * 'VMSFTPEntryParser.parseFTPEntry' of commons-net: final variables which are assigned in every case of a switch, with null in the default one; javac writes a range of the local
 * variable table for each case, and the null of the default one comes after the end of the range of the previous case
 */
public class JavacNullInBranch {
    static String join(int variant, String first, String second) {
        final String group;
        final String user;

        switch (variant) {
            case 1:
                group = null;
                user = first.trim();
                break;
            case 2:
                group = first.trim();
                user = second.trim();
                break;
            default:
                group = null;
                user = null;
        }
        return group + ":" + user;
    }
}
