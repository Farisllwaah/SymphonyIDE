package eu.compassresearch.core.interpreter;

import org.overture.ast.analysis.AnalysisException;
import org.overture.ast.node.INode;
import org.overture.interpreter.runtime.Context;
import org.overture.interpreter.values.NameValuePairList;

import eu.compassresearch.ast.actions.AStopAction;
import eu.compassresearch.ast.process.AAlphabetisedParallelismProcess;
import eu.compassresearch.ast.process.AAlphabetisedParallelismReplicatedProcess;
import eu.compassresearch.ast.process.AChannelRenamingProcess;
import eu.compassresearch.ast.process.AEndDeadlineProcess;
import eu.compassresearch.ast.process.AExternalChoiceProcess;
import eu.compassresearch.ast.process.AExternalChoiceReplicatedProcess;
import eu.compassresearch.ast.process.AGeneralisedParallelismProcess;
import eu.compassresearch.ast.process.AGeneralisedParallelismReplicatedProcess;
import eu.compassresearch.ast.process.AHidingProcess;
import eu.compassresearch.ast.process.AInterleavingProcess;
import eu.compassresearch.ast.process.AInterleavingReplicatedProcess;
import eu.compassresearch.ast.process.AInternalChoiceProcess;
import eu.compassresearch.ast.process.AInternalChoiceReplicatedProcess;
import eu.compassresearch.ast.process.AInterruptProcess;
import eu.compassresearch.ast.process.ASequentialCompositionProcess;
import eu.compassresearch.ast.process.ASequentialCompositionReplicatedProcess;
import eu.compassresearch.ast.process.AStartDeadlineProcess;
import eu.compassresearch.ast.process.ATimedInterruptProcess;
import eu.compassresearch.ast.process.ATimeoutProcess;
import eu.compassresearch.ast.process.AUntimedTimeoutProcess;
import eu.compassresearch.core.interpreter.api.CmlBehaviorFactory;
import eu.compassresearch.core.interpreter.api.CmlBehaviour;
import eu.compassresearch.core.interpreter.utility.Pair;

@SuppressWarnings("deprecation")
class ProcessSetupVisitor extends CommonSetupVisitor
{

	public ProcessSetupVisitor(CmlBehaviour owner, VisitorAccess visitorAccess,
			CmlBehaviorFactory cmlBehaviorFactory)
	{
		super(owner, visitorAccess, cmlBehaviorFactory);
	}

	@Override
	public Pair<INode, Context> caseAAlphabetisedParallelismProcess(
			AAlphabetisedParallelismProcess node, Context question)
			throws AnalysisException
	{
		return caseAlphabetisedParallelism(node, node.getLeftChansetExpression(), node.getRightChansetExpression(), question);
	}

	@Override
	public Pair<INode, Context> caseAInterruptProcess(AInterruptProcess node,
			Context question) throws AnalysisException
	{
		return caseAInterrupt(node, node.getLeft(), node.getRight(), question);
	}

	@Override
	public Pair<INode, Context> caseASequentialCompositionProcess(
			ASequentialCompositionProcess node, Context question)
			throws AnalysisException
	{
		return caseASequentialComposition(node, node.getLeft(), question);
	}

	@Override
	public Pair<INode, Context> caseATimedInterruptProcess(
			ATimedInterruptProcess node, Context question)
			throws AnalysisException
	{
		return caseATimedInterrupt(node, node.getLeft(), question);
	}

	@Override
	public Pair<INode, Context> caseAChannelRenamingProcess(
			AChannelRenamingProcess node, Context question)
			throws AnalysisException
	{
		return caseChannelRenaming(node, node.getRenameExpression(), node.getProcess(), question);
	}

	@Override
	public Pair<INode, Context> caseAUntimedTimeoutProcess(
			AUntimedTimeoutProcess node, Context question)
			throws AnalysisException
	{
		return caseAUntimedTimeout(node, node.getLeft(), question);
	}

	@Override
	public Pair<INode, Context> caseAHidingProcess(AHidingProcess node,
			Context question) throws AnalysisException
	{
		// We setup the child node for the hiding operator
		setLeftChild(node.getLeft(), question);
		return new Pair<INode, Context>(node, question);
	}

	@Override
	public Pair<INode, Context> caseAStartDeadlineProcess(
			AStartDeadlineProcess node, Context question)
			throws AnalysisException
	{
		return setupTimedOperator(node, node.getLeft(), NamespaceUtility.getStartsByTimeName(), question);
	}

	@Override
	public Pair<INode, Context> caseAEndDeadlineProcess(
			AEndDeadlineProcess node, Context question)
			throws AnalysisException
	{
		return setupTimedOperator(node, node.getLeft(), NamespaceUtility.getEndsByTimeName(), question);
	}

	@Override
	public Pair<INode, Context> caseATimeoutProcess(ATimeoutProcess node,
			Context question) throws AnalysisException
	{
		return setupTimedOperator(node, node.getLeft(), NamespaceUtility.getStartTimeName(), question);
	}

	/*
	 * Replicated processes
	 */

	@Override
	public Pair<INode, Context> caseASequentialCompositionReplicatedProcess(
			final ASequentialCompositionReplicatedProcess node, Context question)
			throws AnalysisException
	{
		Pair<INode, Context> res = caseReplicated(node, node.getReplicationDeclaration(), new AbstractReplicationFactory(node)
		{
			@Override
			public INode createNextReplication()
			{
				return new ASequentialCompositionProcess(node.getLocation(), node.getReplicatedProcess().clone(), node.clone());
			}

		}, question);

		return res.first.apply(ProcessSetupVisitor.this, res.second);
	}


	@Override
	public Pair<INode, Context> caseAExternalChoiceReplicatedProcess(
			final AExternalChoiceReplicatedProcess node, final Context question)
			throws AnalysisException
	{
		Pair<INode, Context> ret = caseReplicated(node, node.getReplicationDeclaration(), new AbstractReplicationFactory(node)
		{

			@Override
			public INode createNextReplication()
			{
				return new AExternalChoiceProcess(node.getLocation(), node.getReplicatedProcess().clone(), node.clone());
			}

			@Override
			INode createTerminator()
			{
				return new AStopAction(node.getLocation());
			}
			
			@Override
			Context createReplicationDelayedChildContext(
					NameValuePairList npvl, INode node, Context outer)
			{
			return super.createReplicationChildContext(npvl, node, outer);
			}

		}, question);

		return ret;
	}

	@Override
	public Pair<INode, Context> caseAGeneralisedParallelismReplicatedProcess(
			final AGeneralisedParallelismReplicatedProcess node,
			Context question) throws AnalysisException
	{

		return caseReplicated(node, node.getReplicationDeclaration(), new AbstractReplicationFactory(node)
		{

			@Override
			public INode createNextReplication()
			{
				return new AGeneralisedParallelismProcess(node.getLocation(), node.getReplicatedProcess().clone(), node.getChansetExpression().clone(), node.clone());
			}
			
			@Override
			Context createReplicationDelayedChildContext(
					NameValuePairList npvl, INode node, Context outer)
			{
			return super.createReplicationChildContext(npvl, node, outer);
			}

		}, question);
	}


	@Override
	public Pair<INode, Context> caseAInterleavingReplicatedProcess(
			final AInterleavingReplicatedProcess node, Context question)
			throws AnalysisException
	{

		return caseReplicated(node, node.getReplicationDeclaration(), new AbstractReplicationFactory(node)
		{

			@Override
			public INode createNextReplication()
			{
				return new AInterleavingProcess(node.getLocation(), node.getReplicatedProcess().clone(), node.clone());
			}
			
			@Override
			Context createReplicationDelayedChildContext(
					NameValuePairList npvl, INode node, Context outer)
			{
			return super.createReplicationChildContext(npvl, node, outer);
			}

		}, question);
	}

	@Override
	public Pair<INode, Context> caseAAlphabetisedParallelismReplicatedProcess(
			final AAlphabetisedParallelismReplicatedProcess node,
			final Context question) throws AnalysisException
	{
		return caseReplicated(node, node.getReplicationDeclaration(), new AbstractReplicationFactory(node)
		{

			@Override
			public INode createNextReplication()
			{

				return new AAlphabetisedParallelismProcess(node.getLocation(), node.getReplicatedProcess().clone(), node.getChansetExpression().clone(), node.getChansetExpression().clone(), node.clone());
			}

		}, question);
	}

	@Override
	public Pair<INode, Context> caseAInternalChoiceReplicatedProcess(
			final AInternalChoiceReplicatedProcess node, Context question)
			throws AnalysisException
	{
		return caseReplicated(node, node.getReplicationDeclaration(), new AbstractReplicationFactory(node)
		{

			@Override
			public INode createNextReplication()
			{
				return new AInternalChoiceProcess(node.getLocation(), node.getReplicatedProcess().clone(), node.clone());
			}

		}, question);
	}

}
