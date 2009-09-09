/* abc - The AspectBench Compiler
 * Copyright (C) 2009 Eric Bodden
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
 
package ca.mcgill.sable.clara.weaving.residues;

import polyglot.util.InternalCompilerError;
import soot.SootMethod;
import soot.jimple.Jimple;
import soot.jimple.Stmt;
import soot.util.Chain;
import abc.soot.util.LocalGeneratorEx;
import abc.weaving.residues.Residue;
import abc.weaving.tagkit.Tagger;
import abc.weaving.weaver.ConstructorInliningMap;
import abc.weaving.weaver.WeavingContext;

public class DumpResidue extends Residue {

	@Override
	public Stmt codeGen(SootMethod method, LocalGeneratorEx localgen,
			Chain units, Stmt begin, Stmt fail, boolean sense, WeavingContext wc) {
		// We don't expect the frontend/matcher to produce a residue that does this. 
		// There's no reason we couldn't just do the standard "automatic fail" thing 
		// if there was ever a need, though.
		if(!sense) 
		    throw new InternalCompilerError("DumpResidue should never be used negated");

		//we always fail, i.e. do not execute the original advice
		Stmt gotoFail=Jimple.v().newGotoStmt(fail);
	    Tagger.tagStmt(gotoFail, wc);
		units.insertAfter(gotoFail,begin);
		return gotoFail;
	}

	@Override
	public Residue inline(ConstructorInliningMap cim) {
		return this;
	}

	@Override
	public Residue optimize() {
		return this;
	}

	@Override
	public String toString() {
		return "dump";
	}

}
