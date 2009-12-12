/**
 * 
 */
package ca.mcgill.sable.clara.fsanalysis.flowanalysis;

import java.util.Map;
import java.util.Set;

import ca.mcgill.sable.clara.weaving.weaver.depadviceopt.ds.Shadow;

import polyglot.util.Position;

public class TransitionInfo {
	protected Shadow shadow;
	protected String symbol;
	protected Map<Integer, Set<Integer>> transitions;
	protected Set<Set<Integer>> liveStatesAfter;
	protected Set<TransitionInfo> relatedTransitionsInSameMethod;
	protected Set<TransitionInfo> overlappingTransitionsInOtherMethods;
	protected String path;

	public void initialize(String path,
			String symbol, Map<Integer, Set<Integer>> transitions,
			Set<Set<Integer>> liveStatesAfter, Shadow shadow,
			Set<TransitionInfo> relatedShadowsInSameMethod,
			Set<TransitionInfo> overlappingShadowsInOtherMethods) {		
		this.symbol = symbol;
		this.path = path;
		this.transitions = transitions;
		this.liveStatesAfter = liveStatesAfter;
		this.shadow = shadow;
		this.relatedTransitionsInSameMethod = relatedShadowsInSameMethod;
		this.overlappingTransitionsInOtherMethods = overlappingShadowsInOtherMethods;
	}
	
	public Position getPosition() {
		return shadow.getPosition();
	}
	public String getSymbol() {
		return symbol;
	}
	public Map<Integer, Set<Integer>> getTransitions() {
		return transitions;
	}
	public Set<Set<Integer>> getLiveStatesAfter() {
		return liveStatesAfter;
	}

	public Set<TransitionInfo> getRelatedTransitionsInSameMethod() {
		return relatedTransitionsInSameMethod;
	}
	
	public Set<TransitionInfo> getOverlappingTransitionsInOtherMethods() {
		return overlappingTransitionsInOtherMethods;
	}
	
	public String getPath() {
		return path;
	}
	
	@Override
	public String toString() {
		return "TransitionInfo [liveStatesAfter=" + liveStatesAfter
				+ ", overlappingTransitionsInOtherMethods="
				+ overlappingTransitionsInOtherMethods
				+ ", relatedTransitionsInSameMethod="
				+ relatedTransitionsInSameMethod + ", shadow=" + shadow
				+ ", symbol=" + symbol + ", transitions=" + transitions + "]";
	}	
}
