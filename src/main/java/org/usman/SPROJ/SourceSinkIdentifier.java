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


public class SourceSinkIdentifier {
	public static void findAllSourcesSinks(DexBackedDexFile dexFile) throws IOException{
		ArrayList<String> toPrint = new ArrayList<String>();
		List<? extends ClassDef> classDefs = Ordering.natural().sortedCopy(dexFile.getClasses());

		for(final ClassDef classDef: classDefs) {
			// dont want library functions.
			if (classDef.getType().startsWith("Landroid")) continue;

			// System.out.println("In Class: "+classDef.getSourceFile()+"  "+classDef.getType()+"\n");

			for(Method method: classDef.getMethods()) {
				// List<BasicBlockInstruction> instructions = ControlFlowGraph.getFlatMethod(method);
				// build CFG
				ControlFlowGraph cfg = new ControlFlowGraph(method);
				// cfg.normalize();
				
				List<BasicBlock> basicblocks = cfg.getBasicBlocks();
				for (BasicBlock basicblock : basicblocks) {

					List<BasicBlockInstruction> instructions = basicblock.instructions;
					for (BasicBlockInstruction instruction : instructions) {

						// look for sinks which can be found by searching for function call instructions
						Opcode opcode = instruction.instruction.getOpcode();

						switch(opcode.referenceType) {
					    	case 3:  // 3 == Method reference type

					    		String possibleSourceSink = InstructionFormater.getFormatedFunctionCall(instruction);
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
											toPrint.add("\t\tSink found: "+ line);
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
											// toPrint.add("\t\tSource found: "+ possibleSourceSink);
											toPrint.add("\t\tSource found: "+ line);
											break;
								        }
								    }
								}
					   //  		ReferenceInstruction i = (ReferenceInstruction)instruction.instruction;
								// DexBackedMethodReference r = (DexBackedMethodReference)i.getReference();
								// String lll = "In function: "+method.getName()+" found a call to: "+ r.getName()+" return type: "+r.getReturnType()+" Defined in class: "+r.getDefiningClass();
								// if (lll.equals("In function: onReceive found a call to: getDeviceId return type: Ljava/lang/String; Defined in class: Landroid/telephony/TelephonyManager;")) {
								// 	System.out.println(possibleSourceSink);
								// 	// System.out.println(lll);
								// }
													
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
}