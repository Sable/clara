/* Clara - Compile-time Approximation of Runtime Analyses
 * Copyright (C) 2010 Eric Bodden
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
package ca.mcgill.sable.clara.weaving.weaver.depadviceopt;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class ResultListeners {

	public static ResultListeners instance;
	
	protected Set<ResultListener> resultListeners = new HashSet<ResultListener>();

	public void registerResultListener(ResultListener l) {
		resultListeners.add(l);
	}
	
	public Set<ResultListener> getResultListeners() {
		return Collections.unmodifiableSet(resultListeners);
	}
	
	public static ResultListeners v() {
		if(instance==null) {
			instance = new ResultListeners();
		}
		return instance;
	}

	private ResultListeners() {
 	}
	
	public static void reset() { instance = null; }

}
