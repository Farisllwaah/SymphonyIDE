package eu.compassresearch.core.analysis.modelchecker.visitors;

import org.overture.ast.analysis.AnalysisException;
import org.overture.ast.node.INode;

import eu.compassresearch.ast.analysis.QuestionAnswerCMLAdaptor;
import eu.compassresearch.core.analysis.modelchecker.ast.MCNode;

public class MCEmptyVisitor extends QuestionAnswerCMLAdaptor<CMLModelcheckerContext, StringBuilder> {

	@Override
	public StringBuilder defaultINode(INode node,
			CMLModelcheckerContext question) throws AnalysisException {
		
		throw new ModelcheckerRuntimeException(ModelcheckerErrorMessages.CASE_NOT_IMPLEMENTED.customizeMessage(node.getClass().getSimpleName()));
	}
	
	@Override
	public StringBuilder createNewReturnValue(INode node,
			CMLModelcheckerContext question) throws AnalysisException
	{
		// TODO Auto-generated method stub
		return null;
	}
	
	@Override
	public StringBuilder createNewReturnValue(Object node,
			CMLModelcheckerContext question) throws AnalysisException {
		// TODO Auto-generated method stub
		return null;
	}

}
