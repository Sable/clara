package ca.mcgill.sable.clara.weaving.weaver.depadviceopt;

import java.util.Set;

import ca.mcgill.sable.clara.fsanalysis.flowanalysis.TransitionInfo;


public interface ResultListener {

	void showResult(Set<TransitionInfo> results);

}
