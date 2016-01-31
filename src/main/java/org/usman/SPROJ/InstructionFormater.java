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

public class InstructionFormater{
	
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
		
		String returnType = InstructionFormater.fixTypes(r.getReturnType());

		List<String> params = r.getParameterTypes();
		List<String> paramsnew = new ArrayList<String>();
		for (String param : params) {
    		paramsnew.add(InstructionFormater.fixTypes(param));
		}
		params = paramsnew;

		String possibleSourceSink = definingClass+" "+returnType+" "+r.getName();
		possibleSourceSink += "(";
		for (String param : params) {
			possibleSourceSink += param + ",";
		}
		if (params.size() > 0) {
			possibleSourceSink = possibleSourceSink.substring(0,possibleSourceSink.length()-1);
		}
		possibleSourceSink += ")>";
		return possibleSourceSink;
	}
}