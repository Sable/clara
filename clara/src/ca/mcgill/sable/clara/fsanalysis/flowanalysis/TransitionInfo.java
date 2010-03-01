/**
 * 
 */
package ca.mcgill.sable.clara.fsanalysis.flowanalysis;

import java.util.Iterator;
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
		this.path = path;
		this.symbol = symbol;
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
		return "TransitionInfo "+shadow.getID()+" [path="+path+", liveStatesAfter=" + liveStatesAfter
				+ ", overlappingTransitionsInOtherMethods="
				+ idsOf(overlappingTransitionsInOtherMethods)
				+ ", relatedTransitionsInSameMethod="
				+ idsOf(relatedTransitionsInSameMethod) + ", shadow=" + shadow
				+ ", symbol=" + symbol + ", transitions=" + transitions + "]";
	}

	private static String idsOf(Set<TransitionInfo> infos) {
		StringBuilder b = new StringBuilder();
		for (Iterator<TransitionInfo> iterator = infos.iterator(); iterator.hasNext();) {
			TransitionInfo ti = iterator.next();
			Shadow s = ti.shadow;
			if(s!=null)
				b.append(s.getID());
			else
				b.append("ID not set yet");
			if(iterator.hasNext()) b.append(",");
		}
		return b.toString();
	}	
	
	public int getShadowId() {
		return shadow.getID();
	}
	
	public String getContainerMethodName() {
		return shadow.getContainer().getName();
	}
	
	public String getContainerClassName() {
		return shadow.getContainer().getDeclaringClass().getName();
	}
	
	public String getContainerMethodSignature() {
		return shadow.getContainer().getSignature();
	}
}
