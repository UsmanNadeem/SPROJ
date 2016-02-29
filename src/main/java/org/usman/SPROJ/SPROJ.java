package org.usman.SPROJ;

import java.io.File;
import java.io.IOException;

import org.jf.dexlib2.DexFileFactory;
import org.jf.dexlib2.dexbacked.DexBackedDexFile;

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
	public static int MAX_DEPTH = 5;
	public static int DEPTH = 0;
	public static void main(String[] args) {
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