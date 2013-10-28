package eu.compassresearch.core.typechecker.assistant;

import java.util.List;
import java.util.Vector;

import org.overture.ast.analysis.AnalysisException;
import org.overture.ast.definitions.PDefinition;
import org.overture.typechecker.assistant.ITypeCheckerAssistantFactory;

import eu.compassresearch.ast.definitions.AActionDefinition;
import eu.compassresearch.ast.definitions.AChannelDefinition;
import eu.compassresearch.ast.definitions.AChansetDefinition;

public class CmlDefinitionCollector extends AbstractCmlDefinitionCollector
{

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public CmlDefinitionCollector(ITypeCheckerAssistantFactory af)
	{
		super(af);
	}

	private List<PDefinition> addSelf(PDefinition node)
	{
		List<PDefinition> defs = new Vector<PDefinition>();
		defs.add(node);
		return defs;
	}

	@Override
	public List<PDefinition> caseAChannelDefinition(
			AChannelDefinition node) throws AnalysisException
	{
		return addSelf(node);
	}

	@Override
	public List<PDefinition> caseAChansetDefinition(AChansetDefinition node)
			throws AnalysisException
	{
		return addSelf(node);
	}

	@Override
	public List<PDefinition> caseAActionDefinition(AActionDefinition node)
			throws AnalysisException
	{
		return addSelf(node);
	}

}