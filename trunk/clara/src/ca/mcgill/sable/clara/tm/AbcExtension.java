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

package ca.mcgill.sable.clara.tm;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import soot.Scene;
import soot.SootClass;
import soot.SootMethod;
import ca.mcgill.sable.clara.HasDAInfo;
import ca.mcgill.sable.clara.weaving.aspectinfo.DAInfo;
import ca.mcgill.sable.clara.weaving.weaver.depadviceopt.DependentAdviceFlowInsensitiveAnalysis;
import ca.mcgill.sable.clara.weaving.weaver.depadviceopt.DependentAdviceQuickCheck;
import ca.mcgill.sable.clara.weaving.weaver.depadviceopt.ds.Shadow;
import ca.mcgill.sable.clara.weaving.weaver.tracing.Dumper;
import abc.aspectj.parse.AbcLexer;
import abc.aspectj.parse.LexerAction_c;
import abc.aspectj.parse.PerClauseLexerAction_c;
import abc.ja.tm.parse.JavaParser.Terminals;
import abc.main.CompileSequence;
import abc.main.Debug;
import abc.main.Main;
import abc.main.options.OptionsParser;
import abc.tm.weaving.aspectinfo.PerSymbolTMAdviceDecl;
import abc.tm.weaving.aspectinfo.TMAdviceDecl;
import abc.tm.weaving.aspectinfo.TMGlobalAspectInfo;
import abc.tm.weaving.aspectinfo.TraceMatch;
import abc.tm.weaving.weaver.TMWeaver;
import abc.tmwpopt.tmtoda.TracePatternFromTM;
import abc.weaving.aspectinfo.AbstractAdviceDecl;
import abc.weaving.aspectinfo.Aspect;
import abc.weaving.aspectinfo.GlobalAspectInfo;
import abc.weaving.matching.AdviceApplication;
import abc.weaving.matching.MethodAdviceList;
import abc.weaving.residues.NeverMatch;
import abc.weaving.weaver.AspectCodeGen;
import abc.weaving.weaver.ReweavingPass;
import abc.weaving.weaver.Weaver;

/**
 * Abc extension for static whole-program analysis of tracematches. This extension currently exists of three different
 * static analyses:
 * <ol>
 * <li> a quick check, (ECOOP 2007)
 * <li> a flow-insensitive analysis to detect orphan shadows, (ECOOP 2007) and 
 * <li> a flow-sensitive abstract interpretation of tracematches, which is largely intraprocedural.
 * </ol>
 * In this version of abc, the first two analyses are completely reused from the abc.da extension. We simply generate advice
 * dependencies from each tracematch and abc.da then simply performs the {@link DependentAdviceQuickCheck} and {@link DependentAdviceFlowInsensitiveAnalysis}
 * to resolve these dependencies. Therefore, this extension uses parts of both, abc.tm and ca.mcgill.sable.clara.
 * 
 * <b>WARNING:</b> This extension only re-uses abc.da in the backend, not in the frontend! Therefore, users cannot define both dependent advice and
 * tracematches in source code right now!
 * 
 * @author Eric Bodden
 */
public class AbcExtension extends abc.tm.AbcExtension implements HasDAInfo
{
	
	protected boolean didRunAnalysis = false;
	
	/**
	 * The abc.da extension that we use to conduct the various static whole-program analyses for tracematches.
	 */
	protected ca.mcgill.sable.clara.AbcExtension daExtension = new ca.mcgill.sable.clara.AbcExtension() {
		@Override
		protected DAInfo createDependentAdviceInfo() {
			return new DAInfo() {
				@Override
				protected DependentAdviceFlowInsensitiveAnalysis createFlowInsensitiveAnalysis() {
					return new DependentAdviceFlowInsensitiveAnalysis() {
						@Override
						public void warn(Shadow s, String msg) {
							//if this extension is enabled, only warn when removing shadows that belong to per-symbol advice
							//(otherwise we would report for sync/some/body advice as well)
							if(s.getAdviceDecl() instanceof PerSymbolTMAdviceDecl)
								super.warn(s, msg);
						}
					};
				}
				
				@Override
				protected DependentAdviceQuickCheck createQuickCheck() {
					return new DependentAdviceQuickCheck() {
						@Override
						public boolean analyze() {
							boolean res = super.analyze();
							didRunAnalysis = true;
							return res;
						}
						
						protected void warnShadow(abc.weaving.matching.AdviceApplication aa) {
							//if this extension is enabled, only warn when removing shadows that belong to per-symbol advice
							//(otherwise we would report for sync/some/body advice as well)
							if(aa.advice instanceof PerSymbolTMAdviceDecl) {
								super.warnShadow(aa);
							}
						}
					};
				}			
			};
		}
	};

	public AbcExtension() {
		//if this extension is enabled, we want to warn the user about each individual shadow being removed
		//by abc.da, and not just summary information
		OptionsParser.v().set_warn_about_individual_shadows(true);
	}

    public void collectVersions(StringBuffer versions)
    {
        super.collectVersions(versions);
        versions.append(" with TraceMatching and Whole-Program Optimizations " +
                        new abc.tm.Version().toString() +
                        "\n");
    }
    
	public DAInfo getDependentAdviceInfo() {
		return daExtension.getDependentAdviceInfo();
	}
	
	@Override
	public Weaver createWeaver() {
		TMWeaver weaver = new TMWeaver() {
			@Override
			public void weaveGenerateAspectMethods() {
				DAInfo daInfo = getDependentAdviceInfo();
				//for each tracematch, register appropriate advice dependencies with abc.da
				for (TraceMatch tm : ((TMGlobalAspectInfo)getGlobalAspectInfo()).getTraceMatches()) {
					//register advice names
					for(String sym: tm.getSymbols()) {
						String adviceName = tm.getSymbolAdviceMethod(sym).getName();
						daInfo.registerDependentAdvice(tm.getContainer().getName()+"."+adviceName,
								tm.getContainer().getName()+"."+tm.getName()+"."+sym
						);
					}

					//create dependency for tracematch pattern
					daInfo.registerTracePattern(new TracePatternFromTM(tm));					
				}
				super.weaveGenerateAspectMethods();
			}
			
			@Override
			public void weaveAdvice() {
				if(didRunAnalysis) {
					//disable helper advice (some, synch and body) at shadows at which
					//all advice applications for symbol shadows were disabled
					final GlobalAspectInfo gai = Main.v().getAbcExtension().getGlobalAspectInfo();
					Set<AdviceApplication> aas = new HashSet<AdviceApplication>();
					for(SootClass c: Scene.v().getApplicationClasses()) {
						for(SootMethod m: c.getMethods()) {
							MethodAdviceList adviceList = gai.getAdviceList(m);
							if(adviceList!=null) {
								aas.addAll(adviceList.allAdvice());
							}
						}
					}
					Set<Integer> shadowIDsWithSymbolShadowsApplying = new HashSet<Integer>();
					for (AdviceApplication aa : aas) {
						AbstractAdviceDecl advice = aa.advice;
						if(!NeverMatch.neverMatches(aa.getResidue()) && advice instanceof TMAdviceDecl) {
							TMAdviceDecl tmAdvice = (TMAdviceDecl) advice;
							if(!tmAdvice.isBody() && !tmAdvice.isSome() && !tmAdvice.isSynch()) {
								assert advice instanceof PerSymbolTMAdviceDecl;
								shadowIDsWithSymbolShadowsApplying.add(aa.shadowmatch.shadowId);
							}
						}
					}
					for (AdviceApplication aa : aas) {
						if(!shadowIDsWithSymbolShadowsApplying.contains(aa.shadowmatch.shadowId)) {
							AbstractAdviceDecl advice = aa.advice;
							if(advice instanceof TMAdviceDecl) {
								aa.setResidue(NeverMatch.v());
							} 
						}
					}
				}
				
				super.weaveAdvice();
			}
		};
		if(Debug.v().traceExecution) {
	    	weaver.setAspectCodegen(new AspectCodeGen() {
		    		
	    		@Override
	    		public void fillInAspect(Aspect aspect) {
	    			super.fillInAspect(aspect);
	    			Dumper.replaceDependentAdviceBodiesAndExtendSJPInfos(getDependentAdviceInfo(),aspect);
	    		}
		    		
	    	});
		}
		return weaver;
	}
	
    /**
     * Registers the reweaving passes of the dependent-advice abc extension.
     */
    @Override
	public void createReweavingPasses(List<ReweavingPass> passes) {
    	super.createReweavingPasses(passes);
    	daExtension.createReweavingPasses(passes);
    }
    
    @Override
    public void addBasicClassesToSoot() {
    	super.addBasicClassesToSoot();
    	daExtension.addBasicClassesToSoot();
        if(Debug.v().traceExecution) {
        	Scene.v().addBasicClass("org.aspectbench.tm.runtime.internal.Dumper", SootClass.SIGNATURES);
        }
    }
    
    @Override
    public CompileSequence createCompileSequence() {
    	return new abc.ja.tm.CompileSequence(this);
    }

	public void initLexerKeywords(AbcLexer lexer)
	{
	    // Cannot call super to add base keywords unfortunately.
	
	    lexer.addGlobalKeyword("abstract",      new LexerAction_c(new Integer(Terminals.ABSTRACT)));
	    if(!abc.main.Debug.v().java13)
	    	lexer.addGlobalKeyword("assert",        new LexerAction_c(new Integer(Terminals.ASSERT)));
	    lexer.addGlobalKeyword("boolean",       new LexerAction_c(new Integer(Terminals.BOOLEAN)));
	    lexer.addGlobalKeyword("break",         new LexerAction_c(new Integer(Terminals.BREAK)));
	    lexer.addGlobalKeyword("byte",          new LexerAction_c(new Integer(Terminals.BYTE)));
	    lexer.addGlobalKeyword("case",          new LexerAction_c(new Integer(Terminals.CASE)));
	    lexer.addGlobalKeyword("catch",         new LexerAction_c(new Integer(Terminals.CATCH)));
	    lexer.addGlobalKeyword("char",          new LexerAction_c(new Integer(Terminals.CHAR)));
	    lexer.addGlobalKeyword("class",         new LexerAction_c(new Integer(Terminals.CLASS)) {
	                        public int getToken(AbcLexer lexer) {
	                            if(!lexer.getLastTokenWasDot()) {
	                                lexer.enterLexerState(lexer.currentState() == lexer.aspectj_state() ?
	                                        lexer.aspectj_state() : lexer.java_state());
	                            }
	                            return token.intValue();
	                        }
	                    });
	    lexer.addGlobalKeyword("const",         new LexerAction_c(new Integer(Terminals.EOF))); // Disallow 'const' keyword
	    lexer.addGlobalKeyword("continue",      new LexerAction_c(new Integer(Terminals.CONTINUE)));
	    lexer.addGlobalKeyword("default",       new LexerAction_c(new Integer(Terminals.DEFAULT)));
	    lexer.addGlobalKeyword("do",            new LexerAction_c(new Integer(Terminals.DO)));
	    lexer.addGlobalKeyword("double",        new LexerAction_c(new Integer(Terminals.DOUBLE)));
	    lexer.addGlobalKeyword("else",          new LexerAction_c(new Integer(Terminals.ELSE)));
	    lexer.addGlobalKeyword("extends",       new LexerAction_c(new Integer(Terminals.EXTENDS)));
	    lexer.addGlobalKeyword("final",         new LexerAction_c(new Integer(Terminals.FINAL)));
	    lexer.addGlobalKeyword("finally",       new LexerAction_c(new Integer(Terminals.FINALLY)));
	    lexer.addGlobalKeyword("float",         new LexerAction_c(new Integer(Terminals.FLOAT)));
	    lexer.addGlobalKeyword("for",           new LexerAction_c(new Integer(Terminals.FOR)));
	    lexer.addGlobalKeyword("goto",          new LexerAction_c(new Integer(Terminals.EOF))); // disallow 'goto' keyword
	    // if is handled specifically, as it differs in pointcuts and non-pointcuts.
	    //lexer.addGlobalKeyword("if",            new LexerAction_c(new Integer(Terminals.IF)));
	    lexer.addGlobalKeyword("implements",    new LexerAction_c(new Integer(Terminals.IMPLEMENTS)));
	    lexer.addGlobalKeyword("import",        new LexerAction_c(new Integer(Terminals.IMPORT)));
	    lexer.addGlobalKeyword("instanceof",    new LexerAction_c(new Integer(Terminals.INSTANCEOF)));
	    lexer.addGlobalKeyword("int",           new LexerAction_c(new Integer(Terminals.INT)));
	    lexer.addGlobalKeyword("interface",     new LexerAction_c(new Integer(Terminals.INTERFACE),
	                                        new Integer(lexer.java_state())));
	    lexer.addGlobalKeyword("long",          new LexerAction_c(new Integer(Terminals.LONG)));
	    lexer.addGlobalKeyword("native",        new LexerAction_c(new Integer(Terminals.NATIVE)));
	    lexer.addGlobalKeyword("new",           new LexerAction_c(new Integer(Terminals.NEW)));
	    lexer.addGlobalKeyword("package",       new LexerAction_c(new Integer(Terminals.PACKAGE)));
	    lexer.addGlobalKeyword("private",       new LexerAction_c(new Integer(Terminals.PRIVATE)));
	    /* ------------  keyword added to the Java part ------------------ */
	    lexer.addGlobalKeyword("privileged",    new LexerAction_c(new Integer(Terminals.PRIVILEGED)));
	    /* ------------  keyword added to the Java part ------------------ */
	    lexer.addGlobalKeyword("protected",     new LexerAction_c(new Integer(Terminals.PROTECTED)));
	    lexer.addGlobalKeyword("public",        new LexerAction_c(new Integer(Terminals.PUBLIC)));
	    lexer.addGlobalKeyword("return",        new LexerAction_c(new Integer(Terminals.RETURN)));
	    lexer.addGlobalKeyword("short",         new LexerAction_c(new Integer(Terminals.SHORT)));
	    lexer.addGlobalKeyword("static",        new LexerAction_c(new Integer(Terminals.STATIC)));
	    lexer.addGlobalKeyword("strictfp",      new LexerAction_c(new Integer(Terminals.STRICTFP)));
	    lexer.addGlobalKeyword("super",         new LexerAction_c(new Integer(Terminals.SUPER)));
	    lexer.addGlobalKeyword("switch",        new LexerAction_c(new Integer(Terminals.SWITCH)));
	    lexer.addGlobalKeyword("synchronized",  new LexerAction_c(new Integer(Terminals.SYNCHRONIZED)));
	    // this is handled explicitly, as it differs in pointcuts and non-pointcuts.
	    //lexer.addGlobalKeyword("this",          new LexerAction_c(new Integer(Terminals.THIS)));
	    lexer.addGlobalKeyword("throw",         new LexerAction_c(new Integer(Terminals.THROW)));
	    lexer.addGlobalKeyword("throws",        new LexerAction_c(new Integer(Terminals.THROWS)));
	    lexer.addGlobalKeyword("transient",     new LexerAction_c(new Integer(Terminals.TRANSIENT)));
	    lexer.addGlobalKeyword("try",           new LexerAction_c(new Integer(Terminals.TRY)));
	    lexer.addGlobalKeyword("void",          new LexerAction_c(new Integer(Terminals.VOID)));
	    lexer.addGlobalKeyword("volatile",      new LexerAction_c(new Integer(Terminals.VOLATILE)));
	    lexer.addGlobalKeyword("while",         new LexerAction_c(new Integer(Terminals.WHILE)));
	
	    if(abc.main.Debug.v().java15) {
	      lexer.addJavaKeyword("enum", new LexerAction_c(new Integer(Terminals.ENUM)));
	      lexer.addAspectJKeyword("enum", new LexerAction_c(new Integer(Terminals.ENUM)));
	    }
	
	    lexer.addPointcutKeyword("adviceexecution", new LexerAction_c(new Integer(Terminals.PC_ADVICEEXECUTION)));
	    lexer.addPointcutKeyword("args", new LexerAction_c(new Integer(Terminals.PC_ARGS)));
	    lexer.addPointcutKeyword("call", new LexerAction_c(new Integer(Terminals.PC_CALL)));
	    lexer.addPointcutKeyword("cflow", new LexerAction_c(new Integer(Terminals.PC_CFLOW)));
	    lexer.addPointcutKeyword("cflowbelow", new LexerAction_c(new Integer(Terminals.PC_CFLOWBELOW)));
	    lexer.addPointcutKeyword("error", new LexerAction_c(new Integer(Terminals.PC_ERROR)));
	    lexer.addPointcutKeyword("execution", new LexerAction_c(new Integer(Terminals.PC_EXECUTION)));
	    lexer.addPointcutKeyword("get", new LexerAction_c(new Integer(Terminals.PC_GET)));
	    lexer.addPointcutKeyword("handler", new LexerAction_c(new Integer(Terminals.PC_HANDLER)));
	    lexer.addPointcutKeyword("if", new LexerAction_c(new Integer(Terminals.PC_IF),
	                                new Integer(lexer.pointcutifexpr_state())));
	    lexer.addPointcutKeyword("initialization", new LexerAction_c(new Integer(Terminals.PC_INITIALIZATION)));
	    lexer.addPointcutKeyword("parents", new LexerAction_c(new Integer(Terminals.PC_PARENTS)));
	    lexer.addPointcutKeyword("precedence", new LexerAction_c(new Integer(Terminals.PC_PRECEDENCE)));
	    lexer.addPointcutKeyword("preinitialization", new LexerAction_c(new Integer(Terminals.PC_PREINITIALIZATION)));
	    lexer.addPointcutKeyword("returning", new LexerAction_c(new Integer(Terminals.PC_RETURNING)));
	    lexer.addPointcutKeyword("set", new LexerAction_c(new Integer(Terminals.PC_SET)));
	    lexer.addPointcutKeyword("soft", new LexerAction_c(new Integer(Terminals.PC_SOFT)));
	    lexer.addPointcutKeyword("staticinitialization", new LexerAction_c(new Integer(Terminals.PC_STATICINITIALIZATION)));
	    lexer.addPointcutKeyword("target", new LexerAction_c(new Integer(Terminals.PC_TARGET)));
	    lexer.addPointcutKeyword("this", new LexerAction_c(new Integer(Terminals.PC_THIS)));
	    lexer.addPointcutKeyword("throwing", new LexerAction_c(new Integer(Terminals.PC_THROWING)));
	    lexer.addPointcutKeyword("warning", new LexerAction_c(new Integer(Terminals.PC_WARNING)));
	    lexer.addPointcutKeyword("within", new LexerAction_c(new Integer(Terminals.PC_WITHIN)));
	    lexer.addPointcutKeyword("withincode", new LexerAction_c(new Integer(Terminals.PC_WITHINCODE)));
	
	    /* Special redefinition of aspect keyword so that we don't go out of ASPECTJ state
	        and remain in POINTCUT state */
	    lexer.addPointcutKeyword("aspect", new LexerAction_c(new Integer(Terminals.ASPECT)));
	
	    /* ASPECTJ reserved words - these cannot be used as the names of any identifiers within
	       aspect code. */
	    lexer.addAspectJContextKeyword("after", new LexerAction_c(new Integer(Terminals.AFTER),
	                                new Integer(lexer.pointcut_state())));
	    lexer.addAspectJContextKeyword("around", new LexerAction_c(new Integer(Terminals.AROUND),
	                                new Integer(lexer.pointcut_state())));
	    lexer.addAspectJContextKeyword("before", new LexerAction_c(new Integer(Terminals.BEFORE),
	                                new Integer(lexer.pointcut_state())));
	    lexer.addAspectJContextKeyword("declare", new LexerAction_c(new Integer(Terminals.DECLARE),
	                                new Integer(lexer.pointcut_state())));
	    lexer.addAspectJContextKeyword("issingleton", new LexerAction_c(new Integer(Terminals.ISSINGLETON)));
	    lexer.addAspectJContextKeyword("percflow", new PerClauseLexerAction_c(new Integer(Terminals.PERCFLOW),
	                                new Integer(lexer.pointcut_state())));
	    lexer.addAspectJContextKeyword("percflowbelow", new PerClauseLexerAction_c(
	                                new Integer(Terminals.PERCFLOWBELOW), new Integer(lexer.pointcut_state())));
	    lexer.addAspectJContextKeyword("pertarget", new PerClauseLexerAction_c(new Integer(Terminals.PERTARGET),
	                                new Integer(lexer.pointcut_state())));
	    lexer.addAspectJContextKeyword("perthis", new PerClauseLexerAction_c(new Integer(Terminals.PERTHIS),
	                                new Integer(lexer.pointcut_state())));
	    lexer.addAspectJContextKeyword("proceed", new LexerAction_c(new Integer(Terminals.PROCEED)));
	
	    // Overloaded keywords - they mean different things in pointcuts, hence have to be
	    // declared separately.
	    lexer.addJavaKeyword("if", new LexerAction_c(new Integer(Terminals.IF)));
	    lexer.addAspectJKeyword("if", new LexerAction_c(new Integer(Terminals.IF)));
	    lexer.addPointcutIfExprKeyword("if", new LexerAction_c(new Integer(Terminals.IF)));
	    lexer.addJavaKeyword("this", new LexerAction_c(new Integer(Terminals.THIS)));
	    lexer.addAspectJKeyword("this", new LexerAction_c(new Integer(Terminals.THIS)));
	    lexer.addPointcutIfExprKeyword("this", new LexerAction_c(new Integer(Terminals.THIS)));
	    // keywords added to the Java part:
	    lexer.addAspectJKeyword("aspect", new LexerAction_c(new Integer(Terminals.ASPECT),
	                            new Integer(lexer.aspectj_state())));
	    lexer.addPointcutIfExprKeyword("aspect", new LexerAction_c(new Integer(Terminals.ASPECT),
	                            new Integer(lexer.aspectj_state())));
	    lexer.addAspectJKeyword("pointcut", new LexerAction_c(new Integer(Terminals.POINTCUT),
	                            new Integer(lexer.pointcut_state())));
	    lexer.addPointcutIfExprKeyword("pointcut", new LexerAction_c(new Integer(Terminals.POINTCUT),
	                            new Integer(lexer.pointcut_state())));
	    
	    if(!abc.main.Debug.v().pureJava) {
	        lexer.addJavaKeyword("aspect", new LexerAction_c(new Integer(Terminals.ASPECT),
	        					new Integer(lexer.aspectj_state())));
	        lexer.addJavaKeyword("pointcut", new LexerAction_c(new Integer(Terminals.POINTCUT),
	                            new Integer(lexer.pointcut_state())));
	    }
	
	    // keyword for the "cast" pointcut extension
	    lexer.addPointcutKeyword("cast", new LexerAction_c(new Integer(Terminals.PC_CAST)));
	
	    // keyword for the "throw" pointcut extension
	    lexer.addPointcutKeyword("throw", new LexerAction_c(new Integer(Terminals.PC_THROW)));
	
	    // keyword for the "global pointcut" extension
	    if(!Debug.v().noGlobalPointcut)
	    	lexer.addGlobalKeyword("global", new LexerAction_c(new Integer(Terminals.GLOBAL),
	                        new Integer(lexer.pointcut_state())));
	
	    // keyword for the "cflowdepth" pointcut extension
	    lexer.addPointcutKeyword("cflowdepth", new LexerAction_c(new Integer(Terminals.PC_CFLOWDEPTH)));
	    
	    // keyword for the "cflowbelowdepth" pointcut extension
	    lexer.addPointcutKeyword("cflowbelowdepth", new LexerAction_c(new Integer(Terminals.PC_CFLOWBELOWDEPTH)));
	
	    // keyword for the "let" pointcut extension
	    lexer.addPointcutKeyword("let", new LexerAction_c(new Integer(Terminals.PC_LET),
	            new Integer(lexer.pointcutifexpr_state())));
	
	    
	    
	    // keywords for the "monitorenter/monitorexit" pointcut extension
	    if(Debug.v().enableLockPointcuts) {
	        lexer.addPointcutKeyword("lock", new LexerAction_c(new Integer(Terminals.PC_LOCK)));
	        lexer.addPointcutKeyword("unlock", new LexerAction_c(new Integer(Terminals.PC_UNLOCK)));
	    }
	
	
	    if(!Debug.v().noContainsPointcut) {
	    	//keyword for the "contains" pointcut extension
	    	lexer.addPointcutKeyword("contains", new LexerAction_c(new Integer(Terminals.PC_CONTAINS)));
	    }
	    
	    // Array set/get pointcut keywords
	    lexer.addPointcutKeyword("arrayget", new LexerAction_c(new Integer(Terminals.PC_ARRAYGET)));
	    lexer.addPointcutKeyword("arrayset", new LexerAction_c(new Integer(Terminals.PC_ARRAYSET)));
	
	
	    lexer.addAspectJKeyword("tracematch", new LexerAction_c(
	                        new Integer(Terminals.TRACEMATCH)));
	    lexer.addAspectJKeyword("sym", new LexerAction_c(
	                        new Integer(Terminals.SYM)));
	    lexer.addAspectJKeyword("perthread", new LexerAction_c(
	                        new Integer(Terminals.PERTHREAD)));
	    lexer.addAspectJKeyword("frequent", new LexerAction_c(
	                        new Integer(Terminals.FREQUENT)));
	    lexer.addAspectJKeyword("distinct", new LexerAction_c(
	    					new Integer(Terminals.DISTINCT)));
	    lexer.addAspectJKeyword("filtermatch", new LexerAction_c(
				new Integer(Terminals.FILTERMATCH)));
	    lexer.addAspectJKeyword("skipmatch", new LexerAction_c(
				new Integer(Terminals.SKIPMATCH)));
	}
}
