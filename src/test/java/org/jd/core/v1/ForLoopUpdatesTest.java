/*
 * Copyright (c) 2008, 2019 Emmanuel Dupuy.
 * This project is distributed under the GPLv3 license.
 * This is a Copyleft license that gives the user the right to use,
 * copy and modify the code freely for non-commercial purposes.
 */

package org.jd.core.v1;

import org.jd.core.v1.loader.ClassPathLoader;
import org.jd.core.v1.printer.PlainTextPrinter;
import org.junit.Test;

import java.util.Collections;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A 'for' whose body only contains a guarded 'break' (merged into the condition) keeps its update in the header.
 */
public class ForLoopUpdatesTest extends AbstractJdTest {
    private static final Map<String, Object> REALIGN = Collections.singletonMap("realignLineNumbers", Boolean.TRUE);
    private static final Pattern NUMBERED_LINE = Pattern.compile("(?m)^/\\*\\s*(\\d+):\\s*(\\d+) \\*/(.*)$");

    @Test
    public void testGuardedBreakIsMergedIntoTheConditionOfAForLoop() throws Exception {
        String source = decompileSuccess(new ClassPathLoader(), new PlainTextPrinter(), "org/jd/core/v1/stub/ForLoopUpdates", REALIGN);

        assertTrue(source, source.contains("actualSuffix--, expectedSuffix--);"));
        assertFalse(source, source.contains("while ("));

        Matcher matcher = NUMBERED_LINE.matcher(source);
        int numbered = 0;

        while (matcher.find()) {
            int original = Integer.parseInt(matcher.group(2));

            if (original != 0) {
                numbered++;
                assertEquals("Line " + matcher.group(1) + " is " + original + " in:\n" + source, original, Integer.parseInt(matcher.group(1)));
            }
        }
        assertTrue(source, numbered >= 8);
    }
}
