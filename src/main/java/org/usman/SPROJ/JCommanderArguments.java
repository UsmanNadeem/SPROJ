package org.usman.SPROJ;
import com.beust.jcommander.Parameter;
import java.util.List;
import java.util.ArrayList;

public class JCommanderArguments {
	@Parameter
	private List<String> parameters = new ArrayList<>();

	@Parameter(names = { "-apk", "-dex" }, description = "Name of .dex or .apk file", required = true)
	public String dexFile;

	@Parameter(names = { "-d", "-displayOnly" }, description = "If argument is present only display sources and sinks. Do not find leaks")
	public boolean onlyDisplay = false;
}