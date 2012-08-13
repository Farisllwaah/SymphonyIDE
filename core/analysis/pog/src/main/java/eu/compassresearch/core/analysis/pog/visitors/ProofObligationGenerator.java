/**
 * Proof Obligation Generator Analysis
 *
 * Description: 
 * 
 * This analysis extends the QuestionAnswerAdaptor to generate
 * POs from the AST generated by the CML parser
 *
 */

package eu.compassresearch.core.analysis.pog.visitors;

import eu.compassresearch.core.analysis.pog.obligations.*;
/**
 * Core stuff needed in this simple analysis.
 */
import org.overture.ast.analysis.QuestionAnswerAdaptor;
//import org.overture.ast.expressions.ADivideNumericBinaryExp;
//import org.overture.ast.declarations.ATypeDeclaration;
import org.overture.ast.lex.LexLocation;
import eu.compassresearch.core.analysis.pog.obligations.POContextStack;
import eu.compassresearch.core.analysis.pog.obligations.POContext;
import eu.compassresearch.core.analysis.pog.obligations.ProofObligationList;
import eu.compassresearch.core.analysis.pog.obligations.ProofObligation;
/**
 * Java libraries 
 */
import java.util.LinkedList;
import java.util.List;

public class ProofObligationGenerator extends QuestionAnswerAdaptor<POContextStack, ProofObligationList>
{
    // Constants
    private final static String ANALYSIS_NAME = "Proof Obligation Generator";
    private final static String ANALYSIS_STRING = "Tree location: ";
    
    private int tempcounter = 0;

    // Analysis Result
    private ProofObligationList pos; //should change to ProofObligation type?

    // Constructor setting warnings up
    public ProofObligationGenerator()
    {
		pos = new ProofObligationList(); //change inline with pos type
    }

    /*
 	 * When the DepthFirstAnalysisAdaptor reaches a Divide binary
 	 * expression this method is invoke. Here this analysis wants to
 	 * create a warning and add it to its output.
 	 */
// 	@Override
// 	public void caseADivideNumericBinaryExp(ADivideNumericBinaryExp node) {
// 		super.caseADivideNumericBinaryExp(node);
//    //	String poname = "div" + tempcounter;
    //	tempcounter ++;
 	//	pos.add(createObligation(poname, POType.TEST, node.getLocation()));
// 	}

    /*
 	 * When the DepthFirstAnalysisAdaptor reaches a Type declaration
 	 * this method is invoked. Here this analysis wants to
 	 * create a warning and add it to its output.
 	 */ 	 
// 	@Override
// 	public void caseATypeDeclaration(ATypeDeclaration node) {
// 		super.caseATypeDeclaration(node);
    //	String poname = "type" + tempcounter;
    //	tempcounter ++;
 	//	pos.add(createObligation(poname, POType.TEST, node.getLocation()));
// 	}

    // Pretty warning for the result
 //   private static ProofObligation createObligation(String name, POType kind, LexLocation loc)
//    {
    //	ProofObligation po = new ProofObligation(ProofObligation(name, kind, loc));
//		sb.append(ANALYSIS_STRING);
//		sb.append(" Node found at: ");
//		sb.append(loc.startLine +":"+loc.startPos);
//		sb.append(" to " + loc.endLine + ":"+loc.endPos);
//		return po;
//    }
    
    //output analysis results
 		

    /**
     * Test Method to acquire the result produced by this analysis.
     */
//    public void getResults()
//    {
  //  	System.out.println("   Generation complete. Results:");
 //   	for(String s :pos)
// 		{
// 			System.out.println("\t"+s);
// 		}
//    }
    
    /**
     * The ide/cmdline tool will pick this method up and use it for
     * pretty printing the analysis name. If this method is missing
     * the cmdline tool will use the class name.
     *
     * @return usefriendly name for this analysis.
     */
    public String getAnalysisName() 
    {
		return ANALYSIS_NAME;
    }
    
    
}