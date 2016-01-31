package org.usman.SPROJ;

import java.util.ArrayList;
import java.util.List;
import java.util.HashSet;
import java.util.Set;
import java.util.Arrays;

import java.io.File;
import java.io.FileReader;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.FileNotFoundException;

import java.nio.file.Files;
import java.nio.file.Path;


import org.jf.dexlib2.iface.ClassDef;
import org.jf.dexlib2.iface.instruction.Instruction;
import org.jf.dexlib2.iface.instruction.ReferenceInstruction;
import org.jf.dexlib2.iface.TryBlock;
import org.jf.dexlib2.iface.ExceptionHandler;
import org.jf.dexlib2.iface.Method;
import org.jf.dexlib2.iface.MethodImplementation;
import org.jf.dexlib2.dexbacked.DexBackedDexFile;
import org.jf.dexlib2.DexFileFactory;
import org.jf.util.ExceptionWithContext;

import org.jf.dexlib2.dexbacked.instruction.*;
import org.jf.dexlib2.iface.instruction.*;
import org.jf.dexlib2.dexbacked.instruction.DexBackedInstruction;
import org.jf.dexlib2.dexbacked.reference.*;
import org.jf.dexlib2.iface.reference.*;
import org.jf.dexlib2.ReferenceType;
import org.jf.dexlib2.iface.instruction.ThreeRegisterInstruction;
import org.jf.dexlib2.iface.instruction.TwoRegisterInstruction;
import org.jf.dexlib2.iface.instruction.OneRegisterInstruction;
import org.jf.dexlib2.Format;
import java.util.*;
import java.lang.*;
import org.jf.dexlib2.iface.instruction.formats.*;


import org.jf.dexlib2.*;
import org.jf.dexlib2.Opcode;



public class Analyzer {
	public static ArrayList<TreeSet<Object>> getSourceAndDest (BasicBlockInstruction _instruction) {
		// Give the source/destination operands of an instruction 
		// need to check if it works for arrays
		Format format = _instruction.instruction.getOpcode().format;
		Opcode opcode = _instruction.instruction.getOpcode();
		ArrayList<TreeSet<Object>> retval = new ArrayList<TreeSet<Object>>();  // index 0 is source 1 is dest
		retval.add(new TreeSet<Object>());
		retval.add(new TreeSet<Object>());
		retval.add(new TreeSet<Object>());
		switch (format) {
			// leave these here for reference. might need some of them later
			case Format10t:  // goto
			case Format10x:  // nop, return-void
			case Format20bc:  // throw-verification-error
			case Format20t:  // goto/16
			case Format21t:  // branch
			case Format22cs:  // no such instr
			case Format22t:  // branch
			// case Format25x:  // no such instr
			case Format30t:  // goto
			case Format31t:  // packed-switch seems like a jump
			case Format35ms:  // no such ins
			case Format3rmi:
			case Format3rms:
			case Format35mi:  // no such ins
				return null;

			case Format11n:  // move literal getA
			case Format21ih:  // const getA
			case Format21lh:  // const getA
			case Format21s:  // const getA
			case Format31c:  // const string jumbo getA
			case Format31i:  // const
			case Format51l:
			{
				OneRegisterInstruction instruction = (OneRegisterInstruction)_instruction.instruction;
					retval.get(2).add(instruction.getRegisterA());
				break;
			}

			case Format12x:
			{
				TwoRegisterInstruction instruction = (TwoRegisterInstruction)_instruction.instruction;
				if (opcode.name.contains("/2addr")) {  // both source, 1st is dest
					retval.get(0).add(instruction.getRegisterA());
					retval.get(0).add(instruction.getRegisterB());
					retval.get(1).add(instruction.getRegisterA());
				} else if (opcode.name.equals("move-wide") || opcode.name.equals("move") || opcode.name.equals("move-object")) {  // 1st dest 2ndsource
					retval.get(0).add(instruction.getRegisterB());
					retval.get(1).add(instruction.getRegisterA());
				} else if (opcode.setsWideRegister()) {  // both source and both dest
					retval.get(0).add(instruction.getRegisterA());
					retval.get(0).add(instruction.getRegisterB());
					retval.get(1).add(instruction.getRegisterA());
					retval.get(1).add(instruction.getRegisterB());
				} else {  // 1st dest 2ndsource
					retval.get(0).add(instruction.getRegisterB());
					retval.get(1).add(instruction.getRegisterA());
				}
				break;
			}
			case Format21c:  // static field
			{
				DexBackedInstruction21c instruction = (DexBackedInstruction21c)_instruction.instruction;
				// sget 1st is dest, 2nd is source  getReference()
				// sput 1st is source, 2nd is dest
				// opcode.name.contains("wide") r+1 is used too
				if (opcode.name.contains("const")) {
					retval.get(2).add(instruction.getRegisterA());
					break;
				}
				if (opcode.name.contains("sget")) {
					retval.get(0).add(instruction.getReference().hashCode());
					retval.get(1).add(instruction.getRegisterA());
					if (opcode.name.contains("wide")) {
						retval.get(1).add(instruction.getRegisterA() + 1);
					}
				} else if (opcode.name.contains("sput")) {
					retval.get(0).add(instruction.getRegisterA());
					if (opcode.name.contains("wide")) {
						retval.get(0).add(instruction.getRegisterA() + 1);
					}
					retval.get(1).add(instruction.getReference().hashCode());
				}
				break;
			}
			case Format22c:  // instance field
			{
				DexBackedInstruction22c instruction = (DexBackedInstruction22c)_instruction.instruction;
				// same as above but also need to check for 3rd argument i.e. fields. 
				if (opcode.name.contains("const")) {
					break;
				}
				if (opcode.name.contains("iget")) {
					retval.get(0).add(instruction.getRegisterB());
					retval.get(1).add(instruction.getRegisterA());
					if (opcode.name.contains("wide")) {
						retval.get(1).add(instruction.getRegisterA() + 1);
					}
				} else if (opcode.name.contains("iput")) {
					retval.get(0).add(instruction.getRegisterA());
					if (opcode.name.contains("wide")) {
						retval.get(0).add(instruction.getRegisterA() + 1);
					}
					retval.get(1).add(instruction.getRegisterB());
				}
				break;
			}
			case Format22b:
			case Format22s:
			case Format22x:  // move 
			case Format32x:
			{
				// 2nd reg source 1st reg dest
				TwoRegisterInstruction instruction = (TwoRegisterInstruction)_instruction.instruction;
				retval.get(0).add(instruction.getRegisterB());
				retval.get(1).add(instruction.getRegisterA());
				break;
			}
			case Format23x:
			{
				ThreeRegisterInstruction instruction = (ThreeRegisterInstruction)_instruction.instruction;
				// if name starts with cmp then break
				if (opcode.name.startsWith("cmp")) {
					break;
				}
				if (opcode.name.contains("aget")) {  // 1st is dest, 2nd is source
					retval.get(0).add(instruction.getRegisterB());
					retval.get(1).add(instruction.getRegisterA());
				} else if (opcode.name.contains("aput")) {  // 1st is source, 2nd is dest
					retval.get(0).add(instruction.getRegisterA());
					retval.get(1).add(instruction.getRegisterB());
				} else {  // 1st dest 2nd and 3rd source
					retval.get(0).add(instruction.getRegisterB());
					retval.get(0).add(instruction.getRegisterC());
					retval.get(1).add(instruction.getRegisterA());
				}
				break;
			}
			// case Format11x:  // return something into a register

			// need to look in detail multiple args
			// case Format35c:   
			// case Format3rc:
			// {

			// 	break;
			// }

			default: return null;
		}
		return retval;
	} 

	public static List<Object> getVarThisFunctionTouches(BasicBlockInstruction instruction, BasicBlock basicblock) {
		Format format = instruction.instruction.getOpcode().format;
		Opcode opcode = instruction.instruction.getOpcode();
		List<Object> retval = new ArrayList<Object>();

		if (format == Format.Format35c) {  // function call
			FiveRegisterInstruction instr = (FiveRegisterInstruction)instruction.instruction;
			for (int i = 0;i<instr.getRegisterCount() ;++i ) {
				switch (i) {
					case 0: retval.add(instr.getRegisterC()); break;
					case 1: retval.add(instr.getRegisterD()); break;
					case 2: retval.add(instr.getRegisterE()); break;
					case 3: retval.add(instr.getRegisterF()); break;
					case 4: retval.add(instr.getRegisterG()); break;
				}
			}

			int i = basicblock.instructions.indexOf(instruction) + 1;
			// check for return call
			if (i < basicblock.instructions.size() && basicblock.instructions.get(i).instruction.getOpcode().format == Format.Format11x) {
				OneRegisterInstruction retInstr = (OneRegisterInstruction)basicblock.instructions.get(i).instruction;
				retval.add(retInstr.getRegisterA());
			}
		} else if (format == Format.Format3rc) {
			for (int i = 0;i<((Instruction3rc)instruction.instruction).getRegisterCount() ;++i ) {
				retval.add( ((Instruction3rc)instruction.instruction).getStartRegister()+i);
			}
			// System.out.println("************ "+((Instruction3rc)instruction.instruction).getRegisterCount() );
			// System.out.println("************ "+((Instruction3rc)instruction.instruction).getStartRegister() );
			int i = basicblock.instructions.indexOf(instruction) + 1;
			// check for return call
			if (i < basicblock.instructions.size() && basicblock.instructions.get(i).instruction.getOpcode().format == Format.Format11x) {
				OneRegisterInstruction retInstr = (OneRegisterInstruction)basicblock.instructions.get(i).instruction;
				retval.add(retInstr.getRegisterA());
			}
		}
		
		return retval;
	}
}