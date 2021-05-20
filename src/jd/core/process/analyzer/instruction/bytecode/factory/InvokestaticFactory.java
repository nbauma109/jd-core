/*******************************************************************************
 * Copyright (C) 2007-2019 Emmanuel Dupuy GPLv3
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package jd.core.process.analyzer.instruction.bytecode.factory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Stack;

import jd.core.model.classfile.ClassFile;
import jd.core.model.classfile.Method;
import jd.core.model.classfile.constant.ConstantMethodref;
import jd.core.model.instruction.bytecode.ByteCodeConstants;
import jd.core.model.instruction.bytecode.instruction.Instruction;
import jd.core.model.instruction.bytecode.instruction.Invokestatic;
import jd.core.util.InvalidParameterException;


public class InvokestaticFactory extends InstructionFactory
{
	public int create(
			ClassFile classFile, Method method, List<Instruction> list, 
			List<Instruction> listForAnalyze,  
			Stack<Instruction> stack, byte[] code, int offset, 
			int lineNumber, boolean[] jumps)
	{
		final int opcode = code[offset] & 255;
		final int index = ((code[offset+1] & 255) << 8) | (code[offset+2] & 255);
		
		ConstantMethodref cmr = 
			classFile.getConstantPool().getConstantMethodref(index);
		if (cmr == null)
			throw new InvalidParameterException(
					"Invalid ConstantMethodref index");
		
		int nbrOfParameters = cmr.getNbrOfParameters();
		ArrayList<Instruction> args = new ArrayList<Instruction>(nbrOfParameters);
		
		for (int i=nbrOfParameters; i>0; --i)
			args.add(stack.pop());
		
		Collections.reverse(args);
		
		final Instruction instruction = new Invokestatic(
				opcode, offset, lineNumber, index, args);
			
		if (cmr.returnAResult())
			stack.push(instruction);
		else
			list.add(instruction);
		
		listForAnalyze.add(instruction);
		
		return ByteCodeConstants.NO_OF_OPERANDS[opcode];
	}
}