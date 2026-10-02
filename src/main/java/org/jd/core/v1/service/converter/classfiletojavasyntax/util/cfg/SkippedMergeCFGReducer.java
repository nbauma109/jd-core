package org.jd.core.v1.service.converter.classfiletojavasyntax.util.cfg;

import org.jd.core.v1.service.converter.classfiletojavasyntax.util.ControlFlowGraphReducer;

/**
 * Tried after {@link MinDepthCFGReducer} and {@link CmpDepthCFGReducer} have failed the method, and before the
 * last-resort {@link DuplicateMergeCFGReducer}: some code which follows an 'if ... else if ...' chain is reached from
 * several arms of the chain, while the other arms leave it by a 'break' which goes over that code. Such a continuation
 * is not an arm of any condition: once the arms which go over it are made explicit jumps (see
 * {@link ControlFlowGraphReducer#turnJumpsOverMergesIntoStubs}), the usual construction builds the chain with the
 * continuation as its successor, and the jumps are rendered as 'break' to a label.
 *
 * <p>This is not done for every method, as a continuation reached by several conditions is usually just the arm of a
 * boolean expression ('a && b && c') which the usual construction aggregates.</p>
 */
public class SkippedMergeCFGReducer extends MinDepthCFGReducer {

    public SkippedMergeCFGReducer(boolean preReduce) {
        super(preReduce);
    }

    @Override
    protected void afterPreReduce() {
        turnJumpsOverMergesIntoStubs(getControlFlowGraph());
    }

    @Override
    public String getLabel() {
        return "Show Control Flow Graph with explicit jumps over shared continuations";
    }
}
