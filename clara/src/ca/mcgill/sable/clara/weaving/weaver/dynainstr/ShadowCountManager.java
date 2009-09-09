/*
 * Created on 27-Jul-07
 *
 * To change the template for this generated file go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
package ca.mcgill.sable.clara.weaving.weaver.dynainstr;

import java.util.Set;

import ca.mcgill.sable.clara.weaving.weaver.depadviceopt.ds.Shadow;

import ca.mcgill.sable.clara.weaving.weaver.dynainstr.ShadowCountResidue;
import ca.mcgill.sable.clara.weaving.weaver.dynainstr.SpatialPartitioner;

public class ShadowCountManager {
	
	public static void setCountResidues(Set<Shadow> allEnabledShadows) {
		for (Shadow shadow : allEnabledShadows) {
			shadow.conjoinResidueWith(new ShadowCountResidue(SpatialPartitioner.v().getCodeGenNumber(shadow)));
		}
	}

}
