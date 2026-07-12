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
 * Statement-shaped merges ({@code TYPE_STATEMENTS}) are not duplicated at all - every extra predecessor is
 * simply routed through a one-off jump stub (the same mechanism jd-core already uses for loop-exit gotos, see
 * {@code ControlFlowGraphReducer#changeEndLoopToJump}), and {@code StatementMaker} later wraps the merge
 * content in a synthetic label and turns those stubs into {@code break label;} (see
 * {@code StatementMaker#resolveRemainingJumpsWithLabels}). Value-computing merges ({@code TYPE_TERNARY_OPERATOR})
 * cannot be handled this way - a {@code break} is a statement, it cannot stand in for the value a ternary
 * computes - so those are still duplicated per extra predecessor (see
 * {@code ControlFlowGraphReducer#duplicateForSinglePredecessor}); terminal shapes ({@code TYPE_RETURN},
 * {@code TYPE_RETURN_VALUE}, {@code TYPE_THROW}) are duplicated too, same as before, since they have no
 * meaningful continuation to route through a label in the first place.</p>
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

    private static final int DUPLICABLE_TYPES = TYPE_STATEMENTS | TYPE_RETURN | TYPE_RETURN_VALUE | TYPE_THROW | TYPE_TERNARY_OPERATOR;

    private static final int MAX_SPLIT_ITERATIONS = 1000;
    private static final int MAX_REBUILD_ATTEMPTS = 50;

    // BasicBlock#equals is index-based and every immutable sentinel shares index -1, so a Set (or Set.of,
    // which would reject them outright as "duplicates") cannot be used here: identity is what matters.
    private static final BasicBlock[] IMMUTABLE_SENTINELS = {END, LOOP_END, LOOP_START, LOOP_CONTINUE, SWITCH_BREAK, RETURN};

    @Override
    public boolean reduce(Method method) {
        Set<Integer> forcedOffsets = new HashSet<>();

        for (int attempt = 0; attempt < MAX_REBUILD_ATTEMPTS; attempt++) {
            rebuildControlFlowGraph(method);

            ControlFlowGraph cfg = getControlFlowGraph();

            splitMultiPredecessorMerges(cfg, forcedOffsets);

            BasicBlock start = cfg.getStart();
            BitSet jsrTargets = new BitSet();
            BitSet visited = new BitSet(cfg.getBasicBlocks().size());

            if (!reduce(visited, start, jsrTargets)) {
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

        return false;
    }

    /**
     * Repeatedly scans the graph for merge points with more than one predecessor - either a duplicable-type
     * node found that way naturally, or any node at an offset a previous rebuild attempt found still shared
     * after construction - and gives every predecessor but the first its own private edge, until none remain.
     * A fixed point (not a single pass) is kept as a safety margin: {@code TYPE_TERNARY_OPERATOR} still
     * duplicates its own value-computing node per extra predecessor, and if that ever leaves a fresh clone
     * sharing a further node, this catches it too.
     */
    private void splitMultiPredecessorMerges(ControlFlowGraph cfg, Set<Integer> forcedOffsets) {
        boolean changed = true;
        int iterations = 0;

        while (changed && iterations++ < MAX_SPLIT_ITERATIONS) {
            changed = false;

            for (BasicBlock target : new ArrayList<>(cfg.getBasicBlocks())) {
                boolean matchesNaturally = target.matchType(DUPLICABLE_TYPES);
                boolean forced = forcedOffsets.contains(target.getFromOffset());

                if ((!matchesNaturally && !forced) || target.getPredecessors().size() <= 1) {
                    continue;
                }

                List<BasicBlock> predecessors = new ArrayList<>(target.getPredecessors());

                // Leave the first predecessor pointing at the original; every other predecessor is detached.
                for (int i = 1; i < predecessors.size(); i++) {
                    BasicBlock predecessor = predecessors.get(i);
                    predecessor.replace(target, detachOneEdge(predecessor, target));
                }

                changed = true;
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
        for (BasicBlock sentinel : IMMUTABLE_SENTINELS) {
            if (basicBlock == sentinel) {
                return true;
            }
        }
        return false;
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
            basicBlock.setNext(detachOneEdge(basicBlock, next));
            duplicated = true;
        }
        if (branch.matchType(DUPLICABLE_TYPES) && branch.getPredecessors().size() > 1) {
            basicBlock.setBranch(detachOneEdge(basicBlock, branch));
            duplicated = true;
        }

        return duplicated && reduceConditionalBranch(basicBlock);
    }

    private static BasicBlock detachOneEdge(BasicBlock predecessor, BasicBlock target) {
        if (target.matchType(TYPE_STATEMENTS)) {
            return predecessor.getControlFlowGraph().newJumpBasicBlock(predecessor, target);
        }
        return duplicateForSinglePredecessor(predecessor, target);
    }

    @Override
    public String getLabel() {
        return "Show Control Flow Graph with duplicated unreducible merge points";
    }
}
