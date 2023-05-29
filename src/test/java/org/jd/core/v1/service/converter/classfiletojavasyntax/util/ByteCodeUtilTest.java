package org.jd.core.v1.service.converter.classfiletojavasyntax.util;

import org.junit.Test;

import static org.apache.bcel.Const.ALOAD;
import static org.apache.bcel.Const.IFEQ;
import static org.apache.bcel.Const.IFGE;
import static org.apache.bcel.Const.IFGT;
import static org.apache.bcel.Const.IFLE;
import static org.apache.bcel.Const.IFLT;
import static org.apache.bcel.Const.IFNE;
import static org.apache.bcel.Const.IFNONNULL;
import static org.apache.bcel.Const.IFNULL;
import static org.apache.bcel.Const.IF_ACMPEQ;
import static org.apache.bcel.Const.IF_ACMPNE;
import static org.apache.bcel.Const.IF_ICMPEQ;
import static org.apache.bcel.Const.IF_ICMPGE;
import static org.apache.bcel.Const.IF_ICMPGT;
import static org.apache.bcel.Const.IF_ICMPLE;
import static org.apache.bcel.Const.IF_ICMPLT;
import static org.apache.bcel.Const.IF_ICMPNE;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;

public class ByteCodeUtilTest {

    @Test
    public void testGetOppositeOpCode() {
        assertEquals(IFNULL, ByteCodeUtil.getOppositeOpCode(IFNONNULL));
        assertEquals(IFNONNULL, ByteCodeUtil.getOppositeOpCode(IFNULL));
        assertEquals(IF_ACMPNE, ByteCodeUtil.getOppositeOpCode(IF_ACMPEQ));
        assertEquals(IF_ACMPEQ, ByteCodeUtil.getOppositeOpCode(IF_ACMPNE));
        assertEquals(IF_ICMPNE, ByteCodeUtil.getOppositeOpCode(IF_ICMPEQ));
        assertEquals(IF_ICMPEQ, ByteCodeUtil.getOppositeOpCode(IF_ICMPNE));
        assertEquals(IF_ICMPLT, ByteCodeUtil.getOppositeOpCode(IF_ICMPGE));
        assertEquals(IF_ICMPGE, ByteCodeUtil.getOppositeOpCode(IF_ICMPLT));
        assertEquals(IF_ICMPGT, ByteCodeUtil.getOppositeOpCode(IF_ICMPLE));
        assertEquals(IF_ICMPLE, ByteCodeUtil.getOppositeOpCode(IF_ICMPGT));
        assertEquals(IFNE, ByteCodeUtil.getOppositeOpCode(IFEQ));
        assertEquals(IFEQ, ByteCodeUtil.getOppositeOpCode(IFNE));
        assertEquals(IFLT, ByteCodeUtil.getOppositeOpCode(IFGE));
        assertEquals(IFGE, ByteCodeUtil.getOppositeOpCode(IFLT));
        assertEquals(IFGT, ByteCodeUtil.getOppositeOpCode(IFLE));
        assertEquals(IFLE, ByteCodeUtil.getOppositeOpCode(IFGT));
        assertThrows(IllegalArgumentException.class, () -> ByteCodeUtil.getOppositeOpCode(ALOAD));
    }
}
