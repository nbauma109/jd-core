/*
 * Copyright (c) 2026 Nicolas Baumann (@nbauma109), with Claude Sonnet 5.5.
 * This project is distributed under the GPLv3 license.
 * This is a Copyleft license that gives the user the right to use,
 * copy and modify the code freely for non-commercial purposes.
 */
package org.jd.core.v1.service.converter.classfiletojavasyntax.util;

import org.apache.bcel.Const;
import org.apache.bcel.classfile.Code;
import org.apache.bcel.classfile.ConstantPool;
import org.apache.bcel.classfile.LocalVariable;
import org.apache.bcel.classfile.LocalVariableTable;
import org.apache.bcel.classfile.Method;
import org.apache.bcel.generic.ClassGenException;
import org.apache.bcel.generic.InstructionList;
import org.apache.bcel.generic.Type;
import org.jd.core.v1.api.loader.Loader;

import java.util.HashMap;
import java.util.Map;

import static org.apache.bcel.Const.ALOAD;
import static org.apache.bcel.Const.ALOAD_0;
import static org.apache.bcel.Const.ALOAD_3;
import static org.apache.bcel.Const.ASTORE;
import static org.apache.bcel.Const.ASTORE_0;
import static org.apache.bcel.Const.ASTORE_3;
import static org.apache.bcel.Const.CHECKCAST;
import static org.apache.bcel.Const.IFEQ;
import static org.apache.bcel.Const.INSTANCEOF;

/**
 * The binding of the variable of 'x instanceof T v': javac compiles it to
 * <pre>aload x; instanceof T; ifeq L; aload x; checkcast T; astore v</pre>
 * The three instructions which follow the test only bind the variable: they are no statement.
 *
 * @param storeIndex index of the local variable which is bound
 * @param storeOffset offset of the 'astore'
 * @param storeEndOffset offset which follows the 'astore'
 * @param firstSkippedOffset offset of the 'aload' which follows the test
 * @param skippedLength length in bytes of 'aload; checkcast; astore'
 */
public record InstanceOfPatternBinding(int storeIndex, int storeOffset, int storeEndOffset, int firstSkippedOffset, int skippedLength) {

    /** Java 16 */
    private static final int MIN_MAJOR_VERSION = 60;

    /** The configuration key which turns the patterns off, for output which has to be recompiled with a source level older than 16 */
    public static final String CONFIGURATION_KEY = "instanceOfPatterns";

    private static final ThreadLocal<Boolean> ENABLED = ThreadLocal.withInitial(() -> Boolean.TRUE);

    /** The classes which the bindings of generic types are checked against, for the current thread */
    private static final ThreadLocal<Loader> LOADER = new ThreadLocal<>();

    public static void setLoader(Loader loader) {
        LOADER.set(loader);
    }

    /** Enables the patterns for the current thread, which decompiles one class at a time */
    public static void setEnabled(boolean enabled) {
        ENABLED.set(enabled);
    }

    /** An older class file cannot come from a pattern: its hand-written cast has to stay recompilable with its own source level */
    public static boolean isSupportedBy(int majorVersion) {
        return majorVersion >= MIN_MAJOR_VERSION && ENABLED.get();
    }

    /** @return the bindings of the method, by the offset of their 'instanceof' */
    public static Map<Integer, InstanceOfPatternBinding> find(Method method) {
        Code code = method.getCode();

        Map<Integer, InstanceOfPatternBinding> bindings = find(code.getCode(), parameterSlots(method), code.getLocalVariableTable(), method.getConstantPool());
        LocalVariable[] generics = code.getLocalVariableTypeTable() == null ? new LocalVariable[0] : code.getLocalVariableTypeTable().getLocalVariableTypeTable();

        bindings.entrySet().removeIf(entry -> isUncheckedGeneric(entry.getKey(), entry.getValue(), code.getCode(), generics, method.getConstantPool()));
        return bindings;
    }

    /**
     * 'List&lt;String&gt; l = (List&lt;String&gt;) o;' is an unchecked cast which stays: 'o instanceof List&lt;String&gt; l' is legal only when the
     * type of 'o' has the same type arguments and is a super type of the one of the test which has them too, as 'Collection&lt;String&gt; o' is.
     *
     * @param generics the entries of the local variable type table: the variables whose type has type arguments
     */
    private static boolean isUncheckedGeneric(int instanceOfOffset, InstanceOfPatternBinding binding, byte[] code, LocalVariable[] generics, ConstantPool constants) {
        String variableArguments = null;
        String operandArguments = null;
        String operandType = null;
        int operandIndex = loadedIndex(code, binding.firstSkippedOffset());

        for (LocalVariable generic : generics) {
            if (generic.getIndex() == binding.storeIndex() && generic.getStartPC() == binding.storeEndOffset()) {
                variableArguments = typeArguments(generic.getSignature());
            }
            if (generic.getIndex() == operandIndex && generic.getStartPC() <= instanceOfOffset && instanceOfOffset < generic.getStartPC() + generic.getLength()) {
                operandArguments = typeArguments(generic.getSignature());
                operandType = generic.getSignature();
            }
        }
        // 'Collection<?>' cannot be tested against 'List<String>': the arguments have to be the ones of the operand, and its type a super type
        return variableArguments != null && (!variableArguments.equals(operandArguments)
                || !isSubtype(castDescriptor(code, instanceOfOffset, constants), operandType));
    }

    /** @param tested the descriptor of the class of the test; @param signature the generic signature of the operand */
    private static boolean isSubtype(String tested, String signature) {
        Loader loader = LOADER.get();

        return loader != null && tested != null && ClassMetadata.isSubtypeWithSameTypeArguments(loader, erasure(tested), erasure(signature));
    }

    /** @return the internal name of the class of a descriptor or a generic signature of a class, of an array or not: 'Ljava/util/List&lt;TT;&gt;;' gives 'java/util/List' */
    private static String erasure(String signature) {
        int start = signature.lastIndexOf('[') + 2;
        int end = signature.indexOf('<');

        return signature.substring(start, end < 0 ? signature.length() - 1 : end);
    }

    /** @return the type arguments of a generic signature, 'Ljava/util/List&lt;Ljava/lang/String;&gt;;' gives '&lt;Ljava/lang/String;&gt;' */
    private static String typeArguments(String signature) {
        int start = signature.indexOf('<');

        return start < 0 ? null : signature.substring(start, signature.lastIndexOf('>') + 1);
    }

    /**
     * @param parameterSlots number of slots of 'this' and of the parameters: they are no variable which a pattern declares
     * @param variables the local variable table, null when the class has no debug information: a pattern declares a variable whose scope starts at its 'astore'
     * @param constants the constants of the class, null to ignore the type of the variable: the variable is declared with the type of the pattern
     * @return the bindings, by the offset of their 'instanceof'
     */
    public static Map<Integer, InstanceOfPatternBinding> find(byte[] code, int parameterSlots, LocalVariableTable variables, ConstantPool constants) {
        Map<Integer, InstanceOfPatternBinding> bindings = new HashMap<>();
        boolean[] starts = instructionStarts(code);

        for (int p = 1; p + 6 < code.length; p++) {
            if (starts[p] && (code[p] & 255) == INSTANCEOF && starts[p + 3] && (code[p + 3] & 255) == IFEQ) {
                InstanceOfPatternBinding binding = match(code, starts, p);

                if (binding != null && declaresNewVariable(binding, parameterSlots, variables, castDescriptor(code, p, constants))) {
                    bindings.put(p, binding);
                }
            }
        }
        // The variable of a pattern is declared by the test: a variable which the code assigns elsewhere is declared before it
        Map<Integer, InstanceOfPatternBinding> candidates = new HashMap<>(bindings);

        if (variables != null) {
            bindings.values().removeIf(binding -> isAssignedElsewhere(binding, candidates.values(), code, starts, variables));
        } else {
            // Without the table, the slot of the variable is the only evidence
            bindings.entrySet().removeIf(entry -> isStoredInTheOtherBranch(entry.getKey(), entry.getValue(), code, starts));
        }
        return bindings;
    }

    /**
     * 'String s; if (o instanceof String) { s = (String) o; } else { s = "fallback"; } return s;': the range of 's' starts after the store of
     * the first branch, as the one of a pattern does, and another range of the same name starts after a store which is none of a pattern.
     */
    private static boolean isAssignedElsewhere(InstanceOfPatternBinding binding, Iterable<InstanceOfPatternBinding> candidates, byte[] code, boolean[] starts,
            LocalVariableTable variables) {
        LocalVariable declared = null;

        for (LocalVariable variable : variables.getLocalVariableTable()) {
            if (variable.getIndex() == binding.storeIndex() && variable.getStartPC() == binding.storeEndOffset()) {
                declared = variable;
            }
        }
        if (declared == null) {
            return false;
        }
        for (LocalVariable other : variables.getLocalVariableTable()) {
            if (other != declared && other.getIndex() == declared.getIndex() && other.getName().equals(declared.getName())
                    && (isEndOfAnotherStore(other.getStartPC(), declared.getIndex(), candidates, code, starts)
                    || !isEndOfPatternStore(other.getStartPC(), declared.getIndex(), candidates) && isLoaded(code, starts, other)
                    && isDeclaredByAnotherPattern(binding, declared, candidates, variables))) {
                return true;
            }
        }
        for (int p = 0; p < code.length; p++) {
            if (starts[p] && p != binding.storeOffset() && storedIndex(code, p) == binding.storeIndex()
                    && declared.getStartPC() <= p && p < declared.getStartPC() + declared.getLength()) {
                return true;
            }
        }
        return false;
    }

    /** The variable of 'if (a instanceof T) { s = (T) a; } else if (b instanceof T) { s = (T) b; }' is declared by two tests, no one of which declares it */
    private static boolean isDeclaredByAnotherPattern(InstanceOfPatternBinding binding, LocalVariable declared, Iterable<InstanceOfPatternBinding> candidates,
            LocalVariableTable variables) {
        for (InstanceOfPatternBinding candidate : candidates) {
            if (candidate != binding && candidate.storeIndex() == declared.getIndex()) {
                for (LocalVariable variable : variables.getLocalVariableTable()) {
                    if (variable.getIndex() == declared.getIndex() && variable.getStartPC() == candidate.storeEndOffset() && variable.getName().equals(declared.getName())) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    /**
     * 'if (o instanceof String) { s = (String) o; ... } else { s = "fallback"; } return s;': the failed test goes to the other branch, which the
     * successful branch ends by a jump over, and the other branch stores the slot too. A store after the join is the one of another variable
     * which reuses the slot.
     */
    private static boolean isStoredInTheOtherBranch(int instanceOfOffset, InstanceOfPatternBinding binding, byte[] code, boolean[] starts) {
        int ifOffset = instanceOfOffset + 3;
        int elseOffset = ifOffset + (short) ((code[ifOffset + 1] & 255) << 8 | code[ifOffset + 2] & 255);
        int jump = elseOffset - 3;

        if (jump < binding.storeEndOffset() || jump + 2 >= code.length || !starts[jump] || (code[jump] & 255) != Const.GOTO) {
            return false;
        }

        int joinOffset = jump + (short) ((code[jump + 1] & 255) << 8 | code[jump + 2] & 255);

        for (int p = elseOffset; p < joinOffset && p < code.length; p++) {
            if (starts[p] && storedIndex(code, p) == binding.storeIndex()) {
                return true;
            }
        }
        return false;
    }

    private static boolean isEndOfPatternStore(int offset, int index, Iterable<InstanceOfPatternBinding> candidates) {
        for (InstanceOfPatternBinding candidate : candidates) {
            if (candidate.storeIndex() == index && candidate.storeEndOffset() == offset) {
                return true;
            }
        }
        return false;
    }

    /** @return true if the variable is read in its range: a range which no pattern starts is the one of a variable which the code declares elsewhere */
    private static boolean isLoaded(byte[] code, boolean[] starts, LocalVariable variable) {
        for (int p = variable.getStartPC(); p < variable.getStartPC() + variable.getLength() && p < code.length; p++) {
            if (starts[p] && loadedIndex(code, p) == variable.getIndex()) {
                return true;
            }
        }
        return false;
    }

    /** @return true if an 'astore' of the variable ends at the offset and is no store of a pattern */
    private static boolean isEndOfAnotherStore(int offset, int index, Iterable<InstanceOfPatternBinding> candidates, byte[] code, boolean[] starts) {
        if (isEndOfPatternStore(offset, index, candidates)) {
            return false;
        }
        for (int length = 1; length <= 2; length++) {
            int store = offset - length;

            if (store >= 0 && starts[store] && storedIndex(code, store) == index && (code[store] & 255) == ASTORE == (length == 2)) {
                return true;
            }
        }
        return false;
    }

    /** @return the index of the local variable which is stored by the instruction at 'offset', -1 if it is no 'astore' */
    private static int storedIndex(byte[] code, int offset) {
        int opcode = code[offset] & 255;

        if (opcode >= ASTORE_0 && opcode <= ASTORE_3) {
            return opcode - ASTORE_0;
        }
        return opcode == ASTORE && offset + 1 < code.length ? code[offset + 1] & 255 : -1;
    }

    /** @return the descriptor of the class of the 'instanceof' at 'p', null when the constants are not known */
    private static String castDescriptor(byte[] code, int p, ConstantPool constants) {
        if (constants == null) {
            return null;
        }

        String name = constants.getConstantString((code[p + 1] & 255) << 8 | code[p + 2] & 255, Const.CONSTANT_Class);

        return name.startsWith("[") ? name : "L" + name + ";";
    }

    private static int parameterSlots(Method method) {
        int slots = method.isStatic() ? 0 : 1;

        for (Type type : Type.getArgumentTypes(method.getSignature())) {
            slots += type.getSize();
        }
        return slots;
    }

    /** The offsets where an instruction starts: a byte which looks like an opcode may be the operand of another instruction */
    private static boolean[] instructionStarts(byte[] code) {
        boolean[] starts = new boolean[code.length + 1];

        try {
            for (int position : new InstructionList(code).getInstructionPositions()) {
                starts[position] = true;
            }
        } catch (ClassGenException e) {
            // The byte code cannot be decoded: no instruction is trusted
            return new boolean[code.length + 1];
        }
        return starts;
    }

    /** An assignment to a parameter or to a variable which exists already ('if (o instanceof String) s = (String) o;') is no pattern */
    private static boolean declaresNewVariable(InstanceOfPatternBinding binding, int parameterSlots, LocalVariableTable variables, String patternDescriptor) {
        if (binding.storeIndex() < parameterSlots) {
            return false;
        }
        if (variables == null) {
            return true;
        }
        for (LocalVariable variable : variables.getLocalVariableTable()) {
            if (variable.getIndex() == binding.storeIndex()) {
                if (variable.getStartPC() == binding.storeEndOffset()) {
                    // 'Object s = (String) o;' declares a variable which is not the one of the pattern 'o instanceof String s'
                    return patternDescriptor == null || patternDescriptor.equals(variable.getSignature());
                }
                if (variable.getStartPC() <= binding.storeOffset() && binding.storeOffset() < variable.getStartPC() + variable.getLength()) {
                    // The variable exists before the store: it is assigned, not declared
                    return false;
                }
            }
        }
        // No variable is named by the store: the compiler's own temporary, for example the one of a record pattern
        return true;
    }

    /** @return the offsets of the instructions which only bind the variable, with their lengths */
    public Map<Integer, Integer> skippedInstructions(byte[] code) {
        Map<Integer, Integer> lengths = new HashMap<>();
        int offset = firstSkippedOffset;
        int end = offset + skippedLength;

        while (offset < end) {
            int opcode = code[offset] & 255;
            int length = instructionLength(opcode);
            lengths.put(offset, length);
            offset += length;
        }
        return lengths;
    }

    private static int instructionLength(int opcode) {
        if (opcode == CHECKCAST) {
            return 3;
        }
        return opcode == ALOAD || opcode == ASTORE ? 2 : 1;
    }

    private static InstanceOfPatternBinding match(byte[] code, boolean[] starts, int instanceOfOffset) {
        // The operand of 'instanceof' is a local variable
        int operandIndex = operandIndex(code, starts, instanceOfOffset);
        if (operandIndex < 0) {
            return null;
        }

        int offset = instanceOfOffset + 6;
        int loadLength = (code[offset] & 255) == ALOAD ? 2 : 1;
        if (loadedIndex(code, offset) != operandIndex || offset + loadLength + 3 >= code.length) {
            return null;
        }

        int checkcastOffset = offset + loadLength;
        if ((code[checkcastOffset] & 255) != CHECKCAST
                || code[checkcastOffset + 1] != code[instanceOfOffset + 1] || code[checkcastOffset + 2] != code[instanceOfOffset + 2]) {
            return null;
        }

        int storeOffset = checkcastOffset + 3;
        int opcode = code[storeOffset] & 255;
        int storeIndex;
        int storeLength;
        if (opcode == ASTORE) {
            storeIndex = code[storeOffset + 1] & 255;
            storeLength = 2;
        } else if (opcode >= ASTORE_0 && opcode <= ASTORE_3) {
            storeIndex = opcode - ASTORE_0;
            storeLength = 1;
        } else {
            return null;
        }
        // The failed test jumps over the binding: otherwise the cast and the store are no part of the test
        int target = instanceOfOffset + 3 + (short) ((code[instanceOfOffset + 4] & 255) << 8 | code[instanceOfOffset + 5] & 255);

        if (target < storeOffset + storeLength) {
            return null;
        }
        return new InstanceOfPatternBinding(storeIndex, storeOffset, storeOffset + storeLength, offset, loadLength + 3 + storeLength);
    }

    /** @return the index of the local variable which is loaded by the instruction which precedes 'instanceof', -1 if it is no load */
    private static int operandIndex(byte[] code, boolean[] starts, int instanceOfOffset) {
        int previous = instanceOfOffset - 1;
        int opcode = code[previous] & 255;

        if (starts[previous] && opcode >= ALOAD_0 && opcode <= ALOAD_3) {
            return opcode - ALOAD_0;
        }
        return previous >= 1 && starts[previous - 1] && (code[previous - 1] & 255) == ALOAD ? opcode : -1;
    }

    /** @return the index of the local variable which is loaded by the instruction at 'offset', -1 if it is no load */
    private static int loadedIndex(byte[] code, int offset) {
        int opcode = code[offset] & 255;

        if (opcode >= ALOAD_0 && opcode <= ALOAD_3) {
            return opcode - ALOAD_0;
        }
        return opcode == ALOAD && offset + 1 < code.length ? code[offset + 1] & 255 : -1;
    }
}
