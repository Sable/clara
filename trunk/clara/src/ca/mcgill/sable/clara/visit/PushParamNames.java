/* abc - The AspectBench Compiler
 * Copyright (C) 2008 Eric Bodden
 *
 * This compiler is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This compiler is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this compiler, in the file LESSER-GPL;
 * if not, write to the Free Software Foundation, Inc.,
 * 59 Temple Place - Suite 330, Boston, MA 02111-1307, USA.
 */

package ca.mcgill.sable.clara.visit;

import java.util.List;
import java.util.Map;

import ca.mcgill.sable.clara.ast.AdviceName;
import ca.mcgill.sable.clara.ast.AdviceNameAndParams;
import ca.mcgill.sable.clara.types.DAAspectType;
import ca.mcgill.sable.clara.types.DAContext;

import polyglot.ast.Formal;
import polyglot.ast.Node;
import polyglot.ast.NodeFactory;
import polyglot.frontend.Job;
import polyglot.types.TypeSystem;
import polyglot.visit.ContextVisitor;
import polyglot.visit.NodeVisitor;

/**
 * This visitor invoked {@link AdviceNameAndParams#defaultParams(Map)} on each
 * {@link AdviceNameAndParams} object. This will set the default parameter
 * names if no such names were given by the user.
 * 
 * @author Eric Bodden
 */
public class PushParamNames extends ContextVisitor {

	public PushParamNames(Job job, TypeSystem ts, NodeFactory nf) {
		super(job, ts, nf);
	}
	
	@Override
	public Node leave(Node parent, Node old, Node n, NodeVisitor v) {
		if(n instanceof AdviceNameAndParams) {
			AdviceNameAndParams adviceNameAndParams = (AdviceNameAndParams) n;
			DAContext context = (DAContext) context();
			Map<AdviceName, List<Formal>> adviceNameToFormals =
				((DAAspectType)context.currentAspect()).getAdviceNameToFormals();
			n = adviceNameAndParams.defaultParams(adviceNameToFormals);
		}
		return super.leave(parent, old, n, v);
	}

}
