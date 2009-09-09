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

package ca.mcgill.sable.clara.ast;

import polyglot.ast.Ext;
import abc.eaj.ast.EAJExtFactory;

/**
 * Extension factory for the Dependent Advice extension.
 * @author Eric Bodden
 */
public interface DAExtFactory extends EAJExtFactory {

	/**
	 * @return extension for advice dependency
	 */
	public Ext extAdviceDependency();

	/**
	 * @return extension for advice name and params
	 */
	public Ext extAdviceNameAndParam();

	/**
	 * @return extension for advice name
	 */
	public Ext extAdviceName();
}
