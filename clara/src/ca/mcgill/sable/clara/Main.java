package ca.mcgill.sable.clara;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;


public class Main {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		List<String> argList = new LinkedList<String>(Arrays.asList(args));
		boolean foundExt = false;
		for (String arg : argList) {
			if(arg.equals("-ext")) {
				foundExt = true;
				break;
			}
		}
		if(!foundExt) {
			argList.add("-ext");
			argList.add("ca.mcgill.sable.clara");
		}
		abc.main.Main.main(argList.toArray(new String[argList.size()]));
	}

}
