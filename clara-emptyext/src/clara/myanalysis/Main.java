package clara.myanalysis;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;


public class Main {

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
			argList.add(Main.class.getPackage().getName());
		}
		abc.main.Main.main(argList.toArray(new String[argList.size()]));
	}

}
