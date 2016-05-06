package org.usman.SPROJ;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.TreeSet;

import org.jf.dexlib2.Format;
import org.jf.dexlib2.dexbacked.reference.DexBackedMethodReference;
import org.jf.dexlib2.iface.ClassDef;
import org.jf.dexlib2.iface.Method;
import org.jf.dexlib2.iface.instruction.FiveRegisterInstruction;
import org.jf.dexlib2.iface.instruction.OneRegisterInstruction;
import org.jf.dexlib2.iface.instruction.ReferenceInstruction;
import org.jf.dexlib2.iface.instruction.formats.Instruction3rc;

import com.google.common.collect.Ordering;

public class FunctionLeakFinder {
	public FunctionLeakFinder(Method method, ReturnStructure structure, String location) {

		ControlFlowGraph cfg = new ControlFlowGraph(method);
		List<BasicBlock> basicblocks = cfg.getBasicBlocks();
		for (BasicBlock bb : basicblocks) {
			bb.clearVisited();
			bb.clearTaints();
		}
		
		LinkedList<BasicBlock> queue = new LinkedList<BasicBlock>();
		queue.add(basicblocks.get(0));
		basicblocks.get(0).setVisited();
		basicblocks.get(0).setTanitedVarSet(structure.tanitedVarSet);
		
		while (queue.size() != 0) {
			BasicBlock bb = queue.remove();
			try {
				taintAnalysis(bb, basicblocks, structure, location);
			} catch (Exception e) {
				e.printStackTrace();
			}


	    	if (bb.outgoingEdges != 0) {
				LeakFinder.addChildrenToQueue(bb, basicblocks, queue);
			}
		}
	}
	public void taintAnalysis(BasicBlock basicblock, List<BasicBlock> basicblocks, ReturnStructure currentStr, String location) {

		for (BasicBlock bb : basicblocks) {
			if (bb.destinations == null) continue;
			for (Integer destination:bb.destinations) {
				if (basicblock.startInstructionAddress==destination.intValue()) {
					basicblock.tanitedVarSet.addAll(bb.tanitedVarSet);
				}
			}
		}

		for (int i = 0; i<basicblock.instructions.size(); ++i) {
			BasicBlockInstruction ins = basicblock.instructions.get(i);
			if (ins.instruction.getOpcode().format == Format.Format11x) {  // return something

				OneRegisterInstruction retInstr = (OneRegisterInstruction)ins.instruction;
				if (basicblock.tanitedVarSet.contains((Object) retInstr.getRegisterA())) {
					currentStr.setRetValTainted();
				}
			} else if (ins.instruction.getOpcode().referenceType == 3) {  // function call

				List<Object> varsThisInstTouches = Analyzer.getVarThisFunctionTouches(ins, basicblock);
				ReferenceInstruction refIns = (ReferenceInstruction)ins.instruction;
				DexBackedMethodReference reference = (DexBackedMethodReference)refIns.getReference();
				String definingClass = reference.getDefiningClass();
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
					carriesTaint = ( basicblock.tanitedVarSet.contains((Object)r5instr.getRegisterC()) ) ? true : carriesTaint;
					for (int l = 0; l < r5instr.getRegisterCount(); ++l) {
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


				// if (reference.getName().equals("write")) {
				// 	System.out.print(((FiveRegisterInstruction)refIns).getRegisterC() );
				// 	System.out.print(" " + ((FiveRegisterInstruction)refIns).getRegisterD() );
				// 	System.out.println(" " + ((FiveRegisterInstruction)refIns).getRegisterE() );
				// 	System.out.print("Tainted var :");
				// 	for (Object oo : currentStr.tanitedVarSet) {
				// 		System.out.print(oo+" ");
				// 	}
				// 	System.out.println("\n$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$");
				// }

				// check if this function call touches any of the tainted variables
				if (!carriesTaint && varsThisInstTouches.size() > 0) {
					basicblock.tanitedVarSet.remove(varsThisInstTouches.get(varsThisInstTouches.size()-1));  // todo: check for case when no return val
					continue;
				}
				// look for method


				// check if it is a sink
				boolean isSink = false;
				{
					BufferedReader br = null;
					try {
						File sourceFile = new File("Android_4.2_Sinks.txt");
						br = new BufferedReader(new FileReader(sourceFile));
						String line;
						while ((line = br.readLine()) != null) {
							// sink found
							String sink = InstructionFormater.getFormatedFunctionCall(ins);
							if (line.startsWith(sink)) {
//								System.out.println("\n\n\n****************LEAK FOUND:****************");
								System.out.println(location);
								// System.out.println("\nIn Class: "+definingClass+" In function: "+reference.getName());
//								System.out.println("sink = " + line);
								isSink = true;
								break;
							}
						}
					} catch (Exception e) {
						e.printStackTrace();
					} finally {
						try {
							br.close();
						} catch (Exception e) {
							e.printStackTrace();
						}
					}
				}
				// todo: patch up
			    if (isSink) { continue; }  // no need to analyze this function

			    // DEBUG INFO
				// System.out.println(definingClass);
				// System.out.print(ins.instruction.getOpcode());
				// if (format == Format.Format35c) {
				// 	FiveRegisterInstruction r5instr = (FiveRegisterInstruction)refIns;
				// 	for (int l = 0;l<r5instr.getRegisterCount() ;++l ) {
				// 		switch (l) {
				// 			case 0: System.out.print(r5instr.getRegisterC()+" ");
				// 			case 1: System.out.print(r5instr.getRegisterD()+" ");
				// 			case 2: System.out.print(r5instr.getRegisterE()+" ");
				// 			case 3: System.out.print(r5instr.getRegisterF()+" ");
				// 			case 4: System.out.print(r5instr.getRegisterG()+" ");
				// 		}
				// 	}
				// } else if (format == Format.Format3rc) {
				// 	Instruction3rc instr3rc = (Instruction3rc) ins.instruction;
				// 	for (int l = 0; l < instr3rc.getRegisterCount(); ++l) {
				// 		System.out.print((instr3rc.getStartRegister()+l) + " ");
				// 	}
				// }
				// System.out.println("\n"+SPROJ.DEPTH+"\n\n");
				// System.out.println("\n"+reference+"\n\n");
				//

			    // if cant find funcdefinition i.e. library function then no need to analyze this function tanitedVarSet.addAll(varsThisInstTouches);
				// look for method

				boolean functionDefFound = false;
				if (SPROJ.DEPTH < SPROJ.MAX_DEPTH) {
				List<? extends ClassDef> classDefs = Ordering.natural().sortedCopy(SPROJ.FILE.getClasses());
				for(final ClassDef classDef: classDefs) {
					if (functionDefFound) break;
					if (!classDef.getType().startsWith(definingClass)) continue;
					if (classDef.getType().startsWith("Landroid")) continue;
					if (classDef.getType().startsWith("Ljava")) continue;

					for(Method method: classDef.getMethods()) {
						if (method.getName().equals(reference.getName()) && method.getImplementation() != null) {
							functionDefFound = true;
							SPROJ.DEPTH++;
							ReturnStructure newStr = new ReturnStructure(basicblock.tanitedVarSet, ins, method);  // mapping
							new FunctionLeakFinder(method, newStr, location);
							SPROJ.DEPTH--;
							if (newStr.isRetValTainted == true) {
								basicblock.tanitedVarSet.add(varsThisInstTouches.get(varsThisInstTouches.size()-1));
							} else if (varsThisInstTouches.size() > 0){
								basicblock.tanitedVarSet.remove(varsThisInstTouches.get(varsThisInstTouches.size()-1));
							}

							basicblock.tanitedVarSet.addAll(newStr.reverseMap());  // merge sets after doing reverse mapping
							break;
						}
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
}

class ReturnStructure {
	boolean isRetValTainted;
	boolean carriesTaint;
	TreeSet<Object> tanitedVarSet;
	Method method;
	BasicBlockInstruction ins;
	ReturnStructure(TreeSet<Object> _tanitedVarSet, BasicBlockInstruction _ins, Method _method) {
		isRetValTainted = false;
		carriesTaint = false;
		method = _method;
		ins = _ins;
		tanitedVarSet = new TreeSet<Object> ();
		int numRegisters = method.getImplementation().getRegisterCount();
		// two type of function calls
		if (ins.instruction.getOpcode().format == Format.Format35c) {
			FiveRegisterInstruction r5instr = (FiveRegisterInstruction)ins.instruction;
			for (int i = 0;i<r5instr.getRegisterCount() ;++i ) {
				switch (i) {
					case 0 : {
						Object register = r5instr.getRegisterC();
						if ( _tanitedVarSet.contains( register ) ) {
							tanitedVarSet.add(numRegisters - r5instr.getRegisterCount() + i);
							carriesTaint = true;
						}
						break;
					}
					case 1 : {
						Object register = r5instr.getRegisterD();
						if ( _tanitedVarSet.contains( register ) ) {
							tanitedVarSet.add(numRegisters - r5instr.getRegisterCount() + i);
							carriesTaint = true;
						}
						break;
					}
					case 2 : {
						Object register = r5instr.getRegisterE();
						if ( _tanitedVarSet.contains( register ) ) {
							tanitedVarSet.add(numRegisters - r5instr.getRegisterCount() + i);
							carriesTaint = true;
						}
						break;
					}
					case 3 : {
						Object register = r5instr.getRegisterF();
						if ( _tanitedVarSet.contains( register ) ) {
							tanitedVarSet.add(numRegisters - r5instr.getRegisterCount() + i);
							carriesTaint = true;
						}
						break;
					}
					case 4 : {
						Object register = r5instr.getRegisterG();
						if ( _tanitedVarSet.contains( register ) ) {
							tanitedVarSet.add(numRegisters - r5instr.getRegisterCount() + i);
							carriesTaint = true;
						}
						break;
					}
					default:
						break;
				}
			}
			
		} else if (ins.instruction.getOpcode().format == Format.Format3rc) {
			Instruction3rc instr3rc = (Instruction3rc) ins.instruction;
			for (int i = 0; i < instr3rc.getRegisterCount(); ++i) {
				if (_tanitedVarSet.contains( (Object)(instr3rc.getStartRegister()+i))) {
					tanitedVarSet.add(numRegisters - instr3rc.getRegisterCount() + i);
					carriesTaint = true;
				}
			}
		}
	}
	public void setRetValTainted() {
		isRetValTainted = true;
	}
	public TreeSet<Object> reverseMap() {
		TreeSet<Object> retSet = new TreeSet<Object> ();
		int numRegisters = method.getImplementation().getRegisterCount();
		if (ins.instruction.getOpcode().format == Format.Format35c) {
			FiveRegisterInstruction r5instr = (FiveRegisterInstruction)ins.instruction;
			for (int i = 0;i<r5instr.getRegisterCount() ;++i ) {
				switch (i) {
					case 0 : {
						if ( tanitedVarSet.contains( (Object)(numRegisters - r5instr.getRegisterCount() + i)) ) {
							retSet.add(r5instr.getRegisterC());
						}
						break;
					}
					case 1 : {
						if ( tanitedVarSet.contains( (Object)(numRegisters - r5instr.getRegisterCount() + i) ) ) {
							retSet.add(r5instr.getRegisterD());
						}
						break;
					}
					case 2 : {
						if ( tanitedVarSet.contains( (Object)(numRegisters - r5instr.getRegisterCount() + i) ) ) {
							retSet.add(r5instr.getRegisterE());
						}
						break;
					}
					case 3 : {
						if ( tanitedVarSet.contains( (Object)(numRegisters - r5instr.getRegisterCount() + i) ) ) {
							retSet.add(r5instr.getRegisterF());
						}
						break;
					}
					case 4 : {
						if ( tanitedVarSet.contains( (Object)(numRegisters - r5instr.getRegisterCount() + i) ) ) {
							retSet.add(r5instr.getRegisterG());
						}
						break;
					}
				}
			}
		}else if (ins.instruction.getOpcode().format == Format.Format3rc) {
			Instruction3rc instr3rc = (Instruction3rc) ins.instruction;
			for (int i = 0; i < instr3rc.getRegisterCount(); ++i) {
				if (tanitedVarSet.contains( (Object)(numRegisters - instr3rc.getRegisterCount() + i))) {
					retSet.add(instr3rc.getStartRegister()+i);
				}
			}
		}
		return retSet;
	}
}

