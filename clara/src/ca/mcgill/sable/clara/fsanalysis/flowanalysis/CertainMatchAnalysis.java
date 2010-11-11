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
package ca.mcgill.sable.clara.fsanalysis.flowanalysis;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import abc.tm.weaving.matching.SMEdge;
import abc.tm.weaving.matching.SMNode;
import ca.mcgill.sable.clara.weaving.weaver.depadviceopt.ds.Shadow;


public class CertainMatchAnalysis {

	protected Set<Shadow> pointsOfCertainMatches = new HashSet<Shadow>();

	public CertainMatchAnalysis(UnnecessaryShadowsAnalysis unnecessaryShadowsAnalysis) {
		nextShadow:
		for(Shadow s: unnecessaryShadowsAnalysis.job.shadowsForMethodAndTracePattern) {
			
			boolean leadsToFinal = false;
			
			Set<Integer> statesBeforeTransition = unnecessaryShadowsAnalysis.statesBeforeTransition(s);
			
			for (Integer stateNum : statesBeforeTransition) {
				SMNode stateByNumber = unnecessaryShadowsAnalysis.job.tracePattern().getStateMachine().getStateByNumber(stateNum);
				for (Iterator<SMEdge> outEdgeIter=stateByNumber.getOutEdgeIterator(); outEdgeIter.hasNext();) {
					SMEdge outEdge = outEdgeIter.next();
					if(!outEdge.isSkipEdge() && outEdge.getLabel().equals(unnecessaryShadowsAnalysis.job.symbolNameForShadow(s))) {
						if(outEdge.getTarget().isFinalNode()) {
							leadsToFinal = true;
						} else {
							continue nextShadow;
						}					
					}
				}	
				if(!leadsToFinal) continue nextShadow;

			}
			
			if(leadsToFinal) {
				pointsOfCertainMatches.add(s);
			}
		}
	}
	
	public Set<Shadow> pointsOfCertainMatches() {
		return pointsOfCertainMatches ;
	}

}
