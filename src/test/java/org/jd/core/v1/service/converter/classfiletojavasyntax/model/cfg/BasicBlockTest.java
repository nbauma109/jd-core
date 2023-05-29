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
        Method method = new Method();
        byte[] code =     { 25, 5, 18, 111, -74, 0, 112, -103, 0, -45 };
        byte[] expected = { 25, 5, 18, 111, -74, 0, 112, -102, -1, -7 };
        Constant[] constants = {};
        Code attributeCode = new Code(0, code.length, 0, 0, code, null, null, new ConstantPool(constants));
        method.setAttributes(new Attribute[] { attributeCode });
        ControlFlowGraph cfg = new ControlFlowGraph(method);
        BasicBlock bb = cfg.newBasicBlock(0, code.length);
        BasicBlock next = cfg.newBasicBlock(bb);
        BasicBlock branch = cfg.newBasicBlock(bb);
        bb.setNext(next);
        bb.setBranch(branch);
        bb.flip();
        assertEquals(next, bb.getBranch());
        assertEquals(branch, bb.getNext());
        assertArrayEquals(expected, code);
    }
}
