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
package ca.mcgill.sable.clara.weaving.aspectinfo;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import ca.mcgill.sable.clara.ShadowReportForUI;
import ca.mcgill.sable.clara.fianalysis.PathInfoFinder;
import ca.mcgill.sable.clara.fianalysis.PathInfoFinder.PathInfo;
import ca.mcgill.sable.clara.fsanalysis.EnabledShadowSet;
import ca.mcgill.sable.clara.fsanalysis.flowanalysis.ds.Disjunct;
import ca.mcgill.sable.clara.fsanalysis.ranking.OutputDotGraphs;
import ca.mcgill.sable.clara.fsanalysis.ranking.Ranking;
import ca.mcgill.sable.clara.fsanalysis.util.SymbolNames;
import ca.mcgill.sable.clara.weaving.weaver.depadviceopt.DependentAdviceFlowInsensitiveAnalysis;
import ca.mcgill.sable.clara.weaving.weaver.depadviceopt.DependentAdviceIntraproceduralAnalysis;
import ca.mcgill.sable.clara.weaving.weaver.depadviceopt.DependentAdviceQuickCheck;
import ca.mcgill.sable.clara.weaving.weaver.depadviceopt.ResultListeners;
import ca.mcgill.sable.clara.weaving.weaver.depadviceopt.ds.Shadow;

import polyglot.util.ErrorInfo;
import polyglot.util.Position;
import soot.SootClass;
import soot.SootMethod;
import ca.mcgill.sable.clara.weaving.aspectinfo.AdviceDependency;
import ca.mcgill.sable.clara.weaving.aspectinfo.TracePattern;
import abc.main.Main;
import abc.weaving.aspectinfo.AbstractAdviceDecl;
import abc.weaving.aspectinfo.AdviceDecl;
import abc.weaving.aspectinfo.GlobalAspectInfo;

/**
 * An enhanced version of {@link GlobalAspectInfo} which keeps track of advice names
 * and advice dependencies.
 * @author Eric Bodden
 */
public class DAInfo {

	protected Map<String,String> adviceMethodNameToAdviceShortName = new HashMap<String,String>();
	protected Set<AdviceDependency> adviceDependencies = new HashSet<AdviceDependency>();
	protected Set<TracePattern> tracePatterns = new HashSet<TracePattern>();

	/** The quick check for dependent advice. */
	protected DependentAdviceQuickCheck quickCheck;

	/** The flow-insensitive analysis for dependent advice. */
	protected DependentAdviceFlowInsensitiveAnalysis flowInsensitiveAnalysis;
	
	/** The intraprocedural flow-sensitive analysis for dependent advice. */
	protected DependentAdviceIntraproceduralAnalysis intraproceduralAnalysis;
	
	/** The set of enabled shadows that are reachable from the program's entry point. */
	protected EnabledShadowSet reachableActiveShadows;

	/** The set of enabled shadows in the program. */
	protected EnabledShadowSet allActiveShadows;
	
	/** The shadow report for the graphical UI */
	protected ShadowReportForUI report;

	/**
	 * Registers a new advice dependency.
	 */
	public void addAdviceDependency(AdviceDependency ad) {
		adviceDependencies.add(ad);
	}
	
	/**
	 * Returns the unmodifiable set of all advice dependencies.
	 */
	public Set<AdviceDependency> getAdviceDependencies() {
		filterRedundantDependencies(adviceDependencies);		
		return Collections.unmodifiableSet(adviceDependencies);		
	}
	
    /**
     * Registers all necessary advice dependencies for the given TracePattern with the ca.mcgill.sable.clara extension.
     * This method does <i>not</i> register dependent advice themselves, only dependencies between those.
     * Call {@link #registerDependentAdvice(String)} or {@link #registerDependentAdvice(String, String)}
     * to register a dependent advice. 
     * @param tp a TracePattern
     * @return A set that contains references to the dependencies that were registered for this TracePattern.
     */
    public Set<AdviceDependency> registerTracePattern(TracePattern tp) {
    	Set<AdviceDependency> result = new HashSet<AdviceDependency>();
	
    	Set<AdviceDependency> deps = new HashSet<AdviceDependency>();
    	
		//construct path infos and register dependencies
		Set<PathInfo> pathInfos = new PathInfoFinder(tp).getPathInfos();
    	for (PathInfo pathInfo : pathInfos) {
			Map<String,List<String>> strongAdviceNameToVars = new HashMap<String, List<String>>();
			Set<String> strongSymbols = new HashSet<String>(pathInfo.getDominatingLabels());
			for (String strongSymbol : strongSymbols) {
				List<String> variableOrder = tp.getVariableOrder(strongSymbol);
				String adviceName = tp.getSymbolAdviceMethod(strongSymbol).getName();
				adviceName = replaceForHumanReadableName(tp.getContainer().getName()+"."+adviceName);
				strongAdviceNameToVars.put(adviceName, variableOrder);
			}
			
			Map<String,List<String>> weakAdviceNameToVars = new HashMap<String, List<String>>();
			Set<String> weakSymbols = new HashSet<String>(pathInfo.getSkipLoopLabels());
			weakSymbols.removeAll(strongSymbols);	//symbols that are strong, may not be declared weak as well
			for (String weakSymbol : weakSymbols) {
				List<String> variableOrder = tp.getVariableOrder(weakSymbol);
				String adviceName = tp.getSymbolAdviceMethod(weakSymbol).getName();
				adviceName = replaceForHumanReadableName(tp.getContainer().getName()+"."+adviceName);
				weakAdviceNameToVars.put(adviceName, variableOrder);
			}
			
			AdviceDependency adviceDependency = new AdviceDependency(
					strongAdviceNameToVars,
					weakAdviceNameToVars,
					tp.getContainer(),
					Position.compilerGenerated()
			);
			deps.add(adviceDependency);
		}

    	for(AdviceDependency adviceDependency: deps){
			addAdviceDependency(adviceDependency);
			result.add(adviceDependency);
    	}
		
		addTracePattern(tp);
		
		return result; 
    }

	private void filterRedundantDependencies(Set<AdviceDependency> deps) {
		for (Iterator<AdviceDependency> iterator = deps.iterator(); iterator.hasNext();) {
			Set<AdviceDependency> copy = new HashSet<AdviceDependency>(deps);
			AdviceDependency dep = iterator.next();
			for (AdviceDependency dep2 : copy) {
				if(dep.equals(dep2)) continue;
				Set<String> d2StrongAdvice = dep2.strongAdviceNameToVars.keySet();
				Set<String> dStrongAdvice = dep.strongAdviceNameToVars.keySet();
				Set<String> d2AllAdvice = dep2.adviceNames();
				Set<String> dAllAdvice = dep.adviceNames();

				if(dStrongAdvice.containsAll(d2StrongAdvice) && dAllAdvice.containsAll(d2AllAdvice)) {
					iterator.remove();
					break;
				}
			}
		}
	}

	/**
	 * Registers a dependent advice and a human-readable name for it (the name given to it in the frontend and in the
	 * dependency declarations).
	 * Both names must be qualified by the aspect type's name.
	 */
	public void registerDependentAdvice(String internalAdviceName, String humanReadableName) {
		if(!internalAdviceName.contains(".")) {
			throw new IllegalArgumentException("Internal advice name has to be qualified!");
		}
		if(adviceMethodNameToAdviceShortName.containsKey(internalAdviceName)) {
			if(!adviceMethodNameToAdviceShortName.get(internalAdviceName).equals(humanReadableName))
				throw new RuntimeException("already registered different human readable name!");
		}
		
		
		if(!humanReadableName.contains(".")) {
			throw new IllegalArgumentException("Human readable advice name has to be qualified!");
		}
		adviceMethodNameToAdviceShortName.put(internalAdviceName, humanReadableName);
	}
	
	/**
	 * Returns for a fully qualified internal advice name the fully qualified human-readable name
	 * if such a name was previously registered. Otherwise, the original name is returned. 
	 */
	public String replaceForHumanReadableName(String internalAdviceName) {
		String hrn = adviceMethodNameToAdviceShortName.get(internalAdviceName);
		if(hrn!=null) {
			return hrn;
		} else {
			return internalAdviceName;
		}
	}
	
	/**
	 * Returns <code>true</code> if the given advice declaration resembles a dependent advice.
	 */
	public boolean isDependentAdvice(AbstractAdviceDecl ad) {
		//can only be a dependent advice if it is a proper AdviceDecl 
		if(ad instanceof AdviceDecl) {
			AdviceDecl adviceDecl = (AdviceDecl) ad;
			String fullName = qualifiedNameOfAdvice(adviceDecl);
			return adviceMethodNameToAdviceShortName.containsKey(fullName);
		}
		return false;
	}
	
	public boolean isDependentAdviceMethod(SootClass container, SootMethod m) {
		return adviceMethodNameToAdviceShortName.containsKey(container.getName()+"."+m.getName());
	}

	public String qualifiedNameOfAdvice(AdviceDecl adviceDecl) {
		return adviceDecl.getAspect().getName()+"."+adviceDecl.getImpl().getSootMethod().getName();
	}
	
	/**
	 * Performs a consistency check on dependent advice declarations. Each dependent advice must be mentioned in at
	 * least one dependency declaration. Also each dependency declaration may only refer to existing dependent advice. 
	 * @return <code>false</code> is an error was found
	 */
	public boolean consistencyCheckForDependentAdvice() {
		boolean foundError = false;
		
		GlobalAspectInfo gai = Main.v().getAbcExtension().getGlobalAspectInfo();
		
		Set<String> qualifiedDependentAdviceNamesDeclared = new HashSet<String>();
		for (AbstractAdviceDecl ad : gai.getAdviceDecls()) {
			if(isDependentAdvice(ad)) {				
				qualifiedDependentAdviceNamesDeclared.add(replaceForHumanReadableName(qualifiedNameOfAdvice((AdviceDecl) ad)));
			}
		}
		
		Set<String> qualifiedDependentAdviceNamesFound = new HashSet<String>();		
		//check that each advice mentioned really exists in code
		for (AdviceDependency dep : getAdviceDependencies()) {
			for (String qualifiedAdviceName  : dep.adviceNames()) {
				if(!qualifiedDependentAdviceNamesDeclared.contains(qualifiedAdviceName)) {
					Main.v().getAbcExtension().forceReportError(ErrorInfo.SEMANTIC_ERROR,
					"Advice with name '"+qualifiedAdviceName+
							"' mentioned in dependency is not found in aspect "+dep.getContainer().getName()+"!", dep.getPosition());
					foundError = true;
				}
				qualifiedDependentAdviceNamesFound.add(qualifiedAdviceName);
			}
			
			if(dep.strongAdviceNameToVars.isEmpty()) {
				Main.v().getAbcExtension().forceReportError(ErrorInfo.SEMANTIC_ERROR,
						"Dependency group declares no strong advice!", dep.getPosition());
				foundError = true;
			}
		}
		
		for (AbstractAdviceDecl ad : gai.getAdviceDecls()) {
			if(isDependentAdvice(ad)) {
				String qualified = replaceForHumanReadableName(qualifiedNameOfAdvice((AdviceDecl) ad));
				if(!qualifiedDependentAdviceNamesFound.contains(qualified)) {
					Main.v().getAbcExtension().forceReportError(ErrorInfo.SEMANTIC_ERROR,
							"Dependent advice '"+qualified+"' is never " +
							"referenced in any dependency declaration.", ad.getPosition());
					foundError = true;
				}
			}
		}
		return !foundError;
	}
	
	public Set<TracePattern> getTracePatterns() {
		return Collections.unmodifiableSet(tracePatterns);
	}
	
	protected void addTracePattern(TracePattern tp) {
		tracePatterns.add(tp);
	}

	/**
	 * Creates the unique instance of the quick check. Extensions may override this method in order
	 * to instantiate their own version of a quick check instead.
	 */
	protected DependentAdviceQuickCheck createQuickCheck() {
		return new DependentAdviceQuickCheck();
	}
	
	/**
	 * Creates the unique instance of the flow-insensitive analysis. Extensions may override this method in order
	 * to instantiate their own version of the analysis instead.
	 */
	protected DependentAdviceFlowInsensitiveAnalysis createFlowInsensitiveAnalysis() {
		return new DependentAdviceFlowInsensitiveAnalysis();
	}

	/**
	 * Creates the unique instance of the flow-insensitive analysis. Extensions may override this method in order
	 * to instantiate their own version of the analysis instead.
	 */
	protected DependentAdviceIntraproceduralAnalysis createIntraproceduralAnalysis() {
		return new DependentAdviceIntraproceduralAnalysis();
	}

	/**
	 * @inheritDoc
	 */
	public DependentAdviceQuickCheck quickCheck() {
		if(quickCheck==null)
			quickCheck = createQuickCheck();
		return quickCheck;
	}
	
	/**
	 * @inheritDoc
	 */
	public DependentAdviceFlowInsensitiveAnalysis flowInsensitiveAnalysis() {
		if(flowInsensitiveAnalysis==null)
			flowInsensitiveAnalysis = createFlowInsensitiveAnalysis();
		return flowInsensitiveAnalysis;
	}

	/**
	 * @inheritDoc
	 */
	public DependentAdviceIntraproceduralAnalysis intraProceduralAnalysis() {
		if(intraproceduralAnalysis==null)
			intraproceduralAnalysis = createIntraproceduralAnalysis();
		return intraproceduralAnalysis;
	}
	
	/**
	 * Resets all static data structures used for static tracematch optimizations.
	 */
	public void resetAnalysisDataStructures() {
		Ranking.reset();
		SymbolNames.reset();
		OutputDotGraphs.reset();    
		Disjunct.reset();
		ResultListeners.reset();
	}
	
	/**
	 * Returns the set of active shadows that are reachable from the program's entry point(s).
	 * This method returns an {@link EnabledShadowSet}, which is a set from which shadows get
	 * automatically removed as they get disabled.
	 */
	public EnabledShadowSet getReachableActiveShadows() {
		if(reachableActiveShadows==null) {
			reachableActiveShadows = new EnabledShadowSet(Shadow.reachableActiveShadows());
		}
		return reachableActiveShadows;
	}

	/**
	 * Returns the set of active shadows in the program (reachable or not).
	 * This method returns an {@link EnabledShadowSet}, which is a set from which shadows get
	 * automatically removed as they get disabled.
	 */
	public EnabledShadowSet getAllActiveShadows() {
		if(allActiveShadows==null) {
			allActiveShadows = new EnabledShadowSet(Shadow.allActiveShadows());
		}
		return allActiveShadows;
	}
	
	/**
	 * Returns the first advice declaration with the stated unqualified (!) advice name.
	 */
	public AdviceDecl findAdviceDeclWithName(String dependentAdviceName) {
		List<AbstractAdviceDecl> adviceDecls = Main.v().getAbcExtension().getGlobalAspectInfo().getAdviceDecls();
		for(Map.Entry<String,String> entry : adviceMethodNameToAdviceShortName.entrySet()) {
			String humanReadableName = entry.getValue();
			if(humanReadableName.endsWith("."+dependentAdviceName)) {
				String internalAdviceName = entry.getKey();
				for (AbstractAdviceDecl abstractAdviceDecl : adviceDecls) {
					if(abstractAdviceDecl instanceof AdviceDecl) {
						AdviceDecl adviceDecl = (AdviceDecl) abstractAdviceDecl;
						String adviceName = adviceDecl.getAspect().getName()+"."+adviceDecl.getImpl().getName();
						if(adviceName.equals(internalAdviceName)) {
							return adviceDecl;
						}
					}
				}
			}			
		}		
		throw new InternalError("No appropriate advice declaration found");
	}
	
	public ShadowReportForUI shadowReport() {
		if(report == null) report = new ShadowReportForUI();
		return report;
	}
}
