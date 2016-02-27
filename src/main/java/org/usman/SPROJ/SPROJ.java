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

import cern.colt.matrix.impl.SparseDoubleMatrix2D;

import java.util.Map;
import java.util.HashMap;

import com.beust.jcommander.JCommander;


public class SPROJ {
		
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
	
	public static DexBackedDexFile FILE;
	public static void main(String[] args) throws IOException {
		JCommanderArguments jCommArgs = new JCommanderArguments();
		try {
	        new JCommander(jCommArgs, args);
		} catch (Exception e) {
			System.out.println("Usage:");
			System.out.println("\tjava -jar target/SPROJ-1.0-jar-with-dependencies.jar -apk bluetooth.apk");
			return;
		}

		DexBackedDexFile dexFile = SPROJ.loadFile(jCommArgs.dexFile);
		FILE = dexFile;

		if (jCommArgs.onlyDisplay) {
			SourceSinkIdentifier.findAllSourcesSinks(dexFile);
		} else {
			LeakFinder.findLeaks(dexFile);
		}
	}
}