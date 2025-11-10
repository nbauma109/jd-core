/*
 * Copyright (c) 2008-2019 Emmanuel Dupuy.
 * This project is distributed under the GPLv3 license.
 * This is a Copyleft license that gives the user the right to use,
 * copy and modify the code freely for non-commercial purposes.
 */

package org.jd.core.v1.service.writer;

import org.jd.core.v1.api.printer.Printer;
import org.jd.core.v1.model.message.DecompileContext;
import org.jd.core.v1.model.token.Token;
import org.jd.core.v1.service.writer.visitor.PrintTokenVisitor;

import java.util.List;

/**
 * Writes a list of tokens to a {@link org.jd.core.v1.api.printer.Printer}.
 *
 * <p><b>Input:</b> List of {@link org.jd.core.v1.model.token.Token}</p>
 * <p><b>Output:</b> None</p>
 */
public class WriteTokenProcessor {

    public void process(DecompileContext decompileContext) {
        Printer printer = decompileContext.getPrinter();
        List<Token> tokens = decompileContext.getTokens();
        PrintTokenVisitor visitor = new PrintTokenVisitor();
        int maxLineNumber = decompileContext.getMaxLineNumber();
        int majorVersion = decompileContext.getMajorVersion();
        int minorVersion = decompileContext.getMinorVersion();

        printer.start(maxLineNumber, majorVersion, minorVersion);
        visitor.start(printer, tokens);

        for (Token token : tokens) {
            token.accept(visitor);
        }

        visitor.end();
        printer.end();
    }
}
