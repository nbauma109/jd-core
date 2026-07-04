/*
 * Copyright (c) 2008-2019 Emmanuel Dupuy.
 * This project is distributed under the GPLv3 license.
 * This is a Copyleft license that gives the user the right to use,
 * copy and modify the code freely for non-commercial purposes.
 */

package org.jd.core.test;

import java.util.HashMap;
import java.util.Map;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

@SuppressFBWarnings
@SuppressWarnings("all")
public class SwitchEnumBig {

    enum Role {
        TESTS, PROFILE, SOFTWARE_BINS, HARDWARE_BINS, ALARM_CONFIG, SPECS, STDF_CONFIG, TEXTDATALOG_CONFIG, TPVARIABLES
    }

    private final Map<Role, Object> executors = new HashMap<>();

    private void createSomeExecutor(Role role) {
        switch (role) {
        case TESTS:
            executors.put(role, "tests");
            break;
        case PROFILE:
            executors.put(role, "profile");
            break;
        default:
            break;
        }
    }

    private void createExecutorIfNeeded(Role role) {
        if (executors.containsKey(role)) {
            return;
        }

        switch (role) {
        case TESTS:
            executors.put(role, "tests");
            break;
        case PROFILE:
            executors.put(role, "profile");
            break;
        case SOFTWARE_BINS:
            executors.put(role, "softwareBins");
            break;
        case HARDWARE_BINS:
            executors.put(role, "hardwareBins");
            break;
        case ALARM_CONFIG:
            executors.put(role, "alarmConfig");
            break;
        case SPECS:
            executors.put(role, "specs");
            break;
        case STDF_CONFIG:
            executors.put(role, "stdfConfig");
            break;
        case TEXTDATALOG_CONFIG:
            executors.put(role, "textDatalogConfig");
            break;
        case TPVARIABLES:
            executors.put(role, "tpVariables");
            break;
        default:
            throw new IllegalStateException("There is no executor for " + role);
        }
    }
}
