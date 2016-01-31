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

import com.google.common.collect.Ordering;

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

public class FunctionLeakFinder {
	public FunctionLeakFinder(Method method, ReturnStructure structure, String location) {

		ControlFlowGraph cfg = new ControlFlowGraph(method);
		List<BasicBlock> basicblocks = cfg.getBasicBlocks();
		for (BasicBlock bb : basicblocks) {
			bb.clearVisited();
		}
		
		LinkedList<BasicBlock> queue = new LinkedList<BasicBlock>();
		queue.add(basicblocks.get(0));
		basicblocks.get(0).setVisited();
		
		while (queue.size() != 0) {
			BasicBlock bb = queue.remove();
			try {
				taintAnalysis(bb, basicblocks, structure, location);
			} catch (Exception e) {
				// todo: catch execption
			}


	    	if (bb.outgoingEdges != 0) {
				LeakFinder.addChildrenToQueue(bb, basicblocks, queue);
			}
		}
	}
	public void taintAnalysis(BasicBlock basicblock, List<BasicBlock> basicblocks, ReturnStructure oldStr, String location) throws Exception{

		for (int i = 0; i<basicblock.instructions.size(); ++i) {
			BasicBlockInstruction ins = basicblock.instructions.get(i);
			if (ins.instruction.getOpcode().format == Format.Format11x) {  // return something

				OneRegisterInstruction retInstr = (OneRegisterInstruction)ins.instruction;
				if (oldStr.tanitedVarSet.contains((Object) retInstr.getRegisterA())) {
					oldStr.setRetValTainted();
				}
			} else if (ins.instruction.getOpcode().referenceType == 3) {  // function call

				List<Object> varsThisInstTouches = Analyzer.getVarThisFunctionTouches(ins, basicblock);
				Format format = ins.instruction.getOpcode().format;

				// check for return call
				if (i+1<basicblock.instructions.size()) {
					if (basicblock.instructions.get(i+1).instruction.getOpcode().format == Format.Format11x) {
						++i;  // skip the next instruction which is a return call
					}
				}

				boolean carriesTaint = false;
				if (format == Format.Format35c) {
					FiveRegisterInstruction r5instr = (FiveRegisterInstruction)ins.instruction;
					carriesTaint = ( oldStr.tanitedVarSet.contains((Object)r5instr.getRegisterC()) ) ? true : carriesTaint;
					for (int l = 0; l < r5instr.getRegisterCount(); ++l) {
						switch (l) {
							case 0: carriesTaint = ( oldStr.tanitedVarSet.contains((Object)r5instr.getRegisterC()) ) ? true : carriesTaint;
							case 1: carriesTaint = ( oldStr.tanitedVarSet.contains((Object)r5instr.getRegisterD()) ) ? true : carriesTaint;
							case 2: carriesTaint = ( oldStr.tanitedVarSet.contains((Object)r5instr.getRegisterE()) ) ? true : carriesTaint;
							case 3: carriesTaint = ( oldStr.tanitedVarSet.contains((Object)r5instr.getRegisterF()) ) ? true : carriesTaint;
							case 4: carriesTaint = ( oldStr.tanitedVarSet.contains((Object)r5instr.getRegisterG()) ) ? true : carriesTaint;
						}
					}
				} else if (format == Format.Format3rc) {
					Instruction3rc instr3rc = (Instruction3rc) ins.instruction;
					for (int l = 0; l < instr3rc.getRegisterCount(); ++l) {
						carriesTaint = ( oldStr.tanitedVarSet.contains( (Object)(instr3rc.getStartRegister()+i)) ) ? true : carriesTaint;
					}
				}


				ReferenceInstruction refIns = (ReferenceInstruction)ins.instruction;
				DexBackedMethodReference reference = (DexBackedMethodReference)refIns.getReference();
				String definingClass = reference.getDefiningClass();

				// if (reference.getName().equals("putString")) {
				// System.out.println(oldStr.method.getImplementation().getRegisterCount());
				// // // 	System.out.print(definingClass + " ");
				// // // 	System.out.println(reference.getName() + "   "+ins.instruction.getOpcode().format);
				// // // 	// System.out.println(classDef.getType());
				// // 	// System.out.println(r5instr.getRegisterCount() );
				// // // 	System.out.println(((Instruction3rc)ins.instruction).getStartRegister() );
				// 	System.out.print(((FiveRegisterInstruction)refIns).getRegisterC() );
				// 	System.out.print(" " + ((FiveRegisterInstruction)refIns).getRegisterD() );
				// 	System.out.println(" " + ((FiveRegisterInstruction)refIns).getRegisterE() );
				// // // 	// System.out.println(r5instr.getRegisterE() );
				// // // 	// System.out.println(r5instr.getRegisterF() );
				// // 	// System.out.println(r5instr.getRegisterG() );
				// // 	System.out.print("Tainted var :");
				// // 	for (Object oo : tanitedVarSet) {
				// // 		System.out.print(" "+ oo);
				// // 	}
				// // 	System.out.println("\n========================================================================");
				// }
				System.out.println("$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$");

				// check if this function call touches any of the tainted variables
				if (!carriesTaint) { continue; }
				// look for method


				// check if it is a sink
				boolean isSink = false;
				File sourceFile = new File("Android_4.2_Sinks.txt");
			    BufferedReader br = new BufferedReader(new FileReader(sourceFile));
			    String line;
			    while ((line = br.readLine()) != null) {
			    	// sink found
			    	String sink = InstructionFormater.getFormatedFunctionCall(ins);
			        if (line.startsWith(sink)) {
						System.out.println("\n\n\n****************LEAK FOUND:****************");
						System.out.println(location);
						// System.out.println("\nIn Class: "+definingClass+" In function: "+reference.getName());
						System.out.println("sink = " + line);
						isSink = true;
						break;
			        }
			    }
			    // todo: patch up
			    if (isSink) { continue; }  // no need to analyze this function

			    
			    // todo if cant find funcdefinition i.e. library function then no need to analyze this function tanitedVarSet.addAll(varsThisInstTouches);
				// look for method

				boolean functionDefFound = false;
				List<? extends ClassDef> classDefs = Ordering.natural().sortedCopy(SPROJ.FILE.getClasses());
				for(final ClassDef classDef: classDefs) {
					if (functionDefFound) break;
					if (!classDef.getType().startsWith(definingClass)) continue;
					if (!classDef.getType().startsWith(SPROJ.CLASS)) continue;

					for(Method method: classDef.getMethods()) {
						if (method.getName().equals(reference.getName())) {
							functionDefFound = true;
							ReturnStructure newStr = new ReturnStructure(oldStr.tanitedVarSet, ins, method);  // mapping
							new FunctionLeakFinder(method, newStr, location);
							if (newStr.isRetValTainted == true) {
								oldStr.tanitedVarSet.add(varsThisInstTouches.get(varsThisInstTouches.size()-1));
							}

							oldStr.tanitedVarSet.addAll(newStr.reverseMap());  // merge sets after doing reverse mapping
							break;
						}
					}
				}
				if (!functionDefFound) oldStr.tanitedVarSet.addAll(varsThisInstTouches);

			} else {  // this instruction is not a function call
				ArrayList<TreeSet<Object>> varsThisInstTouches = Analyzer.getSourceAndDest(ins);
				if (varsThisInstTouches == null) {
					continue;
				}
				// registers overwritten with a constant are no longer tainted
				for (Object o: varsThisInstTouches.get(2)) {
						oldStr.tanitedVarSet.remove(o);
				}

				if (varsThisInstTouches.get(0).size() == 0 || varsThisInstTouches.get(1).size() == 0) {
					continue;
				}

				boolean taintedInstruction = false;
				for (Object o : oldStr.tanitedVarSet) {
					if (varsThisInstTouches.get(0).contains(o)) {
						oldStr.tanitedVarSet.addAll(varsThisInstTouches.get(1));
						taintedInstruction = true;
						break;
					}
				}

				// destination is overwritten
				if (taintedInstruction == false) {
					oldStr.tanitedVarSet.removeAll(varsThisInstTouches.get(1));
				}
			}
		}
	}
}

class ReturnStructure {
	boolean isRetValTainted;
	boolean carriesTaint;
	TreeSet<Object> tanitedVarSet;
	FiveRegisterInstruction r5instr;
	Method method;
	ReturnStructure(TreeSet<Object> _tanitedVarSet, BasicBlockInstruction ins, Method _method) {
		isRetValTainted = false;
		carriesTaint = false;
		method = _method;
		tanitedVarSet = new TreeSet<Object> ();
		int numRegisters = method.getImplementation().getRegisterCount();
		// todo two type of function calls
		r5instr = (FiveRegisterInstruction)ins.instruction;
		for (int i = 0;i<r5instr.getRegisterCount() ;++i ) {
			switch (i) {
				case 0 : {
					Object register = r5instr.getRegisterC();
					if ( _tanitedVarSet.contains( register ) ) {
						tanitedVarSet.add(1);
						carriesTaint = true;
					}
					break;
				}
				case 1 : {
					Object register = r5instr.getRegisterD();
					if ( _tanitedVarSet.contains( register ) ) {
						tanitedVarSet.add(2);
						carriesTaint = true;
					}
					break;
				}
				case 2 : {
					Object register = r5instr.getRegisterE();
					if ( _tanitedVarSet.contains( register ) ) {
						tanitedVarSet.add(3);
						carriesTaint = true;
					}
					break;
				}
				case 3 : {
					Object register = r5instr.getRegisterF();
					if ( _tanitedVarSet.contains( register ) ) {
						tanitedVarSet.add(4);
						carriesTaint = true;
					}
					break;
				}
				case 4 : {
					Object register = r5instr.getRegisterG();
					if ( _tanitedVarSet.contains( register ) ) {
						tanitedVarSet.add(5);
						carriesTaint = true;
					}
					break;
				}
				default:
					break;
			}
		}
	}
	public void setRetValTainted() {
		isRetValTainted = true;
	}
	public TreeSet<Object> reverseMap() {
		TreeSet<Object> retSet = new TreeSet<Object> ();
		for (int i = 0;i<r5instr.getRegisterCount() ;++i ) {
			switch (i) {
				case 0 : {
					if ( tanitedVarSet.contains( (Object)1 ) ) {
						retSet.add(r5instr.getRegisterC());
					}
					break;
				}
				case 1 : {
					if ( tanitedVarSet.contains( (Object)2 ) ) {
						retSet.add(r5instr.getRegisterD());
					}
					break;
				}
				case 2 : {
					if ( tanitedVarSet.contains( (Object)3 ) ) {
						retSet.add(r5instr.getRegisterE());
					}
					break;
				}
				case 3 : {
					if ( tanitedVarSet.contains( (Object)4 ) ) {
						retSet.add(r5instr.getRegisterF());
					}
					break;
				}
				case 4 : {
					if ( tanitedVarSet.contains( (Object)5 ) ) {
						retSet.add(r5instr.getRegisterG());
					}
					break;
				}
			}
		}
		return retSet;
	}
}

