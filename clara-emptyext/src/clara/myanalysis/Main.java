/* Clara - Compile-time Approximation of Runtime Analyses
 * Copyright (C) 2009 Eric Bodden
 * 
 * This framework uses technology from Soot, abc, JastAdd and
 * others. 
 *
 * This framework is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This framework is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this compiler, in the file LESSER-GPL;
 * if not, write to the Free Software Foundation, Inc.,
 * 59 Temple Place - Suite 330, Boston, MA 02111-1307, USA.
 */
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
