package org.jd.core.v1.service.converter.classfiletojavasyntax.util.cfg;

import org.apache.bcel.classfile.Method;
import org.jd.core.v1.service.converter.classfiletojavasyntax.model.cfg.BasicBlock;
import org.jd.core.v1.service.converter.classfiletojavasyntax.model.cfg.ControlFlowGraph;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.jd.core.v1.service.converter.classfiletojavasyntax.model.cfg.BasicBlock.TYPE_CONDITIONAL_BRANCH;
import static org.jd.core.v1.service.converter.classfiletojavasyntax.model.cfg.BasicBlock.TYPE_RETURN;
import static org.jd.core.v1.service.converter.classfiletojavasyntax.model.cfg.BasicBlock.TYPE_RETURN_VALUE;
import static org.jd.core.v1.service.converter.classfiletojavasyntax.model.cfg.BasicBlock.TYPE_STATEMENTS;
import static org.jd.core.v1.service.converter.classfiletojavasyntax.model.cfg.BasicBlock.TYPE_TERNARY_OPERATOR;
import static org.jd.core.v1.service.converter.classfiletojavasyntax.model.cfg.BasicBlock.TYPE_THROW;

/**
 * Last-resort reducer, tried only after {@link MinDepthCFGReducer} and {@link CmpDepthCFGReducer} have both
 * failed the whole method. A labeled non-loop block with several {@code break label;} sites converging on
 * the same tail (e.g. joda-time's {@code DateTimeFormatterBuilder$TimeZoneOffset#parseInto}) leaves that
 * tail with more than one predecessor, which every construction heuristic in the base class requires
 * exactly one of, so those reducers report the method unreducible rather than risk a wrong guess.
 *
 * <p>Before construction is attempted at all, a pre-pass walks the whole graph and gives every merge point
 * with more than one predecessor a single real predecessor, exactly mirroring what the original source did:
 * a labeled block whose content is rendered once, with every other 'break label;' site jumping to it.
 * Statement-shaped merges ({@code TYPE_STATEMENTS}) route every extra predecessor through a one-off
 * jump stub (the same mechanism jd-core already uses for loop-exit gotos, see
 * {@code ControlFlowGraphReducer#changeEndLoopToJump}), and {@code StatementMaker} later wraps the merge
 * content in a synthetic label and turns those stubs into {@code break label;} (see
 * {@code StatementMaker#resolveRemainingJumpsWithLabels}). Value-computing merges
 * ({@code TYPE_TERNARY_OPERATOR}) use the same mechanism. Terminal shapes ({@code TYPE_RETURN},
 * {@code TYPE_RETURN_VALUE}, {@code TYPE_THROW}) are duplicated too, same as before, since they have no
 * meaningful continuation to route through a label in the first place.</p>
 *
 * <p>Because it only ever runs for methods the other two reducers already gave up on, none of this can regress
 * a method that was decompiling correctly.</p>
 */
public class DuplicateMergeCFGReducer extends CmpDepthCFGReducer {

    private static final int DUPLICABLE_TYPES = TYPE_STATEMENTS | TYPE_RETURN | TYPE_RETURN_VALUE | TYPE_THROW | TYPE_TERNARY_OPERATOR;

    private static final int MAX_SPLIT_ITERATIONS = 1000;

    private final Set<Integer> forcedDuplicateOffsets = new HashSet<>();

    public boolean addForcedDuplicateOffsets(Set<Integer> offsets) {
        return forcedDuplicateOffsets.addAll(offsets);
    }

    @Override
    public boolean reduce(Method method) {
        rebuildControlFlowGraph(method);

        ControlFlowGraph cfg = getControlFlowGraph();
        splitMultiPredecessorMerges(cfg);

        BasicBlock start = cfg.getStart();
        BitSet jsrTargets = new BitSet();
        BitSet visited = new BitSet(cfg.getBasicBlocks().size());
        return reduce(visited, start, jsrTargets);
    }

    /**
     * Repeatedly scans the graph for merge points with more than one predecessor - either a duplicable-type
     * node found that way naturally, or any node at an offset a previous rebuild attempt found still shared
     * after construction - and gives every predecessor but the first its own private edge, until none remain.
     * A fixed point (not a single pass) is kept as a safety margin: {@code TYPE_TERNARY_OPERATOR} still
     * duplicates its own value-computing node per extra predecessor, and if that ever leaves a fresh clone
     * sharing a further node, this catches it too.
     */
    private void splitMultiPredecessorMerges(ControlFlowGraph cfg) {
        boolean changed = true;
        int iterations = 0;

        while (changed && iterations++ < MAX_SPLIT_ITERATIONS) {
            changed = false;

            for (BasicBlock target : new ArrayList<>(cfg.getBasicBlocks())) {
                boolean matchesNaturally = target.matchType(DUPLICABLE_TYPES);
                boolean forced = forcedDuplicateOffsets.contains(target.getFromOffset());

                if ((!matchesNaturally && !forced) || target.getPredecessors().size() <= 1) {
                    continue;
                }

                boolean forceDuplicate = forcedDuplicateOffsets.contains(target.getFromOffset());

                if (forced && target.matchType(TYPE_CONDITIONAL_BRANCH)) {
                    while (aggregateConditionalBranches(target)) {
                        // Normalize the branch before cloning its value flow.
                    }
                    if (!reduceConditionalBranch(target)) {
                        continue;
                    }
                }

                List<BasicBlock> predecessors = new ArrayList<>(target.getPredecessors());

                int startIndex = forceDuplicate ? 0 : 1;

                for (int i = startIndex; i < predecessors.size(); i++) {
                    BasicBlock predecessor = predecessors.get(i);
                    predecessor.replace(target, detachOneEdge(predecessor, target, forceDuplicate));
                }

                changed = true;
            }
        }
    }

    /**
     * Defense in depth: the pre-pass above should leave no duplicable node with more than one predecessor,
     * but if reduction itself creates a new shared reference partway through, fall back to detaching just
     * the one failing edge and retrying, same as the very first version of this reducer did.
     */
    @Override
    protected boolean reduceUnreducibleMerge(BasicBlock basicBlock, BasicBlock next, BasicBlock branch) {
        boolean duplicated = false;

        if (next.matchType(DUPLICABLE_TYPES) && next.getPredecessors().size() > 1) {
            basicBlock.setNext(detachOneEdge(basicBlock, next, forcedDuplicateOffsets.contains(next.getFromOffset())));
            duplicated = true;
        }
        if (branch.matchType(DUPLICABLE_TYPES) && branch.getPredecessors().size() > 1) {
            basicBlock.setBranch(detachOneEdge(basicBlock, branch, forcedDuplicateOffsets.contains(branch.getFromOffset())));
            duplicated = true;
        }

        return duplicated && reduceConditionalBranch(basicBlock);
    }

    private BasicBlock detachOneEdge(BasicBlock predecessor, BasicBlock target, boolean forceDuplicate) {
        if (!forceDuplicate && target.matchType(TYPE_STATEMENTS | TYPE_TERNARY_OPERATOR)) {
            return predecessor.getControlFlowGraph().newJumpBasicBlock(predecessor, target);
        }
        return duplicateForSinglePredecessor(predecessor, target);
    }

    @Override
    public String getLabel() {
        return "Show Control Flow Graph with duplicated unreducible merge points";
    }
}
