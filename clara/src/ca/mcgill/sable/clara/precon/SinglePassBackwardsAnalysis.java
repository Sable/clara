package ca.mcgill.sable.clara.precon;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import soot.Unit;
import soot.jimple.Stmt;
import abc.tm.weaving.matching.SMNode;
import ca.mcgill.sable.clara.fsanalysis.flowanalysis.AnalysisJob;
import ca.mcgill.sable.clara.fsanalysis.flowanalysis.ConfigurationSet;
import ca.mcgill.sable.clara.fsanalysis.flowanalysis.ReachingStatesAnalysis;
import ca.mcgill.sable.clara.fsanalysis.flowanalysis.WorklistAnalysis;
import ca.mcgill.sable.clara.fsanalysis.flowanalysis.ds.Configuration;
import ca.mcgill.sable.clara.fsanalysis.flowanalysis.ds.Disjunct;
import ca.mcgill.sable.clara.weaving.weaver.depadviceopt.ds.Shadow;

public class SinglePassBackwardsAnalysis extends ReachingStatesAnalysis {

	public SinglePassBackwardsAnalysis(AnalysisJob job) {
		super(job, false);
	}
	
	@Override
	protected Set<ca.mcgill.sable.clara.fsanalysis.flowanalysis.WorklistAnalysis.Job<Unit, ConfigurationSet>> initialJobs() {
		Set<Configuration> initialConfigs = new HashSet<Configuration>();;
		
		
		//first we add for every entry unit the set of configurations that transitively
		//reach this unit in at least one step
		Set<SMNode> initialStates = tracePattern().getStateMachine().getInitialStates();
		for (SMNode node : initialStates) {
			initialConfigs.add(new Configuration(this,Collections.singleton(node),Disjunct.TRUE,null));
		}
		Set<Job<Unit, ConfigurationSet>> jobs = new HashSet<Job<Unit, ConfigurationSet>>();
		
		//in backward mode, we also add an initial configuration just behind every unit that may lead into a final state;
		//this takes care of the semantics of dependency state machines: we execute the body after each matched *prefix*
		for(Unit unit : unitGraph()) {
			Set<Shadow> shadows = getJob().enabledShadowsOfStmt((Stmt) unit);
			if(!shadows.isEmpty()) {
				boolean isFinalTransition = false;
				for (Shadow shadow : shadows) {
					if(tracePattern().getInitialSymbols().contains(getJob().symbolNameForShadow(shadow))) {
						isFinalTransition = true;
						break;
					}
				}
				if(isFinalTransition) {
					jobs.add(new WorklistAnalysis.Job<Unit, ConfigurationSet>(unit,new ConfigurationSet(this,initialConfigs)));			
				}
			}
		}
			
		return jobs;
	}
	
	@Override
	protected Set<Unit> externalSuccsOf(Unit u) {
		Set<Unit> succs = super.externalSuccsOf(u);
		succs.removeAll(getHeads());
		return succs;
	}

}
