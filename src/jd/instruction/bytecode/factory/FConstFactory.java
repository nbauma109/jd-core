package jd.instruction.bytecode.factory;

import java.util.List;
import java.util.Stack;

import jd.classfile.ClassFile;
import jd.classfile.Method;
import jd.instruction.bytecode.ByteCodeConstants;
import jd.instruction.bytecode.instruction.FConst;
import jd.instruction.bytecode.instruction.Instruction;


public class FConstFactory extends InstructionFactory
{
	public int create(
			ClassFile classFile, Method method, List<Instruction> list, 
			List<Instruction> listForAnalyze, 
			Stack<Instruction> stack, byte[] code, int offset, 
			int lineNumber, boolean[] jumps)
	{
		final int opcode = code[offset] & 255;
		final int index = opcode - ByteCodeConstants.FCONST_0;
		
		stack.push(new FConst(
			ByteCodeConstants.FCONST, offset, lineNumber, index));
		
		return ByteCodeConstants.NO_OF_OPERANDS[opcode];
	}
}
