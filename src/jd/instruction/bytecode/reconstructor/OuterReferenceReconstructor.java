package jd.instruction.bytecode.reconstructor;

import java.util.HashMap;
import java.util.List;

import jd.classfile.ClassFile;
import jd.classfile.ConstantPool;
import jd.classfile.Method;
import jd.classfile.analyzer.SignatureAnalyzer;
import jd.instruction.bytecode.ByteCodeConstants;
import jd.instruction.bytecode.instruction.Instruction;
import jd.instruction.bytecode.visitor.OuterGetFieldVisitor;
import jd.instruction.bytecode.visitor.OuterGetStaticVisitor;
import jd.instruction.bytecode.visitor.OuterInvokeMethodVisitor;
import jd.instruction.bytecode.visitor.OuterPutFieldVisitor;
import jd.instruction.bytecode.visitor.OuterPutStaticVisitor;
import jd.instruction.bytecode.visitor.ReplaceMultipleOuterReferenceVisitor;
import jd.instruction.bytecode.visitor.ReplaceOuterAccessorVisitor;
import jd.instruction.bytecode.visitor.ReplaceOuterReferenceVisitor;


/*
 * Recontruction des references externes dans le corps des methodes des classes
 * internes.
 */
public class OuterReferenceReconstructor 
{
	private ClassFile classFile;
	
	private ReplaceOuterReferenceVisitor outerReferenceVisitor;
	private ReplaceMultipleOuterReferenceVisitor multipleOuterReference;
	private ReplaceOuterAccessorVisitor outerAccessorVisitor;
	
	private OuterGetStaticVisitor outerGetStaticVisitor;
	private OuterPutStaticVisitor outerPutStaticVisitor;
	private OuterGetFieldVisitor outerGetFieldVisitor;
	private OuterPutFieldVisitor outerPutFieldVisitor;
	private OuterInvokeMethodVisitor outerMethodVisitor;

	
	public OuterReferenceReconstructor(
		HashMap<String, ClassFile> innerClassesMap, ClassFile classFile)
	{
		this.classFile = classFile;
		
		ConstantPool constants = classFile.getConstantPool();
		
		// Initialisation des visiteurs traitant les references des classes externes
		this.outerReferenceVisitor = new ReplaceOuterReferenceVisitor(
			ByteCodeConstants.ALOAD, 1, 
			CreateOuterThisInstructionIndex(classFile));
		this.multipleOuterReference = 
			new ReplaceMultipleOuterReferenceVisitor(classFile);
		this.outerAccessorVisitor = 
			new ReplaceOuterAccessorVisitor(classFile);
		// Initialisation des visiteurs traitant l'acces des champs externes 
		this.outerGetFieldVisitor = 
			new OuterGetFieldVisitor(innerClassesMap, constants);
		this.outerPutFieldVisitor = 
			new OuterPutFieldVisitor(innerClassesMap, constants);
		// Initialisation du visiteur traitant l'acces des champs statics externes 
		this.outerGetStaticVisitor = 
			new OuterGetStaticVisitor(innerClassesMap, constants);
		this.outerPutStaticVisitor = 
			new OuterPutStaticVisitor(innerClassesMap, constants);
		// Initialisation du visiteur traitant l'acces des methodes externes 
		this.outerMethodVisitor = 
			new OuterInvokeMethodVisitor(innerClassesMap, constants);
	}
	
	public void reconstruct(
		Method method, List<Instruction> list)
	{
		// Inner no static class file
		if (classFile.getOuterThisField() != null)
		{			
			// Replace outer reference parameter of constructors
			ConstantPool constants = classFile.getConstantPool();
			if (method.name_index == constants.instanceConstructorIndex)
				this.outerReferenceVisitor.visit(list);
			// Replace multiple outer references
			this.multipleOuterReference.visit(list);
			// Replace static call to "OuterClass access$0(InnerClass)" methods.
			this.outerAccessorVisitor.visit(list);
		}
		
    	// Replace outer field accessors
		this.outerGetFieldVisitor.visit(list);
		this.outerPutFieldVisitor.visit(list);
    	// Replace outer static field accessors
		this.outerGetStaticVisitor.visit(list);
		this.outerPutStaticVisitor.visit(list);
    	// Replace outer methods accessors
		this.outerMethodVisitor.visit(list);	
	}

	// Creation d'une nouvelle constante de type 'Fieldref', dans le 
	// pool, permettant l'affichage de 'OuterClass.this.'
	private static int CreateOuterThisInstructionIndex(ClassFile classFile)
	{
		if (classFile.getOuterClass() == null)
			return 0;
		
		String internalOuterClassName = 
			classFile.getOuterClass().getInternalClassName();
		String outerClassName = 
			SignatureAnalyzer.GetInnerName(internalOuterClassName);
				
		ConstantPool constants = classFile.getConstantPool();
		
		int signatureIndex = constants.addConstantUtf8(outerClassName);
		int classIndex = constants.addConstantClass(signatureIndex);
		int thisIndex = constants.thisLocalVariableNameIndex;
		int descriptorIndex = 
			constants.addConstantUtf8(internalOuterClassName);
		int nameAndTypeIndex = constants.addConstantNameAndType(
			thisIndex, descriptorIndex);
		
		return constants.addConstantFieldref(classIndex, nameAndTypeIndex);
	}
}
