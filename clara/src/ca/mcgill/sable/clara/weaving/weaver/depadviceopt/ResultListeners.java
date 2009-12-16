package ca.mcgill.sable.clara.weaving.weaver.depadviceopt;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import ca.mcgill.sable.clara.fsanalysis.flowanalysis.TransitionInfo;

public class ResultListeners {
	
	

	public static ResultListeners instance;
	
	protected Set<ResultListener> resultListeners = new HashSet<ResultListener>();

	public void registerResultListener(ResultListener l) {
		resultListeners.add(l);
	}
	
	public Set<ResultListener> getResultListeners() {
		return Collections.unmodifiableSet(resultListeners);
	}
	
	public static ResultListeners v() {
		if(instance==null) {
			instance = new ResultListeners();
		}
		return instance;
	}

	private ResultListeners() {
 	}
	
	public static void reset() { instance = null; }

}
