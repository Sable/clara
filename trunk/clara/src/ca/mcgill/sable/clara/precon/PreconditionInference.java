package ca.mcgill.sable.clara.precon;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import abc.tm.weaving.matching.SMEdge;
import abc.tm.weaving.matching.SMNode;

import soot.Unit;
import soot.jimple.toolkits.pointer.InstanceKey;
import soot.jimple.toolkits.pointer.LocalMustAliasAnalysis;
import ca.mcgill.sable.clara.fsanalysis.flowanalysis.AnalysisJob;
import ca.mcgill.sable.clara.fsanalysis.flowanalysis.ConfigurationSet;
import ca.mcgill.sable.clara.fsanalysis.flowanalysis.ReachingStatesAnalysis;
import ca.mcgill.sable.clara.fsanalysis.flowanalysis.ds.Configuration;
import ca.mcgill.sable.clara.fsanalysis.flowanalysis.ds.Disjunct;

public class PreconditionInference {

	public void inferPreconditions(AnalysisJob job) {
		LocalMustAliasAnalysis lmaa = job.localMustAliasAnalysis();
		
		SinglePassBackwardsAnalysis backwardAnalysis = new SinglePassBackwardsAnalysis(job);
		backwardAnalysis.doAnalysis();
		
		for (Unit head : job.unitGraph().getHeads()) {
			ConfigurationSet entryFlow = backwardAnalysis.getFlowAfter(head);
			NextConfig:
			for (Configuration config : entryFlow) {
				Disjunct b = config.getBinding();
				Map<String, HashSet<InstanceKey>> posBinding = b.getPosVarBinding();
				
				Map<String,Set<Integer>> varNameToParamIndices = new HashMap<String, Set<Integer>>();
				
				//check whether all positive bindings in this configuration must-alias
				//"this" or one of the parameters of the current method
				for (Map.Entry<String,HashSet<InstanceKey>> varAndKeys : posBinding.entrySet()) {
					String variableName = varAndKeys.getKey();
					HashSet<InstanceKey> keys = varAndKeys.getValue();
					for (InstanceKey instanceKey : keys) {
						String id = lmaa.instanceKeyString(instanceKey.getLocal(), instanceKey.getStmt());
						if(id.equals("UNKNOWN")) continue NextConfig;
						int idNum = Integer.parseInt(id);
						//if the number is larger than 0 then this value does not must-alias "this"
						//nor some method parameter
						if(idNum>0) {
							continue NextConfig;
						} else {
							Set<Integer> indices = varNameToParamIndices.get(variableName);
							if(indices==null) {
								indices = new HashSet<Integer>();
								varNameToParamIndices.put(variableName, indices);
							}
							indices.add(idNum);
						}
					}
				}
				
				if(varNameToParamIndices.isEmpty()) continue NextConfig;
		
				for (SMNode state : config.getStates()) {
					int stateNum = state.getNumber();
					SMNode fwdState = job.tracePattern().getStateMachine().getStateByNumber(stateNum);
					Set<String> nonEquivalentSymbols = new HashSet<String>();
					for(String symbol: job.tracePattern().getSymbols()) {
						boolean foundEdge = false;
						for(Iterator<SMEdge> outEdgeIter = fwdState.getOutEdgeIterator();outEdgeIter.hasNext();) {
							SMEdge outEdge = outEdgeIter.next();
							if(!outEdge.isSkipEdge() && outEdge.getLabel().equals(symbol)) {
								foundEdge = true;
								if(!outEdge.getTarget().isFinalNode() && !config.getStates().contains(outEdge.getTarget())) {
									nonEquivalentSymbols.add(symbol);
									continue;
								}
							}
						}
						if(!foundEdge) {
							nonEquivalentSymbols.add(symbol);
						}
					}
					if(!nonEquivalentSymbols.isEmpty()) {
						System.err.println(nonEquivalentSymbols);
						System.err.println(varNameToParamIndices);
					}
				}
				
			}
		}
	}
}
