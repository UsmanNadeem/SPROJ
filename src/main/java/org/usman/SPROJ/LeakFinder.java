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

import org.jf.dexlib2.*;
import org.jf.dexlib2.Opcode;
import com.google.common.collect.Ordering;

public class LeakFinder {

	public static void findLeaks(DexBackedDexFile dexFile) throws IOException{
		ArrayList<String> toPrint = new ArrayList<String>();

		List<? extends ClassDef> classDefs = Ordering.natural().sortedCopy(dexFile.getClasses());

		for(final ClassDef classDef: classDefs) {
			// dont want library functions. for now hardcode the main application class.
			if (!classDef.getType().startsWith(SPROJ.CLASS)) continue;

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

									TreeSet<Object> possibleSourceVars = new TreeSet<Object>();
									LinkedList<BasicBlock> queue = new LinkedList<BasicBlock>();
									queue.add(basicblock);
									for (BasicBlock bb : basicblocks) {
										bb.clearVisited();
									}

									basicblock.setVisited();
									boolean firstTime = true;
									
									while (queue.size() != 0) {
										BasicBlock bb = queue.remove();
										if (firstTime) {
											firstTime = false;
											LeakFinder.searchForPathtoSink(instruction, bb, basicblocks, location, possibleSourceVars);
										} else {
											LeakFinder.searchForPathtoSink(null, bb, basicblocks, location, possibleSourceVars);
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

	

	public static void searchForPathtoSink(BasicBlockInstruction srcInstruction, BasicBlock basicblock, List<BasicBlock> basicblocks, String location, TreeSet<Object> possibleSourceVars) throws IOException{

		for (int i = (srcInstruction==null) ? 0 : basicblock.instructions.indexOf(srcInstruction); i<basicblock.instructions.size(); ++i) {
			BasicBlockInstruction ins = basicblock.instructions.get(i);

			if (ins.instruction.getOpcode().referenceType == 3) {  // function call

				TreeSet<Object> varsThisInstTouches = Analyzer.getVarThisFunctionTouches(ins, basicblock);

				// check for return call
				if (i+1<basicblock.instructions.size()) {
					if (basicblock.instructions.get(i+1).instruction.getOpcode().format == Format.Format11x) {
						++i;  // skip the next instruction which is a return call
					}
				}
				if (srcInstruction == ins) {  // this instruction is the source
					possibleSourceVars.addAll(varsThisInstTouches);
					continue;
				}

				// check if this function call touches any of the tainted variables
				if (varsThisInstTouches.size()>0) {
					for (Object o : possibleSourceVars) {
						if (varsThisInstTouches.contains(o)) {
							possibleSourceVars.addAll(varsThisInstTouches);  // add to list of already tainted variables.

							// check if it is a sink
							File sourceFile = new File("Android_4.2_Sinks.txt");
						    BufferedReader br = new BufferedReader(new FileReader(sourceFile));
						    String line;
						    while ((line = br.readLine()) != null) {
						    	// sink found
						    	String sink = InstructionFormater.getFormatedFunctionCall(ins);
						        if (line.startsWith(sink)) {
									System.out.println("****************LEAK FOUND:****************");
									System.out.println(location);
									System.out.println("sink = " + line);
									break;
						        }
						    }
							break;
						}
					}
				}
			} else {  // this instruction is not a function call
				ArrayList<TreeSet<Object>> varsThisInstTouches = Analyzer.getSourceAndDest(ins);
				if (varsThisInstTouches == null) {
					continue;
				}
				// registers overwritten with a constant are no longer tainted
				for (Object o: varsThisInstTouches.get(2)) {
						possibleSourceVars.remove(o);
				}

				if (varsThisInstTouches.get(0).size() == 0 || varsThisInstTouches.get(1).size() == 0) {
					continue;
				}

				boolean taintedInstruction = false;
				for (Object o : possibleSourceVars) {
					if (varsThisInstTouches.get(0).contains(o)) {
						possibleSourceVars.addAll(varsThisInstTouches.get(1));
						taintedInstruction = true;
						break;
					}
				}

				// destination is overwritten
				if (taintedInstruction == false) {
					possibleSourceVars.removeAll(varsThisInstTouches.get(1));
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
