package eu.compassresearch.core.typechecker;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.overture.ast.analysis.AnalysisException;
import org.overture.ast.definitions.AClassClassDefinition;
import org.overture.ast.definitions.PDefinition;
import org.overture.ast.factory.AstFactory;
import org.overture.ast.lex.LexIdentifierToken;
import org.overture.ast.lex.LexLocation;
import org.overture.ast.lex.LexNameList;
import org.overture.ast.lex.LexNameToken;
import org.overture.ast.node.INode;

import eu.compassresearch.ast.actions.ASingleGeneralAssignmentStatementAction;
import eu.compassresearch.ast.analysis.AnalysisCMLAdaptor;
import eu.compassresearch.ast.declarations.ATypeSingleDeclaration;
import eu.compassresearch.ast.declarations.PSingleDeclaration;
import eu.compassresearch.ast.definitions.AChannelNameDefinition;
import eu.compassresearch.ast.definitions.AChannelsDefinition;
import eu.compassresearch.ast.definitions.AClassDefinition;
import eu.compassresearch.ast.definitions.AFunctionsDefinition;
import eu.compassresearch.ast.definitions.AProcessDefinition;
import eu.compassresearch.ast.definitions.ATypesDefinition;
import eu.compassresearch.ast.definitions.AValuesDefinition;
import eu.compassresearch.ast.program.AFileSource;
import eu.compassresearch.ast.program.AInputStreamSource;
import eu.compassresearch.ast.program.ATcpStreamSource;
import eu.compassresearch.ast.program.PSource;
import eu.compassresearch.ast.types.ATypeParagraphType;
import eu.compassresearch.core.typechecker.api.TypeCheckQuestion;
import eu.compassresearch.core.typechecker.api.TypeIssueHandler;

@SuppressWarnings("serial")
public class CollectGlobalStateClass extends AnalysisCMLAdaptor {

	private TypeCheckQuestion question;
	private Collection<PDefinition> members;
	private PSource root;

	public static AClassClassDefinition getGlobalRoot(Collection<PSource> sources,
			TypeIssueHandler issueHandler, CmlTypeCheckInfo info)
			throws AnalysisException {

		// Create visitor and visit each source collecting global definitions
		List<PDefinition> members = new LinkedList<PDefinition>();
		CollectGlobalStateClass me = new CollectGlobalStateClass(members, info);
		for (PSource source : sources) {
			me.root = source;
			source.apply(me);
		}

		// Create surrogate global root class
		LexNameToken className = new LexNameToken("CML",
				new LexIdentifierToken("Global Definitions", false,
						new LexLocation()));
		AClassClassDefinition globalRoot = AstFactory.newAClassClassDefinition(
				className, new LexNameList(), members);

		info.setGlobalClassDefinitions(globalRoot);
		// That's it
		return globalRoot;
	}

	@Override
	public void defaultPSource(PSource node) throws AnalysisException {
		LinkedList<PDefinition> paragraphs = node.getParagraphs();
		for (PDefinition paragraph : paragraphs) {
			paragraph.apply(this);
		}
	}

	private CollectGlobalStateClass( List<PDefinition> members,
			TypeCheckQuestion question) {
		this.members = members;
		this.question = question;

	}

	
	
	

	
/*
	@Override
	public void caseAChannelsDefinition(AChannelsDefinition node)
			throws AnalysisException {

		LinkedList<AChannelNameDefinition> channels = node.getChannelNameDeclarations();
		for(AChannelNameDefinition chanDef: channels)
		{
			if (chanDef.getSingleType() != null)
			{
				ATypeSingleDeclaration typeDecl = chanDef.getSingleType();
				LinkedList<LexIdentifierToken> ids = typeDecl.getIdentifiers();
				for (LexIdentifierToken id : ids)
					question.addChannel(id, chanDef);
			}
		}

	}*/

	@Override
	public void caseATypesDefinition(ATypesDefinition node)
			throws AnalysisException {

		List<PDefinition> defs = TCDeclAndDefVisitor.handleDefinitionsForOverture(node);
		members.addAll(defs);
		super.caseATypesDefinition(node);
	}

	@Override
	public void caseAValuesDefinition(AValuesDefinition node)
			throws AnalysisException {
		List<PDefinition> defs = TCDeclAndDefVisitor.handleDefinitionsForOverture(node);
		members.addAll(defs);
	}

	@Override
	public void caseAFunctionsDefinition(AFunctionsDefinition node)
			throws AnalysisException {
		
		List<PDefinition> defs = TCDeclAndDefVisitor.handleDefinitionsForOverture(node);
		members.addAll(defs);
	}
	
}
