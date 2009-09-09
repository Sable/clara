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

package clara.myanalysis;

import java.util.List;

import abc.weaving.weaver.ReweavingPass;
import abc.weaving.weaver.ReweavingPass.ID;

/**
 * @author Eric Bodden
 */
public class AbcExtension extends ca.mcgill.sable.clara.AbcExtension
{
	/**
	 * Create a new pass ID with the name <i>foo</i>. This will allow users
	 * to schedule the analysis using the <tt>-static-analysis foo</tt> command-line
	 * option.
	 */
    public static final ID MY_PASS_ID = new ReweavingPass.ID("foo");
	
	@Override
	public void createReweavingPasses(List<ReweavingPass> passes) {
		super.createReweavingPasses(passes);

		ReweavingPass myNewPass = new ReweavingPass(
				MY_PASS_ID, //the pass ID (see above)
				new MyNewAnalysis(), //the analysis itself
				DEPENDENT_ADVICE_FLOW_INSENSITIVE_ANALYSIS 	//declare that the pass MY_PASS_ID depends on the pass
															//DEPENDENT_ADVICE_FLOW_INSENSITIVE_ANALYSIS being enabled, too
		);		
		//schedule the pass to be run just after DEPENDENT_ADVICE_FLOW_INSENSITIVE_ANALYSIS 
		addAfterPass(passes, myNewPass, DEPENDENT_ADVICE_FLOW_INSENSITIVE_ANALYSIS);
	}
	
	protected void collectVersions(StringBuffer versions)
    {
		//this String will appear when exeucting Clara with the -version parameter
        versions.append("MyAnalysis " + new Version().toString() + "\n on ");
        super.collectVersions(versions);
    }

	
}
