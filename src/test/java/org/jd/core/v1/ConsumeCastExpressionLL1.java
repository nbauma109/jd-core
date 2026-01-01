package org.jd.core.v1;

import org.eclipse.jdt.internal.compiler.ast.CastExpression;
import org.eclipse.jdt.internal.compiler.ast.Expression;
import org.eclipse.jdt.internal.compiler.ast.TypeReference;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

@SuppressFBWarnings
@SuppressWarnings("all")
public class ConsumeCastExpressionLL1 {
    /*
     * Code pattern found in org.eclipse.jdt.internal.compiler.parser.Parser.consumeCastExpressionLL1()
     * ClassCastException in Frame.createInlineDeclarations(...)
     */
    Expression[] expressionStack;
    int expressionPtr;

    protected void consumeCastExpressionLL1() {
        Expression cast, exp;
        this.expressionStack[this.expressionPtr] =
                cast = new CastExpression(
                    exp=this.expressionStack[this.expressionPtr+1] ,
                    (TypeReference) this.expressionStack[this.expressionPtr]);
    }
}