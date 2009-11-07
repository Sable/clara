/**
 * 
 */
package ca.mcgill.sable.clara.fsanalysis.flowanalysis;

import java.util.Map;
import java.util.Set;

import polyglot.util.Position;

public class TransitionInfo {
	protected final Position pos;
	protected final String symbol;
	protected final Map<Integer, Set<Integer>> transitions;
	protected final Set<Set<Integer>> liveStatesAfter;
	
	public TransitionInfo(String symbol, Map<Integer, Set<Integer>> transitions,
			Set<Set<Integer>> liveStatesAfter,
			Position pos) {
		super();
		this.pos = pos;
		this.symbol = symbol;
		this.transitions = transitions;
		this.liveStatesAfter = liveStatesAfter;
	}
	public Position getPosition() {
		return pos;
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
	
	@Override
	public String toString() {
		return "TransitionInfo [pos=" + pos + ", symbol=" + symbol
				+ ", transitions=" + transitions + ", liveStatesAfter="
				+ liveStatesAfter + "]";
	}
	
	
}