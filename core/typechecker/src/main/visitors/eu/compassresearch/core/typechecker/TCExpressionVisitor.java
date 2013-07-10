package eu.compassresearch.core.typechecker;

import static eu.compassresearch.core.typechecker.CmlTCUtil.successfulType;

import java.util.LinkedList;
import java.util.List;

import org.overture.ast.analysis.AnalysisException;
import org.overture.ast.analysis.QuestionAnswerAdaptor;
import org.overture.ast.definitions.AExplicitFunctionDefinition;
import org.overture.ast.definitions.AImplicitFunctionDefinition;
import org.overture.ast.definitions.APerSyncDefinition;
import org.overture.ast.definitions.AStateDefinition;
import org.overture.ast.definitions.PDefinition;
import org.overture.ast.expressions.AApplyExp;
import org.overture.ast.expressions.AFuncInstatiationExp;
import org.overture.ast.expressions.AIsExp;
import org.overture.ast.expressions.ANilExp;
import org.overture.ast.expressions.ASelfExp;
import org.overture.ast.expressions.AVariableExp;
import org.overture.ast.expressions.PExp;
import org.overture.ast.factory.AstFactory;
import org.overture.ast.intf.lex.ILexIdentifierToken;
import org.overture.ast.intf.lex.ILexNameToken;
import org.overture.ast.node.INode;
import org.overture.ast.patterns.PMultipleBind;
import org.overture.ast.typechecker.NameScope;
import org.overture.ast.types.AFunctionType;
import org.overture.ast.types.AOperationType;
import org.overture.ast.types.AProductType;
import org.overture.ast.types.AVoidType;
import org.overture.ast.types.PType;
import org.overture.ast.types.SMapType;
import org.overture.ast.types.SSeqType;
import org.overture.ast.util.PTypeSet;
import org.overture.config.Release;
import org.overture.config.Settings;
import org.overture.parser.messages.VDMError;
import org.overture.typechecker.LexNameTokenAssistent;
import org.overture.typechecker.TypeCheckException;
import org.overture.typechecker.TypeCheckInfo;
import org.overture.typechecker.TypeChecker;
import org.overture.typechecker.TypeCheckerErrors;
import org.overture.typechecker.assistant.definition.PDefinitionAssistantTC;
import org.overture.typechecker.assistant.expression.AApplyExpAssistantTC;
import org.overture.typechecker.assistant.type.AFunctionTypeAssistantTC;
import org.overture.typechecker.assistant.type.AOperationTypeAssistantTC;
import org.overture.typechecker.assistant.type.PTypeAssistantTC;

import eu.compassresearch.ast.analysis.QuestionAnswerCMLAdaptor;
import eu.compassresearch.ast.definitions.AChannelNameDefinition;
import eu.compassresearch.ast.definitions.AChansetDefinition;
import eu.compassresearch.ast.definitions.AClassDefinition;
import eu.compassresearch.ast.expressions.ABracketedExp;
import eu.compassresearch.ast.expressions.AEnumVarsetExpression;
import eu.compassresearch.ast.expressions.AEnumerationRenameChannelExp;
import eu.compassresearch.ast.expressions.AFatCompVarsetExpression;
import eu.compassresearch.ast.expressions.AFatEnumVarsetExpression;
import eu.compassresearch.ast.expressions.AIdentifierVarsetExpression;
import eu.compassresearch.ast.expressions.AInterVOpVarsetExpression;
import eu.compassresearch.ast.expressions.ANameChannelExp;
import eu.compassresearch.ast.expressions.ASubVOpVarsetExpression;
import eu.compassresearch.ast.expressions.ATupleSelectExp;
import eu.compassresearch.ast.expressions.AUnionVOpVarsetExpression;
import eu.compassresearch.ast.expressions.AUnresolvedPathExp;
import eu.compassresearch.ast.expressions.PVarsetExpression;
import eu.compassresearch.ast.lex.LexNameToken;
import eu.compassresearch.ast.patterns.ARenamePair;
import eu.compassresearch.ast.types.AChannelType;
import eu.compassresearch.ast.types.AChansetType;
import eu.compassresearch.ast.types.AErrorType;
import eu.compassresearch.ast.types.ANamesetsType;
import eu.compassresearch.ast.types.AVarsetExpressionType;
import eu.compassresearch.core.typechecker.api.TypeComparator;
import eu.compassresearch.core.typechecker.api.TypeErrorMessages;
import eu.compassresearch.core.typechecker.api.TypeIssueHandler;

class TCExpressionVisitor extends
		QuestionAnswerCMLAdaptor<org.overture.typechecker.TypeCheckInfo, PType> {

	private final TypeComparator typeComparator;

	@Override
	public PType caseAEnumerationRenameChannelExp(
			AEnumerationRenameChannelExp node, TypeCheckInfo question)
			throws AnalysisException {

		CmlTypeCheckInfo cmlEnv = CmlTCUtil.getCmlEnv(question);
		if (cmlEnv == null) {
			node.setType(issueHandler.addTypeError(
					node,
					TypeErrorMessages.ILLEGAL_CONTEXT.customizeMessage(""
							+ node)));
			return node.getType();
		}

		LinkedList<ARenamePair> pairs = node.getRenamePairs();
		for (ARenamePair p : pairs) {
			ANameChannelExp from = p.getFrom();
			ANameChannelExp to = p.getTo();

			PDefinition fromChanDef = cmlEnv
					.lookupChannel(from.getIdentifier());
			if (fromChanDef == null) {
				node.setType(issueHandler.addTypeError(
						from,
						TypeErrorMessages.UNDEFINED_SYMBOL.customizeMessage(""
								+ from)));
				return node.getType();
			}

			to.setType(fromChanDef.getType());
			// cmlEnv.addChannel(to.getIdentifier(), fromChanDef);
		}

		node.setType(new AChannelType(node.getLocation(), true));
		return node.getType();

	}

	@Override
	public PType caseATupleSelectExp(ATupleSelectExp node,
			TypeCheckInfo question) throws AnalysisException {

		Integer entry = node.getEntry();

		PExp tupleExp = node.getTuple();

		PType tupleExpType = tupleExp.apply(parent, question);
		if (!successfulType(tupleExpType)) {
			node.setType(issueHandler.addTypeError(tupleExp,
					TypeErrorMessages.COULD_NOT_DETERMINE_TYPE
							.customizeMessage(tupleExp + "")));
			return node.getType();
		}

		if (!(tupleExpType instanceof AProductType)) {
			node.setType(issueHandler.addTypeError(tupleExp,
					TypeErrorMessages.INCOMPATIBLE_TYPE.customizeMessage(
							"Tuple type", "" + tupleExpType)));
			return node.getType();
		}

		return node.getType();
	}

	@Override
	public PType caseANameChannelExp(ANameChannelExp node,
			TypeCheckInfo question) throws AnalysisException {

		CmlTypeCheckInfo cmlEnv = CmlTCUtil.getCmlEnv(question);
		if (cmlEnv == null) {
			node.setType(issueHandler.addTypeError(
					node,
					TypeErrorMessages.ILLEGAL_CONTEXT.customizeMessage(node
							+ "")));
			return node.getType();
		}

		PExp expression = node.getExpression();
		ILexNameToken channelId = node.getIdentifier();

		PType expressionType = expression.apply(parent, question);
		if (!successfulType(expressionType)) {
			node.setType(issueHandler.addTypeError(expression,
					TypeErrorMessages.COULD_NOT_DETERMINE_TYPE
							.customizeMessage(expression + "")));
			return node.getType();
		}

		PDefinition chanDef = cmlEnv.lookupChannel(channelId);
		if (!(chanDef instanceof AChannelNameDefinition)) {
			node.setType(issueHandler.addTypeError(node,
					TypeErrorMessages.EXPECTED_A_CHANNEL
							.customizeMessage(channelId + "")));
			return node.getType();
		}

		if (!typeComparator.isSubType(chanDef.getType(), expressionType)) {
			node.setType(issueHandler.addTypeError(
					expression,
					TypeErrorMessages.INCOMPATIBLE_TYPE.customizeMessage(""
							+ chanDef.getType(), "" + expressionType)));
			return node.getType();
		}

		node.setType(new AVarsetExpressionType(node.getLocation(), true));
		return node.getType();
	}

	@Override
	public PType caseAIdentifierVarsetExpression(
			AIdentifierVarsetExpression node, TypeCheckInfo question)
			throws AnalysisException {

		CmlTypeCheckInfo cmlEnv = CmlTCUtil.getCmlEnv(question);
		if (cmlEnv == null) {
			node.setType(issueHandler.addTypeError(node,
					TypeErrorMessages.COULD_NOT_DETERMINE_TYPE
							.customizeMessage(node + "")));
			return node.getType();
		}

		ILexIdentifierToken id = node.getIdentifier();
		PDefinition idDef = cmlEnv.lookupChannel(id);

		if (idDef == null) {
			node.setType(issueHandler.addTypeError(
					node,
					TypeErrorMessages.UNDEFINED_SYMBOL.customizeMessage(node
							+ "")));
			return node.getType();
		}

		if (!(idDef instanceof AChansetDefinition
				|| idDef instanceof AChannelNameDefinition || idDef instanceof AStateDefinition)) {
			node.setType(issueHandler.addTypeError(node,
					TypeErrorMessages.EXPECTED_CHANNEL_OR_STATE
							.customizeMessage(idDef + "")));
			return node.getType();
		}

		node.setType(new AVarsetExpressionType(node.getLocation(), true));
		return node.getType();
	}

	@Override
	public PType caseAFatEnumVarsetExpression(AFatEnumVarsetExpression node,
			TypeCheckInfo question) throws AnalysisException {

		CmlTypeCheckInfo cmlEnv = CmlTCUtil.getCmlEnv(question);
		if (cmlEnv == null) {
			node.setType(issueHandler.addTypeError(node,
					TypeErrorMessages.COULD_NOT_DETERMINE_TYPE
							.customizeMessage(node + "")));
			return node.getType();
		}

		LinkedList<ILexIdentifierToken> ids = node.getIdentifiers();
		boolean seenState = false;
		boolean seenChannel = false;
		for (ILexIdentifierToken id : ids) {
			PDefinition idDef = cmlEnv.lookupChannel(id);
			if (idDef == null) {
				node.setType(issueHandler.addTypeError(
						node,
						TypeErrorMessages.UNDEFINED_SYMBOL.customizeMessage(""
								+ id)));
				return node.getType();
			}

			if (idDef.getType() instanceof AChannelType)
				seenChannel = true;

			if (idDef instanceof AStateDefinition)
				seenState = true;

			if ((seenState && seenChannel)) {
				node.setType(issueHandler.addTypeError(node,
						TypeErrorMessages.MIXING_STATE_AND_CHANNEL_IN_SET
								.customizeMessage(ids + "")));
				return node.getType();
			}

			if (!seenState && !seenChannel) {
				node.setType(issueHandler.addTypeError(node,
						TypeErrorMessages.INCOMPATIBLE_TYPE.customizeMessage(
								"state or channel", idDef.getType() + "")));
				return node.getType();
			}

		}

		PType result = null;
		if (seenState)
			result = new ANamesetsType(node.getLocation(), true);
		if (seenChannel)
			result = new AChansetType(node.getLocation(), true);
		if (result == null)
			result = issueHandler.addTypeError(node,
					TypeErrorMessages.EXPECTED_CHANNEL_OR_STATE
							.customizeMessage("" + node));

		node.setType(result);
		return result;

	}

	@Override
	public PType caseAFatCompVarsetExpression(AFatCompVarsetExpression node,
			TypeCheckInfo question) throws AnalysisException {
		// bnd predicate
		// { a.x | x : int }

		CmlTypeCheckInfo cmlEnv = CmlTCUtil.getCmlEnv(question);
		if (cmlEnv == null) {
			node.setType(issueHandler.addTypeError(
					node,
					TypeErrorMessages.ILLEGAL_CONTEXT.customizeMessage(""
							+ node)));
			return node.getType();
		}

		PExp predicate = node.getPredicate();
		LinkedList<PMultipleBind> bindings = node.getBindings();

		CmlTypeCheckInfo compScope = cmlEnv.newScope();

		for (PMultipleBind mbnd : bindings) {
			PType mbndType = mbnd.apply(parent, question);
			if (!successfulType(mbndType)) {
				node.setType(issueHandler.addTypeError(mbnd,
						TypeErrorMessages.COULD_NOT_DETERMINE_TYPE
								.customizeMessage(node + "")));
				return node.getType();
			}
		}

		if (predicate != null) {
			PType predicateType = predicate.apply(parent, compScope);
			if (!successfulType(predicateType)) {
				node.setType(issueHandler.addTypeError(predicateType,
						TypeErrorMessages.COULD_NOT_DETERMINE_TYPE
								.customizeMessage("" + predicate)));
				return node.getType();
			}
		}
		node.setType(new AVarsetExpressionType(node.getLocation(), true));
		return node.getType();
	}

	@Override
	public PType caseAUnionVOpVarsetExpression(AUnionVOpVarsetExpression node,
			TypeCheckInfo question) throws AnalysisException {

		PVarsetExpression left = node.getLeft();
		PVarsetExpression right = node.getRight();

		PType leftType = left.apply(parent, question);
		if (!successfulType(leftType)) {
			node.setType(issueHandler.addTypeError(left,
					TypeErrorMessages.COULD_NOT_DETERMINE_TYPE
							.customizeMessage("" + left)));
			return node.getType();
		}

		PType rightType = right.apply(parent, question);
		if (!successfulType(rightType)) {
			node.setType(issueHandler.addTypeError(right,
					TypeErrorMessages.COULD_NOT_DETERMINE_TYPE
							.customizeMessage("" + right)));
			return node.getType();
		}

		node.setType(new AChannelType());
		return node.getType();
	}

	@Override
	public PType caseAInterVOpVarsetExpression(AInterVOpVarsetExpression node,
			TypeCheckInfo question) throws AnalysisException {
		PVarsetExpression left = node.getLeft();
		PVarsetExpression right = node.getRight();

		PType leftType = left.apply(parent, question);
		if (!successfulType(leftType)) {
			node.setType(issueHandler.addTypeError(left,
					TypeErrorMessages.COULD_NOT_DETERMINE_TYPE
							.customizeMessage("" + left)));
			return node.getType();
		}

		PType rightType = right.apply(parent, question);
		if (!successfulType(rightType)) {
			node.setType(issueHandler.addTypeError(right,
					TypeErrorMessages.COULD_NOT_DETERMINE_TYPE
							.customizeMessage("" + right)));
			return node.getType();
		}

		node.setType(new AChannelType());
		return node.getType();
	}

	@Override
	public PType caseASubVOpVarsetExpression(ASubVOpVarsetExpression node,
			TypeCheckInfo question) throws AnalysisException {
		PVarsetExpression left = node.getLeft();
		PVarsetExpression right = node.getRight();

		PType leftType = left.apply(parent, question);
		if (!successfulType(leftType)) {
			node.setType(issueHandler.addTypeError(left,
					TypeErrorMessages.COULD_NOT_DETERMINE_TYPE
							.customizeMessage("" + left)));
			return node.getType();
		}

		PType rightType = right.apply(parent, question);
		if (!successfulType(rightType)) {
			node.setType(issueHandler.addTypeError(right,
					TypeErrorMessages.COULD_NOT_DETERMINE_TYPE
							.customizeMessage("" + right)));
			return node.getType();
		}

		node.setType(new AChannelType());
		return node.getType();
	}

	@Override
	public PType caseANilExp(ANilExp node, TypeCheckInfo question)
			throws AnalysisException {

		return AstFactory.newAUnknownType(node.getLocation());
	}

	@Override
	public PType caseAApplyExp(AApplyExp node, TypeCheckInfo question)
			throws AnalysisException {

		try {
			OvertureRootCMLAdapter.pushQuestion(question);
			/*
			 * The following is copied from Overture TypeCheckerExpVisitor
			 */
			TypeChecker.clearErrors();
			if (!(node.getArgtypes() != null && node.getArgtypes().size() == node
					.getArgs().size())) {
				for (PExp a : node.getArgs()) {
					question.qualifiers = null;
					PType argType = a.apply(parent, question);
					node.getArgtypes().add(argType);
				}
			}
			node.setType(node.getRoot().apply(
					parent,
					new TypeCheckInfo(question.assistantFactory,question.env, question.scope, node
							.getArgtypes())));

			if (PTypeAssistantTC.isUnknown(node.getType())) {
				return node.getType();
			}

			PDefinition func = question.env.getEnclosingDefinition();

			boolean inFunction = (func instanceof AExplicitFunctionDefinition
					|| func instanceof AImplicitFunctionDefinition || func instanceof APerSyncDefinition);

			if (inFunction) {
				ILexNameToken called = null;

				if (node.getRoot() instanceof AVariableExp) {
					AVariableExp var = (AVariableExp) node.getRoot();
					called = var.getName();
				} else if (node.getRoot() instanceof AFuncInstatiationExp) {
					AFuncInstatiationExp fie = (AFuncInstatiationExp) node
							.getRoot();

					if (fie.getExpdef() != null) {
						called = fie.getExpdef().getName();
					} else if (fie.getImpdef() != null) {
						called = fie.getImpdef().getName();
					}
				}

				if (called != null) {
					if (func instanceof AExplicitFunctionDefinition) {
						AExplicitFunctionDefinition def = (AExplicitFunctionDefinition) func;

						if (LexNameTokenAssistent
								.isEqual(called, def.getName())) {
							node.setRecursive(def);
							def.setRecursive(true);
						}
					} else if (func instanceof AImplicitFunctionDefinition) {
						AImplicitFunctionDefinition def = (AImplicitFunctionDefinition) func;

						if (LexNameTokenAssistent
								.isEqual(called, def.getName())) {
							node.setRecursive(def);
							def.setRecursive(true);
						}
					}
				}
			}

			boolean isSimple = !PTypeAssistantTC.isUnion(node.getType());
			PTypeSet results = new PTypeSet();

			if (PTypeAssistantTC.isFunction(node.getType())) {
				AFunctionType ft = PTypeAssistantTC.getFunction(node.getType());
				AFunctionTypeAssistantTC.typeResolve(ft, null,
						(QuestionAnswerAdaptor<TypeCheckInfo, PType>) parent,
						question);
				results.add(AApplyExpAssistantTC.functionApply(node, isSimple,
						ft));
			}

			if (PTypeAssistantTC.isOperation(node.getType())) {
				AOperationType ot = PTypeAssistantTC.getOperation(node
						.getType());
				try {
					AOperationTypeAssistantTC
							.typeResolve(
									ot,
									null,
									(QuestionAnswerAdaptor<TypeCheckInfo, PType>) parent,
									question);
				} catch (TypeCheckException tce) {
					node.setType(issueHandler.addTypeError(node,
							tce.getMessage()));
					return node.getType();
				}

				if (inFunction && Settings.release == Release.VDM_10) {
					TypeCheckerErrors.report(3300,
							"Operation '" + node.getRoot()
									+ "' cannot be called from a function",
							node.getLocation(), node);
					results.add(AstFactory.newAUnknownType(node.getLocation()));
				} else {
					// RWL TODO: HACK to satisfy the
					// AApplyExpAssistant.operationApply
					List<PType> ptypes = ot.getParameters();
					if (ptypes.size() == 1) {
						PType p0 = ptypes.get(0);
						if (p0 instanceof AVoidType) {
							ot.setParameters(new LinkedList<PType>());
						}
					}

					results.add(AApplyExpAssistantTC.operationApply(node,
							isSimple, ot));
				}
			}

			if (PTypeAssistantTC.isSeq(node.getType())) {
				SSeqType seq = PTypeAssistantTC.getSeq(node.getType());
				results.add(AApplyExpAssistantTC.sequenceApply(node, isSimple,
						seq));
			}

			if (PTypeAssistantTC.isMap(node.getType())) {
				SMapType map = PTypeAssistantTC.getMap(node.getType());
				results.add(AApplyExpAssistantTC.mapApply(node, isSimple, map));
			}
			/*
			 * Overture copy STOP Now collect any type errors created by
			 * Overture assistants
			 */

			if (results.isEmpty()) {
				TypeCheckerErrors.report(3054, "Type " + node.getType()
						+ " cannot be applied", node.getLocation(), node);
				return AstFactory.newAUnknownType(node.getLocation());
			}

			// Check for errors
			if (TypeChecker.getErrorCount() > 0) {
				List<VDMError> errors = TypeChecker.getErrors();
				for (VDMError e : errors)
					issueHandler.addTypeError(node, e.message);
				node.setType(new AErrorType(node.getLocation(), true));
				return node.getType();
			}

			node.setType(results.getType(node.getLocation()));
			return node.getType(); // Union of possible applications
		} finally {
			OvertureRootCMLAdapter.popQuestion(question);
		}
	}

	@Override
	public PType caseAEnumVarsetExpression(AEnumVarsetExpression node,
			TypeCheckInfo question) throws AnalysisException {

		PType result = AstFactory.newAUnknownType(node.getLocation());

		CmlTypeCheckInfo cmlEnv = CmlTCUtil.getCmlEnv(question);
		if (cmlEnv == null) {
			node.setType(issueHandler.addTypeError(
					node,
					TypeErrorMessages.ILLEGAL_CONTEXT.customizeMessage(""
							+ node)));
			return node.getType();
		}

		LinkedList<ILexIdentifierToken> ids = node.getIdentifiers();
		LinkedList<PDefinition> defs = new LinkedList<PDefinition>();
		boolean seenChannel = false;
		boolean seenState = false;
		for (ILexIdentifierToken id : ids) {
			LexNameToken nameid = new LexNameToken("", id);
			PDefinition def = cmlEnv.lookup(nameid, PDefinition.class);
			if (def == null) {
				def = cmlEnv.lookupChannel(id);
				seenChannel = true;
			} else {
				seenState = true;
			}
			if (def == null) {
				node.setType(issueHandler.addTypeError(
						id,
						TypeErrorMessages.UNDEFINED_SYMBOL.customizeMessage(id
								+ "")));
				return node.getType();
			}
			defs.add(def);
		}

		if (seenChannel && seenState) {
			node.setType(issueHandler.addTypeError(node,
					TypeErrorMessages.MIXING_STATE_AND_CHANNEL_IN_SET
							.customizeMessage("" + node)));
			return node.getType();
		}

		if (seenChannel) {
			result = new AChansetType(node.getLocation(), true);
		}

		if (seenState) {
			result = new ANamesetsType(node.getLocation(), true);
		}
		result.setDefinitions(new LinkedList<PDefinition>());
		result.getDefinitions().addAll(defs);
		node.setType(result);
		return node.getType();
	}

	@Override
	public PType caseAVariableExp(AVariableExp node, TypeCheckInfo question)
			throws AnalysisException {

		// for convenience take out name and env
		org.overture.typechecker.Environment env = question.env;
		ILexNameToken name = node.getName();

		/*
		 * CmlTypeCheckInfo cmlEnv = CmlTCUtil.getCmlEnv(question); if (cmlEnv
		 * != null) { PDefinition def = cmlEnv.lookup(name,PDefinition.class);
		 * if (def != null) return def.getType(); }
		 */

		// the qualifiers are the argument names for potential function
		// or operation... We wish to find an abstraction with the right args.
		name.setTypeQualifier(question.qualifiers);

		// The defintion is set on the VariableExp for convenience
		if (question.scope == null)
			question.scope = NameScope.LOCAL;
		node.setVardef(env.findName(name, question.scope));

		do {

			// definition successfully found with qualifiers on
			if (node.getVardef() != null)
				break;

			// The name was not found immediately, it could be a sequence or map
			// in which case the qualifiers are not supposed to be set. See
			// TypeCheckExpVisitor.java:2510,
			if (question.qualifiers != null) {
				name.setTypeQualifier(null);
				node.setVardef(env.findName(name, question.scope));
				if (node.getVardef() == null)
					name.setTypeQualifier(question.qualifiers);
				else
					break;
			}

			node.setVardef(CmlTCUtil
					.findNearestFunctionOrOperationInEnvironment(name, env));
		} while (false);

		// The name this variable expressions points to was found.
		if (node.getVardef() != null) {

			PType type = question.assistantFactory.createPDefinitionAssistant().getType(node.getVardef());
			if (type == null)
				type = node.getVardef().getType();
			try {
				node.setType(PTypeAssistantTC.typeResolve(type, null,
						(QuestionAnswerAdaptor<TypeCheckInfo, PType>) parent,
						question));
			} catch (TypeCheckException tce) {
				node.setType(issueHandler.addTypeError(node, tce.getMessage()));
			}
			return node.getType();
		}

		// Okay given our best efforts the Overture Type Checking strategy could
		// not find
		// what we are looking for. Maybe its a CML class we are looking at.

		CmlTypeCheckInfo nearestCmlEnvironment = CmlTCUtil.getCmlEnv(question);
		if (nearestCmlEnvironment == null) {
			node.setType(issueHandler.addTypeError(
					node,
					TypeErrorMessages.ILLEGAL_CONTEXT.customizeMessage(node
							+ "")));
			return node.getType();
		}

		// CML also uses LexNameToken.equals to compare names therefore the
		// qualifiers must be taking in to account.
		PDefinition definition = null;
		do {
			// Lookup in the CML context without the type qualifiers (arg
			// types/param names)
			name.setTypeQualifier(null);
			definition = nearestCmlEnvironment.lookup(name, PDefinition.class);
			if (definition != null)
				break;
			else
				name.setTypeQualifier(question.qualifiers);

			// Lookup in the CML context with qualifiers (for operations)
			definition = nearestCmlEnvironment.lookup(name, PDefinition.class);

		} while (false);

		if (definition == null) {
			name.setTypeQualifier(null);
			CmlAssistant assistant = new CmlAssistant();
			while (env != null) {
				PDefinition enclosingDef = env.getEnclosingDefinition();
				if (enclosingDef != null) {
					definition = assistant.findMemberName(enclosingDef, name,
							nearestCmlEnvironment);
					if (definition != null)
						break;
				}
				env = env.getOuter();
			}
		}

		// Check scope
		// any luck?
		if (definition != null) {
			node.setVardef(definition);
			node.setType(definition.getType());
		} else // guess not
		{

			name.setTypeQualifier(null);
			node.setType(issueHandler.addTypeError(
					node,
					TypeErrorMessages.UNDEFINED_SYMBOL.customizeMessage(name
							+ "")));
		}

		if (definition != null && question.scope != null
				&& !CmlTCUtil.checkAccessInScope(definition, question.scope)) {
			node.setType(issueHandler.addTypeError(node,
					TypeErrorMessages.ILLEGAL_ACCESS
							.customizeMessage(node + "")));
			return node.getType();
		}

		return node.getType();
	}

	/**
	 * 
	 */
	private static final long serialVersionUID = -6509187123701383525L;

	// A parent checker may actually not be necessary on this
	final private eu.compassresearch.core.typechecker.api.CmlRootVisitor parent;
	private final TypeIssueHandler issueHandler;
	private final CmlAssistant assist;

	TCExpressionVisitor(
			eu.compassresearch.core.typechecker.api.CmlRootVisitor parentChecker,
			TypeIssueHandler issueHandler, CmlAssistant assistant,
			TypeComparator typeComparator) {
		this.parent = parentChecker;
		this.issueHandler = issueHandler;
		this.assist = assistant;
		this.typeComparator = typeComparator;

	}

	TCExpressionVisitor(
			eu.compassresearch.core.typechecker.api.CmlRootVisitor parentChecker,
			TypeIssueHandler issueHandler, TypeComparator typeComparator) {
		this(parentChecker, issueHandler, new CmlAssistant(), typeComparator);
	}

	/**
	 * Type check expression using Overture's type checker.
	 * 
	 * @param node
	 *            - the expression to type check
	 * @param question
	 *            - environmental stuff, green trees whatever
	 * @return A type checked cml expression
	 * @throws AnalysisException
	 *             - if anythings goes wrong that is not just a type error.
	 */
	@SuppressWarnings("deprecation")
	@Override
	public PType defaultPExp(PExp node,
			org.overture.typechecker.TypeCheckInfo question)
			throws AnalysisException {
		org.overture.typechecker.TypeChecker.clearErrors();

		INode ovtNode = node;

		OvertureRootCMLAdapter overtureExpVisitor = new OvertureRootCMLAdapter(
				parent, issueHandler);

		org.overture.typechecker.TypeCheckInfo quest = new org.overture.typechecker.TypeCheckInfo(question.assistantFactory,
				question.env);
		quest.scope = question.scope;
		quest.qualifiers = new LinkedList<PType>();
		try {
			OvertureRootCMLAdapter.pushQuestion(question);
			ovtNode.apply(overtureExpVisitor, quest);
			OvertureRootCMLAdapter.popQuestion(question);
		} catch (org.overture.ast.analysis.AnalysisException e1) {
			e1.printStackTrace();
		}

		if (org.overture.typechecker.TypeChecker.getErrorCount() > 0) {
			List<VDMError> errorList = TypeChecker.getErrors();
			for (VDMError err : errorList) {
				issueHandler.addTypeError(node, err.toProblemString());
			}
			return new AErrorType(node.getLocation(), true);
		}

		return node.getType();
	}

	@Override
	public PType caseAUnresolvedPathExp(AUnresolvedPathExp node,
			TypeCheckInfo question) throws AnalysisException {

		// So we are going to look up a path of the form <class>.<member> or
		// <identifier>.<member>
		// To find that class there must be a CML Environment as Classes are
		// top-level and CML Specific.
		//

		CmlTypeCheckInfo cmlQuestion = CmlTCUtil.getCmlEnv(question);
		if (cmlQuestion == null) {
			node.setType(issueHandler.addTypeError(
					node,
					TypeErrorMessages.ILLEGAL_CONTEXT.customizeMessage(node
							+ "")));
			return node.getType();
		}

		// All right lets get all the identifiers used in this path
		LinkedList<ILexIdentifierToken> identifiers = node.getIdentifiers();

		// Get parent identifier
		LexNameToken rootName = new LexNameToken("", identifiers.get(0));

		// is it a type like a class or global type this is not a type
		// as we would be in the UnresolvedType case
		// PDefinition root = question.env.findType(rootName, "");
		PDefinition root = null;

		// no then it may be a variable
		if (root == null)
			root = question.env.findName(rootName, question.scope);

		// Use Cml environment to determine what rootName is
		if (root == null)
			root = cmlQuestion.lookup(rootName, PDefinition.class);

		// last option it is not in something else then in must be in this class
		if (root == null) {
			root = question.env.getEnclosingDefinition();
			if (root != null)
				root = assist.findMemberName(root, rootName, cmlQuestion);
		}

		// did we find the top-level
		if (root == null) {
			node.setType(issueHandler.addTypeError(
					node,
					TypeErrorMessages.UNDEFINED_SYMBOL.customizeMessage(""
							+ rootName)));
			return node.getType();
		}

		// Now the root identifier is resolved, lets look for the first member
		// We assume the identifiers are given in order with the outer most
		// definitions coming first
		PType leafType = null;
		PDefinition prevRoot = null;
		List<PDefinition> defs = new LinkedList<PDefinition>();
		defs.add(root);
		PDefinition def = root;
		for (int i = 1; i < identifiers.size(); i++) {
			ILexIdentifierToken id = identifiers.get(i);
			LexNameToken idName = new LexNameToken("", id);
			def = assist.findMemberName(def, idName, cmlQuestion, prevRoot);
			if (def == null) {
				node.setType(issueHandler.addTypeError(
						node,
						TypeErrorMessages.UNDEFINED_SYMBOL.customizeMessage(id
								+ " in " + node)));
				return node.getType();
			}
			defs.add(def);
			leafType = def.getType();
			if (def.getType() == null) {
				issueHandler.addTypeWarning(def, "This \"" + def
						+ "\" entered the environment with no type.");
			}
		}

		// TODO: RWL Possible null if leafType is null ?
		if (leafType == null) {
			issueHandler
					.addTypeWarning(node,
							"This points to something that should have checked already, but isn't.");
		} else {
			node.setType(leafType.clone());
			if (node.getType().getDefinitions() == null)
				node.getType().setDefinitions(new LinkedList<PDefinition>());
			node.getType().getDefinitions().addAll(defs);
		}
		return node.getType();
	}

	@Override
	public PType caseABracketedExp(ABracketedExp node, TypeCheckInfo question)
			throws AnalysisException {

		PType type = node.getExpression().apply(this, question);
		node.setType(type);
		return type;
	}

	/*
	 * Copied from Overture and modified to handle CML Classes.
	 * 
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.overture.ast.analysis.QuestionAnswerAdaptor#caseAIsExp(org.overture
	 * .ast.expressions.AIsExp, java.lang.Object)
	 */
	@Override
	public PType caseAIsExp(AIsExp node, TypeCheckInfo question)
			throws AnalysisException {

		question.qualifiers = null;
		node.getTest().apply(parent, question);

		PType basictype = node.getBasicType();

		if (basictype != null) {
			try {
				basictype = PTypeAssistantTC.typeResolve(basictype, null,
						(QuestionAnswerAdaptor<TypeCheckInfo, PType>) parent,
						question);
			} catch (TypeCheckException tce) {
				node.setType(issueHandler.addTypeError(node, tce.getMessage()));
				return node.getType();
			}
		}

		ILexNameToken typename = node.getTypeName();

		if (typename != null) {
			PDefinition typeFound = question.env.findType(typename, node
					.getLocation().getModule());

			// It maybe an CML Class typically it will be lets look it up in the
			// nearest cml environment
			if (typeFound == null) {
				CmlTypeCheckInfo cmlEnv = CmlTCUtil.getCmlEnv(question);
				typeFound = cmlEnv.lookup(typename, AClassDefinition.class);
			}

			if (typeFound == null) {
				TypeCheckerErrors.report(3113, "Unknown type name '" + typename
						+ "'", node.getLocation(), node);
				node.setType(node.getTest().getType());
				return node.getType();
			}
			node.setTypedef(typeFound.clone());

		}

		node.setType(AstFactory.newABooleanBasicType(node.getLocation()));
		return node.getType();
	}

	/*
	 * Copied from Overture and modified to lookup in surrounding CML
	 * environment if necessary.
	 * 
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.overture.ast.analysis.QuestionAnswerAdaptor#caseASelfExp(org.overture
	 * .ast.expressions.ASelfExp, java.lang.Object)
	 */
	@Override
	public PType caseASelfExp(ASelfExp node, TypeCheckInfo question) {

		PDefinition cdef = question.env
				.findName(node.getName(), question.scope);

		if (cdef == null) {
			// Get Cml Environment
			CmlTypeCheckInfo cmlEnv = CmlTCUtil.getCmlEnv(question);
			if (cmlEnv == null) {
				node.setType(issueHandler.addTypeError(
						node,
						TypeErrorMessages.ILLEGAL_CONTEXT.customizeMessage(""
								+ node)));
				return node.getType();
			}

			// look up
			cdef = cmlEnv.lookup(node.getName(), PDefinition.class);
		}

		if (cdef == null) {
			TypeCheckerErrors.report(3154, node.getName() + " not in scope",
					node.getLocation(), node);
			node.setType(AstFactory.newAUnknownType(node.getLocation()));
			return node.getType();
		}

		node.setType(cdef.getType());
		return cdef.getType();
	}

}
