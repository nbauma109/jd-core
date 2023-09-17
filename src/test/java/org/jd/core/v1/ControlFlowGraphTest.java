/*
 * Copyright (c) 2008, 2019 Emmanuel Dupuy.
 * This project is distributed under the GPLv3 license.
 * This is a Copyleft license that gives the user the right to use,
 * copy and modify the code freely for non-commercial purposes.
 */

package org.jd.core.v1;

import org.apache.bcel.classfile.Attribute;
import org.apache.bcel.classfile.Code;
import org.apache.bcel.classfile.CodeException;
import org.apache.bcel.classfile.Constant;
import org.apache.bcel.classfile.ConstantCP;
import org.apache.bcel.classfile.ConstantClass;
import org.apache.bcel.classfile.ConstantFieldref;
import org.apache.bcel.classfile.ConstantInteger;
import org.apache.bcel.classfile.ConstantInterfaceMethodref;
import org.apache.bcel.classfile.ConstantLong;
import org.apache.bcel.classfile.ConstantMethodref;
import org.apache.bcel.classfile.ConstantNameAndType;
import org.apache.bcel.classfile.ConstantPool;
import org.apache.bcel.classfile.ConstantString;
import org.apache.bcel.classfile.ConstantUtf8;
import org.apache.bcel.classfile.LineNumber;
import org.apache.bcel.classfile.LineNumberTable;
import org.apache.bcel.classfile.Method;
import org.jd.core.v1.api.loader.Loader;
import org.jd.core.v1.cfg.ControlFlowGraphPlantUMLWriter;
import org.jd.core.v1.cfg.ControlFlowGraphPlantURLWriter;
import org.jd.core.v1.cfg.MethodUtil;
import org.jd.core.v1.loader.ClassPathLoader;
import org.jd.core.v1.loader.ZipLoader;
import org.jd.core.v1.service.converter.classfiletojavasyntax.model.cfg.BasicBlock;
import org.jd.core.v1.service.converter.classfiletojavasyntax.model.cfg.BasicBlock.ExceptionHandler;
import org.jd.core.v1.service.converter.classfiletojavasyntax.model.cfg.BasicBlock.SwitchCase;
import org.jd.core.v1.service.converter.classfiletojavasyntax.model.cfg.ControlFlowGraph;
import org.jd.core.v1.service.converter.classfiletojavasyntax.model.cfg.Loop;
import org.jd.core.v1.service.converter.classfiletojavasyntax.processor.ConvertClassFileProcessor;
import org.jd.core.v1.service.converter.classfiletojavasyntax.util.ControlFlowGraphGotoReducer;
import org.jd.core.v1.service.converter.classfiletojavasyntax.util.ControlFlowGraphLoopReducer;
import org.jd.core.v1.service.converter.classfiletojavasyntax.util.ControlFlowGraphMaker;
import org.jd.core.v1.service.converter.classfiletojavasyntax.util.ControlFlowGraphReducer;
import org.jd.core.v1.service.converter.classfiletojavasyntax.util.TypeMaker;
import org.jd.core.v1.service.converter.classfiletojavasyntax.util.WatchDog;
import org.jd.core.v1.service.deserializer.classfile.ClassFileDeserializer;
import org.jd.core.v1.util.StringConstants;
import org.junit.Test;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.jd.core.v1.service.converter.classfiletojavasyntax.model.cfg.BasicBlock.END;
import static org.jd.core.v1.service.converter.classfiletojavasyntax.model.cfg.BasicBlock.GROUP_CONDITION;
import static org.jd.core.v1.service.converter.classfiletojavasyntax.model.cfg.BasicBlock.GROUP_SINGLE_SUCCESSOR;
import static org.jd.core.v1.service.converter.classfiletojavasyntax.model.cfg.BasicBlock.SWITCH_BREAK;
import static org.jd.core.v1.service.converter.classfiletojavasyntax.model.cfg.BasicBlock.TYPE_CONDITION;
import static org.jd.core.v1.service.converter.classfiletojavasyntax.model.cfg.BasicBlock.TYPE_CONDITIONAL_BRANCH;
import static org.jd.core.v1.service.converter.classfiletojavasyntax.model.cfg.BasicBlock.TYPE_CONDITION_AND;
import static org.jd.core.v1.service.converter.classfiletojavasyntax.model.cfg.BasicBlock.TYPE_CONDITION_OR;
import static org.jd.core.v1.service.converter.classfiletojavasyntax.model.cfg.BasicBlock.TYPE_CONDITION_TERNARY_OPERATOR;
import static org.jd.core.v1.service.converter.classfiletojavasyntax.model.cfg.BasicBlock.TYPE_DELETED;
import static org.jd.core.v1.service.converter.classfiletojavasyntax.model.cfg.BasicBlock.TYPE_END;
import static org.jd.core.v1.service.converter.classfiletojavasyntax.model.cfg.BasicBlock.TYPE_GOTO;
import static org.jd.core.v1.service.converter.classfiletojavasyntax.model.cfg.BasicBlock.TYPE_GOTO_IN_TERNARY_OPERATOR;
import static org.jd.core.v1.service.converter.classfiletojavasyntax.model.cfg.BasicBlock.TYPE_IF;
import static org.jd.core.v1.service.converter.classfiletojavasyntax.model.cfg.BasicBlock.TYPE_IF_ELSE;
import static org.jd.core.v1.service.converter.classfiletojavasyntax.model.cfg.BasicBlock.TYPE_JSR;
import static org.jd.core.v1.service.converter.classfiletojavasyntax.model.cfg.BasicBlock.TYPE_JUMP;
import static org.jd.core.v1.service.converter.classfiletojavasyntax.model.cfg.BasicBlock.TYPE_LOOP;
import static org.jd.core.v1.service.converter.classfiletojavasyntax.model.cfg.BasicBlock.TYPE_LOOP_CONTINUE;
import static org.jd.core.v1.service.converter.classfiletojavasyntax.model.cfg.BasicBlock.TYPE_LOOP_END;
import static org.jd.core.v1.service.converter.classfiletojavasyntax.model.cfg.BasicBlock.TYPE_LOOP_START;
import static org.jd.core.v1.service.converter.classfiletojavasyntax.model.cfg.BasicBlock.TYPE_RET;
import static org.jd.core.v1.service.converter.classfiletojavasyntax.model.cfg.BasicBlock.TYPE_RETURN;
import static org.jd.core.v1.service.converter.classfiletojavasyntax.model.cfg.BasicBlock.TYPE_RETURN_VALUE;
import static org.jd.core.v1.service.converter.classfiletojavasyntax.model.cfg.BasicBlock.TYPE_START;
import static org.jd.core.v1.service.converter.classfiletojavasyntax.model.cfg.BasicBlock.TYPE_STATEMENTS;
import static org.jd.core.v1.service.converter.classfiletojavasyntax.model.cfg.BasicBlock.TYPE_SWITCH;
import static org.jd.core.v1.service.converter.classfiletojavasyntax.model.cfg.BasicBlock.TYPE_SWITCH_BREAK;
import static org.jd.core.v1.service.converter.classfiletojavasyntax.model.cfg.BasicBlock.TYPE_SWITCH_DECLARATION;
import static org.jd.core.v1.service.converter.classfiletojavasyntax.model.cfg.BasicBlock.TYPE_TERNARY_OPERATOR;
import static org.jd.core.v1.service.converter.classfiletojavasyntax.model.cfg.BasicBlock.TYPE_THROW;
import static org.jd.core.v1.service.converter.classfiletojavasyntax.model.cfg.BasicBlock.TYPE_TRY;
import static org.jd.core.v1.service.converter.classfiletojavasyntax.model.cfg.BasicBlock.TYPE_TRY_DECLARATION;
import static org.jd.core.v1.service.converter.classfiletojavasyntax.model.cfg.BasicBlock.TYPE_TRY_ECLIPSE;
import static org.jd.core.v1.service.converter.classfiletojavasyntax.model.cfg.BasicBlock.TYPE_TRY_JSR;
import static org.junit.Assert.assertNotEquals;

import junit.framework.TestCase;

public class ControlFlowGraphTest extends TestCase {
    protected ClassFileDeserializer deserializer = new ClassFileDeserializer();
    protected ConvertClassFileProcessor converter = new ConvertClassFileProcessor();
    protected ClassPathLoader loader = new ClassPathLoader();
    protected TypeMaker typeMaker = new TypeMaker(loader);

    // --- Basic test ----------------------------------------------------------------------------------------------- //
    @Test
    public void testJdk170BasicDoSomethingWithString() throws Exception {
        try (InputStream resource = getResource("zip/data-java-jdk-1.7.0.zip")) {
            checkCFGReduction(searchMethod(resource, "org/jd/core/test/Basic", "doSomethingWithString"));
        }
    }

    // --- Test 'if' and 'if-else' ---------------------------------------------------------------------------------- //
    @Test
    public void testJdk170If() throws Exception {
        try (InputStream resource = getResource("zip/data-java-jdk-1.7.0.zip")) {
            checkIfReduction(checkCFGReduction(searchMethod(resource, "org/jd/core/test/IfElse", "if_")));
        }
    }

    @Test
    public void testJdk170IfIf() throws Exception {
        try (InputStream resource = getResource("zip/data-java-jdk-1.7.0.zip")) {
            ControlFlowGraph cfg = checkIfReduction(checkCFGReduction(searchMethod(resource, "org/jd/core/test/IfElse", "ifIf")));

            BasicBlock ifBB = cfg.getStart().getNext().getNext();

            assertEquals(TYPE_IF, ifBB.getType());
            assertEquals(TYPE_IF, ifBB.getSub1().getType());
        }
    }

    @Test
    public void testJdk170MethodCallInIfCondition() throws Exception {
        try (InputStream resource = getResource("zip/data-java-jdk-1.7.0.zip")) {
            checkIfElseReduction(checkCFGReduction(searchMethod(resource, "org/jd/core/test/IfElse", "methodCallInIfCondition")));
        }
    }

    @Test
    public void testJdk170IlElse() throws Exception {
        try (InputStream resource = getResource("zip/data-java-jdk-1.7.0.zip")) {
            ControlFlowGraph cfg = checkIfElseReduction(checkCFGReduction(searchMethod(resource, "org/jd/core/test/IfElse", "ifElse")));
            BasicBlock ifElseBB = cfg.getStart().getNext().getNext();

            assertEquals(TYPE_IF_ELSE, ifElseBB.getType());
            assertEquals(TYPE_CONDITION, ifElseBB.getCondition().getType());
            assertTrue(ifElseBB.getCondition().mustInverseCondition());
        }
    }

    @Test
    public void testJdk170IlElseIfElse() throws Exception {
        try (InputStream resource = getResource("zip/data-java-jdk-1.7.0.zip")) {
            ControlFlowGraph cfg = checkIfElseReduction(checkCFGReduction(searchMethod(resource, "org/jd/core/test/IfElse", "ifElseIfElse")));
            BasicBlock ifElseBB = cfg.getStart().getNext().getNext();

            assertEquals(TYPE_IF_ELSE, ifElseBB.getSub2().getType());
        }
    }

    @Test
    public void testJdk170IfORCondition() throws Exception {
        try (InputStream resource = getResource("zip/data-java-jdk-1.7.0.zip")) {
            ControlFlowGraph cfg = checkIfReduction(checkCFGReduction(searchMethod(resource, "org/jd/core/test/IfElse", "ifORCondition")));
            BasicBlock ifBB = cfg.getStart().getNext().getNext();

            assertEquals(TYPE_CONDITION_OR, ifBB.getCondition().getType());
            assertEquals(TYPE_CONDITION, ifBB.getCondition().getSub1().getType());
            assertFalse(ifBB.getCondition().getSub1().mustInverseCondition());
            assertEquals(TYPE_CONDITION_OR, ifBB.getCondition().getSub2().getType());
            assertEquals(TYPE_CONDITION, ifBB.getCondition().getSub2().getSub1().getType());
            assertFalse(ifBB.getCondition().getSub2().getSub1().mustInverseCondition());
            assertEquals(TYPE_CONDITION, ifBB.getCondition().getSub2().getSub2().getType());
            assertTrue(ifBB.getCondition().getSub2().getSub2().mustInverseCondition());
        }
    }

    @Test
    public void testJdk170IfANDCondition() throws Exception {
        try (InputStream resource = getResource("zip/data-java-jdk-1.7.0.zip")) {
            ControlFlowGraph cfg = checkIfReduction(checkCFGReduction(searchMethod(resource, "org/jd/core/test/IfElse", "ifANDCondition")));
            BasicBlock ifBB = cfg.getStart().getNext().getNext();

            assertEquals(TYPE_CONDITION_AND, ifBB.getCondition().getType());
            assertEquals(TYPE_CONDITION, ifBB.getCondition().getSub1().getType());
            assertTrue(ifBB.getCondition().getSub1().mustInverseCondition());
            assertEquals(TYPE_CONDITION_AND, ifBB.getCondition().getSub2().getType());
            assertEquals(TYPE_CONDITION, ifBB.getCondition().getSub2().getSub1().getType());
            assertTrue(ifBB.getCondition().getSub2().getSub1().mustInverseCondition());
            assertEquals(TYPE_CONDITION, ifBB.getCondition().getSub2().getSub2().getType());
            assertTrue(ifBB.getCondition().getSub2().getSub2().mustInverseCondition());
        }
    }

    @Test
    public void testJdk170IfElseORCondition() throws Exception {
        try (InputStream resource = getResource("zip/data-java-jdk-1.7.0.zip")) {
            ControlFlowGraph cfg = checkIfElseReduction(checkCFGReduction(searchMethod(resource, "org/jd/core/test/IfElse", "ifElseORCondition")));
            BasicBlock ifElseBB = cfg.getStart().getNext().getNext();

            assertEquals(TYPE_CONDITION_OR, ifElseBB.getCondition().getType());
            assertEquals(TYPE_CONDITION, ifElseBB.getCondition().getSub1().getType());
            assertFalse(ifElseBB.getCondition().getSub1().mustInverseCondition());
            assertEquals(TYPE_CONDITION_OR, ifElseBB.getCondition().getSub2().getType());
        }
    }

    @Test
    public void testJdk170IfElseANDCondition() throws Exception {
        try (InputStream resource = getResource("zip/data-java-jdk-1.7.0.zip")) {
            ControlFlowGraph cfg = checkIfElseReduction(checkCFGReduction(searchMethod(resource, "org/jd/core/test/IfElse", "ifElseANDCondition")));
            BasicBlock ifElseBB = cfg.getStart().getNext().getNext();

            assertEquals(TYPE_CONDITION_AND, ifElseBB.getCondition().getType());
            assertEquals(TYPE_CONDITION, ifElseBB.getCondition().getSub1().getType());
            assertTrue(ifElseBB.getCondition().getSub1().mustInverseCondition());
            assertEquals(TYPE_CONDITION_AND, ifElseBB.getCondition().getSub2().getType());
        }
    }

    @Test
    public void testJdk170IfElse6ANDAnd2ORCondition() throws Exception {
        try (InputStream resource = getResource("zip/data-java-jdk-1.7.0.zip")) {
            ControlFlowGraph cfg = checkIfElseReduction(checkCFGReduction(searchMethod(resource, "org/jd/core/test/IfElse", "ifElse6ANDAnd2ORCondition")));
            BasicBlock ifElseBB = cfg.getStart().getNext().getNext();

            assertEquals(TYPE_CONDITION_OR, ifElseBB.getCondition().getType());
            assertEquals(TYPE_CONDITION_AND, ifElseBB.getCondition().getSub1().getType());
            assertEquals(TYPE_CONDITION_OR, ifElseBB.getCondition().getSub2().getType());
        }
    }

    @Test
    public void testJdk170IfElse6ORAnd2ANDCondition() throws Exception {
        try (InputStream resource = getResource("zip/data-java-jdk-1.7.0.zip")) {
            ControlFlowGraph cfg = checkIfElseReduction(checkCFGReduction(searchMethod(resource, "org/jd/core/test/IfElse", "ifElse6ORAnd2ANDCondition")));
            BasicBlock ifElseBB = cfg.getStart().getNext().getNext();

            assertEquals(TYPE_CONDITION_AND, ifElseBB.getCondition().getType());
            assertEquals(TYPE_CONDITION_OR, ifElseBB.getCondition().getSub1().getType());
            assertEquals(TYPE_CONDITION_AND, ifElseBB.getCondition().getSub2().getType());
        }
    }

    @Test
    public void testJdk170IfElseORAndANDConditions() throws Exception {
        try (InputStream resource = getResource("zip/data-java-jdk-1.7.0.zip")) {
            ControlFlowGraph cfg = checkIfElseReduction(checkCFGReduction(searchMethod(resource, "org/jd/core/test/IfElse", "ifElseORAndANDConditions")));
            BasicBlock ifElseBB = cfg.getStart().getNext().getNext();

            assertEquals(TYPE_IF_ELSE, ifElseBB.getType());
            assertEquals(TYPE_CONDITION_AND, ifElseBB.getCondition().getType());
            assertEquals(TYPE_CONDITION_OR, ifElseBB.getCondition().getSub1().getType());
            assertEquals(TYPE_CONDITION_AND, ifElseBB.getCondition().getSub2().getType());
        }
    }

    @Test
    public void testIfElseANDAndORConditions() throws Exception {
        try (InputStream resource = getResource("zip/data-java-jdk-1.7.0.zip")) {
            ControlFlowGraph cfg = checkIfElseReduction(checkCFGReduction(searchMethod(resource, "org/jd/core/test/IfElse", "ifElseANDAndORConditions")));
            BasicBlock ifElseBB = cfg.getStart().getNext().getNext();

            assertEquals(TYPE_IF_ELSE, ifElseBB.getType());
            assertEquals(TYPE_CONDITION_OR, ifElseBB.getCondition().getType());
            assertEquals(TYPE_CONDITION_AND, ifElseBB.getCondition().getSub1().getType());
            assertEquals(TYPE_CONDITION_OR, ifElseBB.getCondition().getSub2().getType());
        }
    }

    protected static ControlFlowGraph checkIfReduction(ControlFlowGraph cfg) throws Exception {
        BasicBlock ifBB = checkIfCommonReduction(cfg);

        assertEquals(TYPE_IF, ifBB.getType());

        return cfg;
    }

    protected static ControlFlowGraph checkIfElseReduction(ControlFlowGraph cfg) throws Exception {
        BasicBlock ifElseBB = checkIfCommonReduction(cfg);

        assertEquals(TYPE_IF_ELSE, ifElseBB.getType());
        assertNotNull(ifElseBB.getSub2());
        assertEquals(ifElseBB.getSub2().getNext(), END);

        return cfg;
    }

    protected static BasicBlock checkIfCommonReduction(ControlFlowGraph cfg) throws Exception {
        BasicBlock startBB = cfg.getStart();

        assertNotNull(startBB);
        assertEquals(TYPE_START, startBB.getType());

        assertNotNull(startBB.getNext());

        BasicBlock ifBB = startBB.getNext().getNext();

        assertNotNull(ifBB.getCondition());
        assertNotNull(ifBB.getSub1());
        assertEquals(ifBB.getSub1().getNext(), END);
        assertNotNull(ifBB.getNext());

        return ifBB;
    }

    // --- Test outer & inner classes ------------------------------------------------------------------------------- //
    @Test
    public void testJdk170OuterClass() throws Exception {
        try (InputStream resource = getResource("zip/data-java-jdk-1.7.0.zip")) {
            checkCFGReduction(searchMethod(resource, "org/jd/core/test/OuterClass", StringConstants.INSTANCE_CONSTRUCTOR));
        }
    }

    // --- Test ternary operator ------------------------------------------------------------------------------------ //
    @Test
    public void testJdk170TernaryOperatorsInTernaryOperator() throws Exception {
        try (InputStream resource = getResource("zip/data-java-jdk-1.7.0.zip")) {
            checkCFGReduction(searchMethod(resource, "org/jd/core/test/TernaryOperator", "ternaryOperatorsInTernaryOperator"));
        }
    }

    @Test
    public void testJdk118TernaryOperatorsInReturn() throws Exception {
        try (InputStream resource = getResource("zip/data-java-jdk-1.1.8.zip")) {
            checkCFGReduction(searchMethod(resource, "org/jd/core/test/TernaryOperator", "ternaryOperatorsInReturn"));
        }
    }

    @Test
    public void testJdk170TernaryOperatorsInReturn() throws Exception {
        try (InputStream resource = getResource("zip/data-java-jdk-1.7.0.zip")) {
            checkCFGReduction(searchMethod(resource, "org/jd/core/test/TernaryOperator", "ternaryOperatorsInReturn"));
        }
    }

    @Test
    public void testJdk118TernaryOperatorInIf1() throws Exception {
        try (InputStream resource = getResource("zip/data-java-jdk-1.1.8.zip")) {
            ControlFlowGraph cfg = checkCFGReduction(searchMethod(resource, "org/jd/core/test/TernaryOperator", "ternaryOperatorInIf1"));

            BasicBlock ifElseBB = cfg.getStart().getNext().getNext();

            assertEquals(TYPE_IF, ifElseBB.getType());
            assertEquals(TYPE_STATEMENTS, ifElseBB.getNext().getType());
            assertEquals(TYPE_RETURN, ifElseBB.getNext().getNext().getType());

            BasicBlock conditionTernaryOperatorBB = ifElseBB.getCondition();

            assertEquals(TYPE_CONDITION_TERNARY_OPERATOR, conditionTernaryOperatorBB.getType());
            assertEquals(TYPE_CONDITION, conditionTernaryOperatorBB.getCondition().getType());
            assertTrue(conditionTernaryOperatorBB.getCondition().mustInverseCondition());
            assertEquals(TYPE_GOTO_IN_TERNARY_OPERATOR, conditionTernaryOperatorBB.getSub1().getType());
            assertEquals(TYPE_STATEMENTS, conditionTernaryOperatorBB.getSub2().getType());
        }
    }

    @Test
    public void testJdk170TernaryOperatorInIf1() throws Exception {
        try (InputStream resource = getResource("zip/data-java-jdk-1.7.0.zip")) {
            checkCFGReduction(searchMethod(resource, "org/jd/core/test/TernaryOperator", "ternaryOperatorInIf1"));
        }
    }

    @Test
    public void testJdk118TernaryOperatorInIfElse1() throws Exception {
        try (InputStream resource = getResource("zip/data-java-jdk-1.1.8.zip")) {
            checkCFGReduction(searchMethod(resource, "org/jd/core/test/TernaryOperator", "ternaryOperatorInIfElse1"));
        }
    }

    @Test
    public void testJdk170TernaryOperatorInIfElse1() throws Exception {
        try (InputStream resource = getResource("zip/data-java-jdk-1.7.0.zip")) {
            checkCFGReduction(searchMethod(resource, "org/jd/core/test/TernaryOperator", "ternaryOperatorInIfElse1"));
        }
    }

    @Test
    public void testJdk118TernaryOperatorInIfElse2() throws Exception {
        try (InputStream resource = getResource("zip/data-java-jdk-1.1.8.zip")) {
            checkCFGReduction(searchMethod(resource, "org/jd/core/test/TernaryOperator", "ternaryOperatorInIfElse2"));
        }
    }

    @Test
    public void testJdk170TernaryOperatorInIfElse2() throws Exception {
        try (InputStream resource = getResource("zip/data-java-jdk-1.7.0.zip")) {
            checkCFGReduction(searchMethod(resource, "org/jd/core/test/TernaryOperator", "ternaryOperatorInIfElse2"));
        }
    }

    @Test
    public void testJdk118TernaryOperatorInIfElse3() throws Exception {
        try (InputStream resource = getResource("zip/data-java-jdk-1.1.8.zip")) {
            ControlFlowGraph cfg = checkCFGReduction(searchMethod(resource, "org/jd/core/test/TernaryOperator", "ternaryOperatorInIfElse3"));

            BasicBlock ifElseBB = cfg.getStart().getNext().getNext();

            assertEquals(TYPE_IF_ELSE, ifElseBB.getType());
            assertEquals(TYPE_STATEMENTS, ifElseBB.getNext().getType());
            assertEquals(TYPE_RETURN, ifElseBB.getNext().getNext().getType());

            BasicBlock conditionAndBB = ifElseBB.getCondition();

            assertEquals(TYPE_CONDITION_AND, conditionAndBB.getType());
            assertEquals(TYPE_CONDITION, conditionAndBB.getSub1().getType());
            assertTrue(conditionAndBB.getSub1().mustInverseCondition());
            assertEquals(TYPE_CONDITION, conditionAndBB.getSub2().getType());
            assertFalse(conditionAndBB.getSub2().mustInverseCondition());
        }
    }

    @Test
    public void testJdk170TernaryOperatorInIfElse3() throws Exception {
        try (InputStream resource = getResource("zip/data-java-jdk-1.7.0.zip")) {
            checkCFGReduction(searchMethod(resource, "org/jd/core/test/TernaryOperator", "ternaryOperatorInIfElse3"));
        }
    }

    @Test
    public void testJdk118TernaryOperatorInIfElse4() throws Exception {
        try (InputStream resource = getResource("zip/data-java-jdk-1.1.8.zip")) {
            checkCFGReduction(searchMethod(resource, "org/jd/core/test/TernaryOperator", "ternaryOperatorInIfElse4"));
        }
    }

    @Test
    public void testJdk118TernaryOperatorInIfElse5() throws Exception {
        try (InputStream resource = getResource("zip/data-java-jdk-1.1.8.zip")) {
            checkCFGReduction(searchMethod(resource, "org/jd/core/test/TernaryOperator", "ternaryOperatorInIfElse5"));
        }
    }

    @Test
    public void testJdk118TernaryOperatorInIfElse6() throws Exception {
        try (InputStream resource = getResource("zip/data-java-jdk-1.1.8.zip")) {
            checkCFGReduction(searchMethod(resource, "org/jd/core/test/TernaryOperator", "ternaryOperatorInIfElse6"));
        }
    }

    @Test
    public void testJdk118TernaryOperatorInIfElseFalse() throws Exception {
        try (InputStream resource = getResource("zip/data-java-jdk-1.1.8.zip")) {
            checkCFGReduction(searchMethod(resource, "org/jd/core/test/TernaryOperator", "ternaryOperatorInIfElseFalse"));
        }
    }

    @Test
    public void testJdk170TernaryOperatorInIfElseFalse() throws Exception {
        try (InputStream resource = getResource("zip/data-java-jdk-1.7.0.zip")) {
            checkCFGReduction(searchMethod(resource, "org/jd/core/test/TernaryOperator", "ternaryOperatorInIfElseFalse"));
        }
    }

    @Test
    public void testJdk118TernaryOperatorInIfElseANDCondition() throws Exception {
        try (InputStream resource = getResource("zip/data-java-jdk-1.1.8.zip")) {
            checkCFGReduction(searchMethod(resource, "org/jd/core/test/TernaryOperator", "ternaryOperatorInIfElseANDCondition"));
        }
    }

    @Test
    public void testJdk170TernaryOperatorInIfElseANDCondition() throws Exception {
        try (InputStream resource = getResource("zip/data-java-jdk-1.7.0.zip")) {
            checkCFGReduction(searchMethod(resource, "org/jd/core/test/TernaryOperator", "ternaryOperatorInIfElseANDCondition"));
        }
    }

    @Test
    public void testJdk118TernaryOperatorInIfElseORCondition() throws Exception {
        try (InputStream resource = getResource("zip/data-java-jdk-1.1.8.zip")) {
            ControlFlowGraph cfg = checkCFGReduction(searchMethod(resource, "org/jd/core/test/TernaryOperator", "ternaryOperatorInIfElseORCondition"));

            BasicBlock ifElseBB = cfg.getStart().getNext().getNext();

            assertEquals(TYPE_IF_ELSE, ifElseBB.getType());
            assertEquals(TYPE_STATEMENTS, ifElseBB.getNext().getType());
            assertEquals(TYPE_RETURN, ifElseBB.getNext().getNext().getType());

            BasicBlock conditionOrBB = ifElseBB.getCondition();

            assertEquals(TYPE_CONDITION_OR, conditionOrBB.getType());
            assertEquals(TYPE_CONDITION, conditionOrBB.getSub2().getType());
            assertTrue(conditionOrBB.getSub2().mustInverseCondition());

            BasicBlock conditionOrBB2 = conditionOrBB.getSub1();

            assertEquals(TYPE_CONDITION_OR, conditionOrBB2.getType());
            assertEquals(TYPE_CONDITION, conditionOrBB2.getSub1().getType());
            assertFalse(conditionOrBB2.getSub1().mustInverseCondition());

            BasicBlock conditionTernaryOperatorBB = conditionOrBB2.getSub2();

            assertEquals(TYPE_CONDITION_TERNARY_OPERATOR, conditionTernaryOperatorBB.getType());
            assertEquals(TYPE_CONDITION, conditionTernaryOperatorBB.getCondition().getType());
            assertTrue(conditionTernaryOperatorBB.getCondition().mustInverseCondition());
            assertEquals(TYPE_GOTO_IN_TERNARY_OPERATOR, conditionTernaryOperatorBB.getSub1().getType());
            assertEquals(TYPE_STATEMENTS, conditionTernaryOperatorBB.getSub2().getType());
        }
    }

    @Test
    public void testJdk170TernaryOperatorInIfElseORCondition() throws Exception {
        try (InputStream resource = getResource("zip/data-java-jdk-1.7.0.zip")) {
            ControlFlowGraph cfg = checkCFGReduction(searchMethod(resource, "org/jd/core/test/TernaryOperator", "ternaryOperatorInIfElseORCondition"));

            BasicBlock ifElseBB = cfg.getStart().getNext().getNext();

            assertEquals(TYPE_IF_ELSE, ifElseBB.getType());
            assertEquals(TYPE_STATEMENTS, ifElseBB.getNext().getType());
            assertEquals(TYPE_RETURN, ifElseBB.getNext().getNext().getType());

            BasicBlock conditionOrBB = ifElseBB.getCondition();

            assertEquals(TYPE_CONDITION_OR, conditionOrBB.getType());
            assertEquals(TYPE_CONDITION, conditionOrBB.getSub2().getType());
            assertTrue(conditionOrBB.getSub2().mustInverseCondition());

            BasicBlock conditionOrBB2 = conditionOrBB.getSub1();

            assertEquals(TYPE_CONDITION_OR, conditionOrBB2.getType());
            assertEquals(TYPE_CONDITION, conditionOrBB2.getSub1().getType());
            assertFalse(conditionOrBB2.getSub1().mustInverseCondition());

            BasicBlock conditionTernaryOperatorBB = conditionOrBB2.getSub2();

            assertEquals(TYPE_CONDITION_TERNARY_OPERATOR, conditionTernaryOperatorBB.getType());
            assertEquals(TYPE_CONDITION, conditionTernaryOperatorBB.getCondition().getType());
            assertTrue(conditionTernaryOperatorBB.getCondition().mustInverseCondition());
            assertEquals(TYPE_CONDITION, conditionTernaryOperatorBB.getSub1().getType());
            assertTrue(conditionTernaryOperatorBB.getSub1().mustInverseCondition());
            assertEquals(TYPE_CONDITION, conditionTernaryOperatorBB.getSub2().getType());
            assertFalse(conditionTernaryOperatorBB.getSub2().mustInverseCondition());
        }
    }

    // --- Test 'switch' -------------------------------------------------------------------------------------------- //
    @Test
    public void testJdk170SimpleSwitch() throws Exception {
        try (InputStream resource = getResource("zip/data-java-jdk-1.7.0.zip")) {
            checkSwitchReduction(checkCFGReduction(searchMethod(resource, "org/jd/core/test/Switch", "simpleSwitch")));
        }
    }

    @Test
    public void testJdk170SwitchFirstBreakMissing() throws Exception {
        try (InputStream resource = getResource("zip/data-java-jdk-1.7.0.zip")) {
            BasicBlock switchBB = checkSwitchReduction(checkCFGReduction(searchMethod(resource, "org/jd/core/test/Switch", "switchFirstBreakMissing")));
            SwitchCase sc0 = switchBB.getSwitchCases().get(0);

            assertFalse(sc0.isDefaultCase());
            assertEquals(0, sc0.getValue());
            assertEquals(sc0.getBasicBlock().getNext(), END);

            SwitchCase sc1 = switchBB.getSwitchCases().get(1);

            assertFalse(sc1.isDefaultCase());
            assertEquals(1, sc1.getValue());
            assertEquals(sc0.getBasicBlock().getNext(), SWITCH_BREAK);
        }
    }

    @Test
    public void testJdk170SwitchSecondBreakMissing() throws Exception {
        try (InputStream resource = getResource("zip/data-java-jdk-1.7.0.zip")) {
            BasicBlock switchBB = checkSwitchReduction(checkCFGReduction(searchMethod(resource, "org/jd/core/test/Switch", "switchSecondBreakMissing")));
            SwitchCase sc0 = switchBB.getSwitchCases().get(0);

            assertTrue(sc0.isDefaultCase());

            SwitchCase sc1 = switchBB.getSwitchCases().get(1);

            assertFalse(sc1.isDefaultCase());
            assertEquals(0, sc1.getValue());
            assertEquals(TYPE_STATEMENTS, sc1.getBasicBlock().getType());
            assertEquals(TYPE_SWITCH_BREAK, sc1.getBasicBlock().getNext().getType());

            SwitchCase sc2 = switchBB.getSwitchCases().get(2);

            assertFalse(sc2.isDefaultCase());
            assertEquals(1, sc2.getValue());
            assertEquals(TYPE_STATEMENTS, sc2.getBasicBlock().getType());
            assertEquals(TYPE_END, sc2.getBasicBlock().getNext().getType());
        }
    }

    @Test
    public void testJdk170SwitchDefault() throws Exception {
        try (InputStream resource = getResource("zip/data-java-jdk-1.7.0.zip")) {
            BasicBlock switchBB = checkSwitchReduction(checkCFGReduction(searchMethod(resource, "org/jd/core/test/Switch", "switchDefault")));
            SwitchCase scDefault = switchBB.getSwitchCases().get(0);

            assertTrue(scDefault.isDefaultCase());

            SwitchCase sc1 = switchBB.getSwitchCases().get(1);

            assertFalse(sc1.isDefaultCase());
            assertEquals(0, sc1.getValue());
            assertEquals(TYPE_STATEMENTS, sc1.getBasicBlock().getType());
            assertEquals(TYPE_END, sc1.getBasicBlock().getNext().getType());

            SwitchCase sc2 = switchBB.getSwitchCases().get(2);

            assertFalse(sc2.isDefaultCase());
            assertEquals(1, sc2.getValue());
            assertEquals(TYPE_STATEMENTS, sc2.getBasicBlock().getType());
            assertEquals(TYPE_SWITCH_BREAK, sc2.getBasicBlock().getNext().getType());
        }
    }

    @Test
    public void testJdk170LookupSwitchDefault() throws Exception {
        try (InputStream resource = getResource("zip/data-java-jdk-1.7.0.zip")) {
            BasicBlock switchBB = checkSwitchReduction(checkCFGReduction(searchMethod(resource, "org/jd/core/test/Switch", "lookupSwitchDefault")));
            SwitchCase scDefault = switchBB.getSwitchCases().get(0);

            assertTrue(scDefault.isDefaultCase());
            assertEquals(TYPE_STATEMENTS, scDefault.getBasicBlock().getType());
            assertEquals(TYPE_SWITCH_BREAK, scDefault.getBasicBlock().getNext().getType());

            SwitchCase sc1 = switchBB.getSwitchCases().get(1);

            assertFalse(sc1.isDefaultCase());
            assertEquals(0, sc1.getValue());
            assertEquals(TYPE_STATEMENTS, sc1.getBasicBlock().getType());
            assertEquals(TYPE_END, sc1.getBasicBlock().getNext().getType());

            SwitchCase sc2 = switchBB.getSwitchCases().get(2);

            assertFalse(sc2.isDefaultCase());
            assertEquals(1, sc2.getValue());
            assertEquals(TYPE_STATEMENTS, sc2.getBasicBlock().getType());
            assertEquals(TYPE_SWITCH_BREAK, sc2.getBasicBlock().getNext().getType());
        }
    }

    @Test
    public void testJdk170SwitchOneExitInFirstCase() throws Exception {
        try (InputStream resource = getResource("zip/data-java-jdk-1.7.0.zip")) {
            BasicBlock switchBB = checkSwitchReduction(checkCFGReduction(searchMethod(resource, "org/jd/core/test/Switch", "switchOneExitInFirstCase")));
            SwitchCase scDefault = switchBB.getSwitchCases().get(0);

            assertTrue(scDefault.isDefaultCase());
            assertEquals(TYPE_STATEMENTS, scDefault.getBasicBlock().getType());
            assertEquals(TYPE_RETURN, scDefault.getBasicBlock().getNext().getType());

            SwitchCase sc2 = switchBB.getSwitchCases().get(2);

            assertFalse(sc2.isDefaultCase());
            assertEquals(1, sc2.getValue());
            assertEquals(TYPE_THROW, sc2.getBasicBlock().getType());
        }
    }

    @Test
    public void testJdk170SwitchOneExitInSecondCase() throws Exception {
        try (InputStream resource = getResource("zip/data-java-jdk-1.7.0.zip")) {
            BasicBlock switchBB = checkSwitchReduction(checkCFGReduction(searchMethod(resource, "org/jd/core/test/Switch", "switchOneExitInSecondCase")));
            SwitchCase scDefault = switchBB.getSwitchCases().get(0);

            assertTrue(scDefault.isDefaultCase());
            assertEquals(TYPE_STATEMENTS, scDefault.getBasicBlock().getType());
            assertEquals(TYPE_RETURN, scDefault.getBasicBlock().getNext().getType());

            SwitchCase sc1 = switchBB.getSwitchCases().get(1);

            assertFalse(sc1.isDefaultCase());
            assertEquals(0, sc1.getValue());
            assertEquals(TYPE_STATEMENTS, sc1.getBasicBlock().getType());
            assertEquals(TYPE_RETURN, sc1.getBasicBlock().getNext().getType());
        }
    }

    @Test
    public void testJdk170SwitchOneExitInLastCase() throws Exception {
        try (InputStream resource = getResource("zip/data-java-jdk-1.7.0.zip")) {
            BasicBlock switchBB = checkSwitchReduction(checkCFGReduction(searchMethod(resource, "org/jd/core/test/Switch", "switchOneExitInLastCase")));
            SwitchCase sc0 = switchBB.getSwitchCases().get(0);

            assertFalse(sc0.isDefaultCase());
            assertEquals(0, sc0.getValue());
            assertEquals(TYPE_STATEMENTS, sc0.getBasicBlock().getType());
            assertEquals(TYPE_RETURN, sc0.getBasicBlock().getNext().getType());

            SwitchCase sc1 = switchBB.getSwitchCases().get(1);

            assertFalse(sc1.isDefaultCase());
            assertEquals(1, sc1.getValue());
            assertEquals(TYPE_STATEMENTS, sc1.getBasicBlock().getType());
            assertEquals(TYPE_RETURN, sc1.getBasicBlock().getNext().getType());
        }
    }

    @Test
    public void testJdk170ComplexSwitch() throws Exception {
        try (InputStream resource = getResource("zip/data-java-jdk-1.7.0.zip")) {
            BasicBlock switchBB = checkSwitchReduction(checkCFGReduction(searchMethod(resource, "org/jd/core/test/Switch", "complexSwitch")));
            SwitchCase scDefault = switchBB.getSwitchCases().get(0);

            assertTrue(scDefault.isDefaultCase());

            SwitchCase sc1 = switchBB.getSwitchCases().get(1);

            assertFalse(sc1.isDefaultCase());
            assertEquals(1, sc1.getValue());

            SwitchCase sc2 = switchBB.getSwitchCases().get(2);

            assertFalse(sc2.isDefaultCase());
            assertEquals(2, sc2.getValue());
        }
    }

    @Test
    public void testJdk170SwitchOnLastPosition() throws Exception {
        try (InputStream resource = getResource("zip/data-java-jdk-1.7.0.zip")) {
            checkCFGReduction(searchMethod(resource, "org/jd/core/test/Switch", "switchOnLastPosition"));
        }
    }

    @Test
    public void testJdk170SwitchFirstIfBreakMissing() throws Exception {
        try (InputStream resource = getResource("zip/data-java-jdk-1.7.0.zip")) {
            checkCFGReduction(searchMethod(resource, "org/jd/core/test/Switch", "switchFirstIfBreakMissing"));
        }
    }

    @Test
    public void testJdk170SwitchString() throws Exception {
        try (InputStream resource = getResource("zip/data-java-jdk-1.7.0.zip")) {
            checkCFGReduction(searchMethod(resource, "org/jd/core/test/AdvancedSwitch", "switchString"));
        }
    }

    protected static BasicBlock checkSwitchReduction(ControlFlowGraph cfg) throws Exception {
        BasicBlock startBB = cfg.getStart();

        assertNotNull(startBB);

        BasicBlock switchBB = startBB.getNext();

        assertNotNull(switchBB);
        assertEquals(TYPE_SWITCH, switchBB.getType());

        BasicBlock next = switchBB.getNext();
        assertNotNull(next);
        assertEquals(TYPE_STATEMENTS, next.getType());

        assertNotNull(next.getNext());
        assertEquals(TYPE_RETURN, next.getNext().getType());

        return switchBB;
    }

    // --- Test 'while' --------------------------------------------------------------------------------------------- //
    @Test
    public void testJdk170SimpleWhile() throws Exception {
        try (InputStream resource = getResource("zip/data-java-jdk-1.7.0.zip")) {
            checkCFGReduction(searchMethod(resource, "org/jd/core/test/While", "simpleWhile"));
        }
    }

    @Test
    public void testJdk170WhileIfContinue() throws Exception {
        try (InputStream resource = getResource("zip/data-java-jdk-1.7.0.zip")) {
            checkCFGReduction(searchMethod(resource, "org/jd/core/test/While", "whileIfContinue"));
        }
    }

    @Test
    public void testJdk170WhileIfBreak() throws Exception {
        try (InputStream resource = getResource("zip/data-java-jdk-1.7.0.zip")) {
            checkCFGReduction(searchMethod(resource, "org/jd/core/test/While", "whileIfBreak"));
        }
    }

    @Test
    public void testJdk170WhileWhile() throws Exception {
        try (InputStream resource = getResource("zip/data-java-jdk-1.7.0.zip")) {
            checkCFGReduction(searchMethod(resource, "org/jd/core/test/While", "whileWhile"));
        }
    }

    @Test
    public void testJdk170WhileThrow() throws Exception {
        try (InputStream resource = getResource("zip/data-java-jdk-1.7.0.zip")) {
            checkCFGReduction(searchMethod(resource, "org/jd/core/test/While", "whileThrow"));
        }
    }

    @Test
    public void testJdk170WhileTrue() throws Exception {
        try (InputStream resource = getResource("zip/data-java-jdk-1.7.0.zip")) {
            checkCFGReduction(searchMethod(resource, "org/jd/core/test/While", "whileTrue"));
        }
    }

    @Test
    public void testJdk170WhileTryFinally() throws Exception {
        try (InputStream resource = getResource("zip/data-java-jdk-1.7.0.zip")) {
            checkCFGReduction(searchMethod(resource, "org/jd/core/test/While", "whileTryFinally"));
        }
    }

    @Test
    public void testJdk170TryWhileFinally() throws Exception {
        try (InputStream resource = getResource("zip/data-java-jdk-1.7.0.zip")) {
            checkCFGReduction(searchMethod(resource, "org/jd/core/test/While", "tryWhileFinally"));
        }
    }

    @Test
    public void testJdk170InfiniteWhileTryFinally() throws Exception {
        try (InputStream resource = getResource("zip/data-java-jdk-1.7.0.zip")) {
            checkCFGReduction(searchMethod(resource, "org/jd/core/test/While", "infiniteWhileTryFinally"));
        }
    }

    @Test
    public void testJdk170TryInfiniteWhileFinally() throws Exception {
        try (InputStream resource = getResource("zip/data-java-jdk-1.7.0.zip")) {
            checkCFGReduction(searchMethod(resource, "org/jd/core/test/While", "tryInfiniteWhileFinally"));
        }
    }

    @Test
    public void testJdk170WhileTrueIf() throws Exception {
        try (InputStream resource = getResource("zip/data-java-jdk-1.7.0.zip")) {
            ControlFlowGraph cfg = checkCFGReduction(searchMethod(resource, "org/jd/core/test/While", "whileTrueIf"));

            BasicBlock mainLoopBB = cfg.getStart().getNext().getNext();

            assertEquals(TYPE_LOOP, mainLoopBB.getType());

            BasicBlock firstIfBB = mainLoopBB.getSub1().getNext();

            assertEquals(TYPE_IF, firstIfBB.getType());

            BasicBlock innerLoopBB = firstIfBB.getSub1().getNext();

            assertEquals(TYPE_LOOP, innerLoopBB.getType());
            assertEquals(TYPE_LOOP_END, innerLoopBB.getNext().getType());

            BasicBlock secondIfBB = firstIfBB.getNext();

            assertEquals(TYPE_LOOP_START, secondIfBB.getSub1().getNext().getType());
        }
    }

    @Test
    public void testJdk170WhileContinueBreak() throws Exception {
        try (InputStream resource = getResource("zip/data-java-jdk-1.7.0.zip")) {
            checkCFGReduction(searchMethod(resource, "org/jd/core/test/While", "whileContinueBreak"));
        }
    }

    @Test
    public void testJdk170TwoWiles() throws Exception {
        try (InputStream resource = getResource("zip/data-java-jdk-1.7.0.zip")) {
            ControlFlowGraph cfg = checkCFGReduction(searchMethod(resource, "org/jd/core/test/While", "twoWiles"));

            BasicBlock firstLoopBB = cfg.getStart().getNext().getNext();

            assertEquals(TYPE_LOOP, firstLoopBB.getType());

            BasicBlock nextLoopBB = firstLoopBB.getNext();

            assertEquals(TYPE_LOOP, nextLoopBB.getType());

            BasicBlock stmtBB = nextLoopBB.getNext();

            assertEquals(TYPE_STATEMENTS, stmtBB.getType());

            BasicBlock returnBB = stmtBB.getNext();

            assertEquals(TYPE_RETURN, returnBB.getType());
        }
    }

    // --- Test 'do-while' ------------------------------------------------------------------------------------------ //
    @Test
    public void testJdk170DoWhileWhile() throws Exception {
        try (InputStream resource = getResource("zip/data-java-jdk-1.7.0.zip")) {
            checkCFGReduction(searchMethod(resource, "org/jd/core/test/DoWhile", "doWhileWhile"));
        }
    }

    @Test
    public void testJdk170DoWhileTestPreInc() throws Exception {
        try (InputStream resource = getResource("zip/data-java-jdk-1.7.0.zip")) {
            checkCFGReduction(searchMethod(resource, "org/jd/core/test/DoWhile", "doWhileTestPreInc"));
        }
    }

    @Test
    public void testJdk170DoWhileTryFinally() throws Exception {
        try (InputStream resource = getResource("zip/data-java-jdk-1.7.0.zip")) {
            checkCFGReduction(searchMethod(resource, "org/jd/core/test/DoWhile", "doWhileTryFinally"));
        }
    }

    @Test
    public void testJdk170TryDoWhileFinally() throws Exception {
        try (InputStream resource = getResource("zip/data-java-jdk-1.7.0.zip")) {
            checkCFGReduction(searchMethod(resource, "org/jd/core/test/DoWhile", "tryDoWhileFinally"));
        }
    }

    // --- Test 'for' ----------------------------------------------------------------------------------------------- //
    @Test
    public void testJdk150ForTryReturn() throws Exception {
        try (InputStream resource = getResource("zip/data-java-jdk-1.5.0.zip")) {
            ControlFlowGraph cfg = checkCFGReduction(searchMethod(resource, "org/jd/core/test/For", "forTryReturn"));
            BasicBlock loopBB = cfg.getStart().getNext().getNext();

            assertEquals(TYPE_LOOP, loopBB.getType());
            assertEquals(TYPE_STATEMENTS, loopBB.getNext().getType());
            assertEquals(TYPE_RETURN, loopBB.getNext().getNext().getType());

            BasicBlock bb = loopBB.getSub1();

            assertEquals(TYPE_IF, bb.getType());
            assertEquals(TYPE_LOOP_END, bb.getNext().getType());

            bb = bb.getSub1();

            assertEquals(TYPE_STATEMENTS, bb.getType());
            assertEquals(TYPE_TRY, bb.getNext().getType());
            assertEquals(TYPE_STATEMENTS, bb.getNext().getNext().getType());
            assertEquals(TYPE_LOOP_START, bb.getNext().getNext().getNext().getType());
        }
    }

    @Test
    public void testJdk170IfForIfReturn() throws Exception {
        try (InputStream resource = getResource("zip/data-java-jdk-1.7.0.zip")) {
            checkCFGReduction(searchMethod(resource, "org/jd/core/test/For", "ifForIfReturn"));
        }
    }

    @Test
    public void testJdk170ForIfContinue() throws Exception {
        try (InputStream resource = getResource("zip/data-java-jdk-1.7.0.zip")) {
            checkCFGReduction(searchMethod(resource, "org/jd/core/test/For", "forIfContinue"));
        }
    }

    @Test
    public void testJdk170ForIfIfContinue() throws Exception {
        try (InputStream resource = getResource("zip/data-java-jdk-1.7.0.zip")) {
            checkCFGReduction(searchMethod(resource, "org/jd/core/test/For", "forIfIfContinue"));
        }
    }

    @Test
    public void testJdk170ForMultipleVariables2() throws Exception {
        try (InputStream resource = getResource("zip/data-java-jdk-1.7.0.zip")) {
            checkCFGReduction(searchMethod(resource, "org/jd/core/test/For", "forMultipleVariables2"));
        }
    }

    @Test
    public void testJdk170ForBreak() throws Exception {
        try (InputStream resource = getResource("zip/data-java-jdk-1.7.0.zip")) {
            checkCFGReduction(searchMethod(resource, "org/jd/core/test/For", "forBreak"));
        }
    }

    // --- Test 'break' and 'continue' ------------------------------------------------------------------------------ //
    @Test
    public void testJdk170DoWhileContinue() throws Exception {
        try (InputStream resource = getResource("zip/data-java-jdk-1.7.0.zip")) {
            ControlFlowGraph cfg = checkCFGReduction(searchMethod(resource, "org/jd/core/test/BreakContinue", "doWhileContinue"));

            BasicBlock mainLoopBB = cfg.getStart().getNext().getNext();

            assertEquals(TYPE_LOOP, mainLoopBB.getType());

            BasicBlock sub1 = mainLoopBB.getSub1();

            assertEquals(TYPE_STATEMENTS, sub1.getType());
            assertEquals(TYPE_IF, sub1.getNext().getType());
            assertEquals(TYPE_IF, sub1.getNext().getSub1().getType());
            assertEquals(TYPE_IF, sub1.getNext().getNext().getType());
            assertEquals(TYPE_LOOP_START, sub1.getNext().getNext().getNext().getType());
        }
    }

    @Test
    public void testJdk170TripleDoWhile1() throws Exception {
        try (InputStream resource = getResource("zip/data-java-jdk-1.7.0.zip")) {
            checkCFGReduction(searchMethod(resource, "org/jd/core/test/BreakContinue", "tripleDoWhile1"));
        }
    }

    @Test
    public void testJdk170TripleDoWhile2() throws Exception {
        try (InputStream resource = getResource("zip/data-java-jdk-1.7.0.zip")) {
            ControlFlowGraph cfg = checkCFGReduction(searchMethod(resource, "org/jd/core/test/BreakContinue", "tripleDoWhile2"));

            BasicBlock mainLoopBB = cfg.getStart().getNext().getNext();

            assertEquals(TYPE_LOOP, mainLoopBB.getType());

            BasicBlock innerLoopBB = mainLoopBB.getSub1();

            assertEquals(TYPE_LOOP, innerLoopBB.getType());
            assertEquals(TYPE_IF, innerLoopBB.getNext().getType());
            assertEquals(TYPE_LOOP_START, innerLoopBB.getNext().getNext().getType());
            assertEquals(TYPE_LOOP_END, innerLoopBB.getNext().getSub1().getType());

            BasicBlock innerInnerLoopBB = innerLoopBB.getSub1();

            assertEquals(TYPE_LOOP, innerInnerLoopBB.getType());
            assertEquals(TYPE_IF, innerInnerLoopBB.getNext().getType());
            assertEquals(TYPE_LOOP_START, innerInnerLoopBB.getNext().getNext().getType());
            assertEquals(TYPE_LOOP_END, innerInnerLoopBB.getNext().getSub1().getType());

            BasicBlock bb = innerInnerLoopBB.getSub1();

            assertEquals(TYPE_STATEMENTS, bb.getType());
            assertEquals(TYPE_IF, bb.getNext().getType());
            assertEquals(TYPE_IF, bb.getNext().getNext().getType());
            assertEquals(TYPE_JUMP, bb.getNext().getNext().getSub1().getType());
            assertEquals(TYPE_STATEMENTS, bb.getNext().getNext().getNext().getType());
            assertEquals(TYPE_IF, bb.getNext().getNext().getNext().getNext().getType());
            assertEquals(TYPE_LOOP_START, bb.getNext().getNext().getNext().getNext().getNext().getType());
            assertEquals(TYPE_LOOP_END, bb.getNext().getNext().getNext().getNext().getSub1().getType());
        }
    }

    @Test
    public void testJdk170DoWhileWhileIf() throws Exception {
        try (InputStream resource = getResource("zip/data-java-jdk-1.7.0.zip")) {
            checkCFGReduction(searchMethod(resource, "org/jd/core/test/BreakContinue", "doWhileWhileIf"));
        }
    }

    @Test
    public void testJdk170DoWhileWhileTryBreak() throws Exception {
        try (InputStream resource = getResource("zip/data-java-jdk-1.7.0.zip")) {
            ControlFlowGraph cfg = checkCFGReduction(searchMethod(resource, "org/jd/core/test/BreakContinue", "doWhileWhileTryBreak"));

            BasicBlock mainLoopBB = cfg.getStart().getNext().getNext();

            assertEquals(TYPE_LOOP, mainLoopBB.getType());

            BasicBlock innerLoopBB = mainLoopBB.getSub1();

            assertEquals(TYPE_LOOP, innerLoopBB.getType());
            assertEquals(TYPE_IF, innerLoopBB.getSub1().getType());
            assertEquals(TYPE_TRY, innerLoopBB.getSub1().getSub1().getType());
            assertEquals(TYPE_IF, innerLoopBB.getSub1().getSub1().getSub1().getType());
            assertEquals(TYPE_JUMP, innerLoopBB.getSub1().getSub1().getSub1().getNext().getType());
        }
    }

    // --- Test 'try-catch-finally' --------------------------------------------------------------------------------- //
    @Test
    public void testJdk170MethodTryCatch() throws Exception {
        try (InputStream resource = getResource("zip/data-java-jdk-1.7.0.zip")) {
            ControlFlowGraph cfg = checkCFGReduction(searchMethod(resource, "org/jd/core/test/TryCatchFinally", "methodTryCatch"));
            BasicBlock startBB = cfg.getStart();

            assertNotNull(startBB);

            BasicBlock simpleBB = startBB.getNext();

            assertEquals(TYPE_STATEMENTS, simpleBB.getType());

            BasicBlock tryBB = simpleBB.getNext();

            assertNotNull(tryBB);
            assertEquals(TYPE_TRY, tryBB.getType());
            assertEquals(1, tryBB.getExceptionHandlers().size());

            BasicBlock bb = tryBB.getSub1();

            assertEquals(TYPE_STATEMENTS, bb.getType());
            assertEquals(TYPE_IF, bb.getNext().getType());
            assertEquals(TYPE_END, bb.getNext().getNext().getType());

            ExceptionHandler eh0 = tryBB.getExceptionHandlers().get(0);

            assertEquals(StringConstants.JAVA_LANG_RUNTIME_EXCEPTION, eh0.getInternalThrowableName());
            assertEquals(TYPE_STATEMENTS, eh0.getBasicBlock().getType());
            assertEquals(TYPE_END, eh0.getBasicBlock().getNext().getType());

            BasicBlock nextSimpleBB = tryBB.getNext();

            assertNotNull(nextSimpleBB);
            assertEquals(TYPE_STATEMENTS, nextSimpleBB.getType());

            BasicBlock returnBB = nextSimpleBB.getNext();

            assertNotNull(returnBB);
            assertEquals(TYPE_RETURN, returnBB.getType());
        }
    }

    @Test
    public void testEclipseJavaCompiler370MethodTrySwitchFinally() throws Exception {
        try (InputStream resource = getResource("zip/data-java-eclipse-java-compiler-3.7.0.zip")) {
            ControlFlowGraph cfg = checkCFGReduction(searchMethod(resource, "org/jd/core/test/TryCatchFinally", "methodTrySwitchFinally"));
            BasicBlock startBB = cfg.getStart();

            assertNotNull(startBB);

            BasicBlock simpleBB = startBB.getNext();

            assertEquals(TYPE_STATEMENTS, simpleBB.getType());

            BasicBlock tryBB = simpleBB.getNext();

            assertNotNull(tryBB);
            assertEquals(TYPE_TRY_ECLIPSE, tryBB.getType());
            assertEquals(1, tryBB.getExceptionHandlers().size());

            BasicBlock bb = tryBB.getSub1();

            assertEquals(TYPE_SWITCH, bb.getType());
            assertEquals(TYPE_END, bb.getNext().getType());

            ExceptionHandler eh0 = tryBB.getExceptionHandlers().get(0);

            assertNull(eh0.getInternalThrowableName());
            assertEquals(TYPE_THROW, eh0.getBasicBlock().getType());

            BasicBlock nextSimpleBB = tryBB.getNext();

            assertNotNull(nextSimpleBB);
            assertEquals(TYPE_STATEMENTS, nextSimpleBB.getType());

            BasicBlock returnBB = nextSimpleBB.getNext();

            assertNotNull(returnBB);
            assertEquals(TYPE_RETURN, returnBB.getType());
        }
    }

    @Test
    public void testEclipseJavaCompiler3130MethodTrySwitchFinally() throws Exception {
        try (InputStream resource = getResource("zip/data-java-eclipse-java-compiler-3.13.0.zip")) {
            ControlFlowGraph cfg = checkCFGReduction(searchMethod(resource, "org/jd/core/test/TryCatchFinally", "methodTrySwitchFinally"));
            BasicBlock startBB = cfg.getStart();

            assertNotNull(startBB);

            BasicBlock simpleBB = startBB.getNext();

            assertEquals(TYPE_STATEMENTS, simpleBB.getType());

            BasicBlock tryBB = simpleBB.getNext();

            assertNotNull(tryBB);
            assertEquals(TYPE_TRY_ECLIPSE, tryBB.getType());
            assertEquals(1, tryBB.getExceptionHandlers().size());

            BasicBlock bb = tryBB.getSub1();

            assertEquals(TYPE_SWITCH, bb.getType());
            assertEquals(TYPE_END, bb.getNext().getType());

            ExceptionHandler eh0 = tryBB.getExceptionHandlers().get(0);

            assertNull(eh0.getInternalThrowableName());
            assertEquals(TYPE_THROW, eh0.getBasicBlock().getType());

            BasicBlock nextSimpleBB = tryBB.getNext();

            assertNotNull(nextSimpleBB);
            assertEquals(TYPE_STATEMENTS, nextSimpleBB.getType());

            BasicBlock returnBB = nextSimpleBB.getNext();

            assertNotNull(returnBB);
            assertEquals(TYPE_RETURN, returnBB.getType());
        }
    }

    @Test
    public void testJdk170MethodTryCatchCatch() throws Exception {
        try (InputStream resource = getResource("zip/data-java-jdk-1.7.0.zip")) {
            BasicBlock tryBB = checkTryCatchFinallyReduction(checkCFGReduction(searchMethod(resource, "org/jd/core/test/TryCatchFinally", "methodTryCatchCatch")));

            assertEquals(2, tryBB.getExceptionHandlers().size());

            BasicBlock next = tryBB.getNext();

            assertEquals(TYPE_STATEMENTS, next.getType());
            assertEquals(TYPE_RETURN, next.getNext().getType());
        }
    }

    @Test
    public void testEclipseJavaCompiler321MethodTryCatchCatch() throws Exception {
        try (InputStream resource = getResource("zip/data-java-eclipse-java-compiler-3.2.1.zip")) {
            BasicBlock tryBB = checkTryCatchFinallyReduction(checkCFGReduction(searchMethod(resource, "org/jd/core/test/TryCatchFinally", "methodTryCatchCatch")));

            assertEquals(2, tryBB.getExceptionHandlers().size());

            BasicBlock next = tryBB.getNext();

            assertEquals(TYPE_STATEMENTS, next.getType());
            assertEquals(TYPE_RETURN, next.getNext().getType());
        }
    }

    @Test
    public void testEclipseJavaCompiler321MethodTryCatchFinally1() throws Exception {
        try (InputStream resource = getResource("zip/data-java-eclipse-java-compiler-3.2.1.zip")) {
            checkCFGReduction(searchMethod(resource, "org/jd/core/test/TryCatchFinally", "methodTryCatchFinally1"));
        }
    }

    @Test
    public void testJdk170MethodTryCatchCatchExitInTry() throws Exception {
        try (InputStream resource = getResource("zip/data-java-jdk-1.7.0.zip")) {
            BasicBlock tryBB = checkTryCatchFinallyReduction(checkCFGReduction(searchMethod(resource, "org/jd/core/test/TryCatchFinally", "methodTryCatchCatchOneExitInTry")));

            assertEquals(2, tryBB.getExceptionHandlers().size());

            BasicBlock bb = tryBB.getSub1();

            assertEquals(TYPE_STATEMENTS, bb.getType());
            assertEquals(TYPE_END, bb.getNext().getType());

            ExceptionHandler eh0 = tryBB.getExceptionHandlers().get(0);

            assertEquals(StringConstants.JAVA_LANG_RUNTIME_EXCEPTION, eh0.getInternalThrowableName());
            assertEquals(TYPE_THROW, eh0.getBasicBlock().getType());

            ExceptionHandler eh1 = tryBB.getExceptionHandlers().get(1);

            assertEquals(StringConstants.JAVA_LANG_EXCEPTION, eh1.getInternalThrowableName());
            assertEquals(TYPE_STATEMENTS, eh1.getBasicBlock().getType());
            assertEquals(TYPE_RETURN, eh1.getBasicBlock().getNext().getType());

            BasicBlock next = tryBB.getNext();

            assertEquals(TYPE_STATEMENTS, next.getType());
            assertEquals(TYPE_RETURN, next.getNext().getType());
        }
    }

    @Test
    public void testJdk170MethodTryCatchCatchExitInFirstCatch() throws Exception {
        try (InputStream resource = getResource("zip/data-java-jdk-1.7.0.zip")) {
            BasicBlock tryBB = checkTryCatchFinallyReduction(checkCFGReduction(searchMethod(resource, "org/jd/core/test/TryCatchFinally", "methodTryCatchCatchOneExitInFirstCatch")));

            assertEquals(2, tryBB.getExceptionHandlers().size());

            BasicBlock bb = tryBB.getSub1();

            assertEquals(TYPE_STATEMENTS, bb.getType());
            assertEquals(TYPE_RETURN, bb.getNext().getType());

            ExceptionHandler eh0 = tryBB.getExceptionHandlers().get(0);

            assertEquals(StringConstants.JAVA_LANG_RUNTIME_EXCEPTION, eh0.getInternalThrowableName());
            assertEquals(TYPE_STATEMENTS, eh0.getBasicBlock().getType());
            assertEquals(TYPE_END, eh0.getBasicBlock().getNext().getType());

            ExceptionHandler eh1 = tryBB.getExceptionHandlers().get(1);

            assertEquals(StringConstants.JAVA_LANG_EXCEPTION, eh1.getInternalThrowableName());
            assertEquals(TYPE_STATEMENTS, eh1.getBasicBlock().getType());
            assertEquals(TYPE_RETURN, eh1.getBasicBlock().getNext().getType());

            BasicBlock next = tryBB.getNext();

            assertEquals(TYPE_STATEMENTS, next.getType());
            assertEquals(TYPE_RETURN, next.getNext().getType());
        }
    }

    @Test
    public void testJdk170MethodTryCatchCatchExitInLastCatch() throws Exception {
        try (InputStream resource = getResource("zip/data-java-jdk-1.7.0.zip")) {
            BasicBlock tryBB = checkTryCatchFinallyReduction(checkCFGReduction(searchMethod(resource, "org/jd/core/test/TryCatchFinally", "methodTryCatchCatchOneExitInLastCatch")));

            assertEquals(2, tryBB.getExceptionHandlers().size());

            BasicBlock bb = tryBB.getSub1();

            assertEquals(TYPE_STATEMENTS, bb.getType());
            assertEquals(TYPE_RETURN, bb.getNext().getType());

            ExceptionHandler eh0 = tryBB.getExceptionHandlers().get(0);

            assertEquals(StringConstants.JAVA_LANG_RUNTIME_EXCEPTION, eh0.getInternalThrowableName());
            assertEquals(TYPE_STATEMENTS, eh0.getBasicBlock().getType());
            assertEquals(TYPE_RETURN, eh0.getBasicBlock().getNext().getType());

            ExceptionHandler eh1 = tryBB.getExceptionHandlers().get(1);

            assertEquals(StringConstants.JAVA_LANG_EXCEPTION, eh1.getInternalThrowableName());
            assertEquals(TYPE_STATEMENTS, eh1.getBasicBlock().getType());
            assertEquals(TYPE_RETURN, eh1.getBasicBlock().getNext().getType());

            BasicBlock next = tryBB.getNext();

            assertEquals(TYPE_END, next.getType());
        }
    }

    @Test
    public void testJdk170MethodTrySwitchFinally() throws Exception {
        try (InputStream resource = getResource("zip/data-java-jdk-1.7.0.zip")) {
            ControlFlowGraph cfg = checkCFGReduction(searchMethod(resource, "org/jd/core/test/TryCatchFinally", "methodTrySwitchFinally"));
            BasicBlock startBB = cfg.getStart();

            assertNotNull(startBB);

            BasicBlock simpleBB = startBB.getNext();

            assertEquals(TYPE_STATEMENTS, simpleBB.getType());

            BasicBlock tryBB = simpleBB.getNext();

            assertNotNull(tryBB);
            assertEquals(TYPE_TRY, tryBB.getType());
            assertEquals(1, tryBB.getExceptionHandlers().size());

            BasicBlock bb = tryBB.getSub1();

            assertEquals(TYPE_SWITCH, bb.getType());
            assertEquals(TYPE_STATEMENTS, bb.getNext().getType());
            assertEquals(TYPE_END, bb.getNext().getNext().getType());

            ExceptionHandler eh0 = tryBB.getExceptionHandlers().get(0);

            assertNull(eh0.getInternalThrowableName());
            assertEquals(TYPE_THROW, eh0.getBasicBlock().getType());

            BasicBlock nextSimpleBB = tryBB.getNext();

            assertNotNull(nextSimpleBB);
            assertEquals(TYPE_STATEMENTS, nextSimpleBB.getType());

            BasicBlock returnBB = nextSimpleBB.getNext();

            assertNotNull(returnBB);
            assertEquals(TYPE_RETURN, returnBB.getType());
        }
    }

    @Test
    public void testJdk131MethodTryCatchFinallyInTryCatchFinally() throws Exception {
        try (InputStream resource = getResource("zip/data-java-jdk-1.3.1.zip")) {
            checkCFGReduction(searchMethod(resource, "org/jd/core/test/TryCatchFinally", "methodTryCatchFinallyInTryCatchFinally"));
        }
    }

    @Test
    public void testJdk170MethodTryCatchFinallyInTryCatchFinally() throws Exception {
        try (InputStream resource = getResource("zip/data-java-jdk-1.7.0.zip")) {
            ControlFlowGraph cfg = checkCFGReduction(searchMethod(resource, "org/jd/core/test/TryCatchFinally", "methodTryCatchFinallyInTryCatchFinally"));
            BasicBlock startBB = cfg.getStart();

            assertNotNull(startBB);

            BasicBlock simpleBB = startBB.getNext();

            assertEquals(TYPE_STATEMENTS, simpleBB.getType());

            BasicBlock tryBB = simpleBB.getNext();

            assertNotNull(tryBB);
            assertEquals(TYPE_TRY, tryBB.getType());
            assertNotNull(tryBB.getExceptionHandlers());
            assertEquals(1, tryBB.getExceptionHandlers().size());

            BasicBlock bb = tryBB.getSub1();

            assertEquals(TYPE_STATEMENTS, bb.getType());
            assertEquals(TYPE_TRY, bb.getNext().getType());
            assertEquals(TYPE_STATEMENTS, bb.getNext().getNext().getType());
            assertEquals(TYPE_TRY, bb.getNext().getNext().getNext().getType());
            assertEquals(TYPE_STATEMENTS, bb.getNext().getNext().getNext().getNext().getType());
            assertEquals(TYPE_LOOP, bb.getNext().getNext().getNext().getNext().getNext().getType());
            assertEquals(TYPE_END, bb.getNext().getNext().getNext().getNext().getNext().getNext().getType());

            ExceptionHandler eh0 = tryBB.getExceptionHandlers().get(0);

            assertNull(eh0.getInternalThrowableName());
            assertEquals(TYPE_STATEMENTS, eh0.getBasicBlock().getType());
            assertEquals(TYPE_LOOP, eh0.getBasicBlock().getNext().getType());
            assertEquals(TYPE_THROW, eh0.getBasicBlock().getNext().getNext().getType());

            BasicBlock returnBB = tryBB.getNext();

            assertNotNull(returnBB);
            assertEquals(TYPE_RETURN_VALUE, returnBB.getType());
        }
    }

    protected static BasicBlock checkTryCatchFinallyReduction(ControlFlowGraph cfg) throws Exception {
        BasicBlock startBB = cfg.getStart();

        assertNotNull(startBB);

        BasicBlock simpleBB = startBB.getNext();

        assertNotNull(simpleBB);
        assertEquals(TYPE_STATEMENTS, simpleBB.getType());

        BasicBlock tryBB = simpleBB.getNext();

        assertNotNull(tryBB);
        assertNotNull(tryBB.getSub1());
        assertNotNull(tryBB.getExceptionHandlers());
        assertEquals(2, tryBB.getExceptionHandlers().size());

        for (ExceptionHandler exceptionHandler : tryBB.getExceptionHandlers()) {
            assertNotNull(exceptionHandler.getInternalThrowableName());
            assertNotNull(exceptionHandler.getBasicBlock());
        }

        return tryBB;
    }

    // --- Test 'try-with-resources' --------------------------------------------------------------------------------- //
    @Test
    public void testJdk170Try1Resource() throws Exception {
        try (InputStream resource = getResource("zip/data-java-jdk-1.7.0.zip")) {
            checkCFGReduction(searchMethod(resource, "org/jd/core/test/TryWithResources", "try1Resource"));
        }
    }

    @Test
    public void testJdk170TryCatch1Resource() throws Exception {
        try (InputStream resource = getResource("zip/data-java-jdk-1.7.0.zip")) {
            checkCFGReduction(searchMethod(resource, "org/jd/core/test/TryWithResources", "tryCatch1Resource"));
        }
    }

    @Test
    public void testJdk170TryFinally1Resource() throws Exception {
        try (InputStream resource = getResource("zip/data-java-jdk-1.7.0.zip")) {
            checkCFGReduction(searchMethod(resource, "org/jd/core/test/TryWithResources", "tryFinally1Resource"));
        }
    }

    @Test
    public void testJdk170TryCatchFinally1Resource() throws Exception {
        try (InputStream resource = getResource("zip/data-java-jdk-1.7.0.zip")) {
            checkCFGReduction(searchMethod(resource, "org/jd/core/test/TryWithResources", "tryCatchFinally1Resource"));
        }
    }

    @Test
    public void testJdk170TryCatchFinally2Resources() throws Exception {
        try (InputStream resource = getResource("zip/data-java-jdk-1.7.0.zip")) {
            checkCFGReduction(searchMethod(resource, "org/jd/core/test/TryWithResources", "tryCatchFinally2Resources"));
        }
    }

    @Test
    public void testJdk170TryCatchFinally4Resources() throws Exception {
        try (InputStream resource = getResource("zip/data-java-jdk-1.7.0.zip")) {
            checkCFGReduction(searchMethod(resource, "org/jd/core/test/TryWithResources", "tryCatchFinally4Resources"));
        }
    }

    // --- methodTryFinallyReturn --- //
    @Test
    public void testJdk170MethodTryFinallyReturn() throws Exception {
        try (InputStream resource = getResource("zip/data-java-jdk-1.7.0.zip")) {
            ControlFlowGraph cfg = checkCFGReduction(searchMethod(resource, "org/jd/core/test/TryCatchFinally", "methodTryFinallyReturn"));
            BasicBlock startBB = cfg.getStart();

            assertNotNull(startBB);

            BasicBlock simpleBB = startBB.getNext();

            assertEquals(TYPE_STATEMENTS, simpleBB.getType());

            BasicBlock tryBB = simpleBB.getNext();

            assertNotNull(tryBB);
            assertEquals(TYPE_TRY, tryBB.getType());
            assertNotNull(tryBB.getExceptionHandlers());
            assertEquals(1, tryBB.getExceptionHandlers().size());

            BasicBlock bb = tryBB.getSub1();

            assertEquals(TYPE_RETURN_VALUE, bb.getType());

            ExceptionHandler eh0 = tryBB.getExceptionHandlers().get(0);

            assertNull(eh0.getInternalThrowableName());
            assertEquals(TYPE_THROW, eh0.getBasicBlock().getType());

            BasicBlock nextBB = tryBB.getNext();

            assertNotNull(nextBB);
            assertEquals(TYPE_END, nextBB.getType());
        }
    }

    @Test
    public void testEclipseJavaCompiler370MethodTryFinallyReturn() throws Exception {
        try (InputStream resource = getResource("zip/data-java-eclipse-java-compiler-3.7.0.zip")) {
            ControlFlowGraph cfg = checkCFGReduction(searchMethod(resource, "org/jd/core/test/TryCatchFinally", "methodTryFinallyReturn"));
            BasicBlock startBB = cfg.getStart();

            assertNotNull(startBB);

            BasicBlock simpleBB = startBB.getNext();

            assertEquals(TYPE_STATEMENTS, simpleBB.getType());

            BasicBlock tryBB = simpleBB.getNext();

            assertNotNull(tryBB);
            assertEquals(TYPE_TRY, tryBB.getType());
            assertNotNull(tryBB.getExceptionHandlers());
            assertEquals(1, tryBB.getExceptionHandlers().size());

            BasicBlock bb = tryBB.getSub1();

            assertEquals(TYPE_STATEMENTS, bb.getType());
            assertEquals(TYPE_RETURN_VALUE, bb.getNext().getType());

            ExceptionHandler eh0 = tryBB.getExceptionHandlers().get(0);

            assertNull(eh0.getInternalThrowableName());
            assertEquals(TYPE_THROW, eh0.getBasicBlock().getType());

            BasicBlock nextBB = tryBB.getNext();

            assertNotNull(nextBB);
            assertEquals(TYPE_END, nextBB.getType());
        }
    }

    @Test
    public void testEclipseJavaCompiler3130MethodTryFinallyReturn() throws Exception {
        try (InputStream resource = getResource("zip/data-java-eclipse-java-compiler-3.13.0.zip")) {
            ControlFlowGraph cfg = checkCFGReduction(searchMethod(resource, "org/jd/core/test/TryCatchFinally", "methodTryFinallyReturn"));
            BasicBlock startBB = cfg.getStart();

            assertNotNull(startBB);

            BasicBlock simpleBB = startBB.getNext();

            assertEquals(TYPE_STATEMENTS, simpleBB.getType());

            BasicBlock tryBB = simpleBB.getNext();

            assertNotNull(tryBB);
            assertEquals(TYPE_TRY, tryBB.getType());
            assertNotNull(tryBB.getExceptionHandlers());
            assertEquals(1, tryBB.getExceptionHandlers().size());

            BasicBlock bb = tryBB.getSub1();

            assertEquals(TYPE_STATEMENTS, bb.getType());
            assertEquals(TYPE_RETURN_VALUE, bb.getNext().getType());

            ExceptionHandler eh0 = tryBB.getExceptionHandlers().get(0);

            assertNull(eh0.getInternalThrowableName());
            assertEquals(TYPE_THROW, eh0.getBasicBlock().getType());

            BasicBlock nextBB = tryBB.getNext();

            assertNotNull(nextBB);
            assertEquals(TYPE_END, nextBB.getType());
        }
    }

    // --- methodTryCatch3 --- //
    @Test
    public void testJdk170MethodTryCatch3() throws Exception {
        try (InputStream resource = getResource("zip/data-java-jdk-1.7.0.zip")) {
            ControlFlowGraph cfg = checkCFGReduction(searchMethod(resource, "org/jd/core/test/TryCatchFinally", "methodTryCatch3"));
            BasicBlock startBB = cfg.getStart();

            assertNotNull(startBB);

            BasicBlock simpleBB = startBB.getNext();

            assertEquals(TYPE_STATEMENTS, simpleBB.getType());

            BasicBlock tryBB = simpleBB.getNext();

            assertNotNull(tryBB);
            assertEquals(TYPE_TRY, tryBB.getType());
            assertNotNull(tryBB.getExceptionHandlers());
            assertEquals(1, tryBB.getExceptionHandlers().size());

            BasicBlock bb = tryBB.getSub1();

            assertEquals(TYPE_RETURN_VALUE, bb.getType());

            ExceptionHandler eh0 = tryBB.getExceptionHandlers().get(0);

            assertEquals(StringConstants.JAVA_LANG_RUNTIME_EXCEPTION, eh0.getInternalThrowableName());
            assertEquals(TYPE_RETURN_VALUE, eh0.getBasicBlock().getType());

            BasicBlock nextBB = tryBB.getNext();

            assertNotNull(nextBB);
            assertEquals(TYPE_END, nextBB.getType());
        }
    }

    @Test
    public void testEclipseJavaCompiler370MethodTryCatch3() throws Exception {
        try (InputStream resource = getResource("zip/data-java-eclipse-java-compiler-3.7.0.zip")) {
            ControlFlowGraph cfg = checkCFGReduction(searchMethod(resource, "org/jd/core/test/TryCatchFinally", "methodTryCatch3"));
            BasicBlock startBB = cfg.getStart();

            assertNotNull(startBB);

            BasicBlock simpleBB = startBB.getNext();

            assertEquals(TYPE_STATEMENTS, simpleBB.getType());

            BasicBlock tryBB = simpleBB.getNext();

            assertNotNull(tryBB);
            assertEquals(TYPE_TRY, tryBB.getType());
            assertNotNull(tryBB.getExceptionHandlers());
            assertEquals(1, tryBB.getExceptionHandlers().size());

            BasicBlock bb = tryBB.getSub1();

            assertEquals(TYPE_RETURN_VALUE, bb.getType());

            ExceptionHandler eh0 = tryBB.getExceptionHandlers().get(0);

            assertEquals(StringConstants.JAVA_LANG_RUNTIME_EXCEPTION, eh0.getInternalThrowableName());
            assertEquals(TYPE_RETURN_VALUE, eh0.getBasicBlock().getType());

            BasicBlock nextBB = tryBB.getNext();

            assertNotNull(nextBB);
            assertEquals(TYPE_END, nextBB.getType());
        }
    }

    @Test
    public void testEclipseJavaCompiler3130MethodTryCatch3() throws Exception {
        try (InputStream resource = getResource("zip/data-java-eclipse-java-compiler-3.13.0.zip")) {
            ControlFlowGraph cfg = checkCFGReduction(searchMethod(resource, "org/jd/core/test/TryCatchFinally", "methodTryCatch3"));
            BasicBlock startBB = cfg.getStart();

            assertNotNull(startBB);

            BasicBlock simpleBB = startBB.getNext();

            assertEquals(TYPE_STATEMENTS, simpleBB.getType());

            BasicBlock tryBB = simpleBB.getNext();

            assertNotNull(tryBB);
            assertEquals(TYPE_TRY, tryBB.getType());
            assertNotNull(tryBB.getExceptionHandlers());
            assertEquals(1, tryBB.getExceptionHandlers().size());

            BasicBlock bb = tryBB.getSub1();

            assertEquals(TYPE_RETURN_VALUE, bb.getType());

            ExceptionHandler eh0 = tryBB.getExceptionHandlers().get(0);

            assertEquals(StringConstants.JAVA_LANG_RUNTIME_EXCEPTION, eh0.getInternalThrowableName());
            assertEquals(TYPE_RETURN_VALUE, eh0.getBasicBlock().getType());

            BasicBlock nextBB = tryBB.getNext();

            assertNotNull(nextBB);
            assertEquals(TYPE_END, nextBB.getType());
        }
    }

    // --- methodTryFinally1 --- //
    @Test
    public void testJdk170MethodTryFinally1() throws Exception {
        try (InputStream resource = getResource("zip/data-java-jdk-1.7.0.zip")) {
            ControlFlowGraph cfg = checkCFGReduction(searchMethod(resource, "org/jd/core/test/TryCatchFinally", "methodTryFinally1"));
            BasicBlock startBB = cfg.getStart();

            assertNotNull(startBB);

            BasicBlock simpleBB = startBB.getNext();

            assertEquals(TYPE_STATEMENTS, simpleBB.getType());

            BasicBlock tryBB = simpleBB.getNext();

            assertNotNull(tryBB);
            assertEquals(TYPE_TRY, tryBB.getType());
            assertEquals(1, tryBB.getExceptionHandlers().size());

            BasicBlock bb = tryBB.getSub1();

            assertEquals(TYPE_STATEMENTS, bb.getType());
            assertEquals(TYPE_END, bb.getNext().getType());

            ExceptionHandler eh0 = tryBB.getExceptionHandlers().get(0);

            assertNull(eh0.getInternalThrowableName());
            assertEquals(TYPE_THROW, eh0.getBasicBlock().getType());

            BasicBlock nextSimpleBB = tryBB.getNext();
            assertNotNull(nextSimpleBB);
            assertEquals(TYPE_STATEMENTS, nextSimpleBB.getType());

            BasicBlock returnBB = nextSimpleBB.getNext();

            assertNotNull(returnBB);
            assertEquals(TYPE_RETURN, returnBB.getType());
        }
    }

    // --- methodTryFinally3 --- //
    @Test
    public void testJdk170MethodTryFinally3() throws Exception {
        try (InputStream resource = getResource("zip/data-java-jdk-1.7.0.zip")) {
            ControlFlowGraph cfg = checkCFGReduction(searchMethod(resource, "org/jd/core/test/TryCatchFinally", "methodTryFinally3"));
            BasicBlock startBB = cfg.getStart();

            assertNotNull(startBB);

            BasicBlock simpleBB = startBB.getNext();

            assertEquals(TYPE_STATEMENTS, simpleBB.getType());

            BasicBlock tryBB = simpleBB.getNext();

            assertNotNull(tryBB);
            assertEquals(TYPE_TRY, tryBB.getType());
            assertNotNull(tryBB.getExceptionHandlers());
            assertEquals(1, tryBB.getExceptionHandlers().size());

            BasicBlock bb = tryBB.getSub1();

            assertEquals(TYPE_THROW, bb.getType());

            ExceptionHandler eh0 = tryBB.getExceptionHandlers().get(0);

            assertNull(eh0.getInternalThrowableName());
            assertEquals(TYPE_THROW, eh0.getBasicBlock().getType());

            BasicBlock nextBB = tryBB.getNext();

            assertNotNull(nextBB);
            assertEquals(TYPE_END, nextBB.getType());
        }
    }

    @Test
    public void testEclipseJavaCompiler370MethodTryFinally3() throws Exception {
        try (InputStream resource = getResource("zip/data-java-eclipse-java-compiler-3.7.0.zip")) {
            ControlFlowGraph cfg = checkCFGReduction(searchMethod(resource, "org/jd/core/test/TryCatchFinally", "methodTryFinally3"));
            BasicBlock startBB = cfg.getStart();

            assertNotNull(startBB);

            BasicBlock simpleBB = startBB.getNext();

            assertEquals(TYPE_STATEMENTS, simpleBB.getType());

            BasicBlock tryBB = simpleBB.getNext();

            assertNotNull(tryBB);
            assertEquals(TYPE_TRY, tryBB.getType());
            assertNotNull(tryBB.getExceptionHandlers());
            assertEquals(1, tryBB.getExceptionHandlers().size());

            BasicBlock bb = tryBB.getSub1();

            assertEquals(TYPE_THROW, bb.getType());

            ExceptionHandler eh0 = tryBB.getExceptionHandlers().get(0);

            assertNull(eh0.getInternalThrowableName());
            assertEquals(TYPE_THROW, eh0.getBasicBlock().getType());

            BasicBlock nextBB = tryBB.getNext();

            assertNotNull(nextBB);
            assertEquals(TYPE_END, nextBB.getType());
        }
    }

    @Test
    public void testEclipseJavaCompiler3130MethodTryFinally3() throws Exception {
        try (InputStream resource = getResource("zip/data-java-eclipse-java-compiler-3.13.0.zip")) {
            ControlFlowGraph cfg = checkCFGReduction(searchMethod(resource, "org/jd/core/test/TryCatchFinally", "methodTryFinally3"));
            BasicBlock startBB = cfg.getStart();

            assertNotNull(startBB);

            BasicBlock simpleBB = startBB.getNext();

            assertEquals(TYPE_STATEMENTS, simpleBB.getType());

            BasicBlock tryBB = simpleBB.getNext();

            assertNotNull(tryBB);
            assertEquals(TYPE_TRY, tryBB.getType());
            assertNotNull(tryBB.getExceptionHandlers());
            assertEquals(1, tryBB.getExceptionHandlers().size());

            BasicBlock bb = tryBB.getSub1();

            assertEquals(TYPE_THROW, bb.getType());

            ExceptionHandler eh0 = tryBB.getExceptionHandlers().get(0);

            assertNull(eh0.getInternalThrowableName());
            assertEquals(TYPE_THROW, eh0.getBasicBlock().getType());

            BasicBlock nextBB = tryBB.getNext();

            assertNotNull(nextBB);
            assertEquals(TYPE_END, nextBB.getType());
        }
    }

    // --- methodTryFinally4 --- //
    @Test
    public void testJdk170MethodTryFinally4() throws Exception {
        try (InputStream resource = getResource("zip/data-java-jdk-1.7.0.zip")) {
            ControlFlowGraph cfg = checkCFGReduction(searchMethod(resource, "org/jd/core/test/TryCatchFinally", "methodTryFinally4"));
            BasicBlock startBB = cfg.getStart();

            assertNotNull(startBB);

            BasicBlock simpleBB = startBB.getNext();

            assertEquals(TYPE_STATEMENTS, simpleBB.getType());

            BasicBlock tryBB = simpleBB.getNext();

            assertNotNull(tryBB);
            assertEquals(TYPE_TRY, tryBB.getType());
            assertNotNull(tryBB.getExceptionHandlers());
            assertEquals(1, tryBB.getExceptionHandlers().size());

            BasicBlock bb = tryBB.getSub1();

            assertEquals(TYPE_STATEMENTS, bb.getType());
            assertEquals(TYPE_IF, bb.getNext().getType());
            assertEquals(TYPE_STATEMENTS, bb.getNext().getNext().getType());
            assertEquals(TYPE_END, bb.getNext().getNext().getNext().getType());
            assertEquals(TYPE_STATEMENTS, bb.getNext().getSub1().getType());
            assertEquals(TYPE_RETURN, bb.getNext().getSub1().getNext().getType());

            ExceptionHandler eh0 = tryBB.getExceptionHandlers().get(0);

            assertNull(eh0.getInternalThrowableName());
            assertEquals(TYPE_THROW, eh0.getBasicBlock().getType());

            BasicBlock nextBB = tryBB.getNext();

            assertNotNull(nextBB);
            assertEquals(TYPE_STATEMENTS, nextBB.getType());
            assertEquals(TYPE_RETURN, nextBB.getNext().getType());
        }
    }

    @Test
    public void testEclipseJavaCompiler370MethodTryFinally4() throws Exception {
        try (InputStream resource = getResource("zip/data-java-eclipse-java-compiler-3.7.0.zip")) {
            ControlFlowGraph cfg = checkCFGReduction(searchMethod(resource, "org/jd/core/test/TryCatchFinally", "methodTryFinally4"));
            BasicBlock startBB = cfg.getStart();

            assertNotNull(startBB);

            BasicBlock simpleBB = startBB.getNext();

            assertEquals(TYPE_STATEMENTS, simpleBB.getType());

            BasicBlock tryBB = simpleBB.getNext();

            assertNotNull(tryBB);
            assertEquals(TYPE_TRY_ECLIPSE, tryBB.getType());
            assertNotNull(tryBB.getExceptionHandlers());
            assertEquals(1, tryBB.getExceptionHandlers().size());

            BasicBlock bb = tryBB.getSub1();

            assertEquals(TYPE_STATEMENTS, bb.getType());
            assertEquals(TYPE_IF, bb.getNext().getType());
            assertEquals(TYPE_END, bb.getNext().getNext().getType());
            assertEquals(TYPE_STATEMENTS, bb.getNext().getSub1().getType());
            assertEquals(TYPE_RETURN, bb.getNext().getSub1().getNext().getType());

            ExceptionHandler eh0 = tryBB.getExceptionHandlers().get(0);

            assertNull(eh0.getInternalThrowableName());
            assertEquals(TYPE_THROW, eh0.getBasicBlock().getType());

            BasicBlock nextBB = tryBB.getNext();

            assertNotNull(nextBB);
            assertEquals(TYPE_STATEMENTS, nextBB.getType());
            assertEquals(TYPE_RETURN, nextBB.getNext().getType());
        }
    }

    @Test
    public void testEclipseJavaCompiler3130MethodTryFinally4() throws Exception {
        try (InputStream resource = getResource("zip/data-java-eclipse-java-compiler-3.13.0.zip")) {
            ControlFlowGraph cfg = checkCFGReduction(searchMethod(resource, "org/jd/core/test/TryCatchFinally", "methodTryFinally4"));
            BasicBlock startBB = cfg.getStart();

            assertNotNull(startBB);

            BasicBlock simpleBB = startBB.getNext();

            assertEquals(TYPE_STATEMENTS, simpleBB.getType());

            BasicBlock tryBB = simpleBB.getNext();

            assertNotNull(tryBB);
            assertEquals(TYPE_TRY_ECLIPSE, tryBB.getType());
            assertNotNull(tryBB.getExceptionHandlers());
            assertEquals(1, tryBB.getExceptionHandlers().size());

            BasicBlock bb = tryBB.getSub1();

            assertEquals(TYPE_STATEMENTS, bb.getType());
            assertEquals(TYPE_IF, bb.getNext().getType());
            assertEquals(TYPE_END, bb.getNext().getNext().getType());
            assertEquals(TYPE_STATEMENTS, bb.getNext().getSub1().getType());
            assertEquals(TYPE_RETURN, bb.getNext().getSub1().getNext().getType());

            ExceptionHandler eh0 = tryBB.getExceptionHandlers().get(0);

            assertNull(eh0.getInternalThrowableName());
            assertEquals(TYPE_THROW, eh0.getBasicBlock().getType());

            BasicBlock nextBB = tryBB.getNext();

            assertNotNull(nextBB);
            assertEquals(TYPE_STATEMENTS, nextBB.getType());
            assertEquals(TYPE_RETURN, nextBB.getNext().getType());
        }
    }

    // --- methodTryCatchFinally2 --- //
    @Test
    public void testJdk170MethodTryCatchFinally2() throws Exception {
        try (InputStream resource = getResource("zip/data-java-jdk-1.7.0.zip")) {
            checkCFGReduction(searchMethod(resource, "org/jd/core/test/TryCatchFinally", "methodTryCatchFinally2"));
        }
    }

    @Test
    public void testEclipseJavaCompiler370MethodTryCatchFinally2() throws Exception {
        try (InputStream resource = getResource("zip/data-java-eclipse-java-compiler-3.7.0.zip")) {
            ControlFlowGraph cfg = checkCFGReduction(searchMethod(resource, "org/jd/core/test/TryCatchFinally", "methodTryCatchFinally2"));
            BasicBlock tryBB = cfg.getStart().getNext().getNext();

            assertEquals(TYPE_TRY_ECLIPSE, tryBB.getType());
            assertEquals(TYPE_STATEMENTS, tryBB.getSub1().getType());
            assertEquals(TYPE_END, tryBB.getSub1().getNext().getType());
            assertEquals(TYPE_STATEMENTS, tryBB.getNext().getType());
            assertEquals(TYPE_STATEMENTS, tryBB.getNext().getNext().getType());
            assertEquals(TYPE_RETURN, tryBB.getNext().getNext().getNext().getType());
        }
    }

    @Test
    public void testEclipseJavaCompiler3130MethodTryCatchFinally2() throws Exception {
        try (InputStream resource = getResource("zip/data-java-eclipse-java-compiler-3.13.0.zip")) {
            ControlFlowGraph cfg = checkCFGReduction(searchMethod(resource, "org/jd/core/test/TryCatchFinally", "methodTryCatchFinally2"));
            BasicBlock tryBB = cfg.getStart().getNext().getNext();

            assertEquals(TYPE_TRY_ECLIPSE, tryBB.getType());
            assertEquals(TYPE_STATEMENTS, tryBB.getSub1().getType());
            assertEquals(TYPE_END, tryBB.getSub1().getNext().getType());
            assertEquals(TYPE_STATEMENTS, tryBB.getNext().getType());
            assertEquals(TYPE_STATEMENTS, tryBB.getNext().getNext().getType());
            assertEquals(TYPE_RETURN, tryBB.getNext().getNext().getNext().getType());
        }
    }

    // --- methodTryCatchFinally4 --- //
    @Test
    public void testJdk170MethodTryCatchFinally4() throws Exception {
        try (InputStream resource = getResource("zip/data-java-jdk-1.7.0.zip")) {
            ControlFlowGraph cfg = checkCFGReduction(searchMethod(resource, "org/jd/core/test/TryCatchFinally", "methodTryCatchFinally4"));
            BasicBlock startBB = cfg.getStart();

            assertNotNull(startBB);

            BasicBlock simpleBB = startBB.getNext();

            assertEquals(TYPE_STATEMENTS, simpleBB.getType());

            BasicBlock tryBB = simpleBB.getNext();

            assertNotNull(tryBB);
            assertEquals(TYPE_TRY, tryBB.getType());
            assertNotNull(tryBB.getExceptionHandlers());
            assertEquals(2, tryBB.getExceptionHandlers().size());

            BasicBlock bb = tryBB.getSub1();

            assertEquals(TYPE_THROW, bb.getType());

            ExceptionHandler eh0 = tryBB.getExceptionHandlers().get(0);

            assertEquals(StringConstants.JAVA_LANG_EXCEPTION, eh0.getInternalThrowableName());
            assertEquals(TYPE_STATEMENTS, eh0.getBasicBlock().getType());
            assertEquals(TYPE_END, eh0.getBasicBlock().getNext().getType());

            ExceptionHandler eh1 = tryBB.getExceptionHandlers().get(1);

            assertNull(eh1.getInternalThrowableName());
            assertEquals(TYPE_THROW, eh1.getBasicBlock().getType());

            BasicBlock returnBB = tryBB.getNext();

            assertNotNull(returnBB);
            assertEquals(TYPE_RETURN_VALUE, returnBB.getType());
        }
    }

    // --- methodTryCatchFinally5 --- //
    @Test
    public void testEclipseJavaCompiler321MethodTryCatchFinally5() throws Exception {
        try (InputStream resource = getResource("zip/data-java-eclipse-java-compiler-3.2.1.zip")) {
            checkCFGReduction(searchMethod(resource, "org/jd/core/test/TryCatchFinally", "methodTryCatchFinally5"));
        }
    }

    @Test
    public void testEclipseJavaCompiler370MethodTryCatchFinally5() throws Exception {
        try (InputStream resource = getResource("zip/data-java-eclipse-java-compiler-3.7.0.zip")) {
            checkCFGReduction(searchMethod(resource, "org/jd/core/test/TryCatchFinally", "methodTryCatchFinally5"));
        }
    }

    @Test
    public void testEclipseJavaCompiler3130MethodTryCatchFinally5() throws Exception {
        try (InputStream resource = getResource("zip/data-java-eclipse-java-compiler-3.13.0.zip")) {
            checkCFGReduction(searchMethod(resource, "org/jd/core/test/TryCatchFinally", "methodTryCatchFinally5"));
        }
    }

    @Test
    public void testJdk170MethodTryCatchFinally5() throws Exception {
        try (InputStream resource = getResource("zip/data-java-jdk-1.7.0.zip")) {
            checkCFGReduction(searchMethod(resource, "org/jd/core/test/TryCatchFinally", "methodTryCatchFinally5"));
        }
    }

    // --- methodTryTryReturnFinally*Finally --- //
    @Test
    public void testJdk170MethodTryTryReturnFinallyFinally() throws Exception {
        try (InputStream resource = getResource("zip/data-java-jdk-1.7.0.zip")) {
            checkCFGReduction(searchMethod(resource, "org/jd/core/test/TryCatchFinally", "methodTryTryReturnFinallyFinally"));
        }
    }

    @Test
    public void testEclipseJavaCompiler321MethodTryTryReturnFinallyCatchFinally() throws Exception {
        try (InputStream resource = getResource("zip/data-java-eclipse-java-compiler-3.2.1.zip")) {
            checkCFGReduction(searchMethod(resource, "org/jd/core/test/TryCatchFinally", "methodTryTryReturnFinallyCatchFinally"));
        }
    }

    @Test
    public void testJdk170MethodTryTryReturnFinallyCatchFinally() throws Exception {
        try (InputStream resource = getResource("zip/data-java-jdk-1.7.0.zip")) {
            checkCFGReduction(searchMethod(resource, "org/jd/core/test/TryCatchFinally", "methodTryTryReturnFinallyCatchFinally"));
        }
    }

    // --- methodTryTryFinallyFinallyTryFinallyReturn --- //
    @Test
    public void testEclipseJavaCompiler370MethodTryTryFinallyFinallyTryFinallyReturn() throws Exception {
        try (InputStream resource = getResource("zip/data-java-eclipse-java-compiler-3.7.0.zip")) {
            checkCFGReduction(searchMethod(resource, "org/jd/core/test/TryCatchFinally", "methodTryTryFinallyFinallyTryFinallyReturn"));
        }
    }

    @Test
    public void testEclipseJavaCompiler3130MethodTryTryFinallyFinallyTryFinallyReturn() throws Exception {
        try (InputStream resource = getResource("zip/data-java-eclipse-java-compiler-3.13.0.zip")) {
            checkCFGReduction(searchMethod(resource, "org/jd/core/test/TryCatchFinally", "methodTryTryFinallyFinallyTryFinallyReturn"));
        }
    }

    @Test
    public void testJdk170MethodTryTryFinallyFinallyTryFinallyReturn() throws Exception {
        try (InputStream resource = getResource("zip/data-java-jdk-1.7.0.zip")) {
            ControlFlowGraph cfg = checkCFGReduction(searchMethod(resource, "org/jd/core/test/TryCatchFinally", "methodTryTryFinallyFinallyTryFinallyReturn"));
            BasicBlock startBB = cfg.getStart();

            assertNotNull(startBB);

            BasicBlock simpleBB = startBB.getNext();

            assertEquals(TYPE_STATEMENTS, simpleBB.getType());

            BasicBlock tryBB = simpleBB.getNext();

            assertNotNull(tryBB);
            assertEquals(TYPE_TRY, tryBB.getType());
            assertNotNull(tryBB.getExceptionHandlers());
            assertEquals(1, tryBB.getExceptionHandlers().size());

            BasicBlock bb = tryBB.getSub1();

            assertEquals(TYPE_TRY, bb.getType());
            assertEquals(TYPE_TRY, bb.getNext().getType());
            assertEquals(TYPE_END, bb.getNext().getNext().getType());

            ExceptionHandler eh0 = tryBB.getExceptionHandlers().get(0);

            assertNull(eh0.getInternalThrowableName());
            assertEquals(TYPE_STATEMENTS, eh0.getBasicBlock().getType());
            assertEquals(TYPE_TRY, eh0.getBasicBlock().getNext().getType());
            assertEquals(TYPE_THROW, eh0.getBasicBlock().getNext().getNext().getType());

            BasicBlock nextBB = tryBB.getNext();

            assertNotNull(nextBB);
            assertEquals(TYPE_STATEMENTS, nextBB.getType());
            assertEquals(TYPE_RETURN, nextBB.getNext().getType());
        }
    }

    // --- complexMethodTryCatchCatchFinally --- //
    @Test
    public void testJdk170MethodTryTryFinallyFinallyTryFinally() throws Exception {
        try (InputStream resource = getResource("zip/data-java-jdk-1.7.0.zip")) {
            checkComplexMethodTryCatchCatchFinally_JDK5(checkCFGReduction(searchMethod(resource, "org/jd/core/test/TryCatchFinally", "complexMethodTryCatchCatchFinally")));
        }
    }

    @Test
    public void testEclipseJavaCompiler321ComplexMethodTryCatchCatchFinally() throws Exception {
        try (InputStream resource = getResource("zip/data-java-eclipse-java-compiler-3.2.1.zip")) {
            ControlFlowGraph cfg = checkCFGReduction(searchMethod(resource, "org/jd/core/test/TryCatchFinally", "complexMethodTryCatchCatchFinally"));
            BasicBlock startBB = cfg.getStart();

            assertNotNull(startBB);

            BasicBlock simpleBB = startBB.getNext();

            assertEquals(TYPE_STATEMENTS, simpleBB.getType());

            BasicBlock tryBB = simpleBB.getNext();

            assertNotNull(tryBB);
            assertEquals(TYPE_TRY_ECLIPSE, tryBB.getType());
        }
    }

    @Test
    public void testEclipseJavaCompiler370ComplexMethodTryCatchCatchFinally() throws Exception {
        try (InputStream resource = getResource("zip/data-java-eclipse-java-compiler-3.7.0.zip")) {
            ControlFlowGraph cfg = checkCFGReduction(searchMethod(resource, "org/jd/core/test/TryCatchFinally", "complexMethodTryCatchCatchFinally"));
            BasicBlock startBB = cfg.getStart();

            assertNotNull(startBB);

            BasicBlock simpleBB = startBB.getNext();

            assertEquals(TYPE_STATEMENTS, simpleBB.getType());

            BasicBlock tryBB = simpleBB.getNext();

            assertNotNull(tryBB);
            assertEquals(TYPE_TRY_ECLIPSE, tryBB.getType());
        }
    }

    @Test
    public void testEclipseJavaCompiler3130ComplexMethodTryCatchCatchFinally() throws Exception {
        try (InputStream resource = getResource("zip/data-java-eclipse-java-compiler-3.13.0.zip")) {
            ControlFlowGraph cfg = checkCFGReduction(searchMethod(resource, "org/jd/core/test/TryCatchFinally", "complexMethodTryCatchCatchFinally"));
            BasicBlock startBB = cfg.getStart();

            assertNotNull(startBB);

            BasicBlock simpleBB = startBB.getNext();

            assertEquals(TYPE_STATEMENTS, simpleBB.getType());

            BasicBlock tryBB = simpleBB.getNext();

            assertNotNull(tryBB);
            assertEquals(TYPE_TRY_ECLIPSE, tryBB.getType());
        }
    }

    @Test
    public void testHarmonyJdkR533500ComplexMethodTryCatchCatchFinally() throws Exception {
        try (InputStream resource = getResource("zip/data-java-harmony-jdk-r533500.zip")) {
            checkComplexMethodTryCatchCatchFinally_JDK118(checkCFGReduction(searchMethod(resource, "org/jd/core/test/TryCatchFinally", "complexMethodTryCatchCatchFinally")));
        }
    }

    @Test
    public void testIbm_J9_VmComplexMethodTryCatchCatchFinally() throws Exception {
        try (InputStream resource = getResource("zip/data-java-ibm-j9_vm.zip")) {
            checkComplexMethodTryCatchCatchFinally_JDK5(checkCFGReduction(searchMethod(resource, "org/jd/core/test/TryCatchFinally", "complexMethodTryCatchCatchFinally")));
        }
    }

    @Test
    public void testJdk118ComplexMethodTryCatchCatchFinally() throws Exception {
        try (InputStream resource = getResource("zip/data-java-jdk-1.1.8.zip")) {
            checkComplexMethodTryCatchCatchFinally_JDK118(checkCFGReduction(searchMethod(resource, "org/jd/core/test/TryCatchFinally", "complexMethodTryCatchCatchFinally")));
        }
    }

    @Test
    public void testJdk131ComplexMethodTryCatchCatchFinally() throws Exception {
        try (InputStream resource = getResource("zip/data-java-jdk-1.3.1.zip")) {
            checkComplexMethodTryCatchCatchFinally_JDK118(checkCFGReduction(searchMethod(resource, "org/jd/core/test/TryCatchFinally", "complexMethodTryCatchCatchFinally")));
        }
    }

    @Test
    public void testJdk142ComplexMethodTryCatchCatchFinally() throws Exception {
        try (InputStream resource = getResource("zip/data-java-jdk-1.4.2.zip")) {
            ControlFlowGraph cfg = checkCFGReduction(searchMethod(resource, "org/jd/core/test/TryCatchFinally", "complexMethodTryCatchCatchFinally"));
            BasicBlock startBB = cfg.getStart();

            assertNotNull(startBB);

            BasicBlock simpleBB = startBB.getNext();

            assertEquals(TYPE_STATEMENTS, simpleBB.getType());

            BasicBlock tryBB = simpleBB.getNext();

            assertNotNull(tryBB);
            assertEquals(TYPE_TRY_JSR, tryBB.getType());
            assertNotNull(tryBB.getExceptionHandlers());
            assertEquals(3, tryBB.getExceptionHandlers().size());

            assertEquals(TYPE_TRY, tryBB.getSub1().getType());
            assertEquals(TYPE_END, tryBB.getSub1().getNext().getType());

            ExceptionHandler eh0 = tryBB.getExceptionHandlers().get(0);

            assertNotNull(eh0.getInternalThrowableName());
            assertEquals(TYPE_STATEMENTS, eh0.getBasicBlock().getType());
            assertEquals(TYPE_TRY, eh0.getBasicBlock().getNext().getType());
            assertEquals(TYPE_END, eh0.getBasicBlock().getNext().getNext().getType());

            ExceptionHandler eh1 = tryBB.getExceptionHandlers().get(1);

            assertNotNull(eh1.getInternalThrowableName());
            assertEquals(TYPE_STATEMENTS, eh1.getBasicBlock().getType());
            assertEquals(TYPE_TRY, eh1.getBasicBlock().getNext().getType());
            assertEquals(TYPE_END, eh1.getBasicBlock().getNext().getNext().getType());

            ExceptionHandler eh2 = tryBB.getExceptionHandlers().get(2);

            assertNull(eh2.getInternalThrowableName());
            assertEquals(TYPE_STATEMENTS, eh2.getBasicBlock().getType());
            assertEquals(TYPE_JSR, eh2.getBasicBlock().getNext().getType());
            assertEquals(TYPE_THROW, eh2.getBasicBlock().getNext().getNext().getType());
            assertEquals(TYPE_STATEMENTS, eh2.getBasicBlock().getNext().getBranch().getType());
            assertEquals(TYPE_TRY, eh2.getBasicBlock().getNext().getBranch().getNext().getType());
            assertEquals(TYPE_RET, eh2.getBasicBlock().getNext().getBranch().getNext().getNext().getType());

            BasicBlock nextBB = tryBB.getNext();

            assertNotNull(nextBB);
            assertEquals(TYPE_STATEMENTS, nextBB.getType());
            assertEquals(TYPE_RETURN, nextBB.getNext().getType());
        }
    }

    @Test
    public void testJdk150ComplexMethodTryCatchCatchFinally() throws Exception {
        try (InputStream resource = getResource("zip/data-java-jdk-1.5.0.zip")) {
            checkComplexMethodTryCatchCatchFinally_JDK5(checkCFGReduction(searchMethod(resource, "org/jd/core/test/TryCatchFinally", "complexMethodTryCatchCatchFinally")));
        }
    }

    @Test
    public void testJdk160ComplexMethodTryCatchCatchFinally() throws Exception {
        try (InputStream resource = getResource("zip/data-java-jdk-1.6.0.zip")) {
            checkComplexMethodTryCatchCatchFinally_JDK5(checkCFGReduction(searchMethod(resource, "org/jd/core/test/TryCatchFinally", "complexMethodTryCatchCatchFinally")));
        }
    }

    @Test
    public void testJdk170ComplexMethodTryCatchCatchFinally() throws Exception {
        try (InputStream resource = getResource("zip/data-java-jdk-1.7.0.zip")) {
            checkComplexMethodTryCatchCatchFinally_JDK5(checkCFGReduction(searchMethod(resource, "org/jd/core/test/TryCatchFinally", "complexMethodTryCatchCatchFinally")));
        }
    }

    @Test
    public void testJikes1_22_1WindowsComplexMethodTryCatchCatchFinally() throws Exception {
        try (InputStream resource = getResource("zip/data-java-jikes-1.22-1.windows.zip")) {
            checkComplexMethodTryCatchCatchFinally_JDK118(checkCFGReduction(searchMethod(resource, "org/jd/core/test/TryCatchFinally", "complexMethodTryCatchCatchFinally")));
        }
    }

    @Test
    public void testJRockit90_150_06ComplexMethodTryCatchCatchFinally() throws Exception {
        try (InputStream resource = getResource("zip/data-java-jrockit-90_150_06.zip")) {
            checkComplexMethodTryCatchCatchFinally_JDK5(checkCFGReduction(searchMethod(resource, "org/jd/core/test/TryCatchFinally", "complexMethodTryCatchCatchFinally")));
        }
    }

    protected void checkComplexMethodTryCatchCatchFinally_JDK5(ControlFlowGraph cfg) throws Exception {
        BasicBlock startBB = cfg.getStart();

        assertNotNull(startBB);

        BasicBlock simpleBB = startBB.getNext();

        assertEquals(TYPE_STATEMENTS, simpleBB.getType());

        BasicBlock tryBB = simpleBB.getNext();

        assertNotNull(tryBB);
        assertEquals(TYPE_TRY, tryBB.getType());
        assertNotNull(tryBB.getExceptionHandlers());
        assertEquals(3, tryBB.getExceptionHandlers().size());

        BasicBlock bb = tryBB.getSub1();

        assertEquals(TYPE_TRY, bb.getType());
        assertEquals(TYPE_TRY, bb.getNext().getType());
        assertEquals(TYPE_END, bb.getNext().getNext().getType());
        assertEquals(3, bb.getExceptionHandlers().size());
        assertEquals(3, bb.getNext().getExceptionHandlers().size());

        ExceptionHandler eh0 = tryBB.getExceptionHandlers().get(0);

        assertEquals(StringConstants.JAVA_LANG_RUNTIME_EXCEPTION, eh0.getInternalThrowableName());
        assertEquals(TYPE_STATEMENTS, eh0.getBasicBlock().getType());
        assertEquals(TYPE_TRY, eh0.getBasicBlock().getNext().getType());
        assertEquals(TYPE_TRY, eh0.getBasicBlock().getNext().getNext().getType());
        assertEquals(TYPE_END, eh0.getBasicBlock().getNext().getNext().getNext().getType());
        assertEquals(3, eh0.getBasicBlock().getNext().getExceptionHandlers().size());
        assertEquals(3, eh0.getBasicBlock().getNext().getNext().getExceptionHandlers().size());

        ExceptionHandler eh1 = tryBB.getExceptionHandlers().get(1);

        assertEquals(StringConstants.JAVA_LANG_EXCEPTION, eh1.getInternalThrowableName());
        assertEquals(TYPE_STATEMENTS, eh1.getBasicBlock().getType());
        assertEquals(TYPE_TRY, eh1.getBasicBlock().getNext().getType());
        assertEquals(TYPE_TRY, eh1.getBasicBlock().getNext().getNext().getType());
        assertEquals(TYPE_END, eh1.getBasicBlock().getNext().getNext().getNext().getType());
        assertEquals(3, eh1.getBasicBlock().getNext().getExceptionHandlers().size());
        assertEquals(3, eh1.getBasicBlock().getNext().getNext().getExceptionHandlers().size());

        ExceptionHandler eh2 = tryBB.getExceptionHandlers().get(2);

        assertNull(eh2.getInternalThrowableName());
        assertEquals(TYPE_STATEMENTS, eh2.getBasicBlock().getType());
        assertEquals(TYPE_TRY, eh2.getBasicBlock().getNext().getType());
        assertEquals(TYPE_THROW, eh2.getBasicBlock().getNext().getNext().getType());
        assertEquals(3, eh2.getBasicBlock().getNext().getExceptionHandlers().size());

        BasicBlock nextBB = tryBB.getNext();

        assertNotNull(nextBB);
        assertEquals(TYPE_STATEMENTS, nextBB.getType());
        assertEquals(TYPE_RETURN, nextBB.getNext().getType());
    }

    protected void checkComplexMethodTryCatchCatchFinally_JDK118(ControlFlowGraph cfg) throws Exception {
        BasicBlock startBB = cfg.getStart();

        assertNotNull(startBB);

        BasicBlock simpleBB = startBB.getNext();

        assertEquals(TYPE_STATEMENTS, simpleBB.getType());

        BasicBlock tryBB = simpleBB.getNext();

        assertNotNull(tryBB);
        assertEquals(TYPE_TRY_JSR, tryBB.getType());
        assertNotNull(tryBB.getExceptionHandlers());
        assertEquals(3, tryBB.getExceptionHandlers().size());

        BasicBlock sub1 = tryBB.getSub1();

        assertEquals(TYPE_TRY_JSR, sub1.getType());
        assertEquals(TYPE_END, sub1.getNext().getType());
        assertEquals(3, sub1.getExceptionHandlers().size());

        ExceptionHandler eh0 = tryBB.getExceptionHandlers().get(0);

        assertEquals(StringConstants.JAVA_LANG_RUNTIME_EXCEPTION, eh0.getInternalThrowableName());
        assertEquals(TYPE_STATEMENTS, eh0.getBasicBlock().getType());
        assertEquals(TYPE_TRY_JSR, eh0.getBasicBlock().getNext().getType());
        assertEquals(TYPE_END, eh0.getBasicBlock().getNext().getNext().getType());
        assertEquals(3, eh0.getBasicBlock().getNext().getExceptionHandlers().size());

        ExceptionHandler eh1 = tryBB.getExceptionHandlers().get(1);

        assertEquals(StringConstants.JAVA_LANG_EXCEPTION, eh1.getInternalThrowableName());
        assertEquals(TYPE_STATEMENTS, eh1.getBasicBlock().getType());
        assertEquals(TYPE_TRY_JSR, eh1.getBasicBlock().getNext().getType());
        assertEquals(TYPE_END, eh1.getBasicBlock().getNext().getNext().getType());
        assertEquals(3, eh1.getBasicBlock().getNext().getExceptionHandlers().size());

        ExceptionHandler eh2 = tryBB.getExceptionHandlers().get(2);

        assertNull(eh2.getInternalThrowableName());
        assertEquals(TYPE_STATEMENTS, eh2.getBasicBlock().getType());
        assertEquals(TYPE_JSR, eh2.getBasicBlock().getNext().getType());
        assertEquals(TYPE_THROW, eh2.getBasicBlock().getNext().getNext().getType());
        assertEquals(TYPE_STATEMENTS, eh2.getBasicBlock().getNext().getBranch().getType());
        assertEquals(TYPE_TRY_JSR, eh2.getBasicBlock().getNext().getBranch().getNext().getType());
        assertEquals(3, eh2.getBasicBlock().getNext().getBranch().getNext().getExceptionHandlers().size());

        BasicBlock nextBB = tryBB.getNext();

        assertNotNull(nextBB);
        assertEquals(TYPE_STATEMENTS, nextBB.getType());
        assertEquals(TYPE_RETURN, nextBB.getNext().getType());
    }

    // --- methodIfIfTryCatch --- //
    @Test
    public void testJdk118MethodIfIfTryCatch() throws Exception {
        try (InputStream resource = getResource("zip/data-java-jdk-1.1.8.zip")) {
            ControlFlowGraph cfg = checkCFGReduction(searchMethod(resource, "org/jd/core/test/TryCatchFinally", "methodIfIfTryCatch"));
            BasicBlock startBB = cfg.getStart();

            assertNotNull(startBB);

            BasicBlock ifBB = startBB.getNext();

            assertEquals(TYPE_IF, ifBB.getType());
            assertEquals(TYPE_RETURN, ifBB.getNext().getType());

            BasicBlock ifElseBB = ifBB.getSub1();

            assertEquals(TYPE_IF_ELSE, ifElseBB.getType());

            assertEquals(TYPE_STATEMENTS, ifElseBB.getSub1().getType());
            assertEquals(TYPE_TRY, ifElseBB.getSub1().getNext().getType());
            assertEquals(TYPE_END, ifElseBB.getSub1().getNext().getNext().getType());

            assertEquals(TYPE_STATEMENTS, ifElseBB.getSub2().getType());
            assertEquals(TYPE_END, ifElseBB.getSub2().getNext().getType());
        }
    }

    // --- methodTryCatchFinallyReturn --- //
    @Test
    public void testJdk170MethodTryCatchFinallyReturn() throws Exception {
        try (InputStream resource = getResource("zip/data-java-jdk-1.7.0.zip")) {
            ControlFlowGraph cfg = checkCFGReduction(searchMethod(resource, "org/jd/core/test/TryCatchFinally", "methodTryCatchFinallyReturn"));
            BasicBlock startBB = cfg.getStart();

            assertNotNull(startBB);

            BasicBlock simpleBB = startBB.getNext();

            assertEquals(TYPE_STATEMENTS, simpleBB.getType());

            BasicBlock tryBB = simpleBB.getNext();

            assertNotNull(tryBB);
            assertEquals(TYPE_TRY, tryBB.getType());
            assertNotNull(tryBB.getExceptionHandlers());
            assertEquals(2, tryBB.getExceptionHandlers().size());

            BasicBlock bb = tryBB.getSub1();

            assertEquals(TYPE_RETURN_VALUE, bb.getType());

            ExceptionHandler eh0 = tryBB.getExceptionHandlers().get(0);

            assertEquals(StringConstants.JAVA_LANG_RUNTIME_EXCEPTION, eh0.getInternalThrowableName());
            assertEquals(TYPE_RETURN_VALUE, eh0.getBasicBlock().getType());

            ExceptionHandler eh1 = tryBB.getExceptionHandlers().get(1);

            assertNull(eh1.getInternalThrowableName());
            assertEquals(TYPE_THROW, eh1.getBasicBlock().getType());

            BasicBlock nextBB = tryBB.getNext();

            assertNotNull(nextBB);
            assertEquals(TYPE_END, nextBB.getType());
        }
    }

    @Test
    public void testEclipseJavaCompiler370MethodTryCatchFinallyReturn() throws Exception {
        try (InputStream resource = getResource("zip/data-java-eclipse-java-compiler-3.7.0.zip")) {
            ControlFlowGraph cfg = checkCFGReduction(searchMethod(resource, "org/jd/core/test/TryCatchFinally", "methodTryCatchFinallyReturn"));
            BasicBlock startBB = cfg.getStart();

            assertNotNull(startBB);

            BasicBlock simpleBB = startBB.getNext();

            assertEquals(TYPE_STATEMENTS, simpleBB.getType());

            BasicBlock tryBB = simpleBB.getNext();

            assertNotNull(tryBB);
            assertEquals(TYPE_TRY, tryBB.getType());
            assertNotNull(tryBB.getExceptionHandlers());
            assertEquals(2, tryBB.getExceptionHandlers().size());

            BasicBlock bb = tryBB.getSub1();

            assertEquals(TYPE_STATEMENTS, bb.getType());

            ExceptionHandler eh0 = tryBB.getExceptionHandlers().get(0);

            assertEquals(StringConstants.JAVA_LANG_RUNTIME_EXCEPTION, eh0.getInternalThrowableName());
            assertEquals(TYPE_STATEMENTS, eh0.getBasicBlock().getType());

            ExceptionHandler eh1 = tryBB.getExceptionHandlers().get(1);

            assertNull(eh1.getInternalThrowableName());
            assertEquals(TYPE_THROW, eh1.getBasicBlock().getType());

            BasicBlock nextBB = tryBB.getNext();

            assertNotNull(nextBB);
            assertEquals(TYPE_END, nextBB.getType());
        }
    }

    @Test
    public void testEclipseJavaCompiler3130MethodTryCatchFinallyReturn() throws Exception {
        try (InputStream resource = getResource("zip/data-java-eclipse-java-compiler-3.13.0.zip")) {
            ControlFlowGraph cfg = checkCFGReduction(searchMethod(resource, "org/jd/core/test/TryCatchFinally", "methodTryCatchFinallyReturn"));
            BasicBlock startBB = cfg.getStart();

            assertNotNull(startBB);

            BasicBlock simpleBB = startBB.getNext();

            assertEquals(TYPE_STATEMENTS, simpleBB.getType());

            BasicBlock tryBB = simpleBB.getNext();

            assertNotNull(tryBB);
            assertEquals(TYPE_TRY, tryBB.getType());
            assertNotNull(tryBB.getExceptionHandlers());
            assertEquals(2, tryBB.getExceptionHandlers().size());

            BasicBlock bb = tryBB.getSub1();

            assertEquals(TYPE_STATEMENTS, bb.getType());

            ExceptionHandler eh0 = tryBB.getExceptionHandlers().get(0);

            assertEquals(StringConstants.JAVA_LANG_RUNTIME_EXCEPTION, eh0.getInternalThrowableName());
            assertEquals(TYPE_STATEMENTS, eh0.getBasicBlock().getType());

            ExceptionHandler eh1 = tryBB.getExceptionHandlers().get(1);

            assertNull(eh1.getInternalThrowableName());
            assertEquals(TYPE_THROW, eh1.getBasicBlock().getType());

            BasicBlock nextBB = tryBB.getNext();

            assertNotNull(nextBB);
            assertEquals(TYPE_END, nextBB.getType());
        }
    }

    // --- complexMethodTryFinallyReturn --- //
    @Test
    public void testJdk170MethodComplexTryCatchCatchFinally() throws Exception {
        try (InputStream resource = getResource("zip/data-java-jdk-1.7.0.zip")) {
            ControlFlowGraph cfg = checkCFGReduction(searchMethod(resource, "org/jd/core/test/TryCatchFinally", "methodTryTryFinallyFinallyTryFinallyReturn"));
            BasicBlock startBB = cfg.getStart();

            assertNotNull(startBB);

            BasicBlock simpleBB = startBB.getNext();

            assertEquals(TYPE_STATEMENTS, simpleBB.getType());

            BasicBlock tryBB = simpleBB.getNext();

            assertNotNull(tryBB);
            assertEquals(TYPE_TRY, tryBB.getType());
            assertNotNull(tryBB.getExceptionHandlers());
            assertEquals(1, tryBB.getExceptionHandlers().size());
            assertEquals(TYPE_STATEMENTS, tryBB.getNext().getType());
            assertEquals(TYPE_RETURN, tryBB.getNext().getNext().getType());

            BasicBlock bb = tryBB.getSub1();

            assertEquals(TYPE_TRY, bb.getType());

            ExceptionHandler eh0 = tryBB.getExceptionHandlers().get(0);

            assertNull(eh0.getInternalThrowableName());
            assertEquals(TYPE_STATEMENTS, eh0.getBasicBlock().getType());

            BasicBlock subTryBB = tryBB.getSub1();

            assertNotNull(subTryBB);
            assertEquals(TYPE_TRY, subTryBB.getType());
            assertEquals(TYPE_TRY, subTryBB.getNext().getType());
            assertEquals(TYPE_END, subTryBB.getNext().getNext().getType());
        }
    }

    @Test
    public void testJdk170MethodTryCatchTryCatchThrow() throws Exception {
        try (InputStream resource = getResource("zip/data-java-jdk-1.7.0.zip")) {
            checkCFGReduction(searchMethod(resource, "org/jd/core/test/TryCatchFinally", "methodTryCatchTryCatchThrow"));
        }
    }

    // --- methodTryTryFinallyFinallyTryFinally --- //
    @Test
    public void testEclipseJavaCompiler370MethodTryTryFinallyFinallyTryFinally() throws Exception {
        try (InputStream resource = getResource("zip/data-java-eclipse-java-compiler-3.7.0.zip")) {
            ControlFlowGraph cfg = checkCFGReduction(searchMethod(resource, "org/jd/core/test/TryCatchFinally", "methodTryTryFinallyFinallyTryFinally"));
            BasicBlock startBB = cfg.getStart();

            assertNotNull(startBB);

            BasicBlock simpleBB = startBB.getNext();

            assertEquals(TYPE_STATEMENTS, simpleBB.getType());

            BasicBlock tryBB = simpleBB.getNext();

            assertNotNull(tryBB);
            assertEquals(TYPE_TRY_ECLIPSE, tryBB.getType());
            assertNotNull(tryBB.getExceptionHandlers());
            assertEquals(1, tryBB.getExceptionHandlers().size());
            assertEquals(TYPE_TRY_ECLIPSE, tryBB.getNext().getType());
            assertEquals(TYPE_STATEMENTS, tryBB.getNext().getNext().getType());
            assertEquals(TYPE_RETURN, tryBB.getNext().getNext().getNext().getType());

            ExceptionHandler eh0 = tryBB.getExceptionHandlers().get(0);

            assertNull(eh0.getInternalThrowableName());
            assertEquals(TYPE_STATEMENTS, eh0.getBasicBlock().getType());

            BasicBlock subTryBB = tryBB.getSub1();

            assertEquals(TYPE_TRY_ECLIPSE, subTryBB.getType());
            assertNotNull(subTryBB.getExceptionHandlers());
            assertEquals(1, subTryBB.getExceptionHandlers().size());
            assertEquals(TYPE_STATEMENTS, subTryBB.getNext().getType());
            assertEquals(TYPE_END, subTryBB.getNext().getNext().getType());

            eh0 = subTryBB.getExceptionHandlers().get(0);

            assertNull(eh0.getInternalThrowableName());
            assertEquals(TYPE_THROW, eh0.getBasicBlock().getType());
        }
    }

    @Test
    public void testEclipseJavaCompiler3130MethodTryTryFinallyFinallyTryFinally() throws Exception {
        try (InputStream resource = getResource("zip/data-java-eclipse-java-compiler-3.13.0.zip")) {
            ControlFlowGraph cfg = checkCFGReduction(searchMethod(resource, "org/jd/core/test/TryCatchFinally", "methodTryTryFinallyFinallyTryFinally"));
            BasicBlock startBB = cfg.getStart();

            assertNotNull(startBB);

            BasicBlock simpleBB = startBB.getNext();

            assertEquals(TYPE_STATEMENTS, simpleBB.getType());

            BasicBlock tryBB = simpleBB.getNext();

            assertNotNull(tryBB);
            assertEquals(TYPE_TRY_ECLIPSE, tryBB.getType());
            assertNotNull(tryBB.getExceptionHandlers());
            assertEquals(1, tryBB.getExceptionHandlers().size());
            assertEquals(TYPE_TRY_ECLIPSE, tryBB.getNext().getType());
            assertEquals(TYPE_STATEMENTS, tryBB.getNext().getNext().getType());
            assertEquals(TYPE_RETURN, tryBB.getNext().getNext().getNext().getType());

            ExceptionHandler eh0 = tryBB.getExceptionHandlers().get(0);

            assertNull(eh0.getInternalThrowableName());
            assertEquals(TYPE_STATEMENTS, eh0.getBasicBlock().getType());

            BasicBlock subTryBB = tryBB.getSub1();

            assertEquals(TYPE_TRY_ECLIPSE, subTryBB.getType());
            assertNotNull(subTryBB.getExceptionHandlers());
            assertEquals(1, subTryBB.getExceptionHandlers().size());
            assertEquals(TYPE_STATEMENTS, subTryBB.getNext().getType());
            assertEquals(TYPE_END, subTryBB.getNext().getNext().getType());

            eh0 = subTryBB.getExceptionHandlers().get(0);

            assertNull(eh0.getInternalThrowableName());
            assertEquals(TYPE_THROW, eh0.getBasicBlock().getType());
        }
    }

    @Test
    public void testJdk118MethodTryTryFinallyFinallyTryFinally() throws Exception {
        try (InputStream resource = getResource("zip/data-java-jdk-1.1.8.zip")) {
            checkCFGReduction(searchMethod(resource, "org/jd/core/test/TryCatchFinally", "methodTryTryFinallyFinallyTryFinally"));
        }
    }

    @Test
    public void testJdk131MethodTryTryFinallyFinallyTryFinally() throws Exception {
        try (InputStream resource = getResource("zip/data-java-jdk-1.3.1.zip")) {
            checkCFGReduction(searchMethod(resource, "org/jd/core/test/TryCatchFinally", "methodTryTryFinallyFinallyTryFinally"));
        }
    }

    @Test
    public void testMethodWithTry() throws Exception {
        Method method = new Method();
        Constant[] constants = new Constant[1177];
        constants[1] = new ConstantString(623);
        constants[2] = new ConstantMethodref(396, 624);
        constants[3] = new ConstantMethodref(396, 625);
        constants[4] = new ConstantMethodref(626, 627);
        constants[5] = new ConstantClass(628);
        constants[6] = new ConstantMethodref(5, 629);
        constants[7] = new ConstantMethodref(396, 630);
        constants[8] = new ConstantMethodref(396, 631);
        constants[9] = new ConstantFieldref(396, 632);
        constants[10] = new ConstantMethodref(86, 633);
        constants[11] = new ConstantFieldref(396, 634);
        constants[12] = new ConstantString(635);
        constants[13] = new ConstantString(636);
        constants[14] = new ConstantMethodref(637, 638);
        constants[15] = new ConstantMethodref(100, 639);
        constants[16] = new ConstantClass(640);
        constants[17] = new ConstantMethodref(16, 641);
        constants[18] = new ConstantMethodref(100, 642);
        constants[19] = new ConstantString(643);
        constants[20] = new ConstantMethodref(100, 644);
        constants[21] = new ConstantClass(645);
        constants[22] = new ConstantMethodref(21, 646);
        constants[23] = new ConstantMethodref(100, 647);
        constants[24] = new ConstantMethodref(626, 648);
        constants[25] = new ConstantMethodref(86, 647);
        constants[26] = new ConstantFieldref(396, 649);
        constants[27] = new ConstantClass(650);
        constants[28] = new ConstantMethodref(88, 651);
        constants[29] = new ConstantString(652);
        constants[30] = new ConstantMethodref(88, 639);
        constants[31] = new ConstantMethodref(88, 647);
        constants[32] = new ConstantFieldref(396, 653);
        constants[33] = new ConstantString(654);
        constants[34] = new ConstantFieldref(396, 655);
        constants[35] = new ConstantMethodref(90, 647);
        constants[36] = new ConstantFieldref(396, 656);
        constants[37] = new ConstantMethodref(92, 627);
        constants[38] = new ConstantMethodref(92, 647);
        constants[39] = new ConstantFieldref(396, 657);
        constants[40] = new ConstantString(658);
        constants[41] = new ConstantMethodref(94, 639);
        constants[42] = new ConstantString(659);
        constants[43] = new ConstantMethodref(94, 660);
        constants[44] = new ConstantMethodref(94, 661);
        constants[45] = new ConstantFieldref(396, 662);
        constants[46] = new ConstantString(663);
        constants[47] = new ConstantString(664);
        constants[48] = new ConstantMethodref(92, 648);
        constants[49] = new ConstantFieldref(396, 665);
        constants[50] = new ConstantFieldref(396, 666);
        constants[51] = new ConstantFieldref(396, 667);
        constants[52] = new ConstantFieldref(396, 668);
        constants[53] = new ConstantFieldref(396, 669);
        constants[54] = new ConstantMethodref(626, 670);
        constants[55] = new ConstantFieldref(396, 671);
        constants[56] = new ConstantFieldref(396, 672);
        constants[57] = new ConstantFieldref(396, 673);
        constants[58] = new ConstantString(674);
        constants[59] = new ConstantString(675);
        constants[60] = new ConstantString(676);
        constants[61] = new ConstantString(677);
        constants[62] = new ConstantString(678);
        constants[63] = new ConstantString(679);
        constants[64] = new ConstantString(680);
        constants[65] = new ConstantString(681);
        constants[66] = new ConstantString(682);
        constants[67] = new ConstantString(683);
        constants[68] = new ConstantMethodref(94, 647);
        constants[69] = new ConstantString(684);
        constants[70] = new ConstantString(685);
        constants[71] = new ConstantFieldref(396, 686);
        constants[72] = new ConstantMethodref(96, 687);
        constants[73] = new ConstantMethodref(96, 688);
        constants[74] = new ConstantFieldref(396, 689);
        constants[75] = new ConstantMethodref(690, 648);
        constants[76] = new ConstantMethodref(96, 647);
        constants[77] = new ConstantMethodref(98, 661);
        constants[78] = new ConstantClass(691);
        constants[79] = new ConstantMethodref(78, 641);
        constants[80] = new ConstantMethodref(94, 692);
        constants[81] = new ConstantClass(693);
        constants[82] = new ConstantMethodref(81, 641);
        constants[83] = new ConstantMethodref(396, 694);
        constants[84] = new ConstantMethodref(397, 695);
        constants[85] = new ConstantFieldref(396, 696);
        constants[86] = new ConstantClass(697);
        constants[87] = new ConstantMethodref(86, 695);
        constants[88] = new ConstantClass(698);
        constants[89] = new ConstantMethodref(88, 695);
        constants[90] = new ConstantClass(699);
        constants[91] = new ConstantMethodref(90, 695);
        constants[92] = new ConstantClass(700);
        constants[93] = new ConstantMethodref(92, 695);
        constants[94] = new ConstantClass(701);
        constants[95] = new ConstantMethodref(94, 695);
        constants[96] = new ConstantClass(702);
        constants[97] = new ConstantMethodref(96, 695);
        constants[98] = new ConstantClass(703);
        constants[99] = new ConstantMethodref(98, 695);
        constants[100] = new ConstantClass(704);
        constants[101] = new ConstantMethodref(100, 695);
        constants[102] = new ConstantFieldref(396, 705);
        constants[103] = new ConstantFieldref(396, 706);
        constants[104] = new ConstantFieldref(396, 707);
        constants[105] = new ConstantClass(708);
        constants[106] = new ConstantMethodref(105, 695);
        constants[107] = new ConstantFieldref(396, 709);
        constants[108] = new ConstantMethodref(396, 710);
        constants[109] = new ConstantClass(711);
        constants[110] = new ConstantMethodref(712, 713);
        constants[111] = new ConstantMethodref(396, 714);
        constants[112] = new ConstantMethodref(715, 716);
        constants[113] = new ConstantMethodref(396, 695);
        constants[114] = new ConstantMethodref(717, 718);
        constants[115] = new ConstantClass(719);
        constants[116] = new ConstantMethodref(115, 720);
        constants[117] = new ConstantMethodref(717, 721);
        constants[118] = new ConstantMethodref(397, 631);
        constants[119] = new ConstantMethodref(396, 722);
        constants[120] = new ConstantMethodref(397, 723);
        constants[121] = new ConstantMethodref(396, 724);
        constants[122] = new ConstantMethodref(396, 725);
        constants[123] = new ConstantMethodref(726, 727);
        constants[124] = new ConstantMethodref(728, 729);
        constants[125] = new ConstantFieldref(5, 730);
        constants[126] = new ConstantFieldref(731, 732);
        constants[127] = new ConstantFieldref(731, 733);
        constants[128] = new ConstantFieldref(5, 734);
        constants[129] = new ConstantFieldref(731, 735);
        constants[130] = new ConstantFieldref(731, 736);
        constants[131] = new ConstantMethodref(396, 737);
        constants[132] = new ConstantClass(738);
        constants[133] = new ConstantMethodref(194, 739);
        constants[134] = new ConstantMethodref(740, 741);
        constants[135] = new ConstantMethodref(740, 742);
        constants[136] = new ConstantMethodref(743, 744);
        constants[137] = new ConstantMethodref(740, 745);
        constants[138] = new ConstantInterfaceMethodref(746, 747);
        constants[139] = new ConstantMethodref(748, 749);
        constants[140] = new ConstantMethodref(149, 750);
        constants[141] = new ConstantString(751);
        constants[142] = new ConstantMethodref(715, 752);
        constants[143] = new ConstantMethodref(753, 754);
        constants[144] = new ConstantMethodref(86, 639);
        constants[145] = new ConstantMethodref(396, 755);
        constants[146] = new ConstantInterfaceMethodref(746, 756);
        constants[147] = new ConstantMethodref(740, 757);
        constants[148] = new ConstantMethodref(149, 758);
        constants[149] = new ConstantClass(759);
        constants[150] = new ConstantMethodref(396, 760);
        constants[151] = new ConstantMethodref(396, 761);
        constants[152] = new ConstantFieldref(396, 762);
        constants[153] = new ConstantMethodref(149, 763);
        constants[154] = new ConstantString(764);
        constants[155] = new ConstantMethodref(149, 765);
        constants[156] = new ConstantMethodref(98, 639);
        constants[157] = new ConstantMethodref(149, 766);
        constants[158] = new ConstantMethodref(100, 767);
        constants[159] = new ConstantFieldref(396, 768);
        constants[160] = new ConstantMethodref(232, 760);
        constants[161] = new ConstantFieldref(396, 769);
        constants[162] = new ConstantMethodref(236, 760);
        constants[163] = new ConstantFieldref(396, 770);
        constants[164] = new ConstantMethodref(239, 760);
        constants[165] = new ConstantFieldref(396, 771);
        constants[166] = new ConstantMethodref(242, 760);
        constants[167] = new ConstantFieldref(396, 772);
        constants[168] = new ConstantMethodref(253, 760);
        constants[169] = new ConstantFieldref(396, 773);
        constants[170] = new ConstantMethodref(250, 760);
        constants[171] = new ConstantFieldref(396, 774);
        constants[172] = new ConstantMethodref(311, 760);
        constants[173] = new ConstantFieldref(396, 775);
        constants[174] = new ConstantMethodref(247, 760);
        constants[175] = new ConstantFieldref(396, 776);
        constants[176] = new ConstantMethodref(271, 760);
        constants[177] = new ConstantFieldref(396, 777);
        constants[178] = new ConstantMethodref(257, 760);
        constants[179] = new ConstantFieldref(396, 778);
        constants[180] = new ConstantMethodref(262, 760);
        constants[181] = new ConstantFieldref(396, 779);
        constants[182] = new ConstantMethodref(265, 760);
        constants[183] = new ConstantFieldref(396, 780);
        constants[184] = new ConstantMethodref(268, 760);
        constants[185] = new ConstantFieldref(396, 781);
        constants[186] = new ConstantMethodref(280, 760);
        constants[187] = new ConstantFieldref(396, 782);
        constants[188] = new ConstantMethodref(283, 760);
        constants[189] = new ConstantMethodref(396, 783);
        constants[190] = new ConstantMethodref(149, 784);
        constants[191] = new ConstantMethodref(396, 785);
        constants[192] = new ConstantMethodref(90, 786);
        constants[193] = new ConstantMethodref(194, 787);
        constants[194] = new ConstantClass(788);
        constants[195] = new ConstantString(789);
        constants[196] = new ConstantMethodref(194, 790);
        constants[197] = new ConstantString(791);
        constants[198] = new ConstantMethodref(715, 792);
        constants[199] = new ConstantMethodref(194, 793);
        constants[200] = new ConstantClass(794);
        constants[201] = new ConstantMethodref(200, 695);
        constants[202] = new ConstantString(795);
        constants[203] = new ConstantMethodref(200, 796);
        constants[204] = new ConstantMethodref(200, 797);
        constants[205] = new ConstantMethodref(715, 798);
        constants[206] = new ConstantInterfaceMethodref(746, 799);
        constants[207] = new ConstantMethodref(105, 800);
        constants[208] = new ConstantString(801);
        constants[209] = new ConstantString(802);
        constants[210] = new ConstantMethodref(753, 803);
        constants[211] = new ConstantMethodref(194, 804);
        constants[212] = new ConstantMethodref(740, 805);
        constants[213] = new ConstantMethodref(218, 806);
        constants[214] = new ConstantMethodref(105, 807);
        constants[215] = new ConstantInterfaceMethodref(746, 808);
        constants[216] = new ConstantMethodref(740, 809);
        constants[217] = new ConstantMethodref(132, 750);
        constants[218] = new ConstantClass(810);
        constants[219] = new ConstantString(811);
        constants[220] = new ConstantInterfaceMethodref(812, 813);
        constants[221] = new ConstantMethodref(86, 814);
        constants[222] = new ConstantMethodref(371, 815);
        constants[223] = new ConstantMethodref(715, 816);
        constants[224] = new ConstantMethodref(396, 817);
        constants[225] = new ConstantString(818);
        constants[226] = new ConstantMethodref(396, 819);
        constants[227] = new ConstantMethodref(396, 820);
        constants[228] = new ConstantClass(821);
        constants[229] = new ConstantMethodref(396, 822);
        constants[230] = new ConstantMethodref(715, 823);
        constants[231] = new ConstantMethodref(396, 824);
        constants[232] = new ConstantClass(825);
        constants[233] = new ConstantMethodref(232, 641);
        constants[234] = new ConstantMethodref(90, 648);
        constants[235] = new ConstantMethodref(232, 631);
        constants[236] = new ConstantClass(826);
        constants[237] = new ConstantMethodref(236, 641);
        constants[238] = new ConstantMethodref(236, 631);
        constants[239] = new ConstantClass(827);
        constants[240] = new ConstantMethodref(239, 641);
        constants[241] = new ConstantMethodref(239, 631);
        constants[242] = new ConstantClass(828);
        constants[243] = new ConstantMethodref(242, 641);
        constants[244] = new ConstantMethodref(242, 631);
        constants[245] = new ConstantString(829);
        constants[246] = new ConstantMethodref(830, 831);
        constants[247] = new ConstantClass(832);
        constants[248] = new ConstantMethodref(247, 833);
        constants[249] = new ConstantMethodref(247, 631);
        constants[250] = new ConstantClass(834);
        constants[251] = new ConstantMethodref(250, 641);
        constants[252] = new ConstantMethodref(250, 631);
        constants[253] = new ConstantClass(835);
        constants[254] = new ConstantMethodref(253, 641);
        constants[255] = new ConstantMethodref(253, 631);
        constants[256] = new ConstantMethodref(396, 836);
        constants[257] = new ConstantClass(837);
        constants[258] = new ConstantMethodref(257, 641);
        constants[259] = new ConstantMethodref(257, 631);
        constants[260] = new ConstantString(838);
        constants[261] = new ConstantMethodref(839, 840);
        constants[262] = new ConstantClass(841);
        constants[263] = new ConstantMethodref(262, 641);
        constants[264] = new ConstantMethodref(262, 631);
        constants[265] = new ConstantClass(842);
        constants[266] = new ConstantMethodref(265, 641);
        constants[267] = new ConstantMethodref(265, 631);
        constants[268] = new ConstantClass(843);
        constants[269] = new ConstantMethodref(268, 641);
        constants[270] = new ConstantMethodref(268, 631);
        constants[271] = new ConstantClass(844);
        constants[272] = new ConstantMethodref(271, 641);
        constants[273] = new ConstantMethodref(271, 631);
        constants[274] = new ConstantMethodref(740, 845);
        constants[275] = new ConstantInterfaceMethodref(846, 847);
        constants[276] = new ConstantClass(848);
        constants[277] = new ConstantString(849);
        constants[278] = new ConstantMethodref(850, 851);
        constants[279] = new ConstantString(852);
        constants[280] = new ConstantClass(853);
        constants[281] = new ConstantMethodref(280, 854);
        constants[282] = new ConstantMethodref(280, 631);
        constants[283] = new ConstantClass(855);
        constants[284] = new ConstantMethodref(283, 641);
        constants[285] = new ConstantMethodref(283, 631);
        constants[286] = new ConstantString(856);
        constants[287] = new ConstantMethodref(90, 857);
        constants[288] = new ConstantString(858);
        constants[289] = new ConstantString(859);
        constants[290] = new ConstantString(860);
        constants[291] = new ConstantString(861);
        constants[292] = new ConstantString(862);
        constants[293] = new ConstantString(863);
        constants[294] = new ConstantString(864);
        constants[295] = new ConstantString(865);
        constants[296] = new ConstantString(866);
        constants[297] = new ConstantString(867);
        constants[298] = new ConstantString(868);
        constants[299] = new ConstantString(869);
        constants[300] = new ConstantString(870);
        constants[301] = new ConstantString(871);
        constants[302] = new ConstantClass(872);
        constants[303] = new ConstantMethodref(302, 695);
        constants[304] = new ConstantMethodref(302, 873);
        constants[305] = new ConstantMethodref(302, 874);
        constants[306] = new ConstantFieldref(302, 875);
        constants[307] = new ConstantClass(876);
        constants[308] = new ConstantMethodref(307, 695);
        constants[309] = new ConstantClass(877);
        constants[310] = new ConstantMethodref(309, 878);
        constants[311] = new ConstantClass(879);
        constants[312] = new ConstantMethodref(311, 880);
        constants[313] = new ConstantMethodref(307, 881);
        constants[314] = new ConstantMethodref(311, 631);
        constants[315] = new ConstantMethodref(396, 882);
        constants[316] = new ConstantMethodref(232, 883);
        constants[317] = new ConstantMethodref(236, 883);
        constants[318] = new ConstantMethodref(239, 883);
        constants[319] = new ConstantMethodref(242, 883);
        constants[320] = new ConstantMethodref(253, 883);
        constants[321] = new ConstantMethodref(250, 883);
        constants[322] = new ConstantMethodref(247, 883);
        constants[323] = new ConstantMethodref(271, 883);
        constants[324] = new ConstantMethodref(311, 883);
        constants[325] = new ConstantMethodref(257, 883);
        constants[326] = new ConstantMethodref(262, 883);
        constants[327] = new ConstantMethodref(265, 883);
        constants[328] = new ConstantMethodref(268, 883);
        constants[329] = new ConstantMethodref(283, 883);
        constants[330] = new ConstantMethodref(280, 884);
        constants[331] = new ConstantInterfaceMethodref(746, 885);
        constants[332] = new ConstantString(886);
        constants[333] = new ConstantMethodref(753, 887);
        constants[334] = new ConstantString(888);
        constants[335] = new ConstantMethodref(889, 890);
        constants[336] = new ConstantMethodref(94, 891);
        constants[337] = new ConstantMethodref(753, 892);
        constants[338] = new ConstantMethodref(371, 893);
        constants[339] = new ConstantString(894);
        constants[340] = new ConstantString(895);
        constants[341] = new ConstantString(896);
        constants[342] = new ConstantString(897);
        constants[343] = new ConstantClass(898);
        constants[344] = new ConstantString(899);
        constants[345] = new ConstantMethodref(712, 900);
        constants[346] = new ConstantString(901);
        constants[347] = new ConstantMethodref(715, 902);
        constants[348] = new ConstantMethodref(105, 903);
        constants[349] = new ConstantMethodref(105, 904);
        constants[350] = new ConstantString(905);
        constants[351] = new ConstantString(906);
        constants[352] = new ConstantString(907);
        constants[353] = new ConstantString(908);
        constants[354] = new ConstantClass(909);
        constants[355] = new ConstantMethodref(354, 910);
        constants[356] = new ConstantMethodref(396, 911);
        constants[357] = new ConstantMethodref(371, 912);
        constants[358] = new ConstantString(913);
        constants[359] = new ConstantString(914);
        constants[360] = new ConstantString(915);
        constants[361] = new ConstantMethodref(149, 916);
        constants[362] = new ConstantMethodref(149, 917);
        constants[363] = new ConstantMethodref(311, 918);
        constants[364] = new ConstantMethodref(149, 919);
        constants[365] = new ConstantMethodref(98, 814);
        constants[366] = new ConstantMethodref(149, 920);
        constants[367] = new ConstantMethodref(149, 921);
        constants[368] = new ConstantMethodref(100, 922);
        constants[369] = new ConstantMethodref(149, 923);
        constants[370] = new ConstantMethodref(105, 924);
        constants[371] = new ConstantClass(925);
        constants[372] = new ConstantMethodref(149, 926);
        constants[373] = new ConstantMethodref(396, 927);
        constants[374] = new ConstantMethodref(149, 928);
        constants[375] = new ConstantInterfaceMethodref(746, 929);
        constants[376] = new ConstantMethodref(194, 930);
        constants[377] = new ConstantString(931);
        constants[378] = new ConstantMethodref(105, 932);
        constants[379] = new ConstantMethodref(715, 933);
        constants[380] = new ConstantMethodref(105, 934);
        constants[381] = new ConstantMethodref(149, 935);
        constants[382] = new ConstantString(936);
        constants[383] = new ConstantMethodref(715, 937);
        constants[384] = new ConstantMethodref(149, 938);
        constants[385] = new ConstantMethodref(90, 939);
        constants[386] = new ConstantMethodref(90, 940);
        constants[387] = new ConstantMethodref(90, 941);
        constants[388] = new ConstantString(942);
        constants[389] = new ConstantFieldref(396, 943);
        constants[390] = new ConstantFieldref(396, 944);
        constants[391] = new ConstantMethodref(105, 945);
        constants[392] = new ConstantMethodref(105, 946);
        constants[393] = new ConstantMethodref(748, 947);
        constants[394] = new ConstantString(948);
        constants[395] = new ConstantString(949);
        constants[396] = new ConstantClass(950);
        constants[397] = new ConstantClass(951);
        constants[398] = new ConstantUtf8("");
        constants[399] = new ConstantUtf8("");
        constants[400] = new ConstantUtf8("");
        constants[401] = new ConstantUtf8("");
        constants[402] = new ConstantUtf8("");
        constants[403] = new ConstantUtf8("");
        constants[404] = new ConstantLong(0L);
        constants[405] = null;
        constants[406] = new ConstantUtf8("");
        constants[407] = new ConstantUtf8("");
        constants[408] = new ConstantString(952);
        constants[409] = new ConstantUtf8("");
        constants[410] = new ConstantUtf8("");
        constants[411] = new ConstantUtf8("");
        constants[412] = new ConstantUtf8("");
        constants[413] = new ConstantUtf8("");
        constants[414] = new ConstantUtf8("");
        constants[415] = new ConstantUtf8("");
        constants[416] = new ConstantUtf8("");
        constants[417] = new ConstantUtf8("");
        constants[418] = new ConstantUtf8("");
        constants[419] = new ConstantUtf8("");
        constants[420] = new ConstantUtf8("");
        constants[421] = new ConstantUtf8("");
        constants[422] = new ConstantUtf8("");
        constants[423] = new ConstantUtf8("");
        constants[424] = new ConstantUtf8("");
        constants[425] = new ConstantUtf8("");
        constants[426] = new ConstantUtf8("");
        constants[427] = new ConstantUtf8("");
        constants[428] = new ConstantUtf8("");
        constants[429] = new ConstantUtf8("");
        constants[430] = new ConstantUtf8("");
        constants[431] = new ConstantUtf8("");
        constants[432] = new ConstantUtf8("");
        constants[433] = new ConstantUtf8("");
        constants[434] = new ConstantUtf8("");
        constants[435] = new ConstantUtf8("");
        constants[436] = new ConstantUtf8("");
        constants[437] = new ConstantUtf8("");
        constants[438] = new ConstantUtf8("");
        constants[439] = new ConstantUtf8("");
        constants[440] = new ConstantUtf8("");
        constants[441] = new ConstantUtf8("");
        constants[442] = new ConstantUtf8("");
        constants[443] = new ConstantUtf8("");
        constants[444] = new ConstantUtf8("");
        constants[445] = new ConstantUtf8("");
        constants[446] = new ConstantUtf8("");
        constants[447] = new ConstantUtf8("");
        constants[448] = new ConstantUtf8("");
        constants[449] = new ConstantUtf8("");
        constants[450] = new ConstantUtf8("");
        constants[451] = new ConstantUtf8("");
        constants[452] = new ConstantUtf8("");
        constants[453] = new ConstantUtf8("");
        constants[454] = new ConstantUtf8("");
        constants[455] = new ConstantUtf8("");
        constants[456] = new ConstantUtf8("");
        constants[457] = new ConstantUtf8("");
        constants[458] = new ConstantUtf8("");
        constants[459] = new ConstantUtf8("");
        constants[460] = new ConstantUtf8("");
        constants[461] = new ConstantUtf8("");
        constants[462] = new ConstantUtf8("");
        constants[463] = new ConstantUtf8("");
        constants[464] = new ConstantUtf8("");
        constants[465] = new ConstantUtf8("");
        constants[466] = new ConstantUtf8("");
        constants[467] = new ConstantUtf8("");
        constants[468] = new ConstantUtf8("");
        constants[469] = new ConstantUtf8("");
        constants[470] = new ConstantUtf8("");
        constants[471] = new ConstantUtf8("");
        constants[472] = new ConstantUtf8("");
        constants[473] = new ConstantUtf8("");
        constants[474] = new ConstantUtf8("");
        constants[475] = new ConstantUtf8("");
        constants[476] = new ConstantUtf8("");
        constants[477] = new ConstantUtf8("");
        constants[478] = new ConstantUtf8("");
        constants[479] = new ConstantUtf8("");
        constants[480] = new ConstantUtf8("");
        constants[481] = new ConstantUtf8("");
        constants[482] = new ConstantUtf8("");
        constants[483] = new ConstantUtf8("");
        constants[484] = new ConstantUtf8("");
        constants[485] = new ConstantUtf8("");
        constants[486] = new ConstantUtf8("");
        constants[487] = new ConstantUtf8("");
        constants[488] = new ConstantUtf8("");
        constants[489] = new ConstantUtf8("");
        constants[490] = new ConstantUtf8("");
        constants[491] = new ConstantUtf8("");
        constants[492] = new ConstantUtf8("");
        constants[493] = new ConstantClass(950);
        constants[494] = new ConstantClass(711);
        constants[495] = new ConstantUtf8("");
        constants[496] = new ConstantUtf8("");
        constants[497] = new ConstantUtf8("");
        constants[498] = new ConstantUtf8("");
        constants[499] = new ConstantUtf8("");
        constants[500] = new ConstantUtf8("");
        constants[501] = new ConstantUtf8("");
        constants[502] = new ConstantUtf8("");
        constants[503] = new ConstantUtf8("");
        constants[504] = new ConstantUtf8("");
        constants[505] = new ConstantUtf8("");
        constants[506] = new ConstantUtf8("");
        constants[507] = new ConstantUtf8("");
        constants[508] = new ConstantUtf8("");
        constants[509] = new ConstantUtf8("");
        constants[510] = new ConstantClass(628);
        constants[511] = new ConstantClass(953);
        constants[512] = new ConstantClass(954);
        constants[513] = new ConstantUtf8("");
        constants[514] = new ConstantUtf8("");
        constants[515] = new ConstantUtf8("");
        constants[516] = new ConstantUtf8("");
        constants[517] = new ConstantUtf8("");
        constants[518] = new ConstantUtf8("");
        constants[519] = new ConstantUtf8("");
        constants[520] = new ConstantUtf8("");
        constants[521] = new ConstantUtf8("");
        constants[522] = new ConstantUtf8("");
        constants[523] = new ConstantUtf8("");
        constants[524] = new ConstantUtf8("");
        constants[525] = new ConstantUtf8("");
        constants[526] = new ConstantClass(955);
        constants[527] = new ConstantClass(708);
        constants[528] = new ConstantClass(925);
        constants[529] = new ConstantClass(956);
        constants[530] = new ConstantUtf8("");
        constants[531] = new ConstantUtf8("");
        constants[532] = new ConstantClass(759);
        constants[533] = new ConstantUtf8("");
        constants[534] = new ConstantUtf8("");
        constants[535] = new ConstantClass(697);
        constants[536] = new ConstantUtf8("");
        constants[537] = new ConstantUtf8("");
        constants[538] = new ConstantUtf8("");
        constants[539] = new ConstantUtf8("");
        constants[540] = new ConstantUtf8("");
        constants[541] = new ConstantUtf8("");
        constants[542] = new ConstantUtf8("");
        constants[543] = new ConstantClass(810);
        constants[544] = new ConstantClass(738);
        constants[545] = new ConstantUtf8("");
        constants[546] = new ConstantUtf8("");
        constants[547] = new ConstantUtf8("");
        constants[548] = new ConstantUtf8("");
        constants[549] = new ConstantUtf8("");
        constants[550] = new ConstantUtf8("");
        constants[551] = new ConstantUtf8("");
        constants[552] = new ConstantUtf8("");
        constants[553] = new ConstantUtf8("");
        constants[554] = new ConstantUtf8("");
        constants[555] = new ConstantUtf8("");
        constants[556] = new ConstantUtf8("");
        constants[557] = new ConstantClass(957);
        constants[558] = new ConstantClass(848);
        constants[559] = new ConstantUtf8("");
        constants[560] = new ConstantUtf8("");
        constants[561] = new ConstantUtf8("");
        constants[562] = new ConstantUtf8("");
        constants[563] = new ConstantUtf8("");
        constants[564] = new ConstantUtf8("");
        constants[565] = new ConstantUtf8("");
        constants[566] = new ConstantUtf8("");
        constants[567] = new ConstantUtf8("");
        constants[568] = new ConstantUtf8("");
        constants[569] = new ConstantUtf8("");
        constants[570] = new ConstantUtf8("");
        constants[571] = new ConstantUtf8("");
        constants[572] = new ConstantUtf8("");
        constants[573] = new ConstantUtf8("");
        constants[574] = new ConstantUtf8("");
        constants[575] = new ConstantUtf8("");
        constants[576] = new ConstantUtf8("");
        constants[577] = new ConstantUtf8("");
        constants[578] = new ConstantUtf8("");
        constants[579] = new ConstantUtf8("");
        constants[580] = new ConstantUtf8("");
        constants[581] = new ConstantUtf8("");
        constants[582] = new ConstantUtf8("");
        constants[583] = new ConstantUtf8("");
        constants[584] = new ConstantUtf8("");
        constants[585] = new ConstantUtf8("");
        constants[586] = new ConstantUtf8("");
        constants[587] = new ConstantUtf8("");
        constants[588] = new ConstantUtf8("");
        constants[589] = new ConstantUtf8("");
        constants[590] = new ConstantUtf8("");
        constants[591] = new ConstantUtf8("");
        constants[592] = new ConstantUtf8("");
        constants[593] = new ConstantUtf8("");
        constants[594] = new ConstantUtf8("");
        constants[595] = new ConstantUtf8("");
        constants[596] = new ConstantClass(958);
        constants[597] = new ConstantClass(898);
        constants[598] = new ConstantClass(586);
        constants[599] = new ConstantUtf8("");
        constants[600] = new ConstantUtf8("");
        constants[601] = new ConstantUtf8("");
        constants[602] = new ConstantUtf8("");
        constants[603] = new ConstantUtf8("");
        constants[604] = new ConstantUtf8("");
        constants[605] = new ConstantUtf8("");
        constants[606] = new ConstantUtf8("");
        constants[607] = new ConstantUtf8("");
        constants[608] = new ConstantUtf8("");
        constants[609] = new ConstantUtf8("");
        constants[610] = new ConstantUtf8("");
        constants[611] = new ConstantUtf8("");
        constants[612] = new ConstantClass(794);
        constants[613] = new ConstantUtf8("");
        constants[614] = new ConstantUtf8("");
        constants[615] = new ConstantUtf8("");
        constants[616] = new ConstantUtf8("");
        constants[617] = new ConstantUtf8("");
        constants[618] = new ConstantUtf8("");
        constants[619] = new ConstantUtf8("");
        constants[620] = new ConstantUtf8("");
        constants[621] = new ConstantUtf8("");
        constants[622] = new ConstantUtf8("");
        constants[623] = new ConstantUtf8("");
        constants[624] = new ConstantNameAndType(959, 495);
        constants[625] = new ConstantNameAndType(960, 961);
        constants[626] = new ConstantClass(962);
        constants[627] = new ConstantNameAndType(963, 964);
        constants[628] = new ConstantUtf8("");
        constants[629] = new ConstantNameAndType(489, 965);
        constants[630] = new ConstantNameAndType(966, 967);
        constants[631] = new ConstantNameAndType(498, 499);
        constants[632] = new ConstantNameAndType(411, 412);
        constants[633] = new ConstantNameAndType(968, 499);
        constants[634] = new ConstantNameAndType(435, 436);
        constants[635] = new ConstantUtf8("");
        constants[636] = new ConstantUtf8("");
        constants[637] = new ConstantClass(969);
        constants[638] = new ConstantNameAndType(970, 971);
        constants[639] = new ConstantNameAndType(972, 495);
        constants[640] = new ConstantUtf8("");
        constants[641] = new ConstantNameAndType(489, 973);
        constants[642] = new ConstantNameAndType(974, 975);
        constants[643] = new ConstantUtf8("");
        constants[644] = new ConstantNameAndType(976, 495);
        constants[645] = new ConstantUtf8("");
        constants[646] = new ConstantNameAndType(489, 977);
        constants[647] = new ConstantNameAndType(978, 979);
        constants[648] = new ConstantNameAndType(980, 981);
        constants[649] = new ConstantNameAndType(413, 414);
        constants[650] = new ConstantUtf8("");
        constants[651] = new ConstantNameAndType(982, 983);
        constants[652] = new ConstantUtf8("");
        constants[653] = new ConstantNameAndType(415, 414);
        constants[654] = new ConstantUtf8("");
        constants[655] = new ConstantNameAndType(416, 417);
        constants[656] = new ConstantNameAndType(418, 419);
        constants[657] = new ConstantNameAndType(420, 421);
        constants[658] = new ConstantUtf8("");
        constants[659] = new ConstantUtf8("");
        constants[660] = new ConstantNameAndType(984, 495);
        constants[661] = new ConstantNameAndType(978, 977);
        constants[662] = new ConstantNameAndType(422, 421);
        constants[663] = new ConstantUtf8("");
        constants[664] = new ConstantUtf8("");
        constants[665] = new ConstantNameAndType(423, 421);
        constants[666] = new ConstantNameAndType(424, 421);
        constants[667] = new ConstantNameAndType(425, 421);
        constants[668] = new ConstantNameAndType(426, 421);
        constants[669] = new ConstantNameAndType(428, 412);
        constants[670] = new ConstantNameAndType(980, 985);
        constants[671] = new ConstantNameAndType(430, 421);
        constants[672] = new ConstantNameAndType(429, 421);
        constants[673] = new ConstantNameAndType(427, 414);
        constants[674] = new ConstantUtf8("");
        constants[675] = new ConstantUtf8("");
        constants[676] = new ConstantUtf8("");
        constants[677] = new ConstantUtf8("");
        constants[678] = new ConstantUtf8("");
        constants[679] = new ConstantUtf8("");
        constants[680] = new ConstantUtf8("");
        constants[681] = new ConstantUtf8("");
        constants[682] = new ConstantUtf8("");
        constants[683] = new ConstantUtf8("");
        constants[684] = new ConstantUtf8("");
        constants[685] = new ConstantUtf8("");
        constants[686] = new ConstantNameAndType(431, 432);
        constants[687] = new ConstantNameAndType(986, 499);
        constants[688] = new ConstantNameAndType(987, 988);
        constants[689] = new ConstantNameAndType(433, 434);
        constants[690] = new ConstantClass(989);
        constants[691] = new ConstantUtf8("");
        constants[692] = new ConstantNameAndType(990, 991);
        constants[693] = new ConstantUtf8("");
        constants[694] = new ConstantNameAndType(992, 993);
        constants[695] = new ConstantNameAndType(489, 479);
        constants[696] = new ConstantNameAndType(409, 410);
        constants[697] = new ConstantUtf8("");
        constants[698] = new ConstantUtf8("");
        constants[699] = new ConstantUtf8("");
        constants[700] = new ConstantUtf8("");
        constants[701] = new ConstantUtf8("");
        constants[702] = new ConstantUtf8("");
        constants[703] = new ConstantUtf8("");
        constants[704] = new ConstantUtf8("");
        constants[705] = new ConstantNameAndType(437, 438);
        constants[706] = new ConstantNameAndType(439, 410);
        constants[707] = new ConstantNameAndType(440, 410);
        constants[708] = new ConstantUtf8("");
        constants[709] = new ConstantNameAndType(474, 475);
        constants[710] = new ConstantNameAndType(478, 479);
        constants[711] = new ConstantUtf8("");
        constants[712] = new ConstantClass(994);
        constants[713] = new ConstantNameAndType(995, 996);
        constants[714] = new ConstantNameAndType(547, 479);
        constants[715] = new ConstantClass(997);
        constants[716] = new ConstantNameAndType(998, 999);
        constants[717] = new ConstantClass(1000);
        constants[718] = new ConstantNameAndType(1001, 1002);
        constants[719] = new ConstantUtf8("");
        constants[720] = new ConstantNameAndType(489, 1003);
        constants[721] = new ConstantNameAndType(1004, 1005);
        constants[722] = new ConstantNameAndType(1006, 1007);
        constants[723] = new ConstantNameAndType(501, 479);
        constants[724] = new ConstantNameAndType(1008, 1009);
        constants[725] = new ConstantNameAndType(1010, 1011);
        constants[726] = new ConstantClass(1012);
        constants[727] = new ConstantNameAndType(1013, 1014);
        constants[728] = new ConstantClass(954);
        constants[729] = new ConstantNameAndType(1015, 1007);
        constants[730] = new ConstantNameAndType(1016, 509);
        constants[731] = new ConstantClass(953);
        constants[732] = new ConstantNameAndType(1017, 509);
        constants[733] = new ConstantNameAndType(1018, 509);
        constants[734] = new ConstantNameAndType(1019, 509);
        constants[735] = new ConstantNameAndType(1020, 509);
        constants[736] = new ConstantNameAndType(1021, 509);
        constants[737] = new ConstantNameAndType(966, 965);
        constants[738] = new ConstantUtf8("");
        constants[739] = new ConstantNameAndType(1022, 1023);
        constants[740] = new ConstantClass(1024);
        constants[741] = new ConstantNameAndType(1025, 1026);
        constants[742] = new ConstantNameAndType(1027, 1028);
        constants[743] = new ConstantClass(956);
        constants[744] = new ConstantNameAndType(1029, 1030);
        constants[745] = new ConstantNameAndType(1031, 1032);
        constants[746] = new ConstantClass(958);
        constants[747] = new ConstantNameAndType(1033, 1034);
        constants[748] = new ConstantClass(1035);
        constants[749] = new ConstantNameAndType(1036, 1037);
        constants[750] = new ConstantNameAndType(1038, 1030);
        constants[751] = new ConstantUtf8("");
        constants[752] = new ConstantNameAndType(1039, 1040);
        constants[753] = new ConstantClass(1041);
        constants[754] = new ConstantNameAndType(1042, 1043);
        constants[755] = new ConstantNameAndType(530, 495);
        constants[756] = new ConstantNameAndType(1036, 1044);
        constants[757] = new ConstantNameAndType(1045, 1002);
        constants[758] = new ConstantNameAndType(1046, 1047);
        constants[759] = new ConstantUtf8("");
        constants[760] = new ConstantNameAndType(530, 533);
        constants[761] = new ConstantNameAndType(536, 515);
        constants[762] = new ConstantNameAndType(441, 438);
        constants[763] = new ConstantNameAndType(1048, 1030);
        constants[764] = new ConstantUtf8("");
        constants[765] = new ConstantNameAndType(1049, 1030);
        constants[766] = new ConstantNameAndType(1050, 1002);
        constants[767] = new ConstantNameAndType(1051, 499);
        constants[768] = new ConstantNameAndType(442, 443);
        constants[769] = new ConstantNameAndType(444, 445);
        constants[770] = new ConstantNameAndType(446, 447);
        constants[771] = new ConstantNameAndType(448, 449);
        constants[772] = new ConstantNameAndType(450, 451);
        constants[773] = new ConstantNameAndType(462, 463);
        constants[774] = new ConstantNameAndType(452, 453);
        constants[775] = new ConstantNameAndType(454, 455);
        constants[776] = new ConstantNameAndType(466, 467);
        constants[777] = new ConstantNameAndType(456, 457);
        constants[778] = new ConstantNameAndType(458, 459);
        constants[779] = new ConstantNameAndType(460, 461);
        constants[780] = new ConstantNameAndType(464, 465);
        constants[781] = new ConstantNameAndType(468, 469);
        constants[782] = new ConstantNameAndType(470, 471);
        constants[783] = new ConstantNameAndType(513, 499);
        constants[784] = new ConstantNameAndType(489, 1052);
        constants[785] = new ConstantNameAndType(567, 479);
        constants[786] = new ConstantNameAndType(1053, 1054);
        constants[787] = new ConstantNameAndType(1055, 479);
        constants[788] = new ConstantUtf8("");
        constants[789] = new ConstantUtf8("");
        constants[790] = new ConstantNameAndType(1056, 1043);
        constants[791] = new ConstantUtf8("");
        constants[792] = new ConstantNameAndType(1057, 1058);
        constants[793] = new ConstantNameAndType(1059, 1023);
        constants[794] = new ConstantUtf8("");
        constants[795] = new ConstantUtf8("");
        constants[796] = new ConstantNameAndType(1060, 1061);
        constants[797] = new ConstantNameAndType(1062, 1030);
        constants[798] = new ConstantNameAndType(1063, 1064);
        constants[799] = new ConstantNameAndType(1065, 1066);
        constants[800] = new ConstantNameAndType(502, 1067);
        constants[801] = new ConstantUtf8("");
        constants[802] = new ConstantUtf8("");
        constants[803] = new ConstantNameAndType(1068, 1069);
        constants[804] = new ConstantNameAndType(1070, 1071);
        constants[805] = new ConstantNameAndType(1072, 1030);
        constants[806] = new ConstantNameAndType(1072, 1073);
        constants[807] = new ConstantNameAndType(1074, 1075);
        constants[808] = new ConstantNameAndType(1076, 1077);
        constants[809] = new ConstantNameAndType(1078, 1079);
        constants[810] = new ConstantUtf8("");
        constants[811] = new ConstantUtf8("");
        constants[812] = new ConstantClass(1080);
        constants[813] = new ConstantNameAndType(1081, 1082);
        constants[814] = new ConstantNameAndType(1083, 1030);
        constants[815] = new ConstantNameAndType(1084, 1075);
        constants[816] = new ConstantNameAndType(1085, 1086);
        constants[817] = new ConstantNameAndType(575, 576);
        constants[818] = new ConstantUtf8("");
        constants[819] = new ConstantNameAndType(545, 515);
        constants[820] = new ConstantNameAndType(1087, 479);
        constants[821] = new ConstantUtf8("");
        constants[822] = new ConstantNameAndType(1088, 983);
        constants[823] = new ConstantNameAndType(1089, 1090);
        constants[824] = new ConstantNameAndType(548, 479);
        constants[825] = new ConstantUtf8("");
        constants[826] = new ConstantUtf8("");
        constants[827] = new ConstantUtf8("");
        constants[828] = new ConstantUtf8("");
        constants[829] = new ConstantUtf8("");
        constants[830] = new ConstantClass(1091);
        constants[831] = new ConstantNameAndType(1092, 1093);
        constants[832] = new ConstantUtf8("");
        constants[833] = new ConstantNameAndType(1094, 973);
        constants[834] = new ConstantUtf8("");
        constants[835] = new ConstantUtf8("");
        constants[836] = new ConstantNameAndType(559, 479);
        constants[837] = new ConstantUtf8("");
        constants[838] = new ConstantUtf8("");
        constants[839] = new ConstantClass(1095);
        constants[840] = new ConstantNameAndType(1096, 1097);
        constants[841] = new ConstantUtf8("");
        constants[842] = new ConstantUtf8("");
        constants[843] = new ConstantUtf8("");
        constants[844] = new ConstantUtf8("");
        constants[845] = new ConstantNameAndType(1098, 1099);
        constants[846] = new ConstantClass(1100);
        constants[847] = new ConstantNameAndType(1101, 1102);
        constants[848] = new ConstantUtf8("");
        constants[849] = new ConstantUtf8("");
        constants[850] = new ConstantClass(957);
        constants[851] = new ConstantNameAndType(1096, 1066);
        constants[852] = new ConstantUtf8("");
        constants[853] = new ConstantUtf8("");
        constants[854] = new ConstantNameAndType(489, 1103);
        constants[855] = new ConstantUtf8("");
        constants[856] = new ConstantUtf8("");
        constants[857] = new ConstantNameAndType(1104, 1105);
        constants[858] = new ConstantUtf8("");
        constants[859] = new ConstantUtf8("");
        constants[860] = new ConstantUtf8("");
        constants[861] = new ConstantUtf8("");
        constants[862] = new ConstantUtf8("");
        constants[863] = new ConstantUtf8("");
        constants[864] = new ConstantUtf8("");
        constants[865] = new ConstantUtf8("");
        constants[866] = new ConstantUtf8("");
        constants[867] = new ConstantUtf8("");
        constants[868] = new ConstantUtf8("");
        constants[869] = new ConstantUtf8("");
        constants[870] = new ConstantUtf8("");
        constants[871] = new ConstantUtf8("");
        constants[872] = new ConstantUtf8("");
        constants[873] = new ConstantNameAndType(1106, 1107);
        constants[874] = new ConstantNameAndType(1108, 1109);
        constants[875] = new ConstantNameAndType(1110, 509);
        constants[876] = new ConstantUtf8("");
        constants[877] = new ConstantUtf8("");
        constants[878] = new ConstantNameAndType(489, 1111);
        constants[879] = new ConstantUtf8("");
        constants[880] = new ConstantNameAndType(489, 1112);
        constants[881] = new ConstantNameAndType(1113, 1114);
        constants[882] = new ConstantNameAndType(546, 515);
        constants[883] = new ConstantNameAndType(1115, 479);
        constants[884] = new ConstantNameAndType(1116, 533);
        constants[885] = new ConstantNameAndType(1117, 1118);
        constants[886] = new ConstantUtf8("");
        constants[887] = new ConstantNameAndType(1119, 1120);
        constants[888] = new ConstantUtf8("");
        constants[889] = new ConstantClass(1121);
        constants[890] = new ConstantNameAndType(1122, 1123);
        constants[891] = new ConstantNameAndType(1124, 499);
        constants[892] = new ConstantNameAndType(1068, 1125);
        constants[893] = new ConstantNameAndType(1126, 1043);
        constants[894] = new ConstantUtf8("");
        constants[895] = new ConstantUtf8("");
        constants[896] = new ConstantUtf8("");
        constants[897] = new ConstantUtf8("");
        constants[898] = new ConstantUtf8("");
        constants[899] = new ConstantUtf8("");
        constants[900] = new ConstantNameAndType(995, 1127);
        constants[901] = new ConstantUtf8("");
        constants[902] = new ConstantNameAndType(1085, 1058);
        constants[903] = new ConstantNameAndType(489, 1128);
        constants[904] = new ConstantNameAndType(1129, 1130);
        constants[905] = new ConstantUtf8("");
        constants[906] = new ConstantUtf8("");
        constants[907] = new ConstantUtf8("");
        constants[908] = new ConstantUtf8("");
        constants[909] = new ConstantUtf8("");
        constants[910] = new ConstantNameAndType(489, 983);
        constants[911] = new ConstantNameAndType(1131, 1132);
        constants[912] = new ConstantNameAndType(1133, 1067);
        constants[913] = new ConstantUtf8("");
        constants[914] = new ConstantUtf8("");
        constants[915] = new ConstantUtf8("");
        constants[916] = new ConstantNameAndType(1134, 1067);
        constants[917] = new ConstantNameAndType(1135, 983);
        constants[918] = new ConstantNameAndType(1136, 533);
        constants[919] = new ConstantNameAndType(1137, 495);
        constants[920] = new ConstantNameAndType(1138, 495);
        constants[921] = new ConstantNameAndType(1139, 495);
        constants[922] = new ConstantNameAndType(1140, 1002);
        constants[923] = new ConstantNameAndType(1141, 499);
        constants[924] = new ConstantNameAndType(1142, 1143);
        constants[925] = new ConstantUtf8("");
        constants[926] = new ConstantNameAndType(1144, 1145);
        constants[927] = new ConstantNameAndType(599, 600);
        constants[928] = new ConstantNameAndType(1146, 499);
        constants[929] = new ConstantNameAndType(1147, 1148);
        constants[930] = new ConstantNameAndType(1149, 1150);
        constants[931] = new ConstantUtf8("");
        constants[932] = new ConstantNameAndType(1151, 1152);
        constants[933] = new ConstantNameAndType(1153, 1154);
        constants[934] = new ConstantNameAndType(1155, 479);
        constants[935] = new ConstantNameAndType(1156, 533);
        constants[936] = new ConstantUtf8("");
        constants[937] = new ConstantNameAndType(1085, 1157);
        constants[938] = new ConstantNameAndType(1158, 1159);
        constants[939] = new ConstantNameAndType(1160, 1161);
        constants[940] = new ConstantNameAndType(1162, 1067);
        constants[941] = new ConstantNameAndType(1163, 983);
        constants[942] = new ConstantUtf8("");
        constants[943] = new ConstantNameAndType(473, 407);
        constants[944] = new ConstantNameAndType(472, 407);
        constants[945] = new ConstantNameAndType(1164, 1075);
        constants[946] = new ConstantNameAndType(980, 1075);
        constants[947] = new ConstantNameAndType(1165, 1030);
        constants[948] = new ConstantUtf8("");
        constants[949] = new ConstantUtf8("");
        constants[950] = new ConstantUtf8("");
        constants[951] = new ConstantUtf8("");
        constants[952] = new ConstantUtf8("");
        constants[953] = new ConstantUtf8("");
        constants[954] = new ConstantUtf8("");
        constants[955] = new ConstantUtf8("");
        constants[956] = new ConstantUtf8("");
        constants[957] = new ConstantUtf8("");
        constants[958] = new ConstantUtf8("");
        constants[959] = new ConstantUtf8("");
        constants[960] = new ConstantUtf8("");
        constants[961] = new ConstantUtf8("");
        constants[962] = new ConstantUtf8("");
        constants[963] = new ConstantUtf8("");
        constants[964] = new ConstantUtf8("");
        constants[965] = new ConstantUtf8("");
        constants[966] = new ConstantUtf8("");
        constants[967] = new ConstantUtf8("");
        constants[968] = new ConstantUtf8("");
        constants[969] = new ConstantUtf8("");
        constants[970] = new ConstantUtf8("");
        constants[971] = new ConstantUtf8("");
        constants[972] = new ConstantUtf8("");
        constants[973] = new ConstantUtf8("");
        constants[974] = new ConstantUtf8("");
        constants[975] = new ConstantUtf8("");
        constants[976] = new ConstantUtf8("");
        constants[977] = new ConstantUtf8("");
        constants[978] = new ConstantUtf8("");
        constants[979] = new ConstantUtf8("");
        constants[980] = new ConstantUtf8("");
        constants[981] = new ConstantUtf8("");
        constants[982] = new ConstantUtf8("");
        constants[983] = new ConstantUtf8("");
        constants[984] = new ConstantUtf8("");
        constants[985] = new ConstantUtf8("");
        constants[986] = new ConstantUtf8("");
        constants[987] = new ConstantUtf8("");
        constants[988] = new ConstantUtf8("");
        constants[989] = new ConstantUtf8("");
        constants[990] = new ConstantUtf8("");
        constants[991] = new ConstantUtf8("");
        constants[992] = new ConstantUtf8("");
        constants[993] = new ConstantUtf8("");
        constants[994] = new ConstantUtf8("");
        constants[995] = new ConstantUtf8("");
        constants[996] = new ConstantUtf8("");
        constants[997] = new ConstantUtf8("");
        constants[998] = new ConstantUtf8("");
        constants[999] = new ConstantUtf8("");
        constants[1000] = new ConstantUtf8("");
        constants[1001] = new ConstantUtf8("");
        constants[1002] = new ConstantUtf8("");
        constants[1003] = new ConstantUtf8("");
        constants[1004] = new ConstantUtf8("");
        constants[1005] = new ConstantUtf8("");
        constants[1006] = new ConstantUtf8("");
        constants[1007] = new ConstantUtf8("");
        constants[1008] = new ConstantUtf8("");
        constants[1009] = new ConstantUtf8("");
        constants[1010] = new ConstantUtf8("");
        constants[1011] = new ConstantUtf8("");
        constants[1012] = new ConstantUtf8("");
        constants[1013] = new ConstantUtf8("");
        constants[1014] = new ConstantUtf8("");
        constants[1015] = new ConstantUtf8("");
        constants[1016] = new ConstantUtf8("");
        constants[1017] = new ConstantUtf8("");
        constants[1018] = new ConstantUtf8("");
        constants[1019] = new ConstantUtf8("");
        constants[1020] = new ConstantUtf8("");
        constants[1021] = new ConstantUtf8("");
        constants[1022] = new ConstantUtf8("");
        constants[1023] = new ConstantUtf8("");
        constants[1024] = new ConstantUtf8("");
        constants[1025] = new ConstantUtf8("");
        constants[1026] = new ConstantUtf8("");
        constants[1027] = new ConstantUtf8("");
        constants[1028] = new ConstantUtf8("");
        constants[1029] = new ConstantUtf8("");
        constants[1030] = new ConstantUtf8("");
        constants[1031] = new ConstantUtf8("");
        constants[1032] = new ConstantUtf8("");
        constants[1033] = new ConstantUtf8("");
        constants[1034] = new ConstantUtf8("");
        constants[1035] = new ConstantUtf8("");
        constants[1036] = new ConstantUtf8("");
        constants[1037] = new ConstantUtf8("");
        constants[1038] = new ConstantUtf8("");
        constants[1039] = new ConstantUtf8("");
        constants[1040] = new ConstantUtf8("");
        constants[1041] = new ConstantUtf8("");
        constants[1042] = new ConstantUtf8("");
        constants[1043] = new ConstantUtf8("");
        constants[1044] = new ConstantUtf8("");
        constants[1045] = new ConstantUtf8("");
        constants[1046] = new ConstantUtf8("");
        constants[1047] = new ConstantUtf8("");
        constants[1048] = new ConstantUtf8("");
        constants[1049] = new ConstantUtf8("");
        constants[1050] = new ConstantUtf8("");
        constants[1051] = new ConstantUtf8("");
        constants[1052] = new ConstantUtf8("");
        constants[1053] = new ConstantUtf8("");
        constants[1054] = new ConstantUtf8("");
        constants[1055] = new ConstantUtf8("");
        constants[1056] = new ConstantUtf8("");
        constants[1057] = new ConstantUtf8("");
        constants[1058] = new ConstantUtf8("");
        constants[1059] = new ConstantUtf8("");
        constants[1060] = new ConstantUtf8("");
        constants[1061] = new ConstantUtf8("");
        constants[1062] = new ConstantUtf8("");
        constants[1063] = new ConstantUtf8("");
        constants[1064] = new ConstantUtf8("");
        constants[1065] = new ConstantUtf8("");
        constants[1066] = new ConstantUtf8("");
        constants[1067] = new ConstantUtf8("");
        constants[1068] = new ConstantUtf8("");
        constants[1069] = new ConstantUtf8("");
        constants[1070] = new ConstantUtf8("");
        constants[1071] = new ConstantUtf8("");
        constants[1072] = new ConstantUtf8("");
        constants[1073] = new ConstantUtf8("");
        constants[1074] = new ConstantUtf8("");
        constants[1075] = new ConstantUtf8("");
        constants[1076] = new ConstantUtf8("");
        constants[1077] = new ConstantUtf8("");
        constants[1078] = new ConstantUtf8("");
        constants[1079] = new ConstantUtf8("");
        constants[1080] = new ConstantUtf8("");
        constants[1081] = new ConstantUtf8("");
        constants[1082] = new ConstantUtf8("");
        constants[1083] = new ConstantUtf8("");
        constants[1084] = new ConstantUtf8("");
        constants[1085] = new ConstantUtf8("");
        constants[1086] = new ConstantUtf8("");
        constants[1087] = new ConstantUtf8("");
        constants[1088] = new ConstantUtf8("");
        constants[1089] = new ConstantUtf8("");
        constants[1090] = new ConstantUtf8("");
        constants[1091] = new ConstantUtf8("");
        constants[1092] = new ConstantUtf8("");
        constants[1093] = new ConstantUtf8("");
        constants[1094] = new ConstantUtf8("");
        constants[1095] = new ConstantUtf8("");
        constants[1096] = new ConstantUtf8("");
        constants[1097] = new ConstantUtf8("");
        constants[1098] = new ConstantUtf8("");
        constants[1099] = new ConstantUtf8("");
        constants[1100] = new ConstantUtf8("");
        constants[1101] = new ConstantUtf8("");
        constants[1102] = new ConstantUtf8("");
        constants[1103] = new ConstantUtf8("");
        constants[1104] = new ConstantUtf8("");
        constants[1105] = new ConstantUtf8("");
        constants[1106] = new ConstantUtf8("");
        constants[1107] = new ConstantUtf8("");
        constants[1108] = new ConstantUtf8("");
        constants[1109] = new ConstantUtf8("");
        constants[1110] = new ConstantUtf8("");
        constants[1111] = new ConstantUtf8("");
        constants[1112] = new ConstantUtf8("");
        constants[1113] = new ConstantUtf8("");
        constants[1114] = new ConstantUtf8("");
        constants[1115] = new ConstantUtf8("");
        constants[1116] = new ConstantUtf8("");
        constants[1117] = new ConstantUtf8("");
        constants[1118] = new ConstantUtf8("");
        constants[1119] = new ConstantUtf8("");
        constants[1120] = new ConstantUtf8("");
        constants[1121] = new ConstantUtf8("");
        constants[1122] = new ConstantUtf8("");
        constants[1123] = new ConstantUtf8("");
        constants[1124] = new ConstantUtf8("");
        constants[1125] = new ConstantUtf8("");
        constants[1126] = new ConstantUtf8("");
        constants[1127] = new ConstantUtf8("");
        constants[1128] = new ConstantUtf8("");
        constants[1129] = new ConstantUtf8("");
        constants[1130] = new ConstantUtf8("");
        constants[1131] = new ConstantUtf8("");
        constants[1132] = new ConstantUtf8("");
        constants[1133] = new ConstantUtf8("");
        constants[1134] = new ConstantUtf8("");
        constants[1135] = new ConstantUtf8("");
        constants[1136] = new ConstantUtf8("");
        constants[1137] = new ConstantUtf8("");
        constants[1138] = new ConstantUtf8("");
        constants[1139] = new ConstantUtf8("");
        constants[1140] = new ConstantUtf8("");
        constants[1141] = new ConstantUtf8("");
        constants[1142] = new ConstantUtf8("");
        constants[1143] = new ConstantUtf8("");
        constants[1144] = new ConstantUtf8("");
        constants[1145] = new ConstantUtf8("");
        constants[1146] = new ConstantUtf8("");
        constants[1147] = new ConstantUtf8("");
        constants[1148] = new ConstantUtf8("");
        constants[1149] = new ConstantUtf8("");
        constants[1150] = new ConstantUtf8("");
        constants[1151] = new ConstantUtf8("");
        constants[1152] = new ConstantUtf8("");
        constants[1153] = new ConstantUtf8("");
        constants[1154] = new ConstantUtf8("");
        constants[1155] = new ConstantUtf8("");
        constants[1156] = new ConstantUtf8("");
        constants[1157] = new ConstantUtf8("");
        constants[1158] = new ConstantUtf8("");
        constants[1159] = new ConstantUtf8("");
        constants[1160] = new ConstantUtf8("");
        constants[1161] = new ConstantUtf8("");
        constants[1162] = new ConstantUtf8("");
        constants[1163] = new ConstantUtf8("");
        constants[1164] = new ConstantUtf8("");
        constants[1165] = new ConstantUtf8("");
        constants[1166] = new ConstantUtf8("");
        constants[1167] = new ConstantClass(1166);
        constants[1168] = new ConstantMethodref(1167, 814);
        constants[1169] = new ConstantUtf8("");
        constants[1170] = new ConstantClass(1169);
        constants[1171] = new ConstantUtf8("");
        constants[1172] = new ConstantUtf8("");
        constants[1173] = new ConstantNameAndType(1171, 1172);
        constants[1174] = new ConstantMethodref(1170, 1173);
        constants[1175] = new ConstantUtf8("");
        constants[1176] = new ConstantClass(1175);
        ConstantPool cp = new ConstantPool(constants);
        for (int i = 0; i < constants.length; i++) {
            if (constants[i] instanceof ConstantCP) {
                ConstantCP constantCP = (ConstantCP) constants[i];
                ConstantNameAndType cnat = cp.getConstant(constantCP.getNameAndTypeIndex());
                constants[cnat.getSignatureIndex()] = new ConstantUtf8("()V");
            }
        }
        List<CodeException> codeExceptions = new ArrayList<>();
        codeExceptions.add(new CodeException(43, 52, 55, 343));
        codeExceptions.add(new CodeException(211, 237, 821, 109));
        codeExceptions.add(new CodeException(211, 237, 848, 0));
        codeExceptions.add(new CodeException(250, 689, 821, 109));
        codeExceptions.add(new CodeException(250, 689, 848, 0));
        codeExceptions.add(new CodeException(702, 806, 821, 109));
        codeExceptions.add(new CodeException(702, 806, 848, 0));
        codeExceptions.add(new CodeException(821, 835, 848, 0));
        codeExceptions.add(new CodeException(848, 850, 848, 0));
        method.setConstantPool(cp);
        byte[] bytecode = new byte[] { -72, 0, -63, 19, 1, 84, -72, 0, -60, 62, 19, 1, 85, -72, 0, -60, 54, 4, 21, 4, -102, 0, 15, 29, -102, 0, 11, 19, 1, 86, 42, -72, 0, -58, -79, -72, 0, -122, -74, 0, -119, 58, 5, 25, 5, -71, 1, 75, 1, 0, 58, 6, -89, 0, 22, 58, 7, 42, 19, 1, 88, 25, 7, -72, 1, 89, 19, 1, 90, 42, -72, 1, 91, -79, 16, 11, -72, 0, -57, 58, 7, -69, 0, 105, 89, 25, 6, -73, 1, 92, 58, 8, 25, 8, 25, 7, -74, 1, 93, 87, 44, 58, 9, 25, 9, -72, 0, -113, -102, 0, 103, 25, 6, 25, 9, -74, 0, -42, -103, 0, 81, 29, -102, 0, 35, -69, 0, -56, 89, -73, 0, -55, 19, 1, 94, -74, 0, -53, 25, 9, -74, 0, -53, 19, 1, 95, -74, 0, -53, -74, 0, -52, 42, -72, 0, -58, -79, 25, 8, 25, 9, -74, 0, -42, -102, 0, 47, -69, 0, -56, 89, -73, 0, -55, 19, 1, 96, -74, 0, -53, 25, 9, -74, 0, -53, 19, 1, 95, -74, 0, -53, -74, 0, -52, 42, -72, 0, -58, -79, 21, 4, -102, 0, 10, 19, 1, 97, 42, -72, 0, -58, 25, 9, -72, 0, -113, -103, 0, -19, 43, 18, -115, 25, 8, 1, 4, 42, -72, 0, -114, 58, 10, 25, 10, -57, 0, 16, 42, -69, 1, 98, 89, 3, -73, 1, 99, -74, 1, 100, -79, 25, 10, -74, 1, 101, -102, 0, 13, 19, 1, 102, 42, -72, 0, -58, -89, -1, -54, 25, 6, 25, 10, -74, 0, -42, -103, 0, -108, 29, -102, 0, 37, -69, 0, -56, 89, -73, 0, -55, 19, 1, 94, -74, 0, -53, 25, 10, -74, 0, -53, 19, 1, 95, -74, 0, -53, -74, 0, -52, 42, -72, 0, -58, -89, -1, -102, 25, 8, 25, 10, -74, 0, -42, -102, 0, 37, -69, 0, -56, 89, -73, 0, -55, 19, 1, 96, -74, 0, -53, 25, 10, -74, 0, -53, 19, 1, 95, -74, 0, -53, -74, 0, -52, 42, -72, 0, -58, -89, -1, 110, -69, 0, -56, 89, -73, 0, -55, 19, 1, 103, -74, 0, -53, 25, 10, -74, 0, -53, 19, 1, 104, -74, 0, -53, -74, 0, -52, 42, -72, 0, -51, -102, 0, 6, -89, -1, 73, 25, 5, 25, 10, -71, 0, -110, 2, 0, 58, 11, 42, -76, 0, -104, 25, 11, -74, 1, 105, -74, 1, 106, -89, 0, 26, 21, 4, -102, 0, 13, 19, 1, 97, 42, -72, 0, -58, -89, -1, 32, 42, -76, 0, -104, 3, -74, 1, 106, 25, 10, 58, 9, -89, -1, 17, 42, -76, 0, 9, 25, 9, -74, 0, -112, 42, -69, 1, 98, 89, 6, -73, 1, 99, -74, 1, 100, 42, -76, 0, -85, 42, -76, 0, -104, -74, 1, 107, 42, -76, 0, -104, 25, 9, -74, 1, 108, 42, -76, 0, -104, 42, -76, 0, 74, 42, -76, 0, 74, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, -74, 4, -112, -72, 4, -106, -74, 1, 110, 42, -76, 0, -104, -72, 0, -122, -74, 0, -44, -74, 1, 111, 42, -76, 0, -104, 42, -76, 0, 11, -74, 1, 112, -74, 1, 113, 42, -76, 0, 53, 42, -76, 0, 53, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, -74, 4, -112, -72, 4, -106, 58, 10, 1, 58, 11, 25, 10, -72, 0, -113, -102, 0, 76, 25, 10, 19, 1, 76, -72, 1, 77, 58, 12, 25, 12, -74, 0, -49, -67, 0, -107, 58, 11, 3, 54, 13, 21, 13, 25, 12, -74, 0, -49, -94, 0, 31, 25, 11, 21, 13, 25, 5, 25, 12, 21, 13, -74, 1, 114, -64, 1, 115, -71, 0, -110, 2, 0, 83, -124, 13, 1, -89, -1, -35, 42, -76, 0, -104, 25, 11, -74, 1, 116, -89, 0, 11, 42, -76, 0, -104, 1, -74, 1, 116, 42, 42, -76, 0, -104, 25, 11, -73, 1, 117, -103, 0, 16, 42, -69, 1, 98, 89, 3, -73, 1, 99, -74, 1, 100, -79, 16, 11, -72, 0, -123, 58, 12, 42, -76, 0, -104, 3, -74, 1, 118, 25, 5, 42, -76, 0, -104, -71, 1, 119, 2, 0, 54, 13, 42, -76, 0, -104, 21, 13, -74, 1, 106, 42, -76, 0, 102, -58, 0, 11, 42, 42, -76, 0, -104, -75, 0, 102, 16, 11, 18, -37, 42, -76, 0, -104, -74, 0, -116, 25, 12, -72, 1, 120, 42, -76, 0, 107, -74, 0, -49, -98, 0, 29, 42, -76, 0, 107, 19, 1, 121, 3, -74, 1, 122, 42, -76, 0, 107, 42, -72, 1, 123, 42, -76, 0, 107, -74, 1, 124, 42, -69, 1, 98, 89, 3, -73, 1, 99, -74, 1, 100, -89, 0, 47, 58, 10, 42, 25, 10, -72, 0, 110, 25, 10, 42, -72, 0, -33, 42, -69, 1, 98, 89, 3, -73, 1, 99, -74, 1, 100, -79, 58, 14, 42, -69, 1, 98, 89, 3, -73, 1, 99, -74, 1, 100, 25, 14, -65, 42, 3, -74, 0, -67, -79 };
        Code code = new Code(0, 0, 0, 0, bytecode, null, null, cp);
        code.setExceptionTable(codeExceptions.toArray(CodeException[]::new));
        method.setAttributes(new Attribute[] { code });
        checkCFGReduction(method);
    }
    
    @Test
    public void testMethodWithLoopExitCondition() throws Exception {
        Method method = new Method();
        Constant[] constants = new Constant[519];
        constants[1] = new ConstantMethodref(155, 231);
        constants[2] = new ConstantMethodref(154, 232);
        constants[3] = new ConstantString(233);
        constants[4] = new ConstantMethodref(154, 234);
        constants[5] = new ConstantMethodref(155, 235);
        constants[6] = new ConstantMethodref(155, 236);
        constants[7] = new ConstantClass(237);
        constants[8] = new ConstantMethodref(7, 238);
        constants[9] = new ConstantFieldref(154, 239);
        constants[10] = new ConstantString(240);
        constants[11] = new ConstantMethodref(7, 241);
        constants[12] = new ConstantString(242);
        constants[13] = new ConstantString(243);
        constants[14] = new ConstantString(244);
        constants[15] = new ConstantString(245);
        constants[16] = new ConstantFieldref(154, 246);
        constants[17] = new ConstantMethodref(7, 247);
        constants[18] = new ConstantMethodref(7, 248);
        constants[19] = new ConstantMethodref(249, 250);
        constants[20] = new ConstantClass(251);
        constants[21] = new ConstantMethodref(20, 231);
        constants[22] = new ConstantClass(252);
        constants[23] = new ConstantString(253);
        constants[24] = new ConstantMethodref(20, 254);
        constants[25] = new ConstantString(255);
        constants[26] = new ConstantString(256);
        constants[27] = new ConstantString(257);
        constants[28] = new ConstantString(258);
        constants[29] = new ConstantString(259);
        constants[30] = new ConstantString(260);
        constants[31] = new ConstantString(261);
        constants[32] = new ConstantString(262);
        constants[33] = new ConstantString(263);
        constants[34] = new ConstantString(264);
        constants[35] = new ConstantString(265);
        constants[36] = new ConstantString(266);
        constants[37] = new ConstantString(267);
        constants[38] = new ConstantString(268);
        constants[39] = new ConstantString(269);
        constants[40] = new ConstantString(270);
        constants[41] = new ConstantString(271);
        constants[42] = new ConstantMethodref(58, 272);
        constants[43] = new ConstantMethodref(20, 273);
        constants[44] = new ConstantClass(274);
        constants[45] = new ConstantMethodref(44, 231);
        constants[46] = new ConstantString(275);
        constants[47] = new ConstantMethodref(44, 276);
        constants[48] = new ConstantMethodref(20, 277);
        constants[49] = new ConstantMethodref(44, 278);
        constants[50] = new ConstantMethodref(44, 279);
        constants[51] = new ConstantFieldref(154, 280);
        constants[52] = new ConstantMethodref(281, 282);
        constants[53] = new ConstantClass(283);
        constants[54] = new ConstantMethodref(284, 285);
        constants[55] = new ConstantMethodref(154, 286);
        constants[56] = new ConstantMethodref(154, 287);
        constants[57] = new ConstantMethodref(7, 288);
        constants[58] = new ConstantClass(289);
        constants[59] = new ConstantMethodref(58, 290);
        constants[60] = new ConstantMethodref(291, 292);
        constants[61] = new ConstantMethodref(7, 293);
        constants[62] = new ConstantFieldref(154, 294);
        constants[63] = new ConstantMethodref(58, 295);
        constants[64] = new ConstantMethodref(296, 297);
        constants[65] = new ConstantMethodref(58, 298);
        constants[66] = new ConstantMethodref(58, 299);
        constants[67] = new ConstantMethodref(300, 301);
        constants[68] = new ConstantString(302);
        constants[69] = new ConstantMethodref(58, 303);
        constants[70] = new ConstantFieldref(154, 304);
        constants[71] = new ConstantMethodref(44, 305);
        constants[72] = new ConstantString(306);
        constants[73] = new ConstantMethodref(307, 308);
        constants[74] = new ConstantMethodref(155, 309);
        constants[75] = new ConstantClass(310);
        constants[76] = new ConstantMethodref(75, 231);
        constants[77] = new ConstantClass(311);
        constants[78] = new ConstantMethodref(77, 231);
        constants[79] = new ConstantFieldref(154, 312);
        constants[80] = new ConstantFieldref(154, 313);
        constants[81] = new ConstantMethodref(314, 315);
        constants[82] = new ConstantMethodref(314, 316);
        constants[83] = new ConstantClass(317);
        constants[84] = new ConstantMethodref(22, 318);
        constants[85] = new ConstantMethodref(319, 320);
        constants[86] = new ConstantMethodref(83, 321);
        constants[87] = new ConstantMethodref(83, 322);
        constants[88] = new ConstantMethodref(83, 323);
        constants[89] = new ConstantMethodref(296, 324);
        constants[90] = new ConstantMethodref(300, 290);
        constants[91] = new ConstantMethodref(291, 325);
        constants[92] = new ConstantString(326);
        constants[93] = new ConstantString(327);
        constants[94] = new ConstantString(328);
        constants[95] = new ConstantString(329);
        constants[96] = new ConstantString(330);
        constants[97] = new ConstantString(331);
        constants[98] = new ConstantString(332);
        constants[99] = new ConstantString(333);
        constants[100] = new ConstantString(334);
        constants[101] = new ConstantString(335);
        constants[102] = new ConstantString(336);
        constants[103] = new ConstantString(337);
        constants[104] = new ConstantString(338);
        constants[105] = new ConstantString(339);
        constants[106] = new ConstantString(340);
        constants[107] = new ConstantString(341);
        constants[108] = new ConstantString(342);
        constants[109] = new ConstantString(343);
        constants[110] = new ConstantString(344);
        constants[111] = new ConstantString(345);
        constants[112] = new ConstantMethodref(83, 346);
        constants[113] = new ConstantString(347);
        constants[114] = new ConstantMethodref(83, 348);
        constants[115] = new ConstantMethodref(83, 349);
        constants[116] = new ConstantString(350);
        constants[117] = new ConstantClass(351);
        constants[118] = new ConstantFieldref(117, 352);
        constants[119] = new ConstantMethodref(117, 353);
        constants[120] = new ConstantInterfaceMethodref(354, 355);
        constants[121] = new ConstantString(356);
        constants[122] = new ConstantString(357);
        constants[123] = new ConstantMethodref(319, 358);
        constants[124] = new ConstantInterfaceMethodref(354, 359);
        constants[125] = new ConstantMethodref(154, 360);
        constants[126] = new ConstantString(361);
        constants[127] = new ConstantString(362);
        constants[128] = new ConstantString(363);
        constants[129] = new ConstantMethodref(364, 365);
        constants[130] = new ConstantInterfaceMethodref(366, 367);
        constants[131] = new ConstantInterfaceMethodref(354, 368);
        constants[132] = new ConstantInterfaceMethodref(369, 370);
        constants[133] = new ConstantInterfaceMethodref(369, 371);
        constants[134] = new ConstantMethodref(364, 372);
        constants[135] = new ConstantMethodref(373, 374);
        constants[136] = new ConstantMethodref(77, 375);
        constants[137] = new ConstantMethodref(20, 355);
        constants[138] = new ConstantMethodref(77, 376);
        constants[139] = new ConstantFieldref(154, 377);
        constants[140] = new ConstantMethodref(154, 236);
        constants[141] = new ConstantMethodref(378, 379);
        constants[142] = new ConstantFieldref(154, 380);
        constants[143] = new ConstantMethodref(378, 381);
        constants[144] = new ConstantMethodref(154, 382);
        constants[145] = new ConstantMethodref(314, 383);
        constants[146] = new ConstantMethodref(314, 293);
        constants[147] = new ConstantMethodref(378, 384);
        constants[148] = new ConstantMethodref(314, 385);
        constants[149] = new ConstantMethodref(22, 234);
        constants[150] = new ConstantMethodref(22, 386);
        constants[151] = new ConstantMethodref(22, 387);
        constants[152] = new ConstantFieldref(154, 388);
        constants[153] = new ConstantMethodref(154, 309);
        constants[154] = new ConstantClass(389);
        constants[155] = new ConstantClass(390);
        constants[156] = new ConstantUtf8("");
        constants[157] = new ConstantUtf8("");
        constants[158] = new ConstantUtf8("");
        constants[159] = new ConstantUtf8("");
        constants[160] = new ConstantUtf8("");
        constants[161] = new ConstantUtf8("");
        constants[162] = new ConstantUtf8("");
        constants[163] = new ConstantUtf8("");
        constants[164] = new ConstantUtf8("");
        constants[165] = new ConstantUtf8("");
        constants[166] = new ConstantUtf8("");
        constants[167] = new ConstantUtf8("");
        constants[168] = new ConstantUtf8("");
        constants[169] = new ConstantUtf8("");
        constants[170] = new ConstantUtf8("");
        constants[171] = new ConstantUtf8("");
        constants[172] = new ConstantUtf8("");
        constants[173] = new ConstantUtf8("");
        constants[174] = new ConstantUtf8("");
        constants[175] = new ConstantUtf8("");
        constants[176] = new ConstantUtf8("");
        constants[177] = new ConstantUtf8("");
        constants[178] = new ConstantUtf8("");
        constants[179] = new ConstantClass(251);
        constants[180] = new ConstantClass(389);
        constants[181] = new ConstantClass(283);
        constants[182] = new ConstantUtf8("");
        constants[183] = new ConstantUtf8("");
        constants[184] = new ConstantUtf8("");
        constants[185] = new ConstantUtf8("");
        constants[186] = new ConstantUtf8("");
        constants[187] = new ConstantUtf8("");
        constants[188] = new ConstantUtf8("");
        constants[189] = new ConstantClass(289);
        constants[190] = new ConstantUtf8("");
        constants[191] = new ConstantUtf8("");
        constants[192] = new ConstantUtf8("");
        constants[193] = new ConstantUtf8("");
        constants[194] = new ConstantUtf8("");
        constants[195] = new ConstantUtf8("");
        constants[196] = new ConstantUtf8("");
        constants[197] = new ConstantUtf8("");
        constants[198] = new ConstantUtf8("");
        constants[199] = new ConstantUtf8("");
        constants[200] = new ConstantUtf8("");
        constants[201] = new ConstantUtf8("");
        constants[202] = new ConstantUtf8("");
        constants[203] = new ConstantUtf8("");
        constants[204] = new ConstantUtf8("");
        constants[205] = new ConstantUtf8("");
        constants[206] = new ConstantUtf8("");
        constants[207] = new ConstantUtf8("");
        constants[208] = new ConstantUtf8("");
        constants[209] = new ConstantUtf8("");
        constants[210] = new ConstantUtf8("");
        constants[211] = new ConstantUtf8("");
        constants[212] = new ConstantUtf8("");
        constants[213] = new ConstantUtf8("");
        constants[214] = new ConstantUtf8("");
        constants[215] = new ConstantUtf8("");
        constants[216] = new ConstantUtf8("");
        constants[217] = new ConstantUtf8("");
        constants[218] = new ConstantClass(317);
        constants[219] = new ConstantClass(391);
        constants[220] = new ConstantClass(392);
        constants[221] = new ConstantClass(393);
        constants[222] = new ConstantUtf8("");
        constants[223] = new ConstantUtf8("");
        constants[224] = new ConstantUtf8("");
        constants[225] = new ConstantUtf8("");
        constants[226] = new ConstantUtf8("");
        constants[227] = new ConstantUtf8("");
        constants[228] = new ConstantUtf8("");
        constants[229] = new ConstantUtf8("");
        constants[230] = new ConstantUtf8("");
        constants[231] = new ConstantNameAndType(156, 166);
        constants[232] = new ConstantNameAndType(394, 157);
        constants[233] = new ConstantUtf8("");
        constants[234] = new ConstantNameAndType(156, 157);
        constants[235] = new ConstantNameAndType(156, 167);
        constants[236] = new ConstantNameAndType(170, 166);
        constants[237] = new ConstantUtf8("");
        constants[238] = new ConstantNameAndType(156, 395);
        constants[239] = new ConstantNameAndType(396, 397);
        constants[240] = new ConstantUtf8("");
        constants[241] = new ConstantNameAndType(398, 399);
        constants[242] = new ConstantUtf8("");
        constants[243] = new ConstantUtf8("");
        constants[244] = new ConstantUtf8("");
        constants[245] = new ConstantUtf8("");
        constants[246] = new ConstantNameAndType(400, 401);
        constants[247] = new ConstantNameAndType(402, 403);
        constants[248] = new ConstantNameAndType(404, 166);
        constants[249] = new ConstantClass(405);
        constants[250] = new ConstantNameAndType(406, 407);
        constants[251] = new ConstantUtf8("");
        constants[252] = new ConstantUtf8("");
        constants[253] = new ConstantUtf8("");
        constants[254] = new ConstantNameAndType(222, 408);
        constants[255] = new ConstantUtf8("");
        constants[256] = new ConstantUtf8("");
        constants[257] = new ConstantUtf8("");
        constants[258] = new ConstantUtf8("");
        constants[259] = new ConstantUtf8("");
        constants[260] = new ConstantUtf8("");
        constants[261] = new ConstantUtf8("");
        constants[262] = new ConstantUtf8("");
        constants[263] = new ConstantUtf8("");
        constants[264] = new ConstantUtf8("");
        constants[265] = new ConstantUtf8("");
        constants[266] = new ConstantUtf8("");
        constants[267] = new ConstantUtf8("");
        constants[268] = new ConstantUtf8("");
        constants[269] = new ConstantUtf8("");
        constants[270] = new ConstantUtf8("");
        constants[271] = new ConstantUtf8("");
        constants[272] = new ConstantNameAndType(409, 191);
        constants[273] = new ConstantNameAndType(410, 411);
        constants[274] = new ConstantUtf8("");
        constants[275] = new ConstantUtf8("");
        constants[276] = new ConstantNameAndType(412, 413);
        constants[277] = new ConstantNameAndType(414, 415);
        constants[278] = new ConstantNameAndType(412, 416);
        constants[279] = new ConstantNameAndType(417, 418);
        constants[280] = new ConstantNameAndType(419, 420);
        constants[281] = new ConstantClass(421);
        constants[282] = new ConstantNameAndType(422, 423);
        constants[283] = new ConstantUtf8("");
        constants[284] = new ConstantClass(424);
        constants[285] = new ConstantNameAndType(425, 426);
        constants[286] = new ConstantNameAndType(427, 428);
        constants[287] = new ConstantNameAndType(190, 191);
        constants[288] = new ConstantNameAndType(429, 430);
        constants[289] = new ConstantUtf8("");
        constants[290] = new ConstantNameAndType(431, 411);
        constants[291] = new ConstantClass(432);
        constants[292] = new ConstantNameAndType(433, 434);
        constants[293] = new ConstantNameAndType(435, 436);
        constants[294] = new ConstantNameAndType(437, 438);
        constants[295] = new ConstantNameAndType(439, 411);
        constants[296] = new ConstantClass(440);
        constants[297] = new ConstantNameAndType(441, 442);
        constants[298] = new ConstantNameAndType(443, 418);
        constants[299] = new ConstantNameAndType(444, 411);
        constants[300] = new ConstantClass(393);
        constants[301] = new ConstantNameAndType(445, 418);
        constants[302] = new ConstantUtf8("");
        constants[303] = new ConstantNameAndType(446, 418);
        constants[304] = new ConstantNameAndType(447, 448);
        constants[305] = new ConstantNameAndType(412, 449);
        constants[306] = new ConstantUtf8("");
        constants[307] = new ConstantClass(450);
        constants[308] = new ConstantNameAndType(451, 157);
        constants[309] = new ConstantNameAndType(182, 183);
        constants[310] = new ConstantUtf8("");
        constants[311] = new ConstantUtf8("");
        constants[312] = new ConstantNameAndType(452, 453);
        constants[313] = new ConstantNameAndType(454, 457);
        constants[314] = new ConstantClass(458);
        constants[315] = new ConstantNameAndType(459, 411);
        constants[316] = new ConstantNameAndType(460, 461);
        constants[317] = new ConstantUtf8("");
        constants[318] = new ConstantNameAndType(462, 463);
        constants[319] = new ConstantClass(464);
        constants[320] = new ConstantNameAndType(465, 466);
        constants[321] = new ConstantNameAndType(467, 418);
        constants[322] = new ConstantNameAndType(468, 418);
        constants[323] = new ConstantNameAndType(469, 470);
        constants[324] = new ConstantNameAndType(441, 471);
        constants[325] = new ConstantNameAndType(417, 472);
        constants[326] = new ConstantUtf8("");
        constants[327] = new ConstantUtf8("");
        constants[328] = new ConstantUtf8("");
        constants[329] = new ConstantUtf8("");
        constants[330] = new ConstantUtf8("");
        constants[331] = new ConstantUtf8("");
        constants[332] = new ConstantUtf8("");
        constants[333] = new ConstantUtf8("");
        constants[334] = new ConstantUtf8("");
        constants[335] = new ConstantUtf8("");
        constants[336] = new ConstantUtf8("");
        constants[337] = new ConstantUtf8("");
        constants[338] = new ConstantUtf8("");
        constants[339] = new ConstantUtf8("");
        constants[340] = new ConstantUtf8("");
        constants[341] = new ConstantUtf8("");
        constants[342] = new ConstantUtf8("");
        constants[343] = new ConstantUtf8("");
        constants[344] = new ConstantUtf8("");
        constants[345] = new ConstantUtf8("");
        constants[346] = new ConstantNameAndType(473, 474);
        constants[347] = new ConstantUtf8("");
        constants[348] = new ConstantNameAndType(475, 463);
        constants[349] = new ConstantNameAndType(476, 472);
        constants[350] = new ConstantUtf8("");
        constants[351] = new ConstantUtf8("");
        constants[352] = new ConstantNameAndType(477, 172);
        constants[353] = new ConstantNameAndType(156, 478);
        constants[354] = new ConstantClass(391);
        constants[355] = new ConstantNameAndType(479, 470);
        constants[356] = new ConstantUtf8("");
        constants[357] = new ConstantUtf8("");
        constants[358] = new ConstantNameAndType(480, 481);
        constants[359] = new ConstantNameAndType(482, 481);
        constants[360] = new ConstantNameAndType(483, 484);
        constants[361] = new ConstantUtf8("");
        constants[362] = new ConstantUtf8("");
        constants[363] = new ConstantUtf8("");
        constants[364] = new ConstantClass(485);
        constants[365] = new ConstantNameAndType(486, 487);
        constants[366] = new ConstantClass(488);
        constants[367] = new ConstantNameAndType(489, 490);
        constants[368] = new ConstantNameAndType(491, 492);
        constants[369] = new ConstantClass(392);
        constants[370] = new ConstantNameAndType(493, 494);
        constants[371] = new ConstantNameAndType(495, 496);
        constants[372] = new ConstantNameAndType(497, 498);
        constants[373] = new ConstantClass(499);
        constants[374] = new ConstantNameAndType(500, 501);
        constants[375] = new ConstantNameAndType(502, 503);
        constants[376] = new ConstantNameAndType(504, 505);
        constants[377] = new ConstantNameAndType(506, 174);
        constants[378] = new ConstantClass(507);
        constants[379] = new ConstantNameAndType(508, 408);
        constants[380] = new ConstantNameAndType(509, 420);
        constants[381] = new ConstantNameAndType(510, 430);
        constants[382] = new ConstantNameAndType(511, 428);
        constants[383] = new ConstantNameAndType(512, 411);
        constants[384] = new ConstantNameAndType(513, 496);
        constants[385] = new ConstantNameAndType(514, 166);
        constants[386] = new ConstantNameAndType(515, 430);
        constants[387] = new ConstantNameAndType(516, 517);
        constants[388] = new ConstantNameAndType(518, 174);
        constants[389] = new ConstantUtf8("");
        constants[390] = new ConstantUtf8("");
        constants[391] = new ConstantUtf8("");
        constants[392] = new ConstantUtf8("");
        constants[393] = new ConstantUtf8("");
        constants[394] = new ConstantUtf8("");
        constants[395] = new ConstantUtf8("");
        constants[396] = new ConstantUtf8("");
        constants[397] = new ConstantUtf8("");
        constants[398] = new ConstantUtf8("");
        constants[399] = new ConstantUtf8("");
        constants[400] = new ConstantUtf8("");
        constants[401] = new ConstantUtf8("");
        constants[402] = new ConstantUtf8("");
        constants[403] = new ConstantUtf8("");
        constants[404] = new ConstantUtf8("");
        constants[405] = new ConstantUtf8("");
        constants[406] = new ConstantUtf8("");
        constants[407] = new ConstantUtf8("");
        constants[408] = new ConstantUtf8("");
        constants[409] = new ConstantUtf8("");
        constants[410] = new ConstantUtf8("");
        constants[411] = new ConstantUtf8("");
        constants[412] = new ConstantUtf8("");
        constants[413] = new ConstantUtf8("");
        constants[414] = new ConstantUtf8("");
        constants[415] = new ConstantUtf8("");
        constants[416] = new ConstantUtf8("");
        constants[417] = new ConstantUtf8("");
        constants[418] = new ConstantUtf8("");
        constants[419] = new ConstantUtf8("");
        constants[420] = new ConstantUtf8("");
        constants[421] = new ConstantUtf8("");
        constants[422] = new ConstantUtf8("");
        constants[423] = new ConstantUtf8("");
        constants[424] = new ConstantUtf8("");
        constants[425] = new ConstantUtf8("");
        constants[426] = new ConstantUtf8("");
        constants[427] = new ConstantUtf8("");
        constants[428] = new ConstantUtf8("");
        constants[429] = new ConstantUtf8("");
        constants[430] = new ConstantUtf8("");
        constants[431] = new ConstantUtf8("");
        constants[432] = new ConstantUtf8("");
        constants[433] = new ConstantUtf8("");
        constants[434] = new ConstantUtf8("");
        constants[435] = new ConstantUtf8("");
        constants[436] = new ConstantUtf8("");
        constants[437] = new ConstantUtf8("");
        constants[438] = new ConstantUtf8("");
        constants[439] = new ConstantUtf8("");
        constants[440] = new ConstantUtf8("");
        constants[441] = new ConstantUtf8("");
        constants[442] = new ConstantUtf8("");
        constants[443] = new ConstantUtf8("");
        constants[444] = new ConstantUtf8("");
        constants[445] = new ConstantUtf8("");
        constants[446] = new ConstantUtf8("");
        constants[447] = new ConstantUtf8("");
        constants[448] = new ConstantUtf8("");
        constants[449] = new ConstantUtf8("");
        constants[450] = new ConstantUtf8("");
        constants[451] = new ConstantUtf8("");
        constants[452] = new ConstantUtf8("");
        constants[453] = new ConstantUtf8("");
        constants[454] = new ConstantUtf8("");
        constants[455] = new ConstantUtf8("");
        constants[456] = new ConstantUtf8("");
        constants[457] = new ConstantUtf8("");
        constants[458] = new ConstantUtf8("");
        constants[459] = new ConstantUtf8("");
        constants[460] = new ConstantUtf8("");
        constants[461] = new ConstantUtf8("");
        constants[462] = new ConstantUtf8("");
        constants[463] = new ConstantUtf8("");
        constants[464] = new ConstantUtf8("");
        constants[465] = new ConstantUtf8("");
        constants[466] = new ConstantUtf8("");
        constants[467] = new ConstantUtf8("");
        constants[468] = new ConstantUtf8("");
        constants[469] = new ConstantUtf8("");
        constants[470] = new ConstantUtf8("");
        constants[471] = new ConstantUtf8("");
        constants[472] = new ConstantUtf8("");
        constants[473] = new ConstantUtf8("");
        constants[474] = new ConstantUtf8("");
        constants[475] = new ConstantUtf8("");
        constants[476] = new ConstantUtf8("");
        constants[477] = new ConstantUtf8("");
        constants[478] = new ConstantUtf8("");
        constants[479] = new ConstantUtf8("");
        constants[480] = new ConstantUtf8("");
        constants[481] = new ConstantUtf8("");
        constants[482] = new ConstantUtf8("");
        constants[483] = new ConstantUtf8("");
        constants[484] = new ConstantUtf8("");
        constants[485] = new ConstantUtf8("");
        constants[486] = new ConstantUtf8("");
        constants[487] = new ConstantUtf8("");
        constants[488] = new ConstantUtf8("");
        constants[489] = new ConstantUtf8("");
        constants[490] = new ConstantUtf8("");
        constants[491] = new ConstantUtf8("");
        constants[492] = new ConstantUtf8("");
        constants[493] = new ConstantUtf8("");
        constants[494] = new ConstantUtf8("");
        constants[495] = new ConstantUtf8("");
        constants[496] = new ConstantUtf8("");
        constants[497] = new ConstantUtf8("");
        constants[498] = new ConstantUtf8("");
        constants[499] = new ConstantUtf8("");
        constants[500] = new ConstantUtf8("");
        constants[501] = new ConstantUtf8("");
        constants[502] = new ConstantUtf8("");
        constants[503] = new ConstantUtf8("");
        constants[504] = new ConstantUtf8("");
        constants[505] = new ConstantUtf8("");
        constants[506] = new ConstantUtf8("");
        constants[507] = new ConstantUtf8("");
        constants[508] = new ConstantUtf8("");
        constants[509] = new ConstantUtf8("");
        constants[510] = new ConstantUtf8("");
        constants[511] = new ConstantUtf8("");
        constants[512] = new ConstantUtf8("");
        constants[513] = new ConstantUtf8("");
        constants[514] = new ConstantUtf8("");
        constants[515] = new ConstantUtf8("");
        constants[516] = new ConstantUtf8("");
        constants[517] = new ConstantUtf8("");
        constants[518] = new ConstantUtf8("");
        ConstantPool cp = new ConstantPool(constants);
        for (int i = 0; i < constants.length; i++) {
            if (constants[i] instanceof ConstantCP) {
                ConstantCP constantCP = (ConstantCP) constants[i];
                ConstantNameAndType cnat = cp.getConstant(constantCP.getNameAndTypeIndex());
                constants[cnat.getSignatureIndex()] = new ConstantUtf8("()V");
            }
        }
        List<CodeException> codeExceptions = new ArrayList<>();
        codeExceptions.add(new CodeException(917, 1062, 1063, 53));
        List<LineNumber> lineNumbers = new ArrayList<>();
        lineNumbers.add(new LineNumber(0, 113));
        lineNumbers.add(new LineNumber(2, 114));
        lineNumbers.add(new LineNumber(10, 115));
        lineNumbers.add(new LineNumber(21, 116));
        lineNumbers.add(new LineNumber(23, 117));
        lineNumbers.add(new LineNumber(38, 118));
        lineNumbers.add(new LineNumber(53, 119));
        lineNumbers.add(new LineNumber(71, 120));
        lineNumbers.add(new LineNumber(86, 121));
        lineNumbers.add(new LineNumber(89, 122));
        lineNumbers.add(new LineNumber(92, 123));
        lineNumbers.add(new LineNumber(95, 124));
        lineNumbers.add(new LineNumber(105, 125));
        lineNumbers.add(new LineNumber(115, 126));
        lineNumbers.add(new LineNumber(118, 127));
        lineNumbers.add(new LineNumber(121, 128));
        lineNumbers.add(new LineNumber(130, 130));
        lineNumbers.add(new LineNumber(140, 131));
        lineNumbers.add(new LineNumber(153, 132));
        lineNumbers.add(new LineNumber(172, 133));
        lineNumbers.add(new LineNumber(185, 131));
        lineNumbers.add(new LineNumber(191, 135));
        lineNumbers.add(new LineNumber(194, 136));
        lineNumbers.add(new LineNumber(201, 138));
        lineNumbers.add(new LineNumber(211, 139));
        lineNumbers.add(new LineNumber(218, 141));
        lineNumbers.add(new LineNumber(228, 142));
        lineNumbers.add(new LineNumber(241, 143));
        lineNumbers.add(new LineNumber(259, 144));
        lineNumbers.add(new LineNumber(271, 146));
        lineNumbers.add(new LineNumber(290, 147));
        lineNumbers.add(new LineNumber(303, 142));
        lineNumbers.add(new LineNumber(309, 150));
        lineNumbers.add(new LineNumber(312, 151));
        lineNumbers.add(new LineNumber(319, 153));
        lineNumbers.add(new LineNumber(329, 154));
        lineNumbers.add(new LineNumber(336, 156));
        lineNumbers.add(new LineNumber(346, 157));
        lineNumbers.add(new LineNumber(353, 159));
        lineNumbers.add(new LineNumber(363, 160));
        lineNumbers.add(new LineNumber(370, 162));
        lineNumbers.add(new LineNumber(380, 163));
        lineNumbers.add(new LineNumber(387, 165));
        lineNumbers.add(new LineNumber(397, 166));
        lineNumbers.add(new LineNumber(404, 168));
        lineNumbers.add(new LineNumber(414, 169));
        lineNumbers.add(new LineNumber(421, 171));
        lineNumbers.add(new LineNumber(431, 172));
        lineNumbers.add(new LineNumber(438, 174));
        lineNumbers.add(new LineNumber(448, 175));
        lineNumbers.add(new LineNumber(455, 177));
        lineNumbers.add(new LineNumber(465, 178));
        lineNumbers.add(new LineNumber(472, 180));
        lineNumbers.add(new LineNumber(482, 181));
        lineNumbers.add(new LineNumber(489, 183));
        lineNumbers.add(new LineNumber(499, 184));
        lineNumbers.add(new LineNumber(506, 186));
        lineNumbers.add(new LineNumber(516, 187));
        lineNumbers.add(new LineNumber(523, 189));
        lineNumbers.add(new LineNumber(533, 190));
        lineNumbers.add(new LineNumber(540, 192));
        lineNumbers.add(new LineNumber(550, 193));
        lineNumbers.add(new LineNumber(557, 195));
        lineNumbers.add(new LineNumber(567, 196));
        lineNumbers.add(new LineNumber(574, 198));
        lineNumbers.add(new LineNumber(584, 199));
        lineNumbers.add(new LineNumber(591, 202));
        lineNumbers.add(new LineNumber(601, 203));
        lineNumbers.add(new LineNumber(617, 204));
        lineNumbers.add(new LineNumber(621, 206));
        lineNumbers.add(new LineNumber(630, 207));
        lineNumbers.add(new LineNumber(650, 208));
        lineNumbers.add(new LineNumber(659, 209));
        lineNumbers.add(new LineNumber(687, 211));
        lineNumbers.add(new LineNumber(690, 212));
        lineNumbers.add(new LineNumber(694, 213));
        lineNumbers.add(new LineNumber(702, 214));
        lineNumbers.add(new LineNumber(711, 216));
        lineNumbers.add(new LineNumber(720, 217));
        lineNumbers.add(new LineNumber(725, 218));
        lineNumbers.add(new LineNumber(746, 217));
        lineNumbers.add(new LineNumber(751, 221));
        lineNumbers.add(new LineNumber(756, 222));
        lineNumbers.add(new LineNumber(786, 224));
        lineNumbers.add(new LineNumber(789, 225));
        lineNumbers.add(new LineNumber(797, 226));
        lineNumbers.add(new LineNumber(809, 230));
        lineNumbers.add(new LineNumber(814, 231));
        lineNumbers.add(new LineNumber(823, 232));
        lineNumbers.add(new LineNumber(828, 233));
        lineNumbers.add(new LineNumber(849, 232));
        lineNumbers.add(new LineNumber(854, 238));
        lineNumbers.add(new LineNumber(863, 239));
        lineNumbers.add(new LineNumber(891, 241));
        lineNumbers.add(new LineNumber(894, 242));
        lineNumbers.add(new LineNumber(902, 243));
        lineNumbers.add(new LineNumber(911, 117));
        lineNumbers.add(new LineNumber(917, 246));
        lineNumbers.add(new LineNumber(921, 247));
        lineNumbers.add(new LineNumber(941, 250));
        lineNumbers.add(new LineNumber(958, 251));
        lineNumbers.add(new LineNumber(967, 252));
        lineNumbers.add(new LineNumber(998, 253));
        lineNumbers.add(new LineNumber(1001, 254));
        lineNumbers.add(new LineNumber(1009, 255));
        lineNumbers.add(new LineNumber(1014, 256));
        lineNumbers.add(new LineNumber(1017, 255));
        lineNumbers.add(new LineNumber(1022, 258));
        lineNumbers.add(new LineNumber(1031, 259));
        lineNumbers.add(new LineNumber(1049, 260));
        lineNumbers.add(new LineNumber(1057, 262));
        lineNumbers.add(new LineNumber(1060, 263));
        lineNumbers.add(new LineNumber(1063, 264));
        lineNumbers.add(new LineNumber(1065, 265));
        lineNumbers.add(new LineNumber(1071, 266));
        method.setConstantPool(cp);
        byte[] bytecode = new byte[] { 1, 76, -69, 0, 75, 89, -73, 0, 76, 77, 42, -69, 0, 77, 89, -73, 0, 78, -75, 0, 79, 1, 78, 3, 54, 4, 21, 4, 42, -76, 0, 80, -74, 0, 81, -94, 3, 114, 42, -76, 0, 80, 21, 4, 3, -74, 0, 82, -64, 0, 83, 58, 5, 42, -76, 0, 80, 21, 4, 4, -74, 0, 82, -64, 0, 83, -72, 0, 84, 54, 6, 42, -76, 0, 80, 21, 4, 5, -74, 0, 82, -64, 0, 20, 58, 7, 1, 58, 8, 1, 58, 9, 1, 58, 10, 25, 7, -72, 0, 85, -74, 0, 86, 58, 11, 25, 7, -72, 0, 85, -74, 0, 87, 58, 12, 3, 54, 13, 3, 54, 14, -69, 0, 20, 89, -73, 0, 21, 58, 15, 25, 5, 18, 26, -74, 0, 88, -103, 0, 64, 3, 54, 16, 21, 16, 25, 7, -74, 0, 43, -94, 0, 41, 42, -76, 0, 62, 25, 7, 21, 16, -74, 0, 48, -64, 0, 83, -72, 0, 89, 58, 17, 25, 15, 25, 17, -74, 0, 90, -72, 0, 91, -74, 0, 24, -124, 16, 1, -89, -1, -45, 4, 54, 13, 18, 92, 58, 8, -89, 2, 99, 25, 5, 18, 25, -74, 0, 88, -103, 0, 10, 18, 93, 58, 8, -89, 2, 82, 25, 5, 18, 27, -74, 0, 88, -103, 0, 94, 3, 54, 16, 21, 16, 25, 7, -74, 0, 43, -94, 0, 71, 25, 7, 21, 16, -74, 0, 48, -64, 0, 83, 18, 68, -74, 0, 88, -103, 0, 15, 25, 15, 3, -72, 0, 91, -74, 0, 24, -89, 0, 35, 42, -76, 0, 62, 25, 7, 21, 16, -74, 0, 48, -64, 0, 83, -72, 0, 89, 58, 17, 25, 15, 25, 17, -74, 0, 90, -72, 0, 91, -74, 0, 24, -124, 16, 1, -89, -1, -75, 4, 54, 13, 18, 94, 58, 8, -89, 1, -19, 25, 5, 18, 28, -74, 0, 88, -103, 0, 10, 18, 95, 58, 8, -89, 1, -36, 25, 5, 18, 29, -74, 0, 88, -103, 0, 10, 18, 96, 58, 8, -89, 1, -53, 25, 5, 18, 38, -74, 0, 88, -103, 0, 10, 18, 97, 58, 8, -89, 1, -70, 25, 5, 18, 37, -74, 0, 88, -103, 0, 10, 18, 98, 58, 8, -89, 1, -87, 25, 5, 18, 39, -74, 0, 88, -103, 0, 10, 18, 99, 58, 8, -89, 1, -104, 25, 5, 18, 34, -74, 0, 88, -103, 0, 10, 18, 100, 58, 8, -89, 1, -121, 25, 5, 18, 23, -74, 0, 88, -103, 0, 10, 18, 101, 58, 8, -89, 1, 118, 25, 5, 18, 31, -74, 0, 88, -103, 0, 10, 18, 102, 58, 8, -89, 1, 101, 25, 5, 18, 32, -74, 0, 88, -103, 0, 10, 18, 103, 58, 8, -89, 1, 84, 25, 5, 18, 30, -74, 0, 88, -103, 0, 10, 18, 104, 58, 8, -89, 1, 67, 25, 5, 18, 33, -74, 0, 88, -103, 0, 10, 18, 105, 58, 8, -89, 1, 50, 25, 5, 18, 40, -74, 0, 88, -103, 0, 10, 18, 106, 58, 8, -89, 1, 33, 25, 5, 18, 15, -74, 0, 88, -103, 0, 10, 18, 107, 58, 8, -89, 1, 16, 25, 5, 18, 35, -74, 0, 88, -103, 0, 10, 18, 108, 58, 8, -89, 0, -1, 25, 5, 18, 36, -74, 0, 88, -103, 0, 10, 18, 109, 58, 8, -89, 0, -18, 25, 5, 18, 41, -74, 0, 88, -103, 0, 10, 18, 110, 58, 8, -89, 0, -35, 25, 5, 18, 111, -74, 0, 112, -102, 0, 3, 25, 5, 25, 5, 18, 113, -74, 0, 114, 4, 96, -74, 0, 115, 58, 8, 18, 116, 58, 9, -69, 0, 75, 89, -73, 0, 76, 58, 10, 25, 10, -69, 0, 117, 89, -78, 0, 118, 25, 8, -73, 0, 119, -71, 0, 120, 2, 0, 87, 43, -58, 0, 36, 25, 9, -58, 0, 31, -69, 0, 44, 89, -73, 0, 45, 43, -74, 0, 47, 18, 121, -74, 0, 47, 25, 9, -74, 0, 47, -74, 0, 50, 76, -89, 0, 6, 25, 9, 76, 18, 122, 58, 8, 25, 10, -72, 0, 123, -102, 0, 12, 44, 25, 10, -71, 0, 124, 2, 0, 87, -69, 0, 75, 89, -73, 0, 76, 58, 10, 42, 25, 8, 25, 15, -74, 0, 43, -98, 0, 8, 25, 15, -89, 0, 5, 25, 7, 21, 13, 21, 14, 21, 6, 25, 10, -74, 0, 125, 58, 9, 25, 9, -58, 0, 33, -69, 0, 44, 89, -73, 0, 45, 43, -74, 0, 47, 18, 121, -74, 0, 47, 25, 9, -74, 0, 47, 18, 126, -74, 0, 47, -74, 0, 50, 76, 18, 127, 78, 25, 10, -72, 0, 123, -102, 0, 117, 44, 25, 10, -71, 0, 124, 2, 0, 87, -89, 0, 105, 25, 8, -58, 0, 43, -69, 0, 75, 89, -73, 0, 76, 58, 10, 42, 25, 8, 25, 15, -74, 0, 43, -98, 0, 8, 25, 15, -89, 0, 5, 25, 7, 21, 13, 21, 14, 21, 6, 25, 10, -74, 0, 125, 58, 9, 43, -58, 0, 36, 25, 9, -58, 0, 31, -69, 0, 44, 89, -73, 0, 45, 43, -74, 0, 47, 18, 121, -74, 0, 47, 25, 9, -74, 0, 47, -74, 0, 50, 76, -89, 0, 6, 25, 9, 76, 25, 10, -72, 0, 123, -102, 0, 12, 44, 25, 10, -71, 0, 124, 2, 0, 87, -124, 4, 1, -89, -4, -120, 45, -58, 0, 23, -69, 0, 44, 89, -73, 0, 45, 18, -128, -74, 0, 47, 43, -74, 0, 47, -74, 0, 50, 76, 42, -76, 0, 62, -74, 0, -127, 43, 45, 44, -71, 0, -126, 4, 0, 58, 4, -69, 0, 20, 89, -73, 0, 21, 58, 5, 25, 4, -71, 0, -125, 1, 0, 58, 6, 25, 6, -71, 0, -124, 1, 0, -103, 0, 77, 25, 6, -71, 0, -123, 1, 0, -64, 0, 58, 58, 7, 1, 58, 8, 25, 7, -74, 0, 66, -98, 0, 16, -72, 0, -122, 25, 7, -74, 0, 66, -72, 0, 64, 58, 8, 25, 8, 3, -72, 0, -121, -103, 0, 29, 42, -76, 0, 79, 25, 7, -74, 0, 59, -72, 0, 91, 25, 7, -74, 0, -120, 87, 25, 5, 25, 7, -74, 0, -119, 87, -89, -1, -81, 25, 5, -80, 58, 4, 42, 25, 4, -72, 0, 54, -69, 0, 20, 89, -73, 0, 21, -80 };
        Code code = new Code(0, 0, 0, 0, bytecode, null, null, cp);
        code.setExceptionTable(codeExceptions.toArray(CodeException[]::new));
        code.setAttributes(new Attribute[] { new LineNumberTable(0, 0, lineNumbers.toArray(LineNumber[]::new), cp) });
        method.setAttributes(new Attribute[] { code });
        checkCFGReduction(method);
    }
    
    @Test
    public void testMethodWithOutsideLoop() throws Exception {
        Method method = new Method();
        Constant[] constants = new Constant[1517];
        constants[1] = new ConstantMethodref(30, 792);
        constants[2] = new ConstantString(793);
        constants[3] = new ConstantMethodref(30, 794);
        constants[4] = new ConstantMethodref(30, 795);
        constants[5] = new ConstantClass(796);
        constants[6] = new ConstantMethodref(5, 797);
        constants[7] = new ConstantMethodref(798, 799);
        constants[8] = new ConstantFieldref(30, 800);
        constants[9] = new ConstantMethodref(260, 792);
        constants[10] = new ConstantString(801);
        constants[11] = new ConstantMethodref(798, 802);
        constants[12] = new ConstantFieldref(30, 803);
        constants[13] = new ConstantString(804);
        constants[14] = new ConstantMethodref(260, 802);
        constants[15] = new ConstantClass(805);
        constants[16] = new ConstantMethodref(15, 806);
        constants[17] = new ConstantFieldref(30, 807);
        constants[18] = new ConstantString(808);
        constants[19] = new ConstantClass(809);
        constants[20] = new ConstantMethodref(19, 810);
        constants[21] = new ConstantMethodref(34, 799);
        constants[22] = new ConstantMethodref(811, 812);
        constants[23] = new ConstantMethodref(34, 813);
        constants[24] = new ConstantFieldref(30, 814);
        constants[25] = new ConstantString(815);
        constants[26] = new ConstantString(816);
        constants[27] = new ConstantMethodref(817, 818);
        constants[28] = new ConstantMethodref(244, 819);
        constants[29] = new ConstantClass(820);
        constants[30] = new ConstantClass(821);
        constants[31] = new ConstantMethodref(29, 822);
        constants[32] = new ConstantMethodref(244, 823);
        constants[33] = new ConstantMethodref(244, 824);
        constants[34] = new ConstantClass(825);
        constants[35] = new ConstantMethodref(34, 797);
        constants[36] = new ConstantFieldref(30, 826);
        constants[37] = new ConstantClass(827);
        constants[38] = new ConstantMethodref(246, 828);
        constants[39] = new ConstantString(829);
        constants[40] = new ConstantMethodref(246, 819);
        constants[41] = new ConstantMethodref(246, 823);
        constants[42] = new ConstantMethodref(246, 824);
        constants[43] = new ConstantFieldref(30, 830);
        constants[44] = new ConstantMethodref(248, 823);
        constants[45] = new ConstantClass(831);
        constants[46] = new ConstantInteger(2147483647);
        constants[47] = new ConstantMethodref(248, 824);
        constants[48] = new ConstantMethodref(248, 832);
        constants[49] = new ConstantMethodref(34, 833);
        constants[50] = new ConstantMethodref(834, 835);
        constants[51] = new ConstantClass(836);
        constants[52] = new ConstantMethodref(34, 837);
        constants[53] = new ConstantMethodref(834, 838);
        constants[54] = new ConstantFieldref(30, 839);
        constants[55] = new ConstantString(840);
        constants[56] = new ConstantFieldref(30, 841);
        constants[57] = new ConstantInteger(-2147483648);
        constants[58] = new ConstantMethodref(248, 842);
        constants[59] = new ConstantFieldref(30, 843);
        constants[60] = new ConstantString(844);
        constants[61] = new ConstantMethodref(246, 842);
        constants[62] = new ConstantFieldref(30, 845);
        constants[63] = new ConstantMethodref(250, 823);
        constants[64] = new ConstantMethodref(250, 824);
        constants[65] = new ConstantMethodref(250, 842);
        constants[66] = new ConstantClass(846);
        constants[67] = new ConstantClass(847);
        constants[68] = new ConstantFieldref(69, 848);
        constants[69] = new ConstantClass(849);
        constants[70] = new ConstantMethodref(69, 850);
        constants[71] = new ConstantMethodref(811, 851);
        constants[72] = new ConstantString(852);
        constants[73] = new ConstantMethodref(66, 853);
        constants[74] = new ConstantClass(854);
        constants[75] = new ConstantMethodref(74, 806);
        constants[76] = new ConstantClass(855);
        constants[77] = new ConstantMethodref(76, 856);
        constants[78] = new ConstantFieldref(30, 857);
        constants[79] = new ConstantMethodref(34, 802);
        constants[80] = new ConstantMethodref(34, 842);
        constants[81] = new ConstantMethodref(34, 823);
        constants[82] = new ConstantFieldref(30, 858);
        constants[83] = new ConstantString(859);
        constants[84] = new ConstantFieldref(30, 860);
        constants[85] = new ConstantFieldref(30, 861);
        constants[86] = new ConstantString(862);
        constants[87] = new ConstantString(863);
        constants[88] = new ConstantMethodref(244, 864);
        constants[89] = new ConstantString(865);
        constants[90] = new ConstantMethodref(244, 866);
        constants[91] = new ConstantFieldref(30, 867);
        constants[92] = new ConstantString(868);
        constants[93] = new ConstantMethodref(252, 819);
        constants[94] = new ConstantString(869);
        constants[95] = new ConstantMethodref(252, 866);
        constants[96] = new ConstantMethodref(252, 823);
        constants[97] = new ConstantMethodref(252, 824);
        constants[98] = new ConstantFieldref(30, 870);
        constants[99] = new ConstantString(871);
        constants[100] = new ConstantFieldref(30, 872);
        constants[101] = new ConstantFieldref(30, 873);
        constants[102] = new ConstantString(874);
        constants[103] = new ConstantString(875);
        constants[104] = new ConstantString(876);
        constants[105] = new ConstantFieldref(30, 877);
        constants[106] = new ConstantString(878);
        constants[107] = new ConstantFieldref(30, 879);
        constants[108] = new ConstantFieldref(30, 880);
        constants[109] = new ConstantString(881);
        constants[110] = new ConstantString(882);
        constants[111] = new ConstantString(883);
        constants[112] = new ConstantClass(884);
        constants[113] = new ConstantString(885);
        constants[114] = new ConstantString(886);
        constants[115] = new ConstantMethodref(112, 887);
        constants[116] = new ConstantFieldref(30, 888);
        constants[117] = new ConstantMethodref(112, 837);
        constants[118] = new ConstantMethodref(834, 889);
        constants[119] = new ConstantFieldref(30, 890);
        constants[120] = new ConstantString(891);
        constants[121] = new ConstantFieldref(30, 892);
        constants[122] = new ConstantString(893);
        constants[123] = new ConstantFieldref(30, 894);
        constants[124] = new ConstantString(895);
        constants[125] = new ConstantClass(896);
        constants[126] = new ConstantMethodref(125, 797);
        constants[127] = new ConstantFieldref(30, 897);
        constants[128] = new ConstantMethodref(125, 898);
        constants[129] = new ConstantFieldref(30, 899);
        constants[130] = new ConstantString(900);
        constants[131] = new ConstantMethodref(256, 819);
        constants[132] = new ConstantMethodref(256, 901);
        constants[133] = new ConstantMethodref(256, 828);
        constants[134] = new ConstantMethodref(256, 823);
        constants[135] = new ConstantMethodref(256, 824);
        constants[136] = new ConstantMethodref(256, 842);
        constants[137] = new ConstantString(902);
        constants[138] = new ConstantMethodref(834, 903);
        constants[139] = new ConstantFieldref(30, 904);
        constants[140] = new ConstantMethodref(254, 823);
        constants[141] = new ConstantMethodref(254, 824);
        constants[142] = new ConstantMethodref(254, 842);
        constants[143] = new ConstantMethodref(254, 832);
        constants[144] = new ConstantString(905);
        constants[145] = new ConstantMethodref(254, 864);
        constants[146] = new ConstantFieldref(30, 906);
        constants[147] = new ConstantFieldref(30, 907);
        constants[148] = new ConstantString(908);
        constants[149] = new ConstantFieldref(30, 909);
        constants[150] = new ConstantMethodref(258, 823);
        constants[151] = new ConstantMethodref(258, 824);
        constants[152] = new ConstantMethodref(258, 842);
        constants[153] = new ConstantMethodref(45, 910);
        constants[154] = new ConstantMethodref(258, 911);
        constants[155] = new ConstantMethodref(258, 912);
        constants[156] = new ConstantMethodref(258, 913);
        constants[157] = new ConstantFieldref(30, 914);
        constants[158] = new ConstantMethodref(254, 913);
        constants[159] = new ConstantClass(915);
        constants[160] = new ConstantMethodref(159, 797);
        constants[161] = new ConstantString(916);
        constants[162] = new ConstantMethodref(159, 917);
        constants[163] = new ConstantString(918);
        constants[164] = new ConstantMethodref(919, 920);
        constants[165] = new ConstantMethodref(834, 921);
        constants[166] = new ConstantString(922);
        constants[167] = new ConstantString(923);
        constants[168] = new ConstantString(924);
        constants[169] = new ConstantString(925);
        constants[170] = new ConstantString(926);
        constants[171] = new ConstantFieldref(30, 927);
        constants[172] = new ConstantFieldref(30, 928);
        constants[173] = new ConstantString(929);
        constants[174] = new ConstantString(930);
        constants[175] = new ConstantFieldref(30, 931);
        constants[176] = new ConstantString(932);
        constants[177] = new ConstantString(933);
        constants[178] = new ConstantFieldref(30, 934);
        constants[179] = new ConstantString(935);
        constants[180] = new ConstantString(936);
        constants[181] = new ConstantFieldref(30, 937);
        constants[182] = new ConstantString(938);
        constants[183] = new ConstantFieldref(30, 939);
        constants[184] = new ConstantFieldref(30, 940);
        constants[185] = new ConstantString(941);
        constants[186] = new ConstantString(942);
        constants[187] = new ConstantString(943);
        constants[188] = new ConstantFieldref(30, 944);
        constants[189] = new ConstantString(945);
        constants[190] = new ConstantFieldref(30, 946);
        constants[191] = new ConstantMethodref(34, 947);
        constants[192] = new ConstantClass(948);
        constants[193] = new ConstantMethodref(192, 850);
        constants[194] = new ConstantFieldref(30, 949);
        constants[195] = new ConstantString(950);
        constants[196] = new ConstantString(951);
        constants[197] = new ConstantMethodref(244, 842);
        constants[198] = new ConstantFieldref(30, 952);
        constants[199] = new ConstantString(953);
        constants[200] = new ConstantString(954);
        constants[201] = new ConstantFieldref(30, 955);
        constants[202] = new ConstantString(956);
        constants[203] = new ConstantString(957);
        constants[204] = new ConstantMethodref(244, 913);
        constants[205] = new ConstantFieldref(30, 958);
        constants[206] = new ConstantString(959);
        constants[207] = new ConstantString(960);
        constants[208] = new ConstantFieldref(30, 961);
        constants[209] = new ConstantString(962);
        constants[210] = new ConstantFieldref(30, 963);
        constants[211] = new ConstantString(964);
        constants[212] = new ConstantString(965);
        constants[213] = new ConstantString(966);
        constants[214] = new ConstantClass(967);
        constants[215] = new ConstantMethodref(214, 797);
        constants[216] = new ConstantMethodref(214, 968);
        constants[217] = new ConstantMethodref(214, 969);
        constants[218] = new ConstantClass(970);
        constants[219] = new ConstantMethodref(218, 971);
        constants[220] = new ConstantMethodref(214, 972);
        constants[221] = new ConstantMethodref(214, 973);
        constants[222] = new ConstantMethodref(214, 913);
        constants[223] = new ConstantMethodref(30, 833);
        constants[224] = new ConstantClass(974);
        constants[225] = new ConstantMethodref(224, 806);
        constants[226] = new ConstantMethodref(258, 975);
        constants[227] = new ConstantInterfaceMethodref(976, 977);
        constants[228] = new ConstantMethodref(248, 975);
        constants[229] = new ConstantClass(978);
        constants[230] = new ConstantMethodref(229, 806);
        constants[231] = new ConstantMethodref(254, 979);
        constants[232] = new ConstantMethodref(252, 979);
        constants[233] = new ConstantMethodref(250, 979);
        constants[234] = new ConstantClass(980);
        constants[235] = new ConstantMethodref(234, 806);
        constants[236] = new ConstantMethodref(244, 981);
        constants[237] = new ConstantFieldref(30, 982);
        constants[238] = new ConstantMethodref(256, 981);
        constants[239] = new ConstantClass(983);
        constants[240] = new ConstantMethodref(239, 806);
        constants[241] = new ConstantString(984);
        constants[242] = new ConstantMethodref(15, 985);
        constants[243] = new ConstantMethodref(509, 797);
        constants[244] = new ConstantClass(986);
        constants[245] = new ConstantMethodref(244, 797);
        constants[246] = new ConstantClass(987);
        constants[247] = new ConstantMethodref(246, 797);
        constants[248] = new ConstantClass(988);
        constants[249] = new ConstantMethodref(248, 797);
        constants[250] = new ConstantClass(989);
        constants[251] = new ConstantMethodref(250, 797);
        constants[252] = new ConstantClass(990);
        constants[253] = new ConstantMethodref(252, 797);
        constants[254] = new ConstantClass(991);
        constants[255] = new ConstantMethodref(254, 797);
        constants[256] = new ConstantClass(992);
        constants[257] = new ConstantMethodref(256, 797);
        constants[258] = new ConstantClass(993);
        constants[259] = new ConstantMethodref(258, 797);
        constants[260] = new ConstantClass(994);
        constants[261] = new ConstantMethodref(260, 797);
        constants[262] = new ConstantFieldref(30, 995);
        constants[263] = new ConstantMethodref(30, 996);
        constants[264] = new ConstantClass(997);
        constants[265] = new ConstantMethodref(998, 999);
        constants[266] = new ConstantMethodref(30, 1000);
        constants[267] = new ConstantMethodref(919, 1001);
        constants[268] = new ConstantString(1002);
        constants[269] = new ConstantMethodref(919, 1003);
        constants[270] = new ConstantMethodref(1004, 1005);
        constants[271] = new ConstantString(1006);
        constants[272] = new ConstantMethodref(1007, 1008);
        constants[273] = new ConstantMethodref(1009, 1010);
        constants[274] = new ConstantString(1011);
        constants[275] = new ConstantString(1012);
        constants[276] = new ConstantString(1013);
        constants[277] = new ConstantString(1014);
        constants[278] = new ConstantMethodref(1015, 1016);
        constants[279] = new ConstantString(1017);
        constants[280] = new ConstantMethodref(159, 1018);
        constants[281] = new ConstantMethodref(919, 1019);
        constants[282] = new ConstantClass(1020);
        constants[283] = new ConstantString(1021);
        constants[284] = new ConstantString(1022);
        constants[285] = new ConstantMethodref(919, 1023);
        constants[286] = new ConstantMethodref(30, 1024);
        constants[287] = new ConstantFieldref(30, 1025);
        constants[288] = new ConstantMethodref(248, 819);
        constants[289] = new ConstantMethodref(30, 1026);
        constants[290] = new ConstantString(1027);
        constants[291] = new ConstantMethodref(112, 1028);
        constants[292] = new ConstantMethodref(112, 1029);
        constants[293] = new ConstantClass(1030);
        constants[294] = new ConstantMethodref(293, 797);
        constants[295] = new ConstantMethodref(112, 1031);
        constants[296] = new ConstantMethodref(30, 1032);
        constants[297] = new ConstantMethodref(30, 1033);
        constants[298] = new ConstantMethodref(252, 1034);
        constants[299] = new ConstantClass(1035);
        constants[300] = new ConstantString(1036);
        constants[301] = new ConstantMethodref(254, 1037);
        constants[302] = new ConstantString(1038);
        constants[303] = new ConstantClass(1039);
        constants[304] = new ConstantMethodref(303, 797);
        constants[305] = new ConstantMethodref(76, 1040);
        constants[306] = new ConstantString(1041);
        constants[307] = new ConstantMethodref(250, 1042);
        constants[308] = new ConstantMethodref(30, 1043);
        constants[309] = new ConstantMethodref(30, 1044);
        constants[310] = new ConstantMethodref(1007, 1045);
        constants[311] = new ConstantClass(1046);
        constants[312] = new ConstantMethodref(311, 797);
        constants[313] = new ConstantString(1047);
        constants[314] = new ConstantMethodref(311, 1048);
        constants[315] = new ConstantMethodref(311, 1049);
        constants[316] = new ConstantString(1050);
        constants[317] = new ConstantMethodref(311, 1051);
        constants[318] = new ConstantMethodref(998, 1052);
        constants[319] = new ConstantMethodref(30, 1053);
        constants[320] = new ConstantMethodref(260, 1054);
        constants[321] = new ConstantMethodref(322, 1055);
        constants[322] = new ConstantClass(1056);
        constants[323] = new ConstantMethodref(322, 1057);
        constants[324] = new ConstantMethodref(322, 1058);
        constants[325] = new ConstantMethodref(1009, 1059);
        constants[326] = new ConstantMethodref(322, 1060);
        constants[327] = new ConstantMethodref(322, 1061);
        constants[328] = new ConstantMethodref(322, 1062);
        constants[329] = new ConstantMethodref(322, 1063);
        constants[330] = new ConstantMethodref(322, 1064);
        constants[331] = new ConstantMethodref(322, 1065);
        constants[332] = new ConstantMethodref(322, 1066);
        constants[333] = new ConstantMethodref(322, 1067);
        constants[334] = new ConstantMethodref(322, 1068);
        constants[335] = new ConstantMethodref(30, 1069);
        constants[336] = new ConstantMethodref(30, 1070);
        constants[337] = new ConstantMethodref(322, 1071);
        constants[338] = new ConstantMethodref(252, 901);
        constants[339] = new ConstantMethodref(322, 1072);
        constants[340] = new ConstantMethodref(322, 1073);
        constants[341] = new ConstantMethodref(322, 1074);
        constants[342] = new ConstantMethodref(322, 1075);
        constants[343] = new ConstantMethodref(322, 1076);
        constants[344] = new ConstantString(1077);
        constants[345] = new ConstantInterfaceMethodref(471, 917);
        constants[346] = new ConstantMethodref(322, 1078);
        constants[347] = new ConstantMethodref(322, 1079);
        constants[348] = new ConstantString(1080);
        constants[349] = new ConstantMethodref(322, 1081);
        constants[350] = new ConstantMethodref(322, 1082);
        constants[351] = new ConstantMethodref(322, 1083);
        constants[352] = new ConstantMethodref(322, 1084);
        constants[353] = new ConstantMethodref(322, 1085);
        constants[354] = new ConstantMethodref(1086, 1051);
        constants[355] = new ConstantMethodref(260, 1087);
        constants[356] = new ConstantMethodref(15, 1088);
        constants[357] = new ConstantMethodref(30, 1089);
        constants[358] = new ConstantString(1090);
        constants[359] = new ConstantMethodref(1009, 1091);
        constants[360] = new ConstantString(1092);
        constants[361] = new ConstantMethodref(322, 797);
        constants[362] = new ConstantMethodref(248, 1093);
        constants[363] = new ConstantMethodref(322, 1094);
        constants[364] = new ConstantMethodref(1009, 1095);
        constants[365] = new ConstantMethodref(322, 1096);
        constants[366] = new ConstantMethodref(250, 1097);
        constants[367] = new ConstantClass(1098);
        constants[368] = new ConstantMethodref(322, 1099);
        constants[369] = new ConstantFieldref(30, 1100);
        constants[370] = new ConstantMethodref(322, 1026);
        constants[371] = new ConstantMethodref(112, 1101);
        constants[372] = new ConstantMethodref(322, 1102);
        constants[373] = new ConstantMethodref(112, 1103);
        constants[374] = new ConstantMethodref(322, 1104);
        constants[375] = new ConstantMethodref(254, 1097);
        constants[376] = new ConstantMethodref(322, 1105);
        constants[377] = new ConstantMethodref(322, 1106);
        constants[378] = new ConstantMethodref(256, 1107);
        constants[379] = new ConstantMethodref(322, 1108);
        constants[380] = new ConstantMethodref(322, 1109);
        constants[381] = new ConstantMethodref(322, 1110);
        constants[382] = new ConstantMethodref(258, 1111);
        constants[383] = new ConstantClass(1112);
        constants[384] = new ConstantMethodref(383, 1113);
        constants[385] = new ConstantMethodref(1114, 1115);
        constants[386] = new ConstantMethodref(252, 1107);
        constants[387] = new ConstantMethodref(322, 1116);
        constants[388] = new ConstantMethodref(322, 1117);
        constants[389] = new ConstantMethodref(322, 1118);
        constants[390] = new ConstantMethodref(322, 1119);
        constants[391] = new ConstantMethodref(76, 1120);
        constants[392] = new ConstantMethodref(322, 1121);
        constants[393] = new ConstantMethodref(322, 1122);
        constants[394] = new ConstantMethodref(322, 1123);
        constants[395] = new ConstantMethodref(1004, 1124);
        constants[396] = new ConstantMethodref(322, 1125);
        constants[397] = new ConstantMethodref(322, 1126);
        constants[398] = new ConstantFieldref(30, 1127);
        constants[399] = new ConstantMethodref(1009, 1128);
        constants[400] = new ConstantMethodref(303, 1129);
        constants[401] = new ConstantMethodref(303, 1130);
        constants[402] = new ConstantMethodref(303, 1131);
        constants[403] = new ConstantMethodref(159, 1132);
        constants[404] = new ConstantMethodref(322, 1032);
        constants[405] = new ConstantMethodref(322, 1133);
        constants[406] = new ConstantFieldref(30, 1134);
        constants[407] = new ConstantMethodref(322, 1033);
        constants[408] = new ConstantMethodref(322, 1135);
        constants[409] = new ConstantMethodref(1086, 1136);
        constants[410] = new ConstantMethodref(322, 1137);
        constants[411] = new ConstantString(1138);
        constants[412] = new ConstantMethodref(919, 1139);
        constants[413] = new ConstantMethodref(322, 1140);
        constants[414] = new ConstantMethodref(1004, 1141);
        constants[415] = new ConstantInterfaceMethodref(1142, 1143);
        constants[416] = new ConstantString(1144);
        constants[417] = new ConstantMethodref(919, 1145);
        constants[418] = new ConstantClass(1146);
        constants[419] = new ConstantString(1147);
        constants[420] = new ConstantMethodref(418, 1148);
        constants[421] = new ConstantString(1149);
        constants[422] = new ConstantMethodref(919, 1150);
        constants[423] = new ConstantInterfaceMethodref(1142, 1151);
        constants[424] = new ConstantInterfaceMethodref(1142, 1045);
        constants[425] = new ConstantMethodref(260, 1152);
        constants[426] = new ConstantMethodref(30, 1153);
        constants[427] = new ConstantMethodref(15, 1154);
        constants[428] = new ConstantClass(1155);
        constants[429] = new ConstantMethodref(428, 1156);
        constants[430] = new ConstantMethodref(428, 1157);
        constants[431] = new ConstantMethodref(428, 973);
        constants[432] = new ConstantMethodref(428, 1158);
        constants[433] = new ConstantClass(1159);
        constants[434] = new ConstantString(1160);
        constants[435] = new ConstantString(1161);
        constants[436] = new ConstantMethodref(919, 1162);
        constants[437] = new ConstantString(1163);
        constants[438] = new ConstantMethodref(1007, 1164);
        constants[439] = new ConstantMethodref(293, 1165);
        constants[440] = new ConstantString(1166);
        constants[441] = new ConstantMethodref(1167, 1168);
        constants[442] = new ConstantMethodref(293, 1169);
        constants[443] = new ConstantMethodref(1009, 1170);
        constants[444] = new ConstantString(1171);
        constants[445] = new ConstantString(1172);
        constants[446] = new ConstantMethodref(1007, 1173);
        constants[447] = new ConstantString(1174);
        constants[448] = new ConstantMethodref(919, 1175);
        constants[449] = new ConstantMethodref(159, 1176);
        constants[450] = new ConstantInterfaceMethodref(1177, 1178);
        constants[451] = new ConstantInterfaceMethodref(1177, 1179);
        constants[452] = new ConstantMethodref(303, 1180);
        constants[453] = new ConstantString(1181);
        constants[454] = new ConstantString(1182);
        constants[455] = new ConstantMethodref(919, 1183);
        constants[456] = new ConstantMethodref(303, 1184);
        constants[457] = new ConstantClass(1185);
        constants[458] = new ConstantString(1186);
        constants[459] = new ConstantMethodref(457, 1187);
        constants[460] = new ConstantString(1188);
        constants[461] = new ConstantMethodref(1189, 1190);
        constants[462] = new ConstantString(1191);
        constants[463] = new ConstantMethodref(1007, 1192);
        constants[464] = new ConstantClass(1193);
        constants[465] = new ConstantString(1194);
        constants[466] = new ConstantMethodref(464, 1195);
        constants[467] = new ConstantClass(1196);
        constants[468] = new ConstantMethodref(467, 797);
        constants[469] = new ConstantInterfaceMethodref(1197, 1198);
        constants[470] = new ConstantInterfaceMethodref(1199, 1176);
        constants[471] = new ConstantClass(1200);
        constants[472] = new ConstantMethodref(467, 1132);
        constants[473] = new ConstantMethodref(467, 1201);
        constants[474] = new ConstantString(1202);
        constants[475] = new ConstantString(1203);
        constants[476] = new ConstantString(1204);
        constants[477] = new ConstantString(1205);
        constants[478] = new ConstantString(1206);
        constants[479] = new ConstantString(1207);
        constants[480] = new ConstantString(1208);
        constants[481] = new ConstantInterfaceMethodref(1142, 1209);
        constants[482] = new ConstantMethodref(15, 1210);
        constants[483] = new ConstantMethodref(244, 1211);
        constants[484] = new ConstantString(1212);
        constants[485] = new ConstantMethodref(30, 973);
        constants[486] = new ConstantMethodref(30, 1213);
        constants[487] = new ConstantString(1214);
        constants[488] = new ConstantString(1215);
        constants[489] = new ConstantMethodref(919, 1216);
        constants[490] = new ConstantMethodref(367, 1217);
        constants[491] = new ConstantString(1218);
        constants[492] = new ConstantMethodref(367, 1115);
        constants[493] = new ConstantString(1219);
        constants[494] = new ConstantString(1220);
        constants[495] = new ConstantMethodref(322, 1221);
        constants[496] = new ConstantMethodref(322, 1222);
        constants[497] = new ConstantMethodref(30, 1223);
        constants[498] = new ConstantMethodref(30, 1224);
        constants[499] = new ConstantMethodref(30, 1225);
        constants[500] = new ConstantMethodref(1009, 1226);
        constants[501] = new ConstantString(1227);
        constants[502] = new ConstantMethodref(159, 1228);
        constants[503] = new ConstantMethodref(159, 1229);
        constants[504] = new ConstantMethodref(282, 1230);
        constants[505] = new ConstantInterfaceMethodref(1199, 917);
        constants[506] = new ConstantString(1231);
        constants[507] = new ConstantMethodref(1009, 1232);
        constants[508] = new ConstantInterfaceMethodref(471, 1176);
        constants[509] = new ConstantClass(1233);
        constants[510] = new ConstantUtf8("");
        constants[511] = new ConstantUtf8("");
        constants[512] = new ConstantUtf8("");
        constants[513] = new ConstantClass(1234);
        constants[514] = new ConstantUtf8("");
        constants[515] = new ConstantUtf8("");
        constants[516] = new ConstantClass(1235);
        constants[517] = new ConstantUtf8("");
        constants[518] = new ConstantUtf8("");
        constants[519] = new ConstantUtf8("");
        constants[520] = new ConstantUtf8("");
        constants[521] = new ConstantUtf8("");
        constants[522] = new ConstantUtf8("");
        constants[523] = new ConstantUtf8("");
        constants[524] = new ConstantUtf8("");
        constants[525] = new ConstantUtf8("");
        constants[526] = new ConstantInteger(11);
        constants[527] = new ConstantUtf8("");
        constants[528] = new ConstantInteger(3);
        constants[529] = new ConstantUtf8("");
        constants[530] = new ConstantInteger(21);
        constants[531] = new ConstantUtf8("");
        constants[532] = new ConstantInteger(128);
        constants[533] = new ConstantUtf8("");
        constants[534] = new ConstantUtf8("");
        constants[535] = new ConstantUtf8("");
        constants[536] = new ConstantUtf8("");
        constants[537] = new ConstantUtf8("");
        constants[538] = new ConstantUtf8("");
        constants[539] = new ConstantUtf8("");
        constants[540] = new ConstantUtf8("");
        constants[541] = new ConstantUtf8("");
        constants[542] = new ConstantUtf8("");
        constants[543] = new ConstantUtf8("");
        constants[544] = new ConstantUtf8("");
        constants[545] = new ConstantUtf8("");
        constants[546] = new ConstantUtf8("");
        constants[547] = new ConstantUtf8("");
        constants[548] = new ConstantUtf8("");
        constants[549] = new ConstantUtf8("");
        constants[550] = new ConstantUtf8("");
        constants[551] = new ConstantUtf8("");
        constants[552] = new ConstantUtf8("");
        constants[553] = new ConstantUtf8("");
        constants[554] = new ConstantUtf8("");
        constants[555] = new ConstantUtf8("");
        constants[556] = new ConstantUtf8("");
        constants[557] = new ConstantUtf8("");
        constants[558] = new ConstantUtf8("");
        constants[559] = new ConstantUtf8("");
        constants[560] = new ConstantUtf8("");
        constants[561] = new ConstantUtf8("");
        constants[562] = new ConstantUtf8("");
        constants[563] = new ConstantUtf8("");
        constants[564] = new ConstantUtf8("");
        constants[565] = new ConstantUtf8("");
        constants[566] = new ConstantUtf8("");
        constants[567] = new ConstantUtf8("");
        constants[568] = new ConstantUtf8("");
        constants[569] = new ConstantUtf8("");
        constants[570] = new ConstantUtf8("");
        constants[571] = new ConstantUtf8("");
        constants[572] = new ConstantUtf8("");
        constants[573] = new ConstantUtf8("");
        constants[574] = new ConstantUtf8("");
        constants[575] = new ConstantUtf8("");
        constants[576] = new ConstantUtf8("");
        constants[577] = new ConstantUtf8("");
        constants[578] = new ConstantUtf8("");
        constants[579] = new ConstantUtf8("");
        constants[580] = new ConstantUtf8("");
        constants[581] = new ConstantUtf8("");
        constants[582] = new ConstantUtf8("");
        constants[583] = new ConstantUtf8("");
        constants[584] = new ConstantUtf8("");
        constants[585] = new ConstantUtf8("");
        constants[586] = new ConstantUtf8("");
        constants[587] = new ConstantUtf8("");
        constants[588] = new ConstantUtf8("");
        constants[589] = new ConstantUtf8("");
        constants[590] = new ConstantUtf8("");
        constants[591] = new ConstantUtf8("");
        constants[592] = new ConstantUtf8("");
        constants[593] = new ConstantUtf8("");
        constants[594] = new ConstantUtf8("");
        constants[595] = new ConstantUtf8("");
        constants[596] = new ConstantUtf8("");
        constants[597] = new ConstantUtf8("");
        constants[598] = new ConstantUtf8("");
        constants[599] = new ConstantUtf8("");
        constants[600] = new ConstantUtf8("");
        constants[601] = new ConstantUtf8("");
        constants[602] = new ConstantUtf8("");
        constants[603] = new ConstantUtf8("");
        constants[604] = new ConstantUtf8("");
        constants[605] = new ConstantUtf8("");
        constants[606] = new ConstantUtf8("");
        constants[607] = new ConstantUtf8("");
        constants[608] = new ConstantUtf8("");
        constants[609] = new ConstantUtf8("");
        constants[610] = new ConstantUtf8("");
        constants[611] = new ConstantUtf8("");
        constants[612] = new ConstantUtf8("");
        constants[613] = new ConstantUtf8("");
        constants[614] = new ConstantUtf8("");
        constants[615] = new ConstantUtf8("");
        constants[616] = new ConstantUtf8("");
        constants[617] = new ConstantUtf8("");
        constants[618] = new ConstantUtf8("");
        constants[619] = new ConstantUtf8("");
        constants[620] = new ConstantUtf8("");
        constants[621] = new ConstantUtf8("");
        constants[622] = new ConstantUtf8("");
        constants[623] = new ConstantUtf8("");
        constants[624] = new ConstantUtf8("");
        constants[625] = new ConstantUtf8("");
        constants[626] = new ConstantUtf8("");
        constants[627] = new ConstantUtf8("");
        constants[628] = new ConstantUtf8("");
        constants[629] = new ConstantUtf8("");
        constants[630] = new ConstantUtf8("");
        constants[631] = new ConstantUtf8("");
        constants[632] = new ConstantUtf8("");
        constants[633] = new ConstantUtf8("");
        constants[634] = new ConstantUtf8("");
        constants[635] = new ConstantUtf8("");
        constants[636] = new ConstantUtf8("");
        constants[637] = new ConstantUtf8("");
        constants[638] = new ConstantUtf8("");
        constants[639] = new ConstantUtf8("");
        constants[640] = new ConstantUtf8("");
        constants[641] = new ConstantUtf8("");
        constants[642] = new ConstantUtf8("");
        constants[643] = new ConstantUtf8("");
        constants[644] = new ConstantUtf8("");
        constants[645] = new ConstantUtf8("");
        constants[646] = new ConstantUtf8("");
        constants[647] = new ConstantUtf8("");
        constants[648] = new ConstantUtf8("");
        constants[649] = new ConstantUtf8("");
        constants[650] = new ConstantUtf8("");
        constants[651] = new ConstantUtf8("");
        constants[652] = new ConstantUtf8("");
        constants[653] = new ConstantClass(821);
        constants[654] = new ConstantClass(997);
        constants[655] = new ConstantUtf8("");
        constants[656] = new ConstantUtf8("");
        constants[657] = new ConstantUtf8("");
        constants[658] = new ConstantClass(915);
        constants[659] = new ConstantUtf8("");
        constants[660] = new ConstantUtf8("");
        constants[661] = new ConstantUtf8("");
        constants[662] = new ConstantUtf8("");
        constants[663] = new ConstantUtf8("");
        constants[664] = new ConstantUtf8("");
        constants[665] = new ConstantUtf8("");
        constants[666] = new ConstantUtf8("");
        constants[667] = new ConstantClass(1056);
        constants[668] = new ConstantUtf8("");
        constants[669] = new ConstantUtf8("");
        constants[670] = new ConstantUtf8("");
        constants[671] = new ConstantUtf8("");
        constants[672] = new ConstantUtf8("");
        constants[673] = new ConstantUtf8("");
        constants[674] = new ConstantUtf8("");
        constants[675] = new ConstantUtf8("");
        constants[676] = new ConstantUtf8("");
        constants[677] = new ConstantUtf8("");
        constants[678] = new ConstantUtf8("");
        constants[679] = new ConstantUtf8("");
        constants[680] = new ConstantUtf8("");
        constants[681] = new ConstantUtf8("");
        constants[682] = new ConstantClass(1200);
        constants[683] = new ConstantClass(1236);
        constants[684] = new ConstantClass(991);
        constants[685] = new ConstantClass(1237);
        constants[686] = new ConstantUtf8("");
        constants[687] = new ConstantUtf8("");
        constants[688] = new ConstantUtf8("");
        constants[689] = new ConstantUtf8("");
        constants[690] = new ConstantUtf8("");
        constants[691] = new ConstantClass(1098);
        constants[692] = new ConstantUtf8("");
        constants[693] = new ConstantUtf8("");
        constants[694] = new ConstantUtf8("");
        constants[695] = new ConstantUtf8("");
        constants[696] = new ConstantUtf8("");
        constants[697] = new ConstantUtf8("");
        constants[698] = new ConstantUtf8("");
        constants[699] = new ConstantUtf8("");
        constants[700] = new ConstantUtf8("");
        constants[701] = new ConstantUtf8("");
        constants[702] = new ConstantUtf8("");
        constants[703] = new ConstantUtf8("");
        constants[704] = new ConstantUtf8("");
        constants[705] = new ConstantUtf8("");
        constants[706] = new ConstantUtf8("");
        constants[707] = new ConstantUtf8("");
        constants[708] = new ConstantUtf8("");
        constants[709] = new ConstantUtf8("");
        constants[710] = new ConstantUtf8("");
        constants[711] = new ConstantUtf8("");
        constants[712] = new ConstantUtf8("");
        constants[713] = new ConstantClass(1155);
        constants[714] = new ConstantUtf8("");
        constants[715] = new ConstantUtf8("");
        constants[716] = new ConstantUtf8("");
        constants[717] = new ConstantUtf8("");
        constants[718] = new ConstantUtf8("");
        constants[719] = new ConstantUtf8("");
        constants[720] = new ConstantUtf8("");
        constants[721] = new ConstantClass(1030);
        constants[722] = new ConstantUtf8("");
        constants[723] = new ConstantUtf8("");
        constants[724] = new ConstantUtf8("");
        constants[725] = new ConstantUtf8("");
        constants[726] = new ConstantUtf8("");
        constants[727] = new ConstantUtf8("");
        constants[728] = new ConstantUtf8("");
        constants[729] = new ConstantUtf8("");
        constants[730] = new ConstantUtf8("");
        constants[731] = new ConstantClass(1238);
        constants[732] = new ConstantClass(1039);
        constants[733] = new ConstantClass(1239);
        constants[734] = new ConstantUtf8("");
        constants[735] = new ConstantUtf8("");
        constants[736] = new ConstantUtf8("");
        constants[737] = new ConstantUtf8("");
        constants[738] = new ConstantUtf8("");
        constants[739] = new ConstantUtf8("");
        constants[740] = new ConstantUtf8("");
        constants[741] = new ConstantUtf8("");
        constants[742] = new ConstantUtf8("");
        constants[743] = new ConstantUtf8("");
        constants[744] = new ConstantUtf8("");
        constants[745] = new ConstantClass(1193);
        constants[746] = new ConstantClass(1240);
        constants[747] = new ConstantClass(1196);
        constants[748] = new ConstantUtf8("");
        constants[749] = new ConstantUtf8("");
        constants[750] = new ConstantClass(993);
        constants[751] = new ConstantUtf8("");
        constants[752] = new ConstantUtf8("");
        constants[753] = new ConstantUtf8("");
        constants[754] = new ConstantUtf8("");
        constants[755] = new ConstantUtf8("");
        constants[756] = new ConstantUtf8("");
        constants[757] = new ConstantUtf8("");
        constants[758] = new ConstantUtf8("");
        constants[759] = new ConstantUtf8("");
        constants[760] = new ConstantUtf8("");
        constants[761] = new ConstantUtf8("");
        constants[762] = new ConstantUtf8("");
        constants[763] = new ConstantUtf8("");
        constants[764] = new ConstantUtf8("");
        constants[765] = new ConstantUtf8("");
        constants[766] = new ConstantUtf8("");
        constants[767] = new ConstantUtf8("");
        constants[768] = new ConstantUtf8("");
        constants[769] = new ConstantUtf8("");
        constants[770] = new ConstantUtf8("");
        constants[771] = new ConstantUtf8("");
        constants[772] = new ConstantUtf8("");
        constants[773] = new ConstantUtf8("");
        constants[774] = new ConstantUtf8("");
        constants[775] = new ConstantUtf8("");
        constants[776] = new ConstantUtf8("");
        constants[777] = new ConstantUtf8("");
        constants[778] = new ConstantUtf8("");
        constants[779] = new ConstantUtf8("");
        constants[780] = new ConstantUtf8("");
        constants[781] = new ConstantUtf8("");
        constants[782] = new ConstantUtf8("");
        constants[783] = new ConstantUtf8("");
        constants[784] = new ConstantClass(1241);
        constants[785] = new ConstantUtf8("");
        constants[786] = new ConstantUtf8("");
        constants[787] = new ConstantUtf8("");
        constants[788] = new ConstantUtf8("");
        constants[789] = new ConstantUtf8("");
        constants[790] = new ConstantUtf8("");
        constants[791] = new ConstantUtf8("");
        constants[792] = new ConstantNameAndType(1242, 1243);
        constants[793] = new ConstantUtf8("");
        constants[794] = new ConstantNameAndType(1244, 1245);
        constants[795] = new ConstantNameAndType(1246, 1247);
        constants[796] = new ConstantUtf8("");
        constants[797] = new ConstantNameAndType(649, 604);
        constants[798] = new ConstantClass(1248);
        constants[799] = new ConstantNameAndType(1249, 1250);
        constants[800] = new ConstantNameAndType(593, 594);
        constants[801] = new ConstantUtf8("");
        constants[802] = new ConstantNameAndType(1251, 1252);
        constants[803] = new ConstantNameAndType(597, 598);
        constants[804] = new ConstantUtf8("");
        constants[805] = new ConstantUtf8("");
        constants[806] = new ConstantNameAndType(649, 1253);
        constants[807] = new ConstantNameAndType(595, 596);
        constants[808] = new ConstantUtf8("");
        constants[809] = new ConstantUtf8("");
        constants[810] = new ConstantNameAndType(649, 1254);
        constants[811] = new ConstantClass(1255);
        constants[812] = new ConstantNameAndType(1256, 1257);
        constants[813] = new ConstantNameAndType(1258, 1259);
        constants[814] = new ConstantNameAndType(533, 534);
        constants[815] = new ConstantUtf8("");
        constants[816] = new ConstantUtf8("");
        constants[817] = new ConstantClass(1260);
        constants[818] = new ConstantNameAndType(1261, 1262);
        constants[819] = new ConstantNameAndType(1263, 1245);
        constants[820] = new ConstantUtf8("");
        constants[821] = new ConstantUtf8("");
        constants[822] = new ConstantNameAndType(649, 1243);
        constants[823] = new ConstantNameAndType(1264, 1265);
        constants[824] = new ConstantNameAndType(1266, 1265);
        constants[825] = new ConstantUtf8("");
        constants[826] = new ConstantNameAndType(536, 537);
        constants[827] = new ConstantUtf8("");
        constants[828] = new ConstantNameAndType(1267, 663);
        constants[829] = new ConstantUtf8("");
        constants[830] = new ConstantNameAndType(538, 539);
        constants[831] = new ConstantUtf8("");
        constants[832] = new ConstantNameAndType(1268, 672);
        constants[833] = new ConstantNameAndType(1251, 1269);
        constants[834] = new ConstantClass(1270);
        constants[835] = new ConstantNameAndType(1271, 1272);
        constants[836] = new ConstantUtf8("");
        constants[837] = new ConstantNameAndType(1273, 1274);
        constants[838] = new ConstantNameAndType(1275, 1272);
        constants[839] = new ConstantNameAndType(540, 537);
        constants[840] = new ConstantUtf8("");
        constants[841] = new ConstantNameAndType(541, 539);
        constants[842] = new ConstantNameAndType(1276, 1265);
        constants[843] = new ConstantNameAndType(542, 537);
        constants[844] = new ConstantUtf8("");
        constants[845] = new ConstantNameAndType(543, 544);
        constants[846] = new ConstantUtf8("");
        constants[847] = new ConstantUtf8("");
        constants[848] = new ConstantNameAndType(1277, 1278);
        constants[849] = new ConstantUtf8("");
        constants[850] = new ConstantNameAndType(649, 1279);
        constants[851] = new ConstantNameAndType(1280, 1281);
        constants[852] = new ConstantUtf8("");
        constants[853] = new ConstantNameAndType(649, 1282);
        constants[854] = new ConstantUtf8("");
        constants[855] = new ConstantUtf8("");
        constants[856] = new ConstantNameAndType(649, 1283);
        constants[857] = new ConstantNameAndType(577, 578);
        constants[858] = new ConstantNameAndType(545, 537);
        constants[859] = new ConstantUtf8("");
        constants[860] = new ConstantNameAndType(546, 539);
        constants[861] = new ConstantNameAndType(547, 534);
        constants[862] = new ConstantUtf8("");
        constants[863] = new ConstantUtf8("");
        constants[864] = new ConstantNameAndType(1284, 1245);
        constants[865] = new ConstantUtf8("");
        constants[866] = new ConstantNameAndType(1285, 1245);
        constants[867] = new ConstantNameAndType(553, 554);
        constants[868] = new ConstantUtf8("");
        constants[869] = new ConstantUtf8("");
        constants[870] = new ConstantNameAndType(561, 537);
        constants[871] = new ConstantUtf8("");
        constants[872] = new ConstantNameAndType(562, 539);
        constants[873] = new ConstantNameAndType(563, 534);
        constants[874] = new ConstantUtf8("");
        constants[875] = new ConstantUtf8("");
        constants[876] = new ConstantUtf8("");
        constants[877] = new ConstantNameAndType(548, 537);
        constants[878] = new ConstantUtf8("");
        constants[879] = new ConstantNameAndType(549, 539);
        constants[880] = new ConstantNameAndType(550, 534);
        constants[881] = new ConstantUtf8("");
        constants[882] = new ConstantUtf8("");
        constants[883] = new ConstantUtf8("");
        constants[884] = new ConstantUtf8("");
        constants[885] = new ConstantUtf8("");
        constants[886] = new ConstantUtf8("");
        constants[887] = new ConstantNameAndType(649, 1286);
        constants[888] = new ConstantNameAndType(551, 552);
        constants[889] = new ConstantNameAndType(1287, 1288);
        constants[890] = new ConstantNameAndType(564, 537);
        constants[891] = new ConstantUtf8("");
        constants[892] = new ConstantNameAndType(567, 537);
        constants[893] = new ConstantUtf8("");
        constants[894] = new ConstantNameAndType(569, 537);
        constants[895] = new ConstantUtf8("");
        constants[896] = new ConstantUtf8("");
        constants[897] = new ConstantNameAndType(570, 571);
        constants[898] = new ConstantNameAndType(1251, 1289);
        constants[899] = new ConstantNameAndType(573, 571);
        constants[900] = new ConstantUtf8("");
        constants[901] = new ConstantNameAndType(1290, 672);
        constants[902] = new ConstantUtf8("");
        constants[903] = new ConstantNameAndType(1291, 1292);
        constants[904] = new ConstantNameAndType(565, 566);
        constants[905] = new ConstantUtf8("");
        constants[906] = new ConstantNameAndType(568, 566);
        constants[907] = new ConstantNameAndType(572, 566);
        constants[908] = new ConstantUtf8("");
        constants[909] = new ConstantNameAndType(574, 575);
        constants[910] = new ConstantNameAndType(1293, 1294);
        constants[911] = new ConstantNameAndType(1295, 1296);
        constants[912] = new ConstantNameAndType(1297, 663);
        constants[913] = new ConstantNameAndType(1298, 672);
        constants[914] = new ConstantNameAndType(576, 566);
        constants[915] = new ConstantUtf8("");
        constants[916] = new ConstantUtf8("");
        constants[917] = new ConstantNameAndType(1251, 1299);
        constants[918] = new ConstantUtf8("");
        constants[919] = new ConstantClass(1300);
        constants[920] = new ConstantNameAndType(1301, 1302);
        constants[921] = new ConstantNameAndType(1303, 1288);
        constants[922] = new ConstantUtf8("");
        constants[923] = new ConstantUtf8("");
        constants[924] = new ConstantUtf8("");
        constants[925] = new ConstantUtf8("");
        constants[926] = new ConstantUtf8("");
        constants[927] = new ConstantNameAndType(579, 552);
        constants[928] = new ConstantNameAndType(555, 554);
        constants[929] = new ConstantUtf8("");
        constants[930] = new ConstantUtf8("");
        constants[931] = new ConstantNameAndType(556, 554);
        constants[932] = new ConstantUtf8("");
        constants[933] = new ConstantUtf8("");
        constants[934] = new ConstantNameAndType(557, 554);
        constants[935] = new ConstantUtf8("");
        constants[936] = new ConstantUtf8("");
        constants[937] = new ConstantNameAndType(558, 537);
        constants[938] = new ConstantUtf8("");
        constants[939] = new ConstantNameAndType(559, 539);
        constants[940] = new ConstantNameAndType(560, 534);
        constants[941] = new ConstantUtf8("");
        constants[942] = new ConstantUtf8("");
        constants[943] = new ConstantUtf8("");
        constants[944] = new ConstantNameAndType(580, 537);
        constants[945] = new ConstantUtf8("");
        constants[946] = new ConstantNameAndType(581, 566);
        constants[947] = new ConstantNameAndType(1304, 1274);
        constants[948] = new ConstantUtf8("");
        constants[949] = new ConstantNameAndType(587, 534);
        constants[950] = new ConstantUtf8("");
        constants[951] = new ConstantUtf8("");
        constants[952] = new ConstantNameAndType(586, 534);
        constants[953] = new ConstantUtf8("");
        constants[954] = new ConstantUtf8("");
        constants[955] = new ConstantNameAndType(585, 534);
        constants[956] = new ConstantUtf8("");
        constants[957] = new ConstantUtf8("");
        constants[958] = new ConstantNameAndType(584, 534);
        constants[959] = new ConstantUtf8("");
        constants[960] = new ConstantUtf8("");
        constants[961] = new ConstantNameAndType(583, 534);
        constants[962] = new ConstantUtf8("");
        constants[963] = new ConstantNameAndType(582, 534);
        constants[964] = new ConstantUtf8("");
        constants[965] = new ConstantUtf8("");
        constants[966] = new ConstantUtf8("");
        constants[967] = new ConstantUtf8("");
        constants[968] = new ConstantNameAndType(649, 1305);
        constants[969] = new ConstantNameAndType(1306, 672);
        constants[970] = new ConstantUtf8("");
        constants[971] = new ConstantNameAndType(649, 1307);
        constants[972] = new ConstantNameAndType(1308, 1309);
        constants[973] = new ConstantNameAndType(1310, 672);
        constants[974] = new ConstantUtf8("");
        constants[975] = new ConstantNameAndType(1311, 1312);
        constants[976] = new ConstantClass(1313);
        constants[977] = new ConstantNameAndType(1314, 1315);
        constants[978] = new ConstantUtf8("");
        constants[979] = new ConstantNameAndType(1316, 1317);
        constants[980] = new ConstantUtf8("");
        constants[981] = new ConstantNameAndType(1318, 1319);
        constants[982] = new ConstantNameAndType(535, 534);
        constants[983] = new ConstantUtf8("");
        constants[984] = new ConstantUtf8("");
        constants[985] = new ConstantNameAndType(1320, 1321);
        constants[986] = new ConstantUtf8("");
        constants[987] = new ConstantUtf8("");
        constants[988] = new ConstantUtf8("");
        constants[989] = new ConstantUtf8("");
        constants[990] = new ConstantUtf8("");
        constants[991] = new ConstantUtf8("");
        constants[992] = new ConstantUtf8("");
        constants[993] = new ConstantUtf8("");
        constants[994] = new ConstantUtf8("");
        constants[995] = new ConstantNameAndType(601, 602);
        constants[996] = new ConstantNameAndType(603, 604);
        constants[997] = new ConstantUtf8("");
        constants[998] = new ConstantClass(1322);
        constants[999] = new ConstantNameAndType(1323, 1324);
        constants[1000] = new ConstantNameAndType(655, 604);
        constants[1001] = new ConstantNameAndType(1325, 1326);
        constants[1002] = new ConstantUtf8("");
        constants[1003] = new ConstantNameAndType(1327, 1328);
        constants[1004] = new ConstantClass(1329);
        constants[1005] = new ConstantNameAndType(1330, 1331);
        constants[1006] = new ConstantUtf8("");
        constants[1007] = new ConstantClass(1332);
        constants[1008] = new ConstantNameAndType(1333, 1334);
        constants[1009] = new ConstantClass(1335);
        constants[1010] = new ConstantNameAndType(1336, 1337);
        constants[1011] = new ConstantUtf8("");
        constants[1012] = new ConstantUtf8("");
        constants[1013] = new ConstantUtf8("");
        constants[1014] = new ConstantUtf8("");
        constants[1015] = new ConstantClass(1338);
        constants[1016] = new ConstantNameAndType(1339, 1340);
        constants[1017] = new ConstantUtf8("");
        constants[1018] = new ConstantNameAndType(1251, 1341);
        constants[1019] = new ConstantNameAndType(1342, 1343);
        constants[1020] = new ConstantUtf8("");
        constants[1021] = new ConstantUtf8("");
        constants[1022] = new ConstantUtf8("");
        constants[1023] = new ConstantNameAndType(1301, 1344);
        constants[1024] = new ConstantNameAndType(659, 604);
        constants[1025] = new ConstantNameAndType(599, 600);
        constants[1026] = new ConstantNameAndType(776, 777);
        constants[1027] = new ConstantUtf8("");
        constants[1028] = new ConstantNameAndType(1345, 1245);
        constants[1029] = new ConstantNameAndType(1346, 762);
        constants[1030] = new ConstantUtf8("");
        constants[1031] = new ConstantNameAndType(1347, 1348);
        constants[1032] = new ConstantNameAndType(785, 786);
        constants[1033] = new ConstantNameAndType(789, 786);
        constants[1034] = new ConstantNameAndType(1349, 672);
        constants[1035] = new ConstantUtf8("");
        constants[1036] = new ConstantUtf8("");
        constants[1037] = new ConstantNameAndType(1350, 1296);
        constants[1038] = new ConstantUtf8("");
        constants[1039] = new ConstantUtf8("");
        constants[1040] = new ConstantNameAndType(1351, 1352);
        constants[1041] = new ConstantUtf8("");
        constants[1042] = new ConstantNameAndType(1353, 1296);
        constants[1043] = new ConstantNameAndType(674, 672);
        constants[1044] = new ConstantNameAndType(671, 672);
        constants[1045] = new ConstantNameAndType(1354, 1355);
        constants[1046] = new ConstantUtf8("");
        constants[1047] = new ConstantUtf8("");
        constants[1048] = new ConstantNameAndType(1356, 1357);
        constants[1049] = new ConstantNameAndType(1356, 1358);
        constants[1050] = new ConstantUtf8("");
        constants[1051] = new ConstantNameAndType(1359, 1360);
        constants[1052] = new ConstantNameAndType(1323, 1361);
        constants[1053] = new ConstantNameAndType(675, 676);
        constants[1054] = new ConstantNameAndType(1362, 1363);
        constants[1055] = new ConstantNameAndType(1364, 1365);
        constants[1056] = new ConstantUtf8("");
        constants[1057] = new ConstantNameAndType(1366, 1360);
        constants[1058] = new ConstantNameAndType(1367, 1368);
        constants[1059] = new ConstantNameAndType(1369, 1370);
        constants[1060] = new ConstantNameAndType(1371, 1360);
        constants[1061] = new ConstantNameAndType(1372, 1340);
        constants[1062] = new ConstantNameAndType(1373, 1360);
        constants[1063] = new ConstantNameAndType(1374, 1375);
        constants[1064] = new ConstantNameAndType(1376, 1360);
        constants[1065] = new ConstantNameAndType(1377, 1360);
        constants[1066] = new ConstantNameAndType(1378, 1360);
        constants[1067] = new ConstantNameAndType(1379, 1380);
        constants[1068] = new ConstantNameAndType(1381, 699);
        constants[1069] = new ConstantNameAndType(748, 708);
        constants[1070] = new ConstantNameAndType(749, 708);
        constants[1071] = new ConstantNameAndType(1382, 699);
        constants[1072] = new ConstantNameAndType(1383, 699);
        constants[1073] = new ConstantNameAndType(1384, 699);
        constants[1074] = new ConstantNameAndType(1385, 699);
        constants[1075] = new ConstantNameAndType(1386, 1387);
        constants[1076] = new ConstantNameAndType(1388, 699);
        constants[1077] = new ConstantUtf8("");
        constants[1078] = new ConstantNameAndType(1389, 1387);
        constants[1079] = new ConstantNameAndType(1390, 699);
        constants[1080] = new ConstantUtf8("");
        constants[1081] = new ConstantNameAndType(1391, 1392);
        constants[1082] = new ConstantNameAndType(1393, 1360);
        constants[1083] = new ConstantNameAndType(1394, 1375);
        constants[1084] = new ConstantNameAndType(1395, 1360);
        constants[1085] = new ConstantNameAndType(1396, 1397);
        constants[1086] = new ConstantClass(1236);
        constants[1087] = new ConstantNameAndType(1398, 1363);
        constants[1088] = new ConstantNameAndType(687, 604);
        constants[1089] = new ConstantNameAndType(1399, 1360);
        constants[1090] = new ConstantUtf8("");
        constants[1091] = new ConstantNameAndType(1400, 1401);
        constants[1092] = new ConstantUtf8("");
        constants[1093] = new ConstantNameAndType(1402, 1360);
        constants[1094] = new ConstantNameAndType(1403, 1245);
        constants[1095] = new ConstantNameAndType(1404, 1405);
        constants[1096] = new ConstantNameAndType(1406, 1407);
        constants[1097] = new ConstantNameAndType(1408, 1365);
        constants[1098] = new ConstantUtf8("");
        constants[1099] = new ConstantNameAndType(1409, 1245);
        constants[1100] = new ConstantNameAndType(588, 589);
        constants[1101] = new ConstantNameAndType(1410, 1360);
        constants[1102] = new ConstantNameAndType(1411, 1245);
        constants[1103] = new ConstantNameAndType(1412, 1375);
        constants[1104] = new ConstantNameAndType(1413, 1348);
        constants[1105] = new ConstantNameAndType(1414, 1245);
        constants[1106] = new ConstantNameAndType(1415, 1245);
        constants[1107] = new ConstantNameAndType(1416, 699);
        constants[1108] = new ConstantNameAndType(1417, 1245);
        constants[1109] = new ConstantNameAndType(1418, 663);
        constants[1110] = new ConstantNameAndType(1419, 672);
        constants[1111] = new ConstantNameAndType(1420, 1365);
        constants[1112] = new ConstantUtf8("");
        constants[1113] = new ConstantNameAndType(1421, 1380);
        constants[1114] = new ConstantClass(1237);
        constants[1115] = new ConstantNameAndType(1422, 1299);
        constants[1116] = new ConstantNameAndType(1423, 672);
        constants[1117] = new ConstantNameAndType(1424, 672);
        constants[1118] = new ConstantNameAndType(1425, 672);
        constants[1119] = new ConstantNameAndType(1426, 672);
        constants[1120] = new ConstantNameAndType(1427, 1392);
        constants[1121] = new ConstantNameAndType(1428, 1352);
        constants[1122] = new ConstantNameAndType(1429, 1245);
        constants[1123] = new ConstantNameAndType(1430, 1348);
        constants[1124] = new ConstantNameAndType(1431, 1360);
        constants[1125] = new ConstantNameAndType(1432, 1245);
        constants[1126] = new ConstantNameAndType(1433, 672);
        constants[1127] = new ConstantNameAndType(590, 591);
        constants[1128] = new ConstantNameAndType(1434, 1435);
        constants[1129] = new ConstantNameAndType(1436, 1299);
        constants[1130] = new ConstantNameAndType(1398, 1437);
        constants[1131] = new ConstantNameAndType(1438, 1439);
        constants[1132] = new ConstantNameAndType(1440, 1441);
        constants[1133] = new ConstantNameAndType(1442, 672);
        constants[1134] = new ConstantNameAndType(592, 591);
        constants[1135] = new ConstantNameAndType(1443, 1245);
        constants[1136] = new ConstantNameAndType(1444, 1445);
        constants[1137] = new ConstantNameAndType(1446, 1447);
        constants[1138] = new ConstantUtf8("");
        constants[1139] = new ConstantNameAndType(1448, 1449);
        constants[1140] = new ConstantNameAndType(1450, 1380);
        constants[1141] = new ConstantNameAndType(1451, 1452);
        constants[1142] = new ConstantClass(1453);
        constants[1143] = new ConstantNameAndType(1454, 676);
        constants[1144] = new ConstantUtf8("");
        constants[1145] = new ConstantNameAndType(1455, 1456);
        constants[1146] = new ConstantUtf8("");
        constants[1147] = new ConstantUtf8("");
        constants[1148] = new ConstantNameAndType(1457, 1401);
        constants[1149] = new ConstantUtf8("");
        constants[1150] = new ConstantNameAndType(1458, 1449);
        constants[1151] = new ConstantNameAndType(1459, 1460);
        constants[1152] = new ConstantNameAndType(1461, 1288);
        constants[1153] = new ConstantNameAndType(752, 708);
        constants[1154] = new ConstantNameAndType(1462, 708);
        constants[1155] = new ConstantUtf8("");
        constants[1156] = new ConstantNameAndType(649, 1463);
        constants[1157] = new ConstantNameAndType(1464, 672);
        constants[1158] = new ConstantNameAndType(1465, 1340);
        constants[1159] = new ConstantUtf8("");
        constants[1160] = new ConstantUtf8("");
        constants[1161] = new ConstantUtf8("");
        constants[1162] = new ConstantNameAndType(1466, 1467);
        constants[1163] = new ConstantUtf8("");
        constants[1164] = new ConstantNameAndType(1468, 1334);
        constants[1165] = new ConstantNameAndType(1347, 1352);
        constants[1166] = new ConstantUtf8("");
        constants[1167] = new ConstantClass(1469);
        constants[1168] = new ConstantNameAndType(1412, 1470);
        constants[1169] = new ConstantNameAndType(1412, 1392);
        constants[1170] = new ConstantNameAndType(1471, 1472);
        constants[1171] = new ConstantUtf8("");
        constants[1172] = new ConstantUtf8("");
        constants[1173] = new ConstantNameAndType(1473, 1340);
        constants[1174] = new ConstantUtf8("");
        constants[1175] = new ConstantNameAndType(1474, 1449);
        constants[1176] = new ConstantNameAndType(1475, 1476);
        constants[1177] = new ConstantClass(1239);
        constants[1178] = new ConstantNameAndType(1477, 699);
        constants[1179] = new ConstantNameAndType(1478, 1365);
        constants[1180] = new ConstantNameAndType(1479, 1480);
        constants[1181] = new ConstantUtf8("");
        constants[1182] = new ConstantUtf8("");
        constants[1183] = new ConstantNameAndType(1466, 1481);
        constants[1184] = new ConstantNameAndType(1444, 1437);
        constants[1185] = new ConstantUtf8("");
        constants[1186] = new ConstantUtf8("");
        constants[1187] = new ConstantNameAndType(1482, 1483);
        constants[1188] = new ConstantUtf8("");
        constants[1189] = new ConstantClass(1484);
        constants[1190] = new ConstantNameAndType(1485, 1486);
        constants[1191] = new ConstantUtf8("");
        constants[1192] = new ConstantNameAndType(1487, 1488);
        constants[1193] = new ConstantUtf8("");
        constants[1194] = new ConstantUtf8("");
        constants[1195] = new ConstantNameAndType(1489, 1490);
        constants[1196] = new ConstantUtf8("");
        constants[1197] = new ConstantClass(1240);
        constants[1198] = new ConstantNameAndType(1491, 1492);
        constants[1199] = new ConstantClass(1241);
        constants[1200] = new ConstantUtf8("");
        constants[1201] = new ConstantNameAndType(1493, 1299);
        constants[1202] = new ConstantUtf8("");
        constants[1203] = new ConstantUtf8("");
        constants[1204] = new ConstantUtf8("");
        constants[1205] = new ConstantUtf8("");
        constants[1206] = new ConstantUtf8("");
        constants[1207] = new ConstantUtf8("");
        constants[1208] = new ConstantUtf8("");
        constants[1209] = new ConstantNameAndType(1494, 676);
        constants[1210] = new ConstantNameAndType(1495, 1496);
        constants[1211] = new ConstantNameAndType(1497, 699);
        constants[1212] = new ConstantUtf8("");
        constants[1213] = new ConstantNameAndType(1498, 604);
        constants[1214] = new ConstantUtf8("");
        constants[1215] = new ConstantUtf8("");
        constants[1216] = new ConstantNameAndType(1466, 1499);
        constants[1217] = new ConstantNameAndType(1500, 1380);
        constants[1218] = new ConstantUtf8("");
        constants[1219] = new ConstantUtf8("");
        constants[1220] = new ConstantUtf8("");
        constants[1221] = new ConstantNameAndType(1501, 663);
        constants[1222] = new ConstantNameAndType(1502, 663);
        constants[1223] = new ConstantNameAndType(692, 604);
        constants[1224] = new ConstantNameAndType(698, 699);
        constants[1225] = new ConstantNameAndType(753, 708);
        constants[1226] = new ConstantNameAndType(1434, 1441);
        constants[1227] = new ConstantUtf8("");
        constants[1228] = new ConstantNameAndType(1503, 1380);
        constants[1229] = new ConstantNameAndType(1504, 1505);
        constants[1230] = new ConstantNameAndType(1506, 1360);
        constants[1231] = new ConstantUtf8("");
        constants[1232] = new ConstantNameAndType(1471, 1507);
        constants[1233] = new ConstantUtf8("");
        constants[1234] = new ConstantUtf8("");
        constants[1235] = new ConstantUtf8("");
        constants[1236] = new ConstantUtf8("");
        constants[1237] = new ConstantUtf8("");
        constants[1238] = new ConstantUtf8("");
        constants[1239] = new ConstantUtf8("");
        constants[1240] = new ConstantUtf8("");
        constants[1241] = new ConstantUtf8("");
        constants[1242] = new ConstantUtf8("");
        constants[1243] = new ConstantUtf8("");
        constants[1244] = new ConstantUtf8("");
        constants[1245] = new ConstantUtf8("");
        constants[1246] = new ConstantUtf8("");
        constants[1247] = new ConstantUtf8("");
        constants[1248] = new ConstantUtf8("");
        constants[1249] = new ConstantUtf8("");
        constants[1250] = new ConstantUtf8("");
        constants[1251] = new ConstantUtf8("");
        constants[1252] = new ConstantUtf8("");
        constants[1253] = new ConstantUtf8("");
        constants[1254] = new ConstantUtf8("");
        constants[1255] = new ConstantUtf8("");
        constants[1256] = new ConstantUtf8("");
        constants[1257] = new ConstantUtf8("");
        constants[1258] = new ConstantUtf8("");
        constants[1259] = new ConstantUtf8("");
        constants[1260] = new ConstantUtf8("");
        constants[1261] = new ConstantUtf8("");
        constants[1262] = new ConstantUtf8("");
        constants[1263] = new ConstantUtf8("");
        constants[1264] = new ConstantUtf8("");
        constants[1265] = new ConstantUtf8("");
        constants[1266] = new ConstantUtf8("");
        constants[1267] = new ConstantUtf8("");
        constants[1268] = new ConstantUtf8("");
        constants[1269] = new ConstantUtf8("");
        constants[1270] = new ConstantUtf8("");
        constants[1271] = new ConstantUtf8("");
        constants[1272] = new ConstantUtf8("");
        constants[1273] = new ConstantUtf8("");
        constants[1274] = new ConstantUtf8("");
        constants[1275] = new ConstantUtf8("");
        constants[1276] = new ConstantUtf8("");
        constants[1277] = new ConstantUtf8("");
        constants[1278] = new ConstantUtf8("");
        constants[1279] = new ConstantUtf8("");
        constants[1280] = new ConstantUtf8("");
        constants[1281] = new ConstantUtf8("");
        constants[1282] = new ConstantUtf8("");
        constants[1283] = new ConstantUtf8("");
        constants[1284] = new ConstantUtf8("");
        constants[1285] = new ConstantUtf8("");
        constants[1286] = new ConstantUtf8("");
        constants[1287] = new ConstantUtf8("");
        constants[1288] = new ConstantUtf8("");
        constants[1289] = new ConstantUtf8("");
        constants[1290] = new ConstantUtf8("");
        constants[1291] = new ConstantUtf8("");
        constants[1292] = new ConstantUtf8("");
        constants[1293] = new ConstantUtf8("");
        constants[1294] = new ConstantUtf8("");
        constants[1295] = new ConstantUtf8("");
        constants[1296] = new ConstantUtf8("");
        constants[1297] = new ConstantUtf8("");
        constants[1298] = new ConstantUtf8("");
        constants[1299] = new ConstantUtf8("");
        constants[1300] = new ConstantUtf8("");
        constants[1301] = new ConstantUtf8("");
        constants[1302] = new ConstantUtf8("");
        constants[1303] = new ConstantUtf8("");
        constants[1304] = new ConstantUtf8("");
        constants[1305] = new ConstantUtf8("");
        constants[1306] = new ConstantUtf8("");
        constants[1307] = new ConstantUtf8("");
        constants[1308] = new ConstantUtf8("");
        constants[1309] = new ConstantUtf8("");
        constants[1310] = new ConstantUtf8("");
        constants[1311] = new ConstantUtf8("");
        constants[1312] = new ConstantUtf8("");
        constants[1313] = new ConstantUtf8("");
        constants[1314] = new ConstantUtf8("");
        constants[1315] = new ConstantUtf8("");
        constants[1316] = new ConstantUtf8("");
        constants[1317] = new ConstantUtf8("");
        constants[1318] = new ConstantUtf8("");
        constants[1319] = new ConstantUtf8("");
        constants[1320] = new ConstantUtf8("");
        constants[1321] = new ConstantUtf8("");
        constants[1322] = new ConstantUtf8("");
        constants[1323] = new ConstantUtf8("");
        constants[1324] = new ConstantUtf8("");
        constants[1325] = new ConstantUtf8("");
        constants[1326] = new ConstantUtf8("");
        constants[1327] = new ConstantUtf8("");
        constants[1328] = new ConstantUtf8("");
        constants[1329] = new ConstantUtf8("");
        constants[1330] = new ConstantUtf8("");
        constants[1331] = new ConstantUtf8("");
        constants[1332] = new ConstantUtf8("");
        constants[1333] = new ConstantUtf8("");
        constants[1334] = new ConstantUtf8("");
        constants[1335] = new ConstantUtf8("");
        constants[1336] = new ConstantUtf8("");
        constants[1337] = new ConstantUtf8("");
        constants[1338] = new ConstantUtf8("");
        constants[1339] = new ConstantUtf8("");
        constants[1340] = new ConstantUtf8("");
        constants[1341] = new ConstantUtf8("");
        constants[1342] = new ConstantUtf8("");
        constants[1343] = new ConstantUtf8("");
        constants[1344] = new ConstantUtf8("");
        constants[1345] = new ConstantUtf8("");
        constants[1346] = new ConstantUtf8("");
        constants[1347] = new ConstantUtf8("");
        constants[1348] = new ConstantUtf8("");
        constants[1349] = new ConstantUtf8("");
        constants[1350] = new ConstantUtf8("");
        constants[1351] = new ConstantUtf8("");
        constants[1352] = new ConstantUtf8("");
        constants[1353] = new ConstantUtf8("");
        constants[1354] = new ConstantUtf8("");
        constants[1355] = new ConstantUtf8("");
        constants[1356] = new ConstantUtf8("");
        constants[1357] = new ConstantUtf8("");
        constants[1358] = new ConstantUtf8("");
        constants[1359] = new ConstantUtf8("");
        constants[1360] = new ConstantUtf8("");
        constants[1361] = new ConstantUtf8("");
        constants[1362] = new ConstantUtf8("");
        constants[1363] = new ConstantUtf8("");
        constants[1364] = new ConstantUtf8("");
        constants[1365] = new ConstantUtf8("");
        constants[1366] = new ConstantUtf8("");
        constants[1367] = new ConstantUtf8("");
        constants[1368] = new ConstantUtf8("");
        constants[1369] = new ConstantUtf8("");
        constants[1370] = new ConstantUtf8("");
        constants[1371] = new ConstantUtf8("");
        constants[1372] = new ConstantUtf8("");
        constants[1373] = new ConstantUtf8("");
        constants[1374] = new ConstantUtf8("");
        constants[1375] = new ConstantUtf8("");
        constants[1376] = new ConstantUtf8("");
        constants[1377] = new ConstantUtf8("");
        constants[1378] = new ConstantUtf8("");
        constants[1379] = new ConstantUtf8("");
        constants[1380] = new ConstantUtf8("");
        constants[1381] = new ConstantUtf8("");
        constants[1382] = new ConstantUtf8("");
        constants[1383] = new ConstantUtf8("");
        constants[1384] = new ConstantUtf8("");
        constants[1385] = new ConstantUtf8("");
        constants[1386] = new ConstantUtf8("");
        constants[1387] = new ConstantUtf8("");
        constants[1388] = new ConstantUtf8("");
        constants[1389] = new ConstantUtf8("");
        constants[1390] = new ConstantUtf8("");
        constants[1391] = new ConstantUtf8("");
        constants[1392] = new ConstantUtf8("");
        constants[1393] = new ConstantUtf8("");
        constants[1394] = new ConstantUtf8("");
        constants[1395] = new ConstantUtf8("");
        constants[1396] = new ConstantUtf8("");
        constants[1397] = new ConstantUtf8("");
        constants[1398] = new ConstantUtf8("");
        constants[1399] = new ConstantUtf8("");
        constants[1400] = new ConstantUtf8("");
        constants[1401] = new ConstantUtf8("");
        constants[1402] = new ConstantUtf8("");
        constants[1403] = new ConstantUtf8("");
        constants[1404] = new ConstantUtf8("");
        constants[1405] = new ConstantUtf8("");
        constants[1406] = new ConstantUtf8("");
        constants[1407] = new ConstantUtf8("");
        constants[1408] = new ConstantUtf8("");
        constants[1409] = new ConstantUtf8("");
        constants[1410] = new ConstantUtf8("");
        constants[1411] = new ConstantUtf8("");
        constants[1412] = new ConstantUtf8("");
        constants[1413] = new ConstantUtf8("");
        constants[1414] = new ConstantUtf8("");
        constants[1415] = new ConstantUtf8("");
        constants[1416] = new ConstantUtf8("");
        constants[1417] = new ConstantUtf8("");
        constants[1418] = new ConstantUtf8("");
        constants[1419] = new ConstantUtf8("");
        constants[1420] = new ConstantUtf8("");
        constants[1421] = new ConstantUtf8("");
        constants[1422] = new ConstantUtf8("");
        constants[1423] = new ConstantUtf8("");
        constants[1424] = new ConstantUtf8("");
        constants[1425] = new ConstantUtf8("");
        constants[1426] = new ConstantUtf8("");
        constants[1427] = new ConstantUtf8("");
        constants[1428] = new ConstantUtf8("");
        constants[1429] = new ConstantUtf8("");
        constants[1430] = new ConstantUtf8("");
        constants[1431] = new ConstantUtf8("");
        constants[1432] = new ConstantUtf8("");
        constants[1433] = new ConstantUtf8("");
        constants[1434] = new ConstantUtf8("");
        constants[1435] = new ConstantUtf8("");
        constants[1436] = new ConstantUtf8("");
        constants[1437] = new ConstantUtf8("");
        constants[1438] = new ConstantUtf8("");
        constants[1439] = new ConstantUtf8("");
        constants[1440] = new ConstantUtf8("");
        constants[1441] = new ConstantUtf8("");
        constants[1442] = new ConstantUtf8("");
        constants[1443] = new ConstantUtf8("");
        constants[1444] = new ConstantUtf8("");
        constants[1445] = new ConstantUtf8("");
        constants[1446] = new ConstantUtf8("");
        constants[1447] = new ConstantUtf8("");
        constants[1448] = new ConstantUtf8("");
        constants[1449] = new ConstantUtf8("");
        constants[1450] = new ConstantUtf8("");
        constants[1451] = new ConstantUtf8("");
        constants[1452] = new ConstantUtf8("");
        constants[1453] = new ConstantUtf8("");
        constants[1454] = new ConstantUtf8("");
        constants[1455] = new ConstantUtf8("");
        constants[1456] = new ConstantUtf8("");
        constants[1457] = new ConstantUtf8("");
        constants[1458] = new ConstantUtf8("");
        constants[1459] = new ConstantUtf8("");
        constants[1460] = new ConstantUtf8("");
        constants[1461] = new ConstantUtf8("");
        constants[1462] = new ConstantUtf8("");
        constants[1463] = new ConstantUtf8("");
        constants[1464] = new ConstantUtf8("");
        constants[1465] = new ConstantUtf8("");
        constants[1466] = new ConstantUtf8("");
        constants[1467] = new ConstantUtf8("");
        constants[1468] = new ConstantUtf8("");
        constants[1469] = new ConstantUtf8("");
        constants[1470] = new ConstantUtf8("");
        constants[1471] = new ConstantUtf8("");
        constants[1472] = new ConstantUtf8("");
        constants[1473] = new ConstantUtf8("");
        constants[1474] = new ConstantUtf8("");
        constants[1475] = new ConstantUtf8("");
        constants[1476] = new ConstantUtf8("");
        constants[1477] = new ConstantUtf8("");
        constants[1478] = new ConstantUtf8("");
        constants[1479] = new ConstantUtf8("");
        constants[1480] = new ConstantUtf8("");
        constants[1481] = new ConstantUtf8("");
        constants[1482] = new ConstantUtf8("");
        constants[1483] = new ConstantUtf8("");
        constants[1484] = new ConstantUtf8("");
        constants[1485] = new ConstantUtf8("");
        constants[1486] = new ConstantUtf8("");
        constants[1487] = new ConstantUtf8("");
        constants[1488] = new ConstantUtf8("");
        constants[1489] = new ConstantUtf8("");
        constants[1490] = new ConstantUtf8("");
        constants[1491] = new ConstantUtf8("");
        constants[1492] = new ConstantUtf8("");
        constants[1493] = new ConstantUtf8("");
        constants[1494] = new ConstantUtf8("");
        constants[1495] = new ConstantUtf8("");
        constants[1496] = new ConstantUtf8("");
        constants[1497] = new ConstantUtf8("");
        constants[1498] = new ConstantUtf8("");
        constants[1499] = new ConstantUtf8("");
        constants[1500] = new ConstantUtf8("");
        constants[1501] = new ConstantUtf8("");
        constants[1502] = new ConstantUtf8("");
        constants[1503] = new ConstantUtf8("");
        constants[1504] = new ConstantUtf8("");
        constants[1505] = new ConstantUtf8("");
        constants[1506] = new ConstantUtf8("");
        constants[1507] = new ConstantUtf8("");
        constants[1508] = new ConstantUtf8("");
        constants[1509] = new ConstantClass(1508);
        constants[1510] = new ConstantMethodref(1509, 1093);
        constants[1511] = new ConstantUtf8("");
        constants[1512] = new ConstantClass(1511);
        constants[1513] = new ConstantUtf8("");
        constants[1514] = new ConstantUtf8("");
        constants[1515] = new ConstantNameAndType(1513, 1514);
        constants[1516] = new ConstantMethodref(1512, 1515);
        ConstantPool cp = new ConstantPool(constants);
        for (int i = 0; i < constants.length; i++) {
            if (constants[i] instanceof ConstantCP) {
                ConstantCP constantCP = (ConstantCP) constants[i];
                ConstantNameAndType cnat = cp.getConstant(constantCP.getNameAndTypeIndex());
                constants[cnat.getSignatureIndex()] = new ConstantUtf8("()V");
            }
        }
        List<CodeException> codeExceptions = new ArrayList<>();
        codeExceptions.add(new CodeException(138, 150, 153, 264));
        method.setConstantPool(cp);
        byte[] bytecode = new byte[] { 19, 1, -25, 19, 1, -24, 1, 1, 4, 42, -72, 1, -23, 77, 44, -57, 0, 4, -79, 44, -74, 1, -22, -102, 0, 11, 19, 1, -21, 42, -72, 1, -90, -79, 3, 62, -72, 1, -66, 58, 4, 25, 4, -72, 1, 17, -102, 0, 122, 25, 4, -74, 1, -63, 58, 5, 25, 5, -71, 1, -62, 1, 0, -103, 0, 105, 25, 5, -71, 1, -61, 1, 0, -64, 1, 66, 58, 6, 25, 6, -58, 0, 85, 25, 6, -74, 1, 67, 58, 7, 44, 25, 7, -74, 1, -20, -103, 0, 69, 4, 62, -69, 1, 55, 89, -73, 1, 56, 19, 1, -19, -74, 1, 58, 44, -74, 1, 58, 19, 1, -18, -74, 1, 58, -74, 1, 61, 42, -72, 1, -95, 54, 8, 21, 8, -103, 0, 29, 42, 25, 6, -74, 1, 65, -64, 1, 66, -75, 1, 31, -89, 0, 18, 58, 9, 42, 25, 9, -72, 1, 9, -89, 0, 7, -79, -89, -1, -109, 29, -102, 0, 26, 42, -76, 1, 31, -58, 0, 19, 42, -76, 1, 31, 3, -74, 1, -17, 42, -76, 1, 31, 3, -74, 1, -16, 42, -74, 1, -15, 42, -76, 1, 31, 44, -74, 1, 107, 42, -74, 1, -14, 54, 5, 21, 5, -103, 0, 34, 42, 42, -76, 1, 31, -74, 1, 63, 87, 42, -76, 0, 17, 42, -76, 1, 31, 3, -74, 1, -30, 42, 3, -74, 1, 53, 42, 4, -74, 1, 52, -79 };
        Code code = new Code(0, 0, 0, 0, bytecode, null, null, cp);
        code.setExceptionTable(codeExceptions.toArray(CodeException[]::new));
        method.setAttributes(new Attribute[] { code });
        checkCFGReduction(method);
    }

//    TODO: in progress
//    @Test
//    public void testLabelledBreaks() throws Exception {
//        checkCFGReduction(searchMethod("org/jd/core/v1/Label", "parseInto"));
//    }
    
    protected ControlFlowGraph checkCFGReduction(Method method) throws Exception {
        ControlFlowGraph cfg = new ControlFlowGraphMaker().make(method);

        assertNotNull(cfg);

        BasicBlock root = cfg.getStart();

        assertNotNull(root);

        String plantuml = ControlFlowGraphPlantUMLWriter.write(cfg);
        System.out.println("Step 0: " + ControlFlowGraphPlantURLWriter.writePlantUMLUrl(plantuml));

        ControlFlowGraphGotoReducer.reduce(cfg);

        plantuml = ControlFlowGraphPlantUMLWriter.write(cfg);
        System.out.println("Step 1: " + ControlFlowGraphPlantURLWriter.writePlantUMLUrl(plantuml));

        // --- Test natural loops --- //
        BitSet[] dominators = ControlFlowGraphLoopReducer.buildDominatorIndexes(cfg);
        List<Loop> naturalLoops = ControlFlowGraphLoopReducer.identifyNaturalLoops(cfg, dominators);

        for (Loop loop : naturalLoops) {
            System.out.println(loop);
        }

        ControlFlowGraphLoopReducer.reduce(cfg);
        plantuml = ControlFlowGraphPlantUMLWriter.write(cfg);
        System.out.println("Step 2: " + ControlFlowGraphPlantURLWriter.writePlantUMLUrl(plantuml));

        BitSet visited = new BitSet();
        BitSet jsrTargets = new BitSet();

        for (int i=0, count=3; i<5; i++, count++) {
            List<ControlFlowGraphReducer> preferredReducers = ControlFlowGraphReducer.getPreferredReducers();
            
            boolean reduced = false;
            
            for (ControlFlowGraphReducer controlFlowGraphReducer : preferredReducers) {
            
                reduced = controlFlowGraphReducer.reduce(visited, cfg.getStart(), jsrTargets);
    
                System.out.println("# of visited blocks: " + visited.cardinality());
                visited.clear();
                plantuml = ControlFlowGraphPlantUMLWriter.write(cfg);
                System.out.println("Step " + count + ": " + ControlFlowGraphPlantURLWriter.writePlantUMLUrl(plantuml));
    
                if (reduced) {
                    break;
                }
            }
            
            if (reduced) {
                break;
            }
        }

        checkFinalCFG(cfg);

        return cfg;
    }

    protected static void checkFinalCFG(ControlFlowGraph cfg) {
        List<BasicBlock> list = cfg.getBasicBlocks();

        for (int i=0, len=list.size(); i<len; i++) {
            BasicBlock basicBlock = list.get(i);

            if (basicBlock.getType() == TYPE_DELETED) {
                continue;
            }

            assertEquals("#" + basicBlock.getIndex() + " contains an invalid index", basicBlock.getIndex(), i);
            assertNotEquals("#" + basicBlock.getIndex() + " is a TRY_DECLARATION -> reduction failed", TYPE_TRY_DECLARATION, basicBlock.getType());
            assertNotEquals("#" + basicBlock.getIndex() + " is a SWITCH_DECLARATION -> reduction failed", TYPE_SWITCH_DECLARATION, basicBlock.getType());
            assertNotEquals("#" + basicBlock.getIndex() + " is a CONDITIONAL -> reduction failed", TYPE_CONDITIONAL_BRANCH, basicBlock.getType());
            assertNotEquals("#" + basicBlock.getIndex() + " is a GOTO -> reduction failed", TYPE_GOTO, basicBlock.getType());

            if (!basicBlock.matchType(GROUP_CONDITION)) {
                for (BasicBlock predecessor : basicBlock.getPredecessors()) {
                    assertTrue("#" + predecessor.getIndex() + " is a predecessor of #" + basicBlock.getIndex() + " but #" + predecessor.getIndex() + " does not refer it", predecessor.contains(basicBlock));
                }
            }

            if (basicBlock.matchType(TYPE_IF|TYPE_IF_ELSE)) {
                assertNotSame("#" + basicBlock.getIndex() + " is a IF or a IF_ELSE with a 'then' branch jumping to itself", basicBlock.getSub1(), basicBlock);
            }

            if (basicBlock.getType() == TYPE_IF_ELSE) {
                assertNotSame("#" + basicBlock.getIndex() + " is a IF_ELSE with a 'else' branch jumping to itself", basicBlock.getSub2(), basicBlock);
            }

            if (basicBlock.matchType(TYPE_TRY|TYPE_TRY_JSR)) {
                boolean containsFinally = false;
                Set<String> exceptionNames = new HashSet<>();
                int maxOffset = 0;

                for (ExceptionHandler exceptionHandler : basicBlock.getExceptionHandlers()) {
                    String name = exceptionHandler.getInternalThrowableName();
                    int offset = exceptionHandler.getBasicBlock().getFromOffset();

                    if (name == null) {
                        assertFalse("#" + basicBlock.getIndex() + " contains multiple finally handlers", containsFinally);
                        containsFinally = true;
                    } else {
                        assertFalse("#" + basicBlock.getIndex() + " contains multiple handlers for " + name, exceptionNames.contains(name));
                        exceptionNames.add(name);
                    }

                    assertFalse("#" + basicBlock.getIndex() + " have an invalid exception handler", exceptionHandler.getBasicBlock().matchType(GROUP_CONDITION));

                    if (maxOffset < offset) {
                        maxOffset = offset;
                    }
                }

                for (ExceptionHandler exceptionHandler : basicBlock.getExceptionHandlers()) {
                    BasicBlock bb = exceptionHandler.getBasicBlock();
                    int offset = bb.getFromOffset();

                    if (maxOffset != offset) {
                        // Search last offset
                        BasicBlock next = bb.getNext();

                        while ((bb != next) && next.matchType(GROUP_SINGLE_SUCCESSOR|TYPE_RETURN|TYPE_RETURN_VALUE|TYPE_THROW) && (next.getPredecessors().size() == 1)) {
                            bb = next;
                            next = next.getNext();
                            offset = bb.getFromOffset();
                        }

                        assertTrue("#" + basicBlock.getIndex() + " is a TRY or TRY_WITH_JST -> #" + exceptionHandler.getBasicBlock().getIndex() + " handler reduction failed", offset < maxOffset);
                    }
                }
            }

            if (basicBlock.getType() == TYPE_SWITCH) {
                for (SwitchCase switchCase : basicBlock.getSwitchCases()) {
                    assertFalse("#" + basicBlock.getIndex() + " have an invalid switch case", switchCase.getBasicBlock().matchType(GROUP_CONDITION));
                }
            }

            if (!basicBlock.matchType(GROUP_CONDITION)) {
                assertTrue("#" + basicBlock.getIndex() + " have an invalid next basic block", (basicBlock.getNext() == null) || !basicBlock.getNext().matchType(GROUP_CONDITION));
                assertTrue("#" + basicBlock.getIndex() + " have an invalid branch basic block", (basicBlock.getBranch() == null) || !basicBlock.getBranch().matchType(GROUP_CONDITION));
                assertTrue("#" + basicBlock.getIndex() + " have an invalid sub1 basic block", (basicBlock.getSub1() == null) || !basicBlock.getSub1().matchType(GROUP_CONDITION));
                assertTrue("#" + basicBlock.getIndex() + " have an invalid sub2 basic block", (basicBlock.getSub2() == null) || !basicBlock.getSub2().matchType(GROUP_CONDITION));
            }
        }

        BitSet visited = new BitSet(list.size());
        SilentWatchDog watchdog = new SilentWatchDog();
        BasicBlock result = checkBasicBlock(visited, cfg.getStart(), watchdog);

        assertFalse("DELETED basic block detected -> reduction failed", (result != null) && (result.getType() == TYPE_DELETED));
        assertFalse("Cycle detected -> reduction failed", (result != null) && (result.getType() != TYPE_DELETED));
    }

    protected static BasicBlock checkBasicBlock(BitSet visited, BasicBlock basicBlock, SilentWatchDog watchdog) {
        if ((basicBlock == null) || basicBlock.matchType(TYPE_END|TYPE_SWITCH_BREAK|TYPE_LOOP_START|TYPE_LOOP_CONTINUE|TYPE_LOOP_END|TYPE_RETURN)) {
            return null;
        }
        BasicBlock result;

        visited.set(basicBlock.getIndex());

        switch (basicBlock.getType()) {
            case TYPE_DELETED:
                return basicBlock;
            case TYPE_SWITCH_DECLARATION:
            case TYPE_SWITCH:
                for (SwitchCase switchCase : basicBlock.getSwitchCases()) {
                    result = checkBasicBlock(visited, basicBlock, switchCase.getBasicBlock(), watchdog);
                    if (result != null)
                        return result;
                }
                return null;
            case TYPE_TRY:
            case TYPE_TRY_JSR:
                result = checkBasicBlock(visited, basicBlock, basicBlock.getSub1(), watchdog);
                if (result != null)
                    return result;
            case TYPE_TRY_DECLARATION:
                for (ExceptionHandler exceptionHandler : basicBlock.getExceptionHandlers()) {
                    result = checkBasicBlock(visited, basicBlock, exceptionHandler.getBasicBlock(), watchdog);
                    if (result != null)
                        return result;
                }
                return checkBasicBlock(visited, basicBlock, basicBlock.getNext(), watchdog);
            case TYPE_CONDITIONAL_BRANCH:
            case TYPE_JSR:
                result = checkBasicBlock(visited, basicBlock, basicBlock.getNext(), watchdog);
                if (result != null)
                    return result;
                return checkBasicBlock(visited, basicBlock, basicBlock.getBranch(), watchdog);
            case TYPE_IF_ELSE:
            case TYPE_TERNARY_OPERATOR:
                result = checkBasicBlock(visited, basicBlock, basicBlock.getSub2(), watchdog);
                if (result != null)
                    return result;
            case TYPE_IF:
                result = checkBasicBlock(visited, basicBlock, basicBlock.getCondition(), watchdog);
                if (result != null)
                    return result;
            case TYPE_LOOP:
                result = checkBasicBlock(visited, basicBlock, basicBlock.getSub1(), watchdog);
                if (result != null)
                    return result;
            case TYPE_START:
            case TYPE_STATEMENTS:
            case TYPE_GOTO:
                return checkBasicBlock(visited, basicBlock, basicBlock.getNext(), watchdog);
            default:
                return null;
        }
    }

    protected static BasicBlock checkBasicBlock(BitSet visited, BasicBlock parent, BasicBlock child, SilentWatchDog watchdog) {
        if (!child.matchType(BasicBlock.GROUP_END) && !watchdog.silentCheck(parent, child)) {
            return parent;
        }

        return checkBasicBlock(visited, child, watchdog);
    }

    protected static class SilentWatchDog extends WatchDog {
        public boolean silentCheck(BasicBlock parent, BasicBlock child) {
            if (!child.matchType(BasicBlock.GROUP_END)) {
                Link link = new Link(parent, child);

                if (links.contains(link)) {
                    return false;
                }

                links.add(link);
            }

            return true;
        }
    }

    protected InputStream getResource(String zipName) {
        return this.getClass().getResourceAsStream("/" + zipName);
    }

    protected InputStream loadFile(String zipName) throws IOException {
        return new FileInputStream(zipName);
    }

    protected Method searchMethod(String internalTypeName, String methodName) throws Exception {
        return searchMethod(loader, typeMaker, internalTypeName, methodName, null);
    }

    protected Method searchMethod(InputStream is, String internalTypeName, String methodName) throws Exception {
        return searchMethod(is, internalTypeName, methodName, null);
    }

    protected Method searchMethod(InputStream is, String internalTypeName, String methodName, String methodDescriptor) throws Exception {
        if (is == null) {
            return null;
        }
        ZipLoader loader = new ZipLoader(is);
        TypeMaker typeMaker = new TypeMaker(loader);
        return searchMethod(loader, typeMaker, internalTypeName, methodName, methodDescriptor);
    }

    protected Method searchMethod(Loader loader, TypeMaker typeMaker, String internalTypeName, String methodName, String methodDescriptor) throws Exception {
        return MethodUtil.searchMethod(loader, typeMaker, internalTypeName, methodName, methodDescriptor);
    }
}
