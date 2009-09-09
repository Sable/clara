/* abc - The AspectBench Compiler
 * Copyright (C) 2008 Eric Bodden
 *
 * This compiler is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This compiler is distributed in the hope that it will be useful,
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

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import ca.mcgill.sable.clara.HasDAInfo;
import ca.mcgill.sable.clara.fsanalysis.pointsto.CustomizedDemandCSPointsTo;
import ca.mcgill.sable.clara.fsanalysis.ranking.PFGs;
import ca.mcgill.sable.clara.weaving.aspectinfo.AdviceDependency;
import ca.mcgill.sable.clara.weaving.aspectinfo.DAInfo;
import ca.mcgill.sable.clara.weaving.weaver.depadviceopt.ds.Bag;
import ca.mcgill.sable.clara.weaving.weaver.depadviceopt.ds.Shadow;

import polyglot.util.ErrorInfo;
import soot.PackManager;
import soot.PointsToAnalysis;
import soot.Scene;
import soot.SootMethod;
import soot.jimple.spark.ondemand.DemandCSPointsTo;
import soot.jimple.spark.ondemand.LazyContextSensitivePointsToSet;
import abc.main.Debug;
import abc.main.Main;
import abc.main.options.OptionsParser;
import abc.weaving.aspectinfo.AdviceDecl;
import abc.weaving.matching.AdviceApplication;
import abc.weaving.residues.NeverMatch;
import abc.weaving.weaver.AbstractReweavingAnalysis;
import abc.weaving.weaver.AdviceApplicationVisitor;


/**
 * Performs the flow-insensitive pointer analysis for dependent advice.
 * First it produces a set of consistent shadow groups. A consistent shadow group for an advice dependency dep
 * returns strong shadows of strong advice in dep and weak shadows of weak advice in dep, but only if
 * those shadows have overlapping points-to sets at those positions where dep uses the same variable.
 * 
 * A shadow of a dependent advice can be disabled if this shadow is not part of any consistent shadow group.
 * @author Eric Bodden				
 */
public class DependentAdviceFlowInsensitiveAnalysis extends AbstractReweavingAnalysis {

	protected int numEnabledDependentAdviceShadowsBefore = -1;
	
	protected Set<Shadow> stillActiveDependentAdviceShadows = new HashSet<Shadow>();

	/**
	 * {@inheritDoc}
	 */
	public boolean analyze() {
		
		boolean debug = Debug.v().debugDA;
		
		final DAInfo dai = ((HasDAInfo)Main.v().getAbcExtension()).getDependentAdviceInfo();

		if(Debug.v().debugDA) {
			numEnabledDependentAdviceShadowsBefore = 0;
			//disable all advice applications that belong to "other" dependent advice
			AdviceApplicationVisitor.v().traverse(new AdviceApplicationVisitor.AdviceApplicationHandler() {

				public void adviceApplication(AdviceApplication aa, SootMethod m) {
					boolean isDependent = dai.isDependentAdvice(aa.advice);
					if (isDependent && !NeverMatch.neverMatches(aa.getResidue())) {
						numEnabledDependentAdviceShadowsBefore++;
					}
				}			
			});
		}
		
		final Set<AdviceDependency> adviceDependencies = new HashSet<AdviceDependency>(dai.getAdviceDependencies());
		//prune dependencies that already fail the quick check; those we don't care about
		for (Iterator<AdviceDependency> depIter = adviceDependencies.iterator(); depIter.hasNext();) {
			AdviceDependency ad = (AdviceDependency) depIter.next();
			if(!ad.fulfillsQuickCheck()) {
				depIter.remove();
			}
		}		
		if(adviceDependencies.isEmpty()) return false;

		long before = System.currentTimeMillis();

		dai.getAllActiveShadows();
		
		if(debug)
			System.err.println("da: Computing points-to sets");
		//perform pointer analysis if necessary
		if(!Scene.v().hasCallGraph() || !Scene.v().hasPointsToAnalysis()) {
			runCGPhase();
		}

		if(debug) {
			System.err.println("da:    Computing points-to sets: " +(System.currentTimeMillis()-before));
			System.err.println("da: Determining reachable and unreachable shadows");
		}
		before = System.currentTimeMillis();
		
		//compute all active shadows reachable from entry points
		final Set<Shadow> reachableActiveShadows = dai.getReachableActiveShadows();
		
		//filter out shadows that do not belong to a dependent advice
		filterForDependentAdvice(reachableActiveShadows);	
		
		disableAndFilterShadowsWithEmptyPointsToSet(reachableActiveShadows);
		
		//compute all active shadows in the program (including unreachable ones)
		final Set<Shadow> allActiveShadows = dai.getAllActiveShadows();
		
		//filter out shadows that do not belong to a dependent advice
		filterForDependentAdvice(allActiveShadows);
				
		//disable all unreachable shadows
		for (Shadow shadow : allActiveShadows) {
			if(!reachableActiveShadows.contains(shadow)) {
				shadow.disable();
				if(OptionsParser.v().warn_about_individual_shadows())
					warn(shadow,"Shadow disabled because it was deemed unreachable from main method in: "+Scene.v().getMainClass());
			}			
		}

		if(debug) {
			System.err.println("da:    Determining reachable and unreachable shadows took: " +(System.currentTimeMillis()-before));
			System.err.println("da:    Number of reachable DA-Shadows with non-empty points-to sets: "+reachableActiveShadows.size());
			System.err.println("da:    Computing Consistent Shadow Groups");
		}

		//compute consistent shadow groups and build the set of shadows to retain
		before = System.currentTimeMillis();
		for (AdviceDependency dep : adviceDependencies) {
			dep.computeConsistentShadowGroups(reachableActiveShadows);
		}
		if(debug) {
			System.err.println("da:    Shadow groups took: " +(System.currentTimeMillis()-before));
			System.err.println("da: Disabling shadows");
		}

		before = System.currentTimeMillis();

		//disable all shadows that have no support by any group
		Bag<AdviceDecl> shadowsDisabledPerAdviceDecl =
			AdviceDependency.disableShadowsWithNoStrongSupportByAnyGroup(reachableActiveShadows);		

		final Map<AdviceDecl,Integer> adviceToNumTotal = new HashMap<AdviceDecl, Integer>();
		for (Shadow shadow : allActiveShadows) {						
			//count total number
			Integer num = adviceToNumTotal.get(shadow.getAdviceDecl());
			if(num==null) num = 0;
			num++;
			adviceToNumTotal.put(shadow.getAdviceDecl(),num);			
		}
		
		//give warnings
		for (AdviceDecl ad : adviceToNumTotal.keySet()) {
			Integer disabled = shadowsDisabledPerAdviceDecl.countOf(ad);
			if(disabled>0) {
				Integer total = adviceToNumTotal.get(ad);
				
				if(!OptionsParser.v().warn_about_individual_shadows()) {
					//generate summary warning
					
					warn(ad, total, disabled);
				}
			}
		}
		
		stillActiveDependentAdviceShadows = new HashSet<Shadow>(reachableActiveShadows);
		for (Iterator<Shadow> iter = stillActiveDependentAdviceShadows.iterator(); iter.hasNext();) {
			Shadow shadow = iter.next();
			if(!shadow.isEnabled()) {
				iter.remove();
			}
		}
		
		if(Debug.v().debugDA) {
			int numEnabledDependentAdviceShadowsAfter = 0;
			for (Shadow s : reachableActiveShadows) {
				if(s.isEnabled()) {
					numEnabledDependentAdviceShadowsAfter++;
				}
			}			
			
			System.err.println("da:    Disabling shadows took:            "+(System.currentTimeMillis()-before));
			System.err.println("da:    DA-Shadows enabled before FlowIns: "+numEnabledDependentAdviceShadowsBefore);  
			System.err.println("da:    DA-Shadows enabled after  FlowIns: "+numEnabledDependentAdviceShadowsAfter);  

			CustomizedDemandCSPointsTo pta = (CustomizedDemandCSPointsTo) Scene.v().getPointsToAnalysis();
			System.err.println("da:    PTA requests: "+pta.requests);
			System.err.println("da:    PTA retries:  "+pta.retry);
			System.err.println("da:    PTA success:  "+pta.success);
			int all=0, cs=0;
			for (Shadow s : reachableActiveShadows) {
				for(String var: s.getAdviceFormalNames()) {
					LazyContextSensitivePointsToSet pointsToSetOf = (LazyContextSensitivePointsToSet) s.pointsToSetOf(var);
					all++;
					if(pointsToSetOf.isContextSensitive()) {
						cs++;
					}
				}
			}
			
    		System.err.println("Points-to sets: "+all);
    		System.err.println("Points-to sets for which context was tried to be computed: "+cs);
			
			if(Debug.v().outputPFGs)
				PFGs.v().dump("after first flow-insensitive stage", stillActiveDependentAdviceShadows, true);
		}
		
		return false;
	}
	
	protected void disableAndFilterShadowsWithEmptyPointsToSet(Set<Shadow> shadows) {
		for (Iterator<Shadow> iter = shadows.iterator(); iter.hasNext();) {
			Shadow shadow = iter.next();
			for (String var : shadow.getAdviceFormalNames()) {				
				if(!shadow.isPrimitiveFormal(var) && shadow.pointsToSetOf(var).isEmpty()) {
					shadow.disable();
					if(OptionsParser.v().warn_about_individual_shadows())
						warn(shadow,"Shadow disabled because it has empty points-to sets.");
					iter.remove();
					break;
				}
			}
		}
		
	}

	public Set<Shadow> getDependentAdviceShadowsEnabledAfterThisStage() {
		return stillActiveDependentAdviceShadows;
	}

	protected void filterForDependentAdvice(Set<Shadow> reachableActiveShadows) {
		final DAInfo dai = ((HasDAInfo)Main.v().getAbcExtension()).getDependentAdviceInfo();
		for (Iterator<Shadow> shadowIter = reachableActiveShadows.iterator(); shadowIter.hasNext();) {
			Shadow s = shadowIter.next();
			if(!dai.isDependentAdvice(s.getAdviceDecl())) {
				shadowIter.remove();
			}
		}
	}

	protected void runCGPhase() {
		PackManager.v().getPack("cg").apply();
		
		PointsToAnalysis pta = Scene.v().getPointsToAnalysis();
		CustomizedDemandCSPointsTo customPta = new CustomizedDemandCSPointsTo((DemandCSPointsTo) pta);
		Scene.v().setPointsToAnalysis(customPta);
	}

	public void warn(Shadow s,String msg) {
		Main.v().getAbcExtension().forceReportError(
				ErrorInfo.WARNING,
				msg,
				s.getPosition()
		);
	}

	public void warn(AdviceDecl ad, int total, int disabled) {
		String msg = disabled + " out of " + total;
		
		Main.v().getAbcExtension().forceReportError(
				ErrorInfo.WARNING,
				msg + " shadows of dependent advice will not be executed because " +
					  "their dependencies are not fulfilled.",
				ad.getPosition()
		);
	}
	
	/** 
     * {@inheritDoc}
     */
	@Override
    public void defaultSootArgs(List<String> sootArgs) {
        //keep line numbers
        sootArgs.add("-keep-line-number");
    	//enable whole program mode
        sootArgs.add("-w");
        //disable all packs we do not need
        sootArgs.add("-p");
        sootArgs.add("wjtp");
        sootArgs.add("enabled:false");
        sootArgs.add("-p");
        sootArgs.add("wjop");
        sootArgs.add("enabled:false");
        sootArgs.add("-p");
        sootArgs.add("wjap");
        sootArgs.add("enabled:false");
        
    	//enable points-to analysis
        sootArgs.add("-p");
        sootArgs.add("cg");
        sootArgs.add("enabled:true");

        //enable Spark
        sootArgs.add("-p");
        sootArgs.add("cg.spark");
        sootArgs.add("enabled:true");

        //use on-demand points-to analysis within Spark
        sootArgs.add("-p");
        sootArgs.add("cg.spark");
        sootArgs.add("cs-demand:true");
        
        //model singletons EMPTY_LIST etc. as distinct allocation sites
        sootArgs.add("-p");
        sootArgs.add("cg.spark");
        sootArgs.add("empties-as-allocs:true");   
        
//        sootArgs.add("-p");
//        sootArgs.add("cg.spark");
//        sootArgs.add("on-fly-cg:false");
        
        OptionsParser.v().set_tag_instructions(true);
    }
	
	@Override
	public void cleanup() {
		stillActiveDependentAdviceShadows = null;
	}

}
