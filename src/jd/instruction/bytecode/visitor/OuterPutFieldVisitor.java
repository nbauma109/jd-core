package jd.instruction.bytecode.visitor;

import java.util.HashMap;

import jd.Constants;
import jd.classfile.ClassFile;
import jd.classfile.ConstantPool;
import jd.classfile.accessor.Accessor;
import jd.classfile.accessor.PutFieldAccessor;
import jd.classfile.constant.ConstantMethodref;
import jd.classfile.constant.ConstantNameAndType;
import jd.instruction.bytecode.ByteCodeConstants;
import jd.instruction.bytecode.instruction.Instruction;
import jd.instruction.bytecode.instruction.Invokestatic;
import jd.instruction.bytecode.instruction.PutField;

/*
 * Replace 'TestInnerClass.access$0(this.this$0, 1)' 
 * par 'this.this$0.text = 1'
 */
public class OuterPutFieldVisitor extends OuterGetStaticVisitor
{
	public OuterPutFieldVisitor(
		HashMap<String, ClassFile> innerClassesMap, ConstantPool constants)
	{
		super(innerClassesMap, constants);
	}

	protected Accessor match(Instruction i)
	{
		if (i.opcode != ByteCodeConstants.INVOKESTATIC)		
			return null; 
		
		Invokestatic is = (Invokestatic)i;
		ConstantMethodref cmr = 
			constants.getConstantMethodref(is.index);
		ConstantNameAndType cnat = 
			constants.getConstantNameAndType(cmr.name_and_type_index);
		String descriptor = 
			constants.getConstantUtf8(cnat.descriptor_index);

		// Two parameters ?
		if (cmr.getNbrOfParameters() != 2)
			return null;

		String className = constants.getConstantClassName(cmr.class_index);
		ClassFile classFile = this.innerClassesMap.get(className);
		if (classFile == null)
			return null;
		
		String name = 
			constants.getConstantUtf8(cnat.name_index);
		
		Accessor accessor = classFile.getAccessor(name, descriptor);
		
		if ((accessor == null) ||
			(accessor.tag != Constants.ACCESSOR_PUTFIELD))
			return null;
		
		return (PutFieldAccessor)accessor;
	}
	
	protected Instruction newInstruction(Instruction i, Accessor a)
	{
		PutFieldAccessor pfa = (PutFieldAccessor)a;		
		Invokestatic is = (Invokestatic)i;
		
		int nameIndex = this.constants.addConstantUtf8(pfa.fieldName);
		int descriptorIndex = 
			this.constants.addConstantUtf8(pfa.fieldDescriptor);
		int cnatIndex = 
			this.constants.addConstantNameAndType(nameIndex, descriptorIndex);		
		
		int classNameIndex = this.constants.addConstantUtf8(pfa.className);	
		int classIndex = this.constants.addConstantClass(classNameIndex);
		
		int cfrIndex = 
			 this.constants.addConstantFieldref(classIndex, cnatIndex);
		
		Instruction valueref = is.args.remove(1);
		Instruction objectref = is.args.remove(0);
		
		return new PutField(
			ByteCodeConstants.PUTFIELD, i.offset, i.lineNumber, cfrIndex, 
			objectref, valueref);
	}
}
