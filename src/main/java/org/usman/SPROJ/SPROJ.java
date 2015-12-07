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

import com.beust.jcommander.JCommander;
import com.beust.jcommander.ParameterException;

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

import cern.colt.matrix.impl.SparseDoubleMatrix2D;

import com.tinkerpop.blueprints.impls.tg.TinkerGraph;
import com.tinkerpop.blueprints.Graph;
import com.tinkerpop.blueprints.Vertex;
import com.tinkerpop.blueprints.Edge;

import java.util.Map;
import java.util.HashMap;

public class SPROJ {

	public static String fixTypes(String param) {
		// Fix the type of function parameters/returntype and format it properly for scanning
		if (param.charAt(0) == '['){
			param = param.substring(1);
			param = param + "[]";
		}
		if (param.length()>=2 && param.charAt(0) == '['){
			param = param.substring(1);
			param = param + "[]";
		}
		if (param.charAt(0) == 'L') {
			param = param.replaceFirst("L","").replace('/', '.').replaceFirst(";","");
		}
		if (param.length()==1 || (param.length()==3&&param.charAt(1)=='['&&param.charAt(2)==']')) {
			switch (param.charAt(0)) {
				case 'V': param = param.replaceFirst("V","void"); break;
				case 'Z': param = param.replaceFirst("Z","boolean"); break;
				case 'B': param = param.replaceFirst("B","byte"); break;
				case 'S': param = param.replaceFirst("S","short"); break;
				case 'C': param = param.replaceFirst("C","char"); break;
				case 'I': param = param.replaceFirst("I","int"); break;
				case 'J': param = param.replaceFirst("J","long"); break;
				case 'F': param = param.replaceFirst("F","float"); break;
				case 'D': param = param.replaceFirst("D","double"); break;
				default: break;
			}
		}
		return param;
	}
	public static String getFormatedFunctionCall(BasicBlockInstruction instruction) {
		ReferenceInstruction i = (ReferenceInstruction)instruction.instruction;
		DexBackedMethodReference r = (DexBackedMethodReference)i.getReference();
		
		// Get information e.g defining class, parameters, function name, return type etc
		// and format them in a readable form so that we can
		// match them with our list

		String definingClass = r.getDefiningClass();
		if (definingClass.charAt(0) == 'L')
			definingClass = definingClass.replaceFirst("L","<");
		definingClass = definingClass.replace('/', '.').replaceFirst(";",":");
		
		String returnType = SPROJ.fixTypes(r.getReturnType());

		List<String> params = r.getParameterTypes();
		List<String> paramsnew = new ArrayList<String>();
		for (String param : params) {
    		paramsnew.add(SPROJ.fixTypes(param));
		}
		params = paramsnew;

		String possibleSourceSink = definingClass+" "+returnType+" "+r.getName();
		possibleSourceSink += "(";
		for (String param : params) {
			possibleSourceSink += param + ",";
		}
		possibleSourceSink = possibleSourceSink.substring(0,possibleSourceSink.length()-1);
		possibleSourceSink += ")>";
		return possibleSourceSink;
	}
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
	public static void findAllSourcesSinks(DexBackedDexFile dexFile) throws IOException{
		ArrayList<String> toPrint = new ArrayList<String>();
		List<? extends ClassDef> classDefs = Ordering.natural().sortedCopy(dexFile.getClasses());

		for(final ClassDef classDef: classDefs) {
			// dont want library functions. for now hardcode the main application class.
			if (!classDef.getType().startsWith(SPROJ.CLASS)) continue;

			// System.out.println("In Class: "+classDef.getSourceFile()+"  "+classDef.getType()+"\n");

			for(Method method: classDef.getMethods()) {
				// List<BasicBlockInstruction> instructions = ControlFlowGraph.getFlatMethod(method);
				// build CFG
				ControlFlowGraph cfg = new ControlFlowGraph(method);
				cfg.normalize();
				
				List<BasicBlock> basicblocks = cfg.getBasicBlocks();
				for (BasicBlock basicblock : basicblocks) {

					List<BasicBlockInstruction> instructions = basicblock.instructions;
					for (BasicBlockInstruction instruction : instructions) {

						// look for sinks which can be found by searching for function call instructions
						Opcode opcode = instruction.instruction.getOpcode();

						switch(opcode.referenceType) {
					    	case 3:  // 3 == Method reference type

					    		String possibleSourceSink = SPROJ.getFormatedFunctionCall(instruction);
								// match with list of sinks
								{
									File sinkFile = new File("Android_4.2_Sinks.txt");
									BufferedReader br = new BufferedReader(new FileReader(sinkFile));
								    String line;
								    while ((line = br.readLine()) != null) {
								    	// sink found
								        if (line.startsWith(possibleSourceSink)) {
											// System.out.print("In Class: "+classDef.getSourceFile());
											// System.out.println("   In function: "+method.getName());
											// System.out.println("Sink found: "+ possibleSourceSink);
											// System.out.println("");

											if (!toPrint.contains("\n\nIn Class: "+classDef.getSourceFile())) {
												toPrint.add("\n\nIn Class: "+classDef.getSourceFile());
											}
											if (!toPrint.contains("\tIn function: "+method.getName())) {
												toPrint.add("\tIn function: "+method.getName());
											}
											toPrint.add("\t\tSink found: "+ possibleSourceSink);
											break;
								        }
								    }
								}

								// match with list of sources
								{
									File sourceFile = new File("Android_4.2_Sources.txt");
								    BufferedReader br = new BufferedReader(new FileReader(sourceFile));
								    String line;
								    while ((line = br.readLine()) != null) {
								    	// source found
								        if (line.startsWith(possibleSourceSink)) {
											// System.out.print("In Class: "+classDef.getSourceFile());
											// System.out.println("   In function: "+method.getName());
											// System.out.println("Source found: "+ possibleSourceSink);
											// System.out.println("");
											if (!toPrint.contains("\n\nIn Class: "+classDef.getSourceFile())) {
												toPrint.add("\n\nIn Class: "+classDef.getSourceFile());
											}
											if (!toPrint.contains("\tIn function: "+method.getName())) { 
												toPrint.add("\tIn function: "+method.getName());
											}
											toPrint.add("\t\tSource found: "+ possibleSourceSink);
											break;
								        }
								    }
								}
					    		
								// System.out.println("In function: "+method.getName()+" found a call to: "+ r.getName()+" return type: "+r.getReturnType()+" Defined in class: "+r.getDefiningClass());
								break;
							default:
								continue;
						}
					}

				}
			}
			for (String line : toPrint) {
				System.out.println(line);
			}
			toPrint.clear();
		}
	}

	public static DexBackedDexFile loadFile(String name) {
		File dexFileFile = new File(name);
		DexBackedDexFile dexFile = null;
		if(!dexFileFile.exists()) {
			System.err.println("Dexfile not found!");
			System.exit(0);
		}

		try {
			dexFile = DexFileFactory.loadDexFile(dexFileFile, 15);
		} catch(org.jf.util.ExceptionWithContext e) {
			System.err.println(e);
			System.exit(0);
		} catch(java.io.FileNotFoundException e) {
			System.err.println("Cannot scan a directory: " + dexFileFile.getPath());
			System.exit(0);
		} catch(Exception e) {
			System.err.println("Error loading file: " + dexFileFile.getPath());
			System.exit(0);
		} 

		if(dexFile.isOdexFile()) {
			System.err.println("Odex not supported!");
			System.exit(0);
		}
		return dexFile;
	}
	
	static String CLASS = "L";
	public static void main(String[] args) throws IOException {
		if (args.length < 3) {
			System.out.println("Usage:");
			System.out.println("\tjava -jar <jar file> <classes.dex/apkfile> <com/example/android/bluetoothchat/> <l or d>");
			System.out.println("\tWhere <com/example/android/bluetoothchat/> is the name of package of interest in the .dex file");
			System.out.println("\tand  l means find leaks and d means display all sources and sinks");
			return;
		}
		DexBackedDexFile dexFile = SPROJ.loadFile(args[0]);
		SPROJ.CLASS += args[1];

		if (args[2].equals("l")) {
			SPROJ.findLeaks(dexFile);
		} else if (args[2].equals("d")) {
			SPROJ.findAllSourcesSinks(dexFile);
		} else {
			System.out.println("Usage:");
			System.out.println("\tjava -jar <jar file> <classes.dex/apkfile> <com/example/android/bluetoothchat/> <l or d>");
			System.out.println("\tWhere <com/example/android/bluetoothchat/> is the name of package of interest in the .dex file");
			System.out.println("\tand  l means find leaks and d means display all sources and sinks");
			return;
		}
	}





//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////




	public static void findLeaks(DexBackedDexFile dexFile) throws IOException{
		ArrayList<String> toPrint = new ArrayList<String>();
		List<? extends ClassDef> classDefs = Ordering.natural().sortedCopy(dexFile.getClasses());

		for(final ClassDef classDef: classDefs) {
			// dont want library functions. for now hardcode the main application class.
			if (!classDef.getType().startsWith(SPROJ.CLASS)) continue;

			for(Method method: classDef.getMethods()) {

				ControlFlowGraph cfg = new ControlFlowGraph(method);
				cfg.normalize();
				List<BasicBlock> basicblocks = cfg.getBasicBlocks();

				for (BasicBlock basicblock : basicblocks) {

					List<BasicBlockInstruction> instructions = basicblock.instructions;

					for (BasicBlockInstruction instruction : instructions) {

						// look for sinks which can be found by searching for function call instructions
						Opcode opcode = instruction.instruction.getOpcode();

						if(opcode.referenceType == 3) {  // 3 == Method reference type
							String possibleSourceSink = SPROJ.getFormatedFunctionCall(instruction);
							// match with list of sources
							File sourceFile = new File("Android_4.2_Sources.txt");
						    BufferedReader br = new BufferedReader(new FileReader(sourceFile));
						    String line;
						    while ((line = br.readLine()) != null) {
						    	// source found
						        if (line.startsWith(possibleSourceSink)) {
									SPROJ.searchForPathtoSink(instruction, basicblock, basicblocks, "In Class: "+classDef.getSourceFile()+" In function: "+method.getName()+ "\nSource: "+possibleSourceSink);
									break;
						        }
						    }
						}
					}
				}
			}
		}
	}

	public static TreeSet<Object> getVarThisFunctionTouches(BasicBlockInstruction instruction) {
		Format format = instruction.instruction.getOpcode().format;
		Opcode opcode = instruction.instruction.getOpcode();
		TreeSet<Object> retval = new TreeSet<Object>();
		if (format == Format.Format35c) {
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
		}
		else if (format == Format.Format11x) {
			OneRegisterInstruction instr = (OneRegisterInstruction)instruction.instruction;
			retval.add(instr.getRegisterA());
		}
		return retval;
		// if (opcode.name.contains("/2addr")) {  // both source, 1st is dest
		// retval.get(0).add(instruction.getRegisterA());
			// case Format11x:  // return	 getRegisterA
			// case Format35c:	 // getRegisterCount CDEFG getReference
			// case Format3rc: todo	
	}

	public static void searchForPathtoSink(BasicBlockInstruction instruction, BasicBlock basicblock, List<BasicBlock> basicblocks, String location) throws IOException{
		TreeSet<Object> possibleSourceVars = SPROJ.getVarThisFunctionTouches(instruction);
		int i = basicblock.instructions.indexOf(instruction);

		if (i+1 <basicblock.instructions.size() && basicblock.instructions.get(i+1).instruction.getOpcode().format == Format.Format11x) {
			i+=2;
			possibleSourceVars.addAll(SPROJ.getVarThisFunctionTouches(basicblock.instructions.get(i-1)));
		}
			
		for (BasicBlock bb : basicblocks) {
			bb.clearVisited();
		}

		for (; i<basicblock.instructions.size(); ++i) {
			BasicBlockInstruction ins = basicblock.instructions.get(i);
			if (ins.instruction.getOpcode().referenceType == 3) {
				TreeSet<Object> varsThisInstTouches = SPROJ.getVarThisFunctionTouches(ins);
				if (i+1<basicblock.instructions.size()) {
					if (basicblock.instructions.get(i+1).instruction.getOpcode().format == Format.Format11x) {
						++i;
						varsThisInstTouches.addAll(SPROJ.getVarThisFunctionTouches(basicblock.instructions.get(i)));
					}
				}
				if (varsThisInstTouches.size()>0) {
					for (Object o : possibleSourceVars) {
						if (varsThisInstTouches.contains(o)) {
							possibleSourceVars.addAll(varsThisInstTouches);

							File sourceFile = new File("Android_4.2_Sinks.txt");
						    BufferedReader br = new BufferedReader(new FileReader(sourceFile));
						    String line;
						    while ((line = br.readLine()) != null) {
						    	// sink found
						    	String sink = SPROJ.getFormatedFunctionCall(ins);
						        if (line.startsWith(sink)) {
									System.out.println("****************LEAK FOUND:****************");
									System.out.println(location);
									System.out.println("sink = " + sink);
									break;
						        }
						    }

							break;
						}
					}
				}
				// check for sink too
			} else {
				ArrayList<TreeSet<Object>> varsThisInstTouches = SPROJ.getSourceAndDest(ins);
				if (varsThisInstTouches != null && varsThisInstTouches.get(2).size() > 0) {
					for (Object o: varsThisInstTouches.get(2)) {
						if (possibleSourceVars.contains(o)) {
							possibleSourceVars.remove(o);
						}
					}
				}
				if (varsThisInstTouches == null || varsThisInstTouches.get(0).size() == 0 || varsThisInstTouches.get(1).size() == 0) {
					continue;
				}
				for (Object o : possibleSourceVars) {
					if (varsThisInstTouches.get(0).contains(o)) {
						possibleSourceVars.addAll(varsThisInstTouches.get(1));
						break;
					}
				}
			}
		}

		basicblock.setVisited();
		//  itteratively search for path to any sink in all basic blocks
		if (basicblock.outgoingEdges == 0) {
			return;
		} else {
			LinkedList<BasicBlock> queue = new LinkedList<BasicBlock>();
			while (queue.size() != 0) {
				BasicBlock bb = queue.remove();
				SPROJ.searchForPathtoSink(possibleSourceVars, bb, location);
				SPROJ.addChildrenToQueue(bb, basicblocks, queue);
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
	public static void searchForPathtoSink(TreeSet<Object> possibleSourceVars, BasicBlock bb, String location) throws IOException{

		for (int i = 0; i<bb.instructions.size(); ++i) {
			BasicBlockInstruction ins = bb.instructions.get(i);

			if (ins.instruction.getOpcode().referenceType == 3) {
				TreeSet<Object> varsThisInstTouches = SPROJ.getVarThisFunctionTouches(ins);
				if (i+1<bb.instructions.size()) {
					if (bb.instructions.get(i+1).instruction.getOpcode().format == Format.Format11x) {
						++i;
						varsThisInstTouches.addAll(SPROJ.getVarThisFunctionTouches(bb.instructions.get(i)));
					}
				}

				if (varsThisInstTouches.size()>0) {
					for (Object o : possibleSourceVars) {
						if (varsThisInstTouches.contains(o)) {
							possibleSourceVars.addAll(varsThisInstTouches);

							File sourceFile = new File("Android_4.2_Sinks.txt");
						    BufferedReader br = new BufferedReader(new FileReader(sourceFile));
						    String line;
						    while ((line = br.readLine()) != null) {
						    	// sink found
						    	String sink = SPROJ.getFormatedFunctionCall(ins);
						        if (line.startsWith(sink)) {
									System.out.println("****************LEAK FOUND:****************");
									System.out.println(location);
									System.out.println("sink = " + sink);
									break;
						        }
						    }

							break;
						}
					}
				}
				// check for sink too
			} else {
				ArrayList<TreeSet<Object>> varsThisInstTouches = SPROJ.getSourceAndDest(ins);
				if (varsThisInstTouches != null && varsThisInstTouches.get(2).size() > 0) {
					for (Object o: varsThisInstTouches.get(2)) {
						if (possibleSourceVars.contains(o)) {
							possibleSourceVars.remove(o);
						}
					}
				}
				if (varsThisInstTouches == null || varsThisInstTouches.get(0).size() == 0 || varsThisInstTouches.get(1).size() == 0) {
					continue;
				}
				for (Object o : possibleSourceVars) {
					if (varsThisInstTouches.get(0).contains(o)) {
						possibleSourceVars.addAll(varsThisInstTouches.get(1));
						break;
					}
				}
			}
		}
	}






























}