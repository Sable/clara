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

import java.util.List;
import java.util.Set;

import abc.main.Main;
import abc.weaving.residues.ResidueBox;
import abc.weaving.weaver.AbstractReweavingAnalysis;
import ca.mcgill.sable.clara.HasDAInfo;
import ca.mcgill.sable.clara.fsanalysis.EnabledShadowSet;
import ca.mcgill.sable.clara.weaving.aspectinfo.DAInfo;
import ca.mcgill.sable.clara.weaving.aspectinfo.TracePattern;
import ca.mcgill.sable.clara.weaving.weaver.depadviceopt.DependentAdviceFlowInsensitiveAnalysis;
import ca.mcgill.sable.clara.weaving.weaver.depadviceopt.DependentAdviceIntraproceduralAnalysis;
import ca.mcgill.sable.clara.weaving.weaver.depadviceopt.DependentAdviceQuickCheck;
import ca.mcgill.sable.clara.weaving.weaver.depadviceopt.ds.Shadow;
import ca.mcgill.sable.clara.weaving.weaver.depadviceopt.ds.Shadow.ShadowDisabledListener;

/**
 * This is a little example that introduces the most important data structures: trace patterns
 * (<i>dependency state machines</i> in the literature) and shadows. Further examples can be found in
 * the classes {@link DependentAdviceQuickCheck}, {@link DependentAdviceFlowInsensitiveAnalysis} and
 * {@link DependentAdviceIntraproceduralAnalysis}.
 */
public class MyNewAnalysis extends AbstractReweavingAnalysis {

    /**
     * Perform the actual analysis.
     * This usually analyzes the woven program looking for optimization potential.
     * When such potential is found, the residues are changed accordingly.
     * abc automatically detects when a {@link ResidueBox} is set and then
     * optimizes the residues before the next analysis or weaving step is run.
     * @return <code>true</code> if the code needs to be rewoven <i>immediately</i>,
     * i.e. before conducting the next analysis; the code is rewoven again once in the end
     * in any case 
     */
    public boolean analyze(){
    	
    	/*
    	 * The most important information that we need to deal with is contained in the Dependent-Advice Info...
    	 */
		DAInfo dai = ((HasDAInfo)Main.v().getAbcExtension()).getDependentAdviceInfo();
		
		/* a tracepattern is essentially a Dependency State Machine;
		 * it combines a state machine with information about the machine's
		 * alphabet
		 */
		Set<TracePattern> tracePatterns = dai.getTracePatterns();
		for (TracePattern tracePattern : tracePatterns) {
			
			tracePattern.getContainer();				//returns the aspect that declares the pattern
			tracePattern.getContainerClass();			//returns the SootClass that implements this aspect
			tracePattern.getFinalSymbols();				//returns all symbols in the alphabet that lead into a final state
			tracePattern.getFormals();					//returns all variable names that are declared for this trace pattern
			tracePattern.getInitialSymbols();			//returns all symbols in the alphabet that lead out of an initial state
			tracePattern.getName();						//returns an informal, unqualified name for this trace pattern; may be compiler-generated; should be unique 
			tracePattern.getStateMachine();				//returns the state machine for this trace pattern
			tracePattern.getSymbolAdviceMethod("foo");	//returns the SootMethod that implements the monitoring advice for symbol "foo"
			tracePattern.getSymbols();					//returns all declared symbols of this trace pattern
			tracePattern.getVariableOrder("foo");		//returns the ordered list of variable names (see getFormals()) that symbol "foo" references
			
		}

		//Let's have a look at all reachable shadows in the program... it's as easy as this!
		EnabledShadowSet reachableActiveShadows = dai.getReachableActiveShadows();
		/* The above call requires a call graph to already have been constructed.
		 * Because we scheduled our pass to run after the Orphan-Shadows Analysis, which
		 * instructs Soot to construct a call graph, this is no problem.
		 */
		
		for (Shadow shadow : reachableActiveShadows) {
			System.err.println(shadow);
			
			shadow.getAdviceBodyInvokeStmt();			//returns the statement that invokes the appropriate advice at the shadow in the woven code
			shadow.getAdviceDecl();						//returns the advice that implements the transition for this shadow's symbol
			shadow.getAdviceFormalNames();				//returns the formal-parameter names of the variables that the shadow's advice binds
			shadow.getAdviceFormalToSootLocal();		//returns a mapping from formal-parameter names to Soot Locals that bind these parameters
			shadow.getBoundSootLocals();				//returns the set of all Soot Locals that the shadow's advice takes as parameter
			shadow.getContainer();						//returns the SootMethod that contains the shadow
			shadow.getID();								//returns the unique ID of this shadow
			shadow.getPosition();						//returns the source code position at which this shadow resides (if available)
			shadow.getSootLocalForAdviceFormalName("x");//returns the local for formal parameter "x" (see getAdviceFormalToSootLocal())
			//shadow.conjoinResidueWith(rhsResidue);	//conjoins this shadow's residue with another residue; useful e.g. for dynamic instrumentation
			//shadow.disable();							//disables this shadow, setting its residue to NeverMatch and notifying all registered listeners
			shadow.registerListener(new ShadowDisabledListener() {	//registers a new listener that is notified when the shadow is disabled;
				public void shadowDisabled(Shadow shadow) {			//EnabledShadowSet is an example of a class that uses this mechanism
				}
			});
			
		}

		int sizeBefore = reachableActiveShadows.size();
		
		//nothing to do
		if(sizeBefore==0) return false;
		
		//An EnabledShadowsSet is a volatile thing. When we disable a shadow... 
		reachableActiveShadows.iterator().next().disable();
		
		//then this automatically removes the shadow from the set!
		assert reachableActiveShadows.size() == sizeBefore-1;
		
		/* This allows analyses to automatically take into account disabled shadows when being
		 * re-iterated. To get a copy of the set that stays as it is, produce a snapshot...
		 */
		
		Set<Shadow> snapshot = reachableActiveShadows.snapshot();
		
		sizeBefore = snapshot.size();

		//when we disable a shadow here... 
		reachableActiveShadows.iterator().next().disable();
		
		//then the snapshot remains as is
		assert snapshot.size() == sizeBefore;
		
    	//do not reweave immediately
    	return false;
    }
	
	
    /**
     * Allows you to add default arguments to Soot, which can be overriden
     * by the user on the commandline.
     * Note that this method does not necessarily to any packs or phases being invoked
     * E.g. providing <code>-p cg enabled</code> does not automatically build a call graph
     * because abc never invokes the <code>cg</code> phase on its own. You have to do so yourself,
     * but can give phase options to the phase by using this method. 
     * @param sootArgs the current list of default arguments; add argument strings
     * as needed
     */
    public void defaultSootArgs(List<String> sootArgs){
    	    //see ca.mcgill.sable.clara.weaving.weaver.depadviceopt.DependentAdviceFlowInsensitiveAnalysis
    		//for an example
    }

    /**
     * Allows you to override arguments to Soot, which were
     * by the user on the commandline.
     * Note that this method does not necessarily to any packs or phases being invoked
     * E.g. providing <code>-p cg enabled</code> does not automatically build a call graph
     * because abc never invokes the <code>cg</code> phase on its own. You have to do so yourself,
     * but can give phase options to the phase by using this method. 
     * @param sootArgs the current list of default arguments; remove, replace or just add
     * arguments as needed
     */
    public void enforceSootArgs(List<String> sootArgs) {
    	
    }
    
    /**
     * This method is invoked immediately before reweaving takes place. It allows
     * you to perform additional work right before reweaving. It is only invoked
     * if {@link #analyze()} returns <code>true</code>.
     */
    public void setupWeaving(){
    	
    }

    /**
     * This method is invoked immediately after reweaving takes place. It allows
     * you to perform additional work right after reweaving. It is only invoked
     * if {@link #analyze()} returns <code>true</code>.
     */
    public void tearDownWeaving(){
    	
    }

	/**
	 * This method is invoked after the last reweaving step. This allows the analysis
	 * to perform cleanup operations, e.g. free memory etc.
	 */
	public void cleanup(){
		
	}
	
}