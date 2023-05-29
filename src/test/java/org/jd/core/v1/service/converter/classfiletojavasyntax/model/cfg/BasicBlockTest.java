package org.jd.core.v1.service.converter.classfiletojavasyntax.model.cfg;

import org.apache.bcel.classfile.Attribute;
import org.apache.bcel.classfile.Code;
import org.apache.bcel.classfile.Constant;
import org.apache.bcel.classfile.ConstantPool;
import org.apache.bcel.classfile.Method;
import org.junit.Test;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

public class BasicBlockTest {

    @Test
    public void testFlip() throws Exception {
        byte[] bytecode = { 25, 5, 18, 111, -74, 0, 112, -103, 0, -45 };
        byte[] expected = { 25, 5, 18, 111, -74, 0, 112, -102, -1, -7 };
        testFlip(bytecode, expected);
    }

    @Test
    public void testFlipEmpty() throws Exception {
        byte[] bytecode = {};
        byte[] expected = {};
        testFlip(bytecode, expected);
    }

    private static void testFlip(byte[] bytecode, byte[] expected) {
        Constant[] constants = {};
        ConstantPool cp = new ConstantPool(constants);
        Code attributeCode = new Code(0, bytecode.length, 0, 0, bytecode, null, null, cp);
        Method method = new Method(0, 0, 0, new Attribute[] { attributeCode }, cp);
        ControlFlowGraph cfg = new ControlFlowGraph(method);
        BasicBlock bb = cfg.newBasicBlock(0, bytecode.length);
        BasicBlock next = cfg.newBasicBlock(bb);
        BasicBlock branch = cfg.newBasicBlock(bb);
        bb.setNext(next);
        bb.setBranch(branch);
        bb.flip();
        assertEquals(next, bb.getBranch());
        assertEquals(branch, bb.getNext());
        assertArrayEquals(expected, bytecode);
    }
}
