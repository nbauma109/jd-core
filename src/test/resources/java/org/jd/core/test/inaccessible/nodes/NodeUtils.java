package org.jd.core.test.inaccessible.nodes;

import org.jd.core.test.inaccessible.parser.HtmlTreeBuilder;
import org.jd.core.test.inaccessible.parser.Parser;

public final class NodeUtils {
    public Parser parser() {
        return new Parser(new HtmlTreeBuilder());
    }
}
