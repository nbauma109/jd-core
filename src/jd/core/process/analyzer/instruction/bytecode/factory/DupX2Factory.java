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

import java.util.List;
import java.util.Stack;

import jd.core.model.classfile.ClassFile;
import jd.core.model.classfile.Method;
import jd.core.model.instruction.bytecode.ByteCodeConstants;
import jd.core.model.instruction.bytecode.instruction.DupStore;
import jd.core.model.instruction.bytecode.instruction.Instruction;


public class DupX2Factory extends InstructionFactory
{
	public int create(
			ClassFile classFile, Method method, List<Instruction> list, 
			List<Instruction> listForAnalyze, 
			Stack<Instruction> stack, byte[] code, int offset, 
			int lineNumber, boolean[] jumps)
	{
		final int opcode = code[offset] & 255;
		Instruction i1 = stack.pop();
		Instruction i2 = stack.pop();

		DupStore dupStore1 = new DupStore(
			ByteCodeConstants.DUPSTORE, offset, lineNumber, i1);
		
		list.add(dupStore1);	

		String signature2 = i2.getReturnedSignature(
				classFile.getConstantPool(), null);
		
		if ("J".equals(signature2) || "D".equals(signature2))
		{
			// ..., value2, value1 => ..., value1, value2, value1
			stack.push(dupStore1.getDupLoad1());
			stack.push(i2);
			stack.push(dupStore1.getDupLoad2());			
		}
		else
		{
			// ..., value3, value2, value1 => ..., value1, value3, value2, value1
			Instruction i3 = stack.pop();

			stack.push(dupStore1.getDupLoad1());
			stack.push(i3);
			stack.push(i2);
			stack.push(dupStore1.getDupLoad2());
		}	
		
		return ByteCodeConstants.NO_OF_OPERANDS[opcode];
	}
}