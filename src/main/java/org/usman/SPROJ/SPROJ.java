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
	
	static String CLASS = "L";
	public static DexBackedDexFile FILE;
	public static void main(String[] args) throws IOException {
		if (args.length < 3) {
			System.out.println("Usage:");
			System.out.println("\tjava -jar <jar file> <classes.dex/apkfile> <com/example/android/bluetoothchat/> <l or d>");
			System.out.println("\tWhere <com/example/android/bluetoothchat/> is the name of package of interest in the .dex file");
			System.out.println("\tand  l means find leaks and d means display all sources and sinks");
			return;
		}
		DexBackedDexFile dexFile = SPROJ.loadFile(args[0]);
		FILE = dexFile;
		SPROJ.CLASS += args[1];

		if (args[2].equals("l")) {
			LeakFinder.findLeaks(dexFile);
		} else if (args[2].equals("d")) {
			SourceSinkIdentifier.findAllSourcesSinks(dexFile);
		} else {
			System.out.println("Usage:");
			System.out.println("\tjava -jar <jar file> <classes.dex/apkfile> <com/example/android/bluetoothchat/> <l or d>");
			System.out.println("\tWhere <com/example/android/bluetoothchat/> is the name of package of interest in the .dex file");
			System.out.println("\tand  l means find leaks and d means display all sources and sinks");
			return;
		}
	}
}