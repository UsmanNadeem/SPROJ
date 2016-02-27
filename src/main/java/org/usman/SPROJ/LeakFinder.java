package org.usman.SPROJ;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.TreeSet;

import org.jf.dexlib2.Format;
import org.jf.dexlib2.Opcode;
import org.jf.dexlib2.dexbacked.DexBackedDexFile;
import org.jf.dexlib2.dexbacked.reference.DexBackedMethodReference;
import org.jf.dexlib2.iface.ClassDef;
import org.jf.dexlib2.iface.Method;
import org.jf.dexlib2.iface.instruction.FiveRegisterInstruction;
import org.jf.dexlib2.iface.instruction.ReferenceInstruction;
import org.jf.dexlib2.iface.instruction.formats.Instruction3rc;

import com.google.common.collect.Ordering;

public class LeakFinder {

	public static void findLeaks(DexBackedDexFile dexFile) throws IOException{

		List<? extends ClassDef> classDefs = Ordering.natural().sortedCopy(dexFile.getClasses());

		for(final ClassDef classDef: classDefs) {
			// dont want library functions.
			if (classDef.getType().startsWith("Landroid")) continue;
			for(Method method: classDef.getMethods()) {

				ControlFlowGraph cfg = new ControlFlowGraph(method);
				List<BasicBlock> basicblocks = cfg.getBasicBlocks();

				for (BasicBlock basicblock : basicblocks) {

					List<BasicBlockInstruction> instructions = basicblock.instructions;

					for (BasicBlockInstruction instruction : instructions) {

						// look for sinks which can be found by searching for function call instructions
						Opcode opcode = instruction.instruction.getOpcode();

						if(opcode.referenceType == 3) {  // 3 == Method reference type
							String possibleSourceSink = InstructionFormater.getFormatedFunctionCall(instruction);
							// match with list of sources
							File sourceFile = new File("Android_4.2_Sources.txt");
						    BufferedReader br = new BufferedReader(new FileReader(sourceFile));
						    String line;
						    while ((line = br.readLine()) != null) {
						    	// source found
						        if (line.startsWith(possibleSourceSink)) {
						        	String location = "In Class: "+classDef.getSourceFile()+" In function: "+method.getName()+ "\nSource: "+line;

									LinkedList<BasicBlock> queue = new LinkedList<BasicBlock>();
									queue.add(basicblock);
									for (BasicBlock bb : basicblocks) {
										bb.clearVisited();
										bb.clearTaints();
									}

									basicblock.setVisited();
									boolean firstTime = true;
									
									while (queue.size() != 0) {
										BasicBlock bb = queue.remove();
										if (firstTime) {
											firstTime = false;
											LeakFinder.searchForPathtoSink(instruction, bb, basicblocks, location);
										} else {
											LeakFinder.searchForPathtoSink(null, bb, basicblocks, location);
										}

							        	if (bb.outgoingEdges != 0) {
											LeakFinder.addChildrenToQueue(bb, basicblocks, queue);
										}
									}
									break;
						        }
						    }
						}
					}
				}
			}
		}
	}

	

	public static void searchForPathtoSink(BasicBlockInstruction srcInstruction, BasicBlock basicblock, 
		List<BasicBlock> basicblocks, String location) throws IOException{

		for (BasicBlock bb : basicblocks) {
			if (bb.destinations == null) continue;
			for (Integer destination:bb.destinations) {
				if (basicblock.startInstructionAddress==destination.intValue()) {
					basicblock.tanitedVarSet.addAll(bb.tanitedVarSet);
				}
			}
		}
		for (int i = (srcInstruction==null) ? 0 : basicblock.instructions.indexOf(srcInstruction); i<basicblock.instructions.size(); ++i) {
			BasicBlockInstruction ins = basicblock.instructions.get(i);

			if (ins.instruction.getOpcode().referenceType == 3) {  // function call
				// look for method
				ReferenceInstruction refIns = (ReferenceInstruction)ins.instruction;
				DexBackedMethodReference reference = (DexBackedMethodReference)refIns.getReference();
				String definingClass = reference.getDefiningClass();
				Format format = ins.instruction.getOpcode().format;

				List<Object> varsThisInstTouches = Analyzer.getVarThisFunctionTouches(ins, basicblock);

				// check for return call
				if (i+1<basicblock.instructions.size()) {
					if (basicblock.instructions.get(i+1).instruction.getOpcode().format == Format.Format11x) {
						++i;  // skip the next instruction which is a return call
					}
				}
				if (srcInstruction == ins) {  // this instruction is the source
					basicblock.tanitedVarSet.addAll(varsThisInstTouches);
					continue;
				}

				if (varsThisInstTouches.size()==0) { continue; }
				
				boolean carriesTaint = false;
				if (format == Format.Format35c) {
					FiveRegisterInstruction r5instr = (FiveRegisterInstruction)refIns;
					carriesTaint = ( basicblock.tanitedVarSet.contains((Object)r5instr.getRegisterC()) ) ? true : carriesTaint;
					for (int l = 0;l<r5instr.getRegisterCount() ;++l ) {
						switch (l) {
							case 0: carriesTaint = ( basicblock.tanitedVarSet.contains((Object)r5instr.getRegisterC()) ) ? true : carriesTaint;
							case 1: carriesTaint = ( basicblock.tanitedVarSet.contains((Object)r5instr.getRegisterD()) ) ? true : carriesTaint;
							case 2: carriesTaint = ( basicblock.tanitedVarSet.contains((Object)r5instr.getRegisterE()) ) ? true : carriesTaint;
							case 3: carriesTaint = ( basicblock.tanitedVarSet.contains((Object)r5instr.getRegisterF()) ) ? true : carriesTaint;
							case 4: carriesTaint = ( basicblock.tanitedVarSet.contains((Object)r5instr.getRegisterG()) ) ? true : carriesTaint;
						}
					}
				} else if (format == Format.Format3rc) {
					Instruction3rc instr3rc = (Instruction3rc) ins.instruction;
					for (int l = 0; l < instr3rc.getRegisterCount(); ++l) {
						carriesTaint = ( basicblock.tanitedVarSet.contains( (Object)(instr3rc.getStartRegister()+l)) ) ? true : carriesTaint;
					}
				}

				// check if this function call touches any of the tainted variables
				if (!carriesTaint) { continue; }

				// if (reference.getName().equals("write")) {
				// 	System.out.println(definingClass + " ");
				// // // 	System.out.println(reference.getName() + "   "+ins.instruction.getOpcode().format);
				// // // 	// System.out.println(classDef.getType());
				// // 	// System.out.println(r5instr.getRegisterCount() );
				// 	// System.out.println(((Instruction3rc)ins.instruction).getStartRegister() );
				// 	System.out.print(((FiveRegisterInstruction)refIns).getRegisterC() );
				// 	System.out.print(" " + ((FiveRegisterInstruction)refIns).getRegisterD() );
				// 	System.out.println(" " +((FiveRegisterInstruction)refIns).getRegisterE() );
				// 	System.out.print("Tainted var :");
				// 	for (Object oo : tanitedVarSet) {
				// 		System.out.print(oo+" ");
				// 	}
				// 	System.out.println("\n========================================================================");
				// }




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
						
						System.out.println("\n"+location.substring(location.indexOf("In Class:"), location.indexOf("Source:")-1));
						System.out.println("Sink = " + line);
						// System.out.println("In Class: "+classDef.getSourceFile()+" In function: "+method.getName());
						isSink = true;
						break;
			        }
			    }
			    // todo: patch up
			    if (isSink) { continue; }  // no need to analyze this function


			    // if cant find funcdefinition i.e. library function then no need to analyze this function tanitedVarSet.addAll(varsThisInstTouches);
				
				boolean functionDefFound = false;
				List<? extends ClassDef> classDefs = Ordering.natural().sortedCopy(SPROJ.FILE.getClasses());
				for(final ClassDef classDef: classDefs) {
					if (functionDefFound) break;
					if (!classDef.getType().startsWith(definingClass)) continue;
					if (classDef.getType().startsWith("Landroid")) continue;
					// if (reference.getName().equals("dummyFunctionForDEMO")) {
					// 	System.out.println(definingClass);
					// 	System.out.println(classDef.getType());
					// 	System.out.println(reference.getName());
					// 	System.out.println("========================================================================");
					// }
					for(Method method: classDef.getMethods()) {
						if (method.getName().equals(reference.getName()) && method.getImplementation() != null) {
							functionDefFound = true;
							ReturnStructure newStr = new ReturnStructure(basicblock.tanitedVarSet, ins, method);  // mapping
							new FunctionLeakFinder(method, newStr, location+"\nIn Class: "+classDef.getSourceFile()+" In function: "+
								method.getName());
							if (newStr.isRetValTainted == true) {
								basicblock.tanitedVarSet.add(varsThisInstTouches.get(varsThisInstTouches.size()-1));
							}
							basicblock.tanitedVarSet.addAll(newStr.reverseMap());  // merge sets after doing reverse mapping
							// System.out.println("\n\n\n*********** "+classDef.getType() + "----> "+ method.getName() +" ---> "+location );
							break;
						}
					}
				}
				if (!functionDefFound) basicblock.tanitedVarSet.addAll(varsThisInstTouches);

			} else {  // this instruction is not a function call
				ArrayList<TreeSet<Object>> varsThisInstTouches = Analyzer.getSourceAndDest(ins);
				if (varsThisInstTouches == null) {
					continue;
				}
				// registers overwritten with a constant are no longer tainted
				for (Object o: varsThisInstTouches.get(2)) {
						basicblock.tanitedVarSet.remove(o);
				}

				if (varsThisInstTouches.get(0).size() == 0 || varsThisInstTouches.get(1).size() == 0) {
					continue;
				}

				boolean taintedInstruction = false;
				for (Object o : basicblock.tanitedVarSet) {
					if (varsThisInstTouches.get(0).contains(o)) {
						basicblock.tanitedVarSet.addAll(varsThisInstTouches.get(1));
						taintedInstruction = true;
						break;
					}
				}

				// destination is overwritten
				if (taintedInstruction == false) {
					basicblock.tanitedVarSet.removeAll(varsThisInstTouches.get(1));
				}
			}
		}
	}
	public static void addChildrenToQueue(BasicBlock basicblock, List<BasicBlock> basicblocks, LinkedList<BasicBlock> queue ){
		for (Integer destination:basicblock.destinations) {
			for (BasicBlock bb:basicblocks) {
				if (!bb.visited && bb.startInstructionAddress==destination.intValue()) {
					bb.setVisited();
					queue.add(bb);
				}
			}
		}
	}
}
