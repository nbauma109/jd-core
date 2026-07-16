package org.jd.core.v1.service.converter.classfiletojavasyntax.util.cfg;

import org.apache.bcel.classfile.Method;
import org.jd.core.v1.service.converter.classfiletojavasyntax.model.cfg.BasicBlock;
import org.jd.core.v1.service.converter.classfiletojavasyntax.model.cfg.ControlFlowGraph;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.jd.core.v1.service.converter.classfiletojavasyntax.model.cfg.BasicBlock.END;
import static org.jd.core.v1.service.converter.classfiletojavasyntax.model.cfg.BasicBlock.LOOP_CONTINUE;
import static org.jd.core.v1.service.converter.classfiletojavasyntax.model.cfg.BasicBlock.LOOP_END;
import static org.jd.core.v1.service.converter.classfiletojavasyntax.model.cfg.BasicBlock.LOOP_START;
import static org.jd.core.v1.service.converter.classfiletojavasyntax.model.cfg.BasicBlock.RETURN;
import static org.jd.core.v1.service.converter.classfiletojavasyntax.model.cfg.BasicBlock.SWITCH_BREAK;
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
 * <p>Ordinary statement merges are left alone on the first construction attempt. If construction reaches a
 * genuinely unreducible shared successor, the reducer records that exact offset, rebuilds the graph, and
 * detaches only that merge on the next attempt. Statement/value continuations use a one-off jump stub, and
 * {@code StatementMaker} later renders the once-only continuation inside one synthetic label. Terminal shapes
 * ({@code TYPE_RETURN}, {@code TYPE_RETURN_VALUE}, {@code TYPE_THROW}) are duplicated because they have no
 * continuation to label.</p>
 *
 * <p>A single upfront pass cannot always predict every merge, though: construction can itself aggregate
 * several predecessors' worth of code into a single bigger construct, which can leave some other node
 * reachable from more than one place even though it had exactly one predecessor going in. So after a full,
 * apparently-successful construction, {@link #reduce(Method)} scans the result for any node still reachable
 * from more than one predecessor; if it finds one, it rebuilds the whole graph from scratch and retries,
 * remembering that offset so the next pre-pass also splits it. This repeats until either nothing is left
 * shared (success) or the same offset shows up shared twice in a row (no further progress possible, reported
 * unreducible exactly as before this reducer existed).</p>
 *
 * <p>Because it only ever runs for methods the other two reducers already gave up on, none of this can regress
 * a method that was decompiling correctly.</p>
 */
public class DuplicateMergeCFGReducer extends CmpDepthCFGReducer {

    private static final int AUTOMATIC_SPLIT_TYPES = TYPE_RETURN | TYPE_RETURN_VALUE | TYPE_THROW | TYPE_TERNARY_OPERATOR;

    private final Set<Integer> forcedDuplicateOffsets = new HashSet<>();
    private final Set<Integer> unreducibleOffsets = new HashSet<>();

    public boolean addForcedDuplicateOffsets(Set<Integer> offsets) {
        return forcedDuplicateOffsets.addAll(offsets);
    }

    @Override
    public boolean reduce(Method method) {
        Set<Integer> forcedOffsets = new HashSet<>();

        while (true) {
            rebuildControlFlowGraph(method);

            ControlFlowGraph cfg = getControlFlowGraph();
            splitMultiPredecessorMerges(cfg, forcedOffsets);

            BasicBlock start = cfg.getStart();
            BitSet jsrTargets = new BitSet();
            BitSet visited = new BitSet(cfg.getBasicBlocks().size());

            unreducibleOffsets.clear();
            if (!reduce(visited, start, jsrTargets)) {
                if (forcedOffsets.addAll(unreducibleOffsets)) {
                    continue;
                }
                return false;
            }

            BasicBlock shared = findResidualSharedNode(cfg);

            if (shared == null) {
                return true;
            }
            if (!forcedOffsets.add(shared.getFromOffset())) {
                // Already forced this exact offset on a previous attempt and it is still shared: no further
                // progress is possible (the node has only one real predecessor, so there is nothing left to
                // detach), so give up exactly as this reducer would have before it existed.
                return false;
            }
        }

    }

    /**
     * Splits value/terminal merges plus the exact offsets identified by a failed or residual construction.
     * Ordinary statement merges are deferred until construction proves one is unreducible.
     */
    private void splitMultiPredecessorMerges(ControlFlowGraph cfg, Set<Integer> forcedOffsets) {
        for (BasicBlock target : new ArrayList<>(cfg.getBasicBlocks())) {
            boolean matchesNaturally = target.matchType(AUTOMATIC_SPLIT_TYPES);
            boolean forced = forcedOffsets.contains(target.getFromOffset());

            if ((!matchesNaturally && !forced) || target.getPredecessors().size() <= 1) {
                continue;
            }

            boolean duplicateEveryPredecessor = forcedDuplicateOffsets.contains(target.getFromOffset());

            if (forced && target.matchType(TYPE_CONDITIONAL_BRANCH)) {
                while (aggregateConditionalBranches(target)) {
                    // Normalize the branch before cloning its value flow.
                }
                if (!reduceConditionalBranch(target)) {
                    continue;
                }
            }

            List<BasicBlock> predecessors = new ArrayList<>(target.getPredecessors());
            int startIndex = duplicateEveryPredecessor ? 0 : 1;

            for (int i = startIndex; i < predecessors.size(); i++) {
                BasicBlock predecessor = predecessors.get(i);
                predecessor.replace(target, detachOneEdge(predecessor, target, duplicateEveryPredecessor));
            }
        }
    }

    /**
     * Finds a node still reachable from more than one predecessor after a fully successful construction -
     * a sign that some merge only became shared as a side effect of aggregating other code, rather than
     * being visible in the graph up front. Excludes the small set of immutable sentinel instances (loop/switch
     * exit markers, the bare 'return' singleton) that always report zero predecessors by design and are not
     * real merge points.
     */
    private static BasicBlock findResidualSharedNode(ControlFlowGraph cfg) {
        for (BasicBlock basicBlock : cfg.getBasicBlocks()) {
            if (basicBlock.getPredecessors().size() > 1 && !isImmutableSentinel(basicBlock)) {
                return basicBlock;
            }
        }
        return null;
    }

    private static boolean isImmutableSentinel(BasicBlock basicBlock) {
        return basicBlock == END || basicBlock == LOOP_END || basicBlock == LOOP_START
                || basicBlock == LOOP_CONTINUE || basicBlock == SWITCH_BREAK || basicBlock == RETURN;
    }

    private BasicBlock detachOneEdge(BasicBlock predecessor, BasicBlock target, boolean forceDuplicate) {
        if (!forceDuplicate && target.matchType(TYPE_STATEMENTS | TYPE_TERNARY_OPERATOR)) {
            return predecessor.getControlFlowGraph().newJumpBasicBlock(predecessor, target);
        }
        return duplicateForSinglePredecessor(predecessor, target);
    }

    @Override
    protected boolean reduceUnreducibleMerge(BasicBlock basicBlock, BasicBlock next, BasicBlock branch) {
        if (next.getPredecessors().size() > 1) {
            unreducibleOffsets.add(next.getFromOffset());
        }
        if (branch.getPredecessors().size() > 1) {
            unreducibleOffsets.add(branch.getFromOffset());
        }
        return false;
    }

    @Override
    public String getLabel() {
        return "Show Control Flow Graph with duplicated unreducible merge points";
    }
}
