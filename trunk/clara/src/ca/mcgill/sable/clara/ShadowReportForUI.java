package ca.mcgill.sable.clara;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import soot.G;
import soot.Unit;
import soot.jimple.Stmt;
import soot.tagkit.SourceFileTag;
import ca.mcgill.sable.clara.fsanalysis.flowanalysis.AnalysisJob;
import ca.mcgill.sable.clara.fsanalysis.flowanalysis.ReachingStatesAnalysis;
import ca.mcgill.sable.clara.fsanalysis.flowanalysis.TransitionInfo;
import ca.mcgill.sable.clara.fsanalysis.flowanalysis.UnnecessaryShadowsAnalysis;
import ca.mcgill.sable.clara.weaving.aspectinfo.AdviceDependency;
import ca.mcgill.sable.clara.weaving.weaver.depadviceopt.ds.Shadow;

public class ShadowReportForUI {	
	
	protected Map<Shadow,TransitionInfo> shadowToInfo = new HashMap<Shadow, TransitionInfo>();
	
	public void initialize(Set<Shadow> allSnabledShadows) {
		for (Shadow shadow : allSnabledShadows) {
			shadowToInfo.put(shadow, new TransitionInfo());
		}
	}
	
	public Set<TransitionInfo> computeResultsForJob(AnalysisJob job) {
		
		SourceFileTag sourceFileTag = (SourceFileTag) job.method().getDeclaringClass().getTag("SourceFileTag");
		if(sourceFileTag==null) {
			G.v().out.println("Cannot find source file for class "+job.method().getDeclaringClass().getName());
			return Collections.emptySet();
		}
		
		String path = sourceFileTag.getAbsolutePath();
		
		Set<Shadow> allShadows = new HashSet<Shadow>(job.allEnabledTMShadowsInMethod());
		
		//compute for every statement, which states can reach this statement from the program entry
		ReachingStatesAnalysis forwardAnalysis = new ReachingStatesAnalysis(job, true);
		forwardAnalysis.doAnalysis();
		
		//compute for every statement the set of states from which one could reach a final state using the remainder of the program
		ReachingStatesAnalysis backwardAnalysis = new ReachingStatesAnalysis(job, false);
		backwardAnalysis.doAnalysis();
		

		//find unnecessary shadows
		UnnecessaryShadowsAnalysis unnecessaryShadowsAnalysis = new UnnecessaryShadowsAnalysis(job,forwardAnalysis,backwardAnalysis);				
				
		Set<TransitionInfo> result = new HashSet<TransitionInfo>();
		
		for (Unit unit : job.unitGraph()) {			
			
			boolean hasShadow = false;
			for (Shadow shadow : allShadows) {
				if(shadow.getAdviceBodyInvokeStmt().equals(unit)) {
					hasShadow = true;
					break;
				}
			}
			if(!hasShadow) continue;
			
			for (Shadow shadow : allShadows) {
				if(shadow.getAdviceBodyInvokeStmt().equals(unit)) {
					
					Set<Shadow> relatedShadowsInSameMethod = relatedSuccessorShadowsInSameMethod(shadow, job);
					
					Set<TransitionInfo> relatedTransInfosInSameMethod = transitionInfosForShadows(relatedShadowsInSameMethod);
					
					Set<Shadow> overlappingShadowsInOtherMethods =
						new HashSet<Shadow>(AdviceDependency.getAllEnabledShadowsOverlappingWith(Collections.singleton(shadow)));
					for (Iterator<Shadow> iterator = overlappingShadowsInOtherMethods.iterator(); iterator.hasNext();) {
						Shadow s = iterator.next();
						if(s.getContainer().equals(job.method())) iterator.remove();
					}
					
					Set<TransitionInfo> overlappingTransInfosInOtherMethods = transitionInfosForShadows(overlappingShadowsInOtherMethods);
					TransitionInfo ti = shadowToInfo.get(shadow);
					ti.initialize(
							path,
							job.symbolNameForShadow(shadow),
							unnecessaryShadowsAnalysis.transitions(shadow),
							unnecessaryShadowsAnalysis.liveStatesSetsAfterTransition(shadow),
							shadow,
							relatedTransInfosInSameMethod,
							overlappingTransInfosInOtherMethods
					);
					result.add(ti);
				}
			}
		}
		
		return result;
	}
	
	private Set<TransitionInfo> transitionInfosForShadows(Set<Shadow> shadows) {
		Set<TransitionInfo> res = new HashSet<TransitionInfo>();
		for (Shadow s : shadows) {
			res.add(shadowToInfo.get(s));
		}
		return res;
	}

	protected Set<Shadow> relatedSuccessorShadowsInSameMethod(Shadow s, AnalysisJob job) {
		Set<Shadow> overlaps = new HashSet<Shadow>(AdviceDependency.getAllEnabledShadowsOverlappingWith(Collections.singleton(s)));
		overlaps.remove(s);
		
		//are there overlapping shadows in the same method at all?
		boolean foundOne = false;
		for (Shadow shadow : overlaps) {
			if(!shadow.getContainer().equals(job.method())) {
				continue;
			}
			foundOne = true;
			break;
		}
		if(!foundOne) {
			return Collections.emptySet();
		}
		
		Set<Shadow> result = new HashSet<Shadow>();
		
		Stmt initialStmt = s.getAdviceBodyInvokeStmt();
		Set<Stmt> visited = new HashSet<Stmt>();
		
		Set<Stmt> worklist = new HashSet<Stmt>();
		worklist.add(initialStmt);
		
		while(!worklist.isEmpty()) {
			Iterator<Stmt> iterator = worklist.iterator();
			Stmt stmt = iterator.next();
			iterator.remove();
			visited.add(stmt);
			Set<Shadow> currShadows = job.enabledShadowsOfStmt(stmt);
			boolean foundShadow = false;
			if(!currShadows.isEmpty()) {
				for(Shadow currShadow: currShadows) {
					if(overlaps.contains(currShadow)) {
						result.add(currShadow);
						foundShadow=true;
					}
				}
			}
			if(!foundShadow) {
				List<Unit> succs = job.unitGraph().getSuccsOf(stmt);
				for (Unit succ : succs) {
					if(!visited.contains(succ)) {
						worklist.add((Stmt)succ);
					}					
				}
			}
		}
		
		return result;
	}

}
