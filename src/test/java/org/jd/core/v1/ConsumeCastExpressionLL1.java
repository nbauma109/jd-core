package org.jd.core.v1;

import org.eclipse.jdt.internal.compiler.ast.CastExpression;
import org.eclipse.jdt.internal.compiler.ast.Expression;
import org.eclipse.jdt.internal.compiler.ast.TypeReference;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

@SuppressWarnings("all")
@SuppressFBWarnings
public class ConsumeCastExpressionLL1 {
    /*
     * Code pattern found in org.eclipse.jdt.internal.compiler.parser.Parser.consumeCastExpressionLL1()
     * ClassCastException in Frame.createInlineDeclarations(...)
     */
    Expression[] expStack;
    int expPtr;

    protected void consumeCastExpressionLL1() {
        Expression cast, exp;
        expStack[expPtr] = cast = new CastExpression(exp=expStack[expPtr+1], (TypeReference)expStack[expPtr]);
    }
}