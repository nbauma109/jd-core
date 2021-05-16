package jd.instruction.bytecode.visitor;

import java.util.List;

import jd.instruction.bytecode.ByteCodeConstants;
import jd.instruction.bytecode.instruction.ANewArray;
import jd.instruction.bytecode.instruction.AThrow;
import jd.instruction.bytecode.instruction.ArrayLength;
import jd.instruction.bytecode.instruction.ArrayLoadInstruction;
import jd.instruction.bytecode.instruction.ArrayStoreInstruction;
import jd.instruction.bytecode.instruction.AssertInstruction;
import jd.instruction.bytecode.instruction.BinaryOperatorInstruction;
import jd.instruction.bytecode.instruction.CheckCast;
import jd.instruction.bytecode.instruction.ComplexConditionalBranchInstruction;
import jd.instruction.bytecode.instruction.ConvertInstruction;
import jd.instruction.bytecode.instruction.DupLoad;
import jd.instruction.bytecode.instruction.DupStore;
import jd.instruction.bytecode.instruction.GetField;
import jd.instruction.bytecode.instruction.IfCmp;
import jd.instruction.bytecode.instruction.IfInstruction;
import jd.instruction.bytecode.instruction.IncInstruction;
import jd.instruction.bytecode.instruction.InitArrayInstruction;
import jd.instruction.bytecode.instruction.InstanceOf;
import jd.instruction.bytecode.instruction.Instruction;
import jd.instruction.bytecode.instruction.InvokeInstruction;
import jd.instruction.bytecode.instruction.InvokeNew;
import jd.instruction.bytecode.instruction.InvokeNoStaticInstruction;
import jd.instruction.bytecode.instruction.LookupSwitch;
import jd.instruction.bytecode.instruction.MonitorEnter;
import jd.instruction.bytecode.instruction.MonitorExit;
import jd.instruction.bytecode.instruction.MultiANewArray;
import jd.instruction.bytecode.instruction.NewArray;
import jd.instruction.bytecode.instruction.Pop;
import jd.instruction.bytecode.instruction.PutField;
import jd.instruction.bytecode.instruction.PutStatic;
import jd.instruction.bytecode.instruction.ReturnInstruction;
import jd.instruction.bytecode.instruction.StoreInstruction;
import jd.instruction.bytecode.instruction.TableSwitch;
import jd.instruction.bytecode.instruction.TernaryOpStore;
import jd.instruction.bytecode.instruction.UnaryOperatorInstruction;


/*
 * Utilis� par TernaryOpReconstructor
 */
public class SearchDupLoadInstructionVisitor 
{
	public static DupLoad visit(Instruction instruction, DupStore dupStore)
	{		
		switch (instruction.opcode)
		{
		case ByteCodeConstants.ARRAYLENGTH:
			return visit(((ArrayLength)instruction).arrayref, dupStore);
		case ByteCodeConstants.ARRAYLOAD:
			{
				ArrayLoadInstruction ali = (ArrayLoadInstruction)instruction;
				DupLoad dupLoad = visit(ali.arrayref, dupStore);
				if (dupLoad != null)
					return dupLoad;
				return visit(ali.indexref, dupStore);
			}
		case ByteCodeConstants.AASTORE:
		case ByteCodeConstants.ARRAYSTORE:
			return visit(((ArrayStoreInstruction)instruction).arrayref, dupStore);
		case ByteCodeConstants.ASSERT:
			{
				AssertInstruction ai = (AssertInstruction)instruction;
				DupLoad dupLoad = visit(ai.test, dupStore);
				if (dupLoad != null)
					return dupLoad;
				if (ai.msg == null)
					return null;
				return visit(ai.msg, dupStore);
			}
		case ByteCodeConstants.ATHROW:
			return visit(((AThrow)instruction).value, dupStore);
		case ByteCodeConstants.UNARYOP:
			return visit(((UnaryOperatorInstruction)instruction).value, dupStore);
		case ByteCodeConstants.BINARYOP:
		case ByteCodeConstants.ASSIGNMENT:
			{
				BinaryOperatorInstruction boi = 
					(BinaryOperatorInstruction)instruction;
				DupLoad dupLoad = visit(boi.value1, dupStore);
				if (instruction != null)
					return dupLoad;
				return visit(boi.value2, dupStore);
			}
		case ByteCodeConstants.CHECKCAST:
			return visit(((CheckCast)instruction).objectref, dupStore);
		case ByteCodeConstants.STORE:
		case ByteCodeConstants.ASTORE:
		case ByteCodeConstants.ISTORE:
			return visit(((StoreInstruction)instruction).valueref, dupStore);
		case ByteCodeConstants.DUPLOAD:
			if (((DupLoad)instruction).dupStore == dupStore)
				return (DupLoad)instruction;
			break;
		case ByteCodeConstants.DUPSTORE:
			return visit(((DupStore)instruction).objectref, dupStore);
		case ByteCodeConstants.CONVERT:
			return visit(((ConvertInstruction)instruction).value, dupStore);
		case ByteCodeConstants.IFCMP:
			{
				IfCmp ifCmp = (IfCmp)instruction;
				DupLoad dupLoad = visit(ifCmp.value1, dupStore);
				if (dupLoad != null)
					return dupLoad;
				return visit(ifCmp.value2, dupStore);
			}
		case ByteCodeConstants.IF:
		case ByteCodeConstants.IFXNULL:
			return visit(((IfInstruction)instruction).value, dupStore);
		case ByteCodeConstants.COMPLEXIF:
			{
				List<Instruction> branchList = 
					((ComplexConditionalBranchInstruction)instruction).instructions;
				for (int i=branchList.size()-1; i>=0; --i)
				{
					DupLoad dupLoad = visit(branchList.get(i), dupStore);
					if (dupLoad != null)
						return dupLoad;
				}
			}
			break;
		case ByteCodeConstants.INSTANCEOF:
			return visit(((InstanceOf)instruction).objectref, dupStore);
		case ByteCodeConstants.INVOKEINTERFACE:
		case ByteCodeConstants.INVOKESPECIAL:
		case ByteCodeConstants.INVOKEVIRTUAL:
			{
				DupLoad dupLoad = visit(
					((InvokeNoStaticInstruction)instruction).objectref, dupStore);
				if (dupLoad != null)
					return dupLoad;
			}
		case ByteCodeConstants.INVOKESTATIC:
			{
				List<Instruction> list = ((InvokeInstruction)instruction).args;
				for (int i=list.size()-1; i>=0; --i)
				{
					DupLoad dupLoad = visit(list.get(i), dupStore);
					if (dupLoad != null)
						return dupLoad;
				}
			}
			break;
		case ByteCodeConstants.INVOKENEW:
			{
				List<Instruction> list = ((InvokeNew)instruction).args;
				for (int i=list.size()-1; i>=0; --i)
				{
					DupLoad dupLoad = visit(list.get(i), dupStore);
					if (dupLoad != null)
						return dupLoad;
				}
			}
			break;
		case ByteCodeConstants.LOOKUPSWITCH:
			return visit(((LookupSwitch)instruction).key, dupStore);
		case ByteCodeConstants.MONITORENTER:
			return visit(((MonitorEnter)instruction).objectref, dupStore);
		case ByteCodeConstants.MONITOREXIT:
			return visit(((MonitorExit)instruction).objectref, dupStore);
		case ByteCodeConstants.MULTIANEWARRAY:
			{
				Instruction[] dimensions = ((MultiANewArray)instruction).dimensions;
				for (int i=dimensions.length-1; i>=0; --i)
				{
					DupLoad dupLoad = visit(dimensions[i], dupStore);
					if (dupLoad != null)
						return dupLoad;
				}
			}
			break;
		case ByteCodeConstants.NEWARRAY:
			return visit(((NewArray)instruction).dimension, dupStore);
		case ByteCodeConstants.ANEWARRAY:
			return visit(((ANewArray)instruction).dimension, dupStore);
		case ByteCodeConstants.POP:
			return visit(((Pop)instruction).objectref, dupStore);
		case ByteCodeConstants.PUTFIELD: 
			{
				PutField putField = (PutField)instruction;
				DupLoad dupLoad = visit(putField.objectref, dupStore);
				if (dupLoad != null)
					return dupLoad;
				return visit(putField.valueref, dupStore);
			}
		case ByteCodeConstants.PUTSTATIC:
			return visit(((PutStatic)instruction).valueref, dupStore);
		case ByteCodeConstants.XRETURN:
			return visit(((ReturnInstruction)instruction).valueref, dupStore);
		case ByteCodeConstants.TABLESWITCH:
			return visit(((TableSwitch)instruction).key, dupStore);
		case ByteCodeConstants.TERNARYOPSTORE:
			return visit(((TernaryOpStore)instruction).objectref, dupStore);
		case ByteCodeConstants.PREINC:			
		case ByteCodeConstants.POSTINC:	
			return visit(((IncInstruction)instruction).value, dupStore);
		case ByteCodeConstants.GETFIELD:
			return visit(((GetField)instruction).objectref, dupStore);
		case ByteCodeConstants.INITARRAY:
		case ByteCodeConstants.NEWANDINITARRAY:
			{
				InitArrayInstruction iai = (InitArrayInstruction)instruction;
				DupLoad dupLoad = visit(iai.newArray, dupStore);
				if (dupLoad != null)
					return dupLoad;
				if (iai.values != null)
					return visit(iai.values, dupStore);
			}
			break;
		case ByteCodeConstants.ACONST_NULL:
		case ByteCodeConstants.LOAD:
		case ByteCodeConstants.ALOAD:
		case ByteCodeConstants.ILOAD:
		case ByteCodeConstants.BIPUSH:
		case ByteCodeConstants.ICONST:
		case ByteCodeConstants.LCONST:
		case ByteCodeConstants.FCONST:
		case ByteCodeConstants.DCONST:
		case ByteCodeConstants.GETSTATIC:
		case ByteCodeConstants.OUTERTHIS:
		case ByteCodeConstants.GOTO:
		case ByteCodeConstants.IINC:			
		case ByteCodeConstants.JSR:			
		case ByteCodeConstants.LDC:
		case ByteCodeConstants.LDC2_W:
		case ByteCodeConstants.NEW:
		case ByteCodeConstants.NOP:
		case ByteCodeConstants.SIPUSH:
		case ByteCodeConstants.RET:
		case ByteCodeConstants.RETURN:
		case ByteCodeConstants.EXCEPTIONLOAD:
		case ByteCodeConstants.RETURNADDRESSLOAD:
			break;
		default:
			System.err.println(
					"Can not search DupLoad instruction in " + 
					instruction.getClass().getName() + 
					", opcode=" + instruction.opcode);
		}
		
		return null;
	}

	private static DupLoad visit(
		List<Instruction> instructions, DupStore dupStore)
	{
		for (int i=instructions.size()-1; i>=0; --i)
		{
			DupLoad dupLoad = visit(instructions.get(i), dupStore);
			if (dupLoad != null)
				return dupLoad;
		}
		
		return null;
	}
}
