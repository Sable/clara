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
package ca.mcgill.sable.clara.fsanalysis.util;

import java.util.HashMap;

import ca.mcgill.sable.clara.HasDAInfo;
import ca.mcgill.sable.clara.weaving.aspectinfo.DAInfo;
import ca.mcgill.sable.clara.weaving.aspectinfo.TracePattern;
import ca.mcgill.sable.clara.weaving.weaver.depadviceopt.ds.Shadow;

import soot.SootMethod;
import ca.mcgill.sable.clara.fsanalysis.util.SymbolNames;
import abc.main.Main;


/**
 * This class keeps track of the TracePattern symbol name associated with a shadow.
 * @author Eric Bodden
 */
public class SymbolNames {

	protected final DAInfo dai;
	protected HashMap<Shadow, String> shadowToSymbolName;
	protected HashMap<SootMethod, TracePattern> adviceMethodToTracePattern;
	
    private SymbolNames() {
    	dai = ((HasDAInfo)Main.v().getAbcExtension()).getDependentAdviceInfo();
    	shadowToSymbolName = new HashMap<Shadow, String>();
		adviceMethodToTracePattern = new HashMap<SootMethod, TracePattern>();
		
		HasDAInfo gai = (HasDAInfo) Main.v().getAbcExtension();
		for (TracePattern tm : gai.getDependentAdviceInfo().getTracePatterns()) {
			for (String tmSymbol : tm.getSymbols()) {
				adviceMethodToTracePattern.put(tm.getSymbolAdviceMethod(tmSymbol), tm);
			}
		}
    }
    
	/**
	 * Returns the symbol name for the given shadow or <code>null</code> if there is none.
	 */
	public String symbolNameForShadow(Shadow s) {
		String symbolName = shadowToSymbolName.get(s);
		if(symbolName==null) {
			//compute symbol name
			SootMethod adviceMethod = s.getAdviceDecl().getImpl().getSootMethod();
			TracePattern tm = adviceMethodToTracePattern.get(adviceMethod);
			if(tm==null)
				return null;
				
	        for (String sym : tm.getSymbols()) {
				SootMethod am = tm.getSymbolAdviceMethod(sym);
				if(am.equals(adviceMethod)) {
					symbolName = sym;
					break;
				}
			}
	        if(symbolName==null) {
	        	throw new RuntimeException("no symbol name found for shadow!");
	        }       
			shadowToSymbolName.put(s, symbolName);
		}
		
		return symbolName;
	}
	
	/**
	 * Returns <code>true</code>, if the symbol name of the given shadow
	 * is <code>newDaCapoRun</code>.
	 */
	public boolean isArtificialShadow(Shadow s) {
		String symbolName = symbolNameForShadow(s);
		return symbolName!=null && symbolName.equals("newDaCapoRun");
	}
	
	
	//singleton pattern
	
	protected static SymbolNames instance;

    public static SymbolNames v() {
		if(instance==null) {
			instance = new SymbolNames();
		}
		return instance;		
	}
	
	/**
	 * Frees the singleton object. 
	 */
	public static void reset() {
		instance = null;
	}

}
