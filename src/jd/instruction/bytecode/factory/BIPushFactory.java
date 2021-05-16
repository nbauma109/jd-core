package jd.instruction.bytecode.factory;

import java.util.List;
import java.util.Stack;

import jd.classfile.ClassFile;
import jd.classfile.Method;
import jd.instruction.bytecode.ByteCodeConstants;
import jd.instruction.bytecode.instruction.BIPush;
import jd.instruction.bytecode.instruction.Instruction;


public class BIPushFactory extends InstructionFactory
{
	public int create(
			ClassFile classFile, Method method, List<Instruction> list,
			List<Instruction> listForAnalyze,
            Stack<Instruction> stack, byte[] code, int offset, 
			int lineNumber, boolean[] jumps)
	{
		final int opcode = code[offset] & 255;

		stack.push(new BIPush(
			opcode, offset, lineNumber, (byte)(code[offset+1] & 255)));
		
		return ByteCodeConstants.NO_OF_OPERANDS[opcode];
	}
}
