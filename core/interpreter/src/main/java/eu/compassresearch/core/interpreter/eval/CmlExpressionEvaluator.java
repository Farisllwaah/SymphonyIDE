package eu.compassresearch.core.interpreter.eval;


import java.io.File;

import org.overture.ast.lex.LexLocation;
import org.overture.ast.lex.LexNameToken;
import org.overture.ast.node.INode;
import org.overture.config.Settings;
import org.overture.interpreter.debug.DBGPReader;
import org.overture.interpreter.runtime.RootContext;
import org.overture.interpreter.runtime.StateContext;
import org.overture.interpreter.scheduler.BasicSchedulableThread;
import org.overture.interpreter.scheduler.InitThread;
import org.overture.interpreter.scheduler.MainThread;
import org.overture.interpreter.values.CPUValue;
import org.overture.interpreter.values.NameValuePair;
import org.overture.interpreter.values.NaturalValue;
import org.overture.interpreter.values.Value;

import eu.compassresearch.ast.analysis.AnalysisException;
import eu.compassresearch.ast.analysis.QuestionAnswerAdaptor;
import eu.compassresearch.ast.expressions.PExp;
import eu.compassresearch.core.interpreter.runtime.CMLContext;
import eu.compassresearch.transformation.CmlAstToOvertureAst;

public class CmlExpressionEvaluator extends QuestionAnswerAdaptor<CMLContext, Value> {

	CmlAstToOvertureAst transform = new CmlAstToOvertureAst();
	org.overture.interpreter.eval.ExpressionEvaluator ovtExpEval = 
    		new org.overture.interpreter.eval.ExpressionEvaluator();
	
	
	
	public CmlExpressionEvaluator()
	{
		
        
       
	}
	
	@Override
	public Value defaultPExp(PExp node, CMLContext question)
			throws AnalysisException {
						
		
        INode ovtNode = transform.defaultINode(node);
       
        /** The initial execution context. */
//        RootContext initialContext = new StateContext(
//				new LexLocation(), "global environment");
//    	initialContext.setThreadState(null, CPUValue.vCPU);
        
    	//TODO For now we diable VDM typechecking, since something is not working
    	Settings.dynamictypechecks = false;
    	
        Value value = null; 
        
        try
        {
            InitThread initThread = new InitThread(Thread.currentThread());
            BasicSchedulableThread.setInitialThread(initThread);
        	
        	org.overture.interpreter.runtime.Context evalContext = question.getOvertureContext();
        			//new org.overture.interpreter.runtime.Context(location, "process expression", initialContext);
        	evalContext.setThreadState(null, CPUValue.vCPU);
        	//LexNameToken dName = new LexNameToken("Default", "d", location);
        	//evalContext.putNew(new NameValuePair(dName, new NaturalValue(10)));
        	
        	value = ovtNode.apply(ovtExpEval, evalContext);	
        } catch (org.overture.ast.analysis.AnalysisException e)
        {
        	throw new AnalysisException(e.getMessage());
        } 
        
        return value;
	}
	
	
	
}
