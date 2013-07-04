package eu.compassresearch.core.interpreter;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.overture.ast.analysis.AnalysisException;
import org.overture.ast.definitions.PDefinition;
import org.overture.ast.intf.lex.ILexNameToken;
import org.overture.ast.node.INode;
import org.overture.interpreter.runtime.Context;
import org.overture.interpreter.runtime.ObjectContext;
import org.overture.interpreter.values.FunctionValue;
import org.overture.interpreter.values.NameValuePair;
import org.overture.interpreter.values.NameValuePairList;
import org.overture.interpreter.values.NameValuePairMap;

import eu.compassresearch.ast.actions.ASkipAction;
import eu.compassresearch.ast.analysis.QuestionAnswerCMLAdaptor;
import eu.compassresearch.ast.definitions.AProcessDefinition;
import eu.compassresearch.ast.expressions.PVarsetExpression;
import eu.compassresearch.ast.lex.LexNameToken;
import eu.compassresearch.ast.process.AActionProcess;
import eu.compassresearch.ast.process.AExternalChoiceProcess;
import eu.compassresearch.ast.process.AGeneralisedParallelismProcess;
import eu.compassresearch.ast.process.AInterleavingProcess;
import eu.compassresearch.ast.process.AInternalChoiceProcess;
import eu.compassresearch.ast.process.AReferenceProcess;
import eu.compassresearch.ast.process.ASequentialCompositionProcess;
import eu.compassresearch.ast.process.PProcess;
import eu.compassresearch.core.interpreter.api.CmlSupervisorEnvironment;
import eu.compassresearch.core.interpreter.api.InterpretationErrorMessages;
import eu.compassresearch.core.interpreter.api.InterpreterRuntimeException;
import eu.compassresearch.core.interpreter.api.behaviour.CmlAlphabet;
import eu.compassresearch.core.interpreter.api.behaviour.CmlBehaviour;
import eu.compassresearch.core.interpreter.api.behaviour.Inspection;
import eu.compassresearch.core.interpreter.api.transitions.ChannelEvent;
import eu.compassresearch.core.interpreter.api.transitions.CmlTock;
import eu.compassresearch.core.interpreter.api.transitions.CmlTransition;
import eu.compassresearch.core.interpreter.api.transitions.ObservableEvent;
import eu.compassresearch.core.interpreter.api.values.ActionValue;
import eu.compassresearch.core.interpreter.api.values.CmlOperationValue;
import eu.compassresearch.core.interpreter.api.values.ProcessObjectValue;
import eu.compassresearch.core.interpreter.utility.Pair;


public class ProcessInspectionVisitor extends CommonInspectionVisitor
{

	public ProcessInspectionVisitor(CmlBehaviour ownerProcess,
			VisitorAccess visitorAccess,
			QuestionAnswerCMLAdaptor<Context, Inspection> parentVisitor) {
		super(ownerProcess, visitorAccess, parentVisitor);
	}

	/**
	 * Transition methods
	 */

	@Override
	public Inspection defaultPProcess(PProcess node, Context question)
			throws AnalysisException {

		throw new InterpreterRuntimeException(InterpretationErrorMessages.CASE_NOT_IMPLEMENTED.customizeMessage(node.getClass().getSimpleName()));

	}

	@Override
	public Inspection caseAActionProcess(final AActionProcess node, final Context question)
			throws AnalysisException {
		return newInspection(createSilentTransition(node,node.getAction()),
				new AbstractCalculationStep(owner, visitorAccess) {

			@Override
			public Pair<INode, Context> execute(CmlSupervisorEnvironment sve)
					throws AnalysisException {
				AProcessDefinition processDef;
				//We have a named process
				if(node.parent() instanceof AProcessDefinition)
				{
					processDef = (AProcessDefinition)node.parent();
				}
				//Unnamed process
				else
				{
					processDef = new AProcessDefinition();
					processDef.setLocation(node.getLocation());
					processDef.setName(new LexNameToken("", "Unnamed Process",node.getLocation()));
				}

				//Create a temporary context to evaluate the definitions in
				Context tmpContext = CmlContextFactory.newContext(node.getLocation(),"tmp",null);

				//Evaluate and add paragraph definitions and add the result to the state
				NameValuePairMap valueMap = new NameValuePairMap();
				for (PDefinition def : node.getDefinitionParagraphs())
				{
					NameValuePairList nvps = def.apply(cmlDefEvaluator, tmpContext);
					tmpContext.putList(nvps);

					for(NameValuePair nvp : nvps)
					{
						ILexNameToken name = nvp.name.getModifiedName(processDef.getName().getSimpleName());

						//This makes sure that operations and functions cannot be updated, while
						//everything else can.
						//TODO This might be incomplete
						if(nvp.value instanceof FunctionValue ||
								nvp.value instanceof CmlOperationValue ||
								nvp.value instanceof ActionValue)
							valueMap.put(new NameValuePair(name,nvp.value));
						else
							valueMap.put(new NameValuePair(name,nvp.value.getUpdatable(null)));
					}
				}

				ProcessObjectValue self = new ProcessObjectValue(processDef,valueMap,question.getSelf());

				ObjectContext processObjectContext = CmlContextFactory.newObjectContext(node.getLocation(), "Action Process Context", question, self);

				//push this node onto the execution stack again since this should execute
				//the action behaviour until it terminates
				return new Pair<INode,Context>(node.getAction(), processObjectContext);
			}
		});
	}


	/**
	 * External Choice D23.2 7.5.4
	 * 
	 *  There four transition rules for external choice:
	 *  
	 *  * External Choice Begin
	 *  
	 *  * External Choice Silent
	 *  
	 *  * External Choice SKIP
	 *  
	 *  * External Choice End
	 *  
	 */
	@Override
	public Inspection caseAExternalChoiceProcess(final AExternalChoiceProcess node,
			final Context question) throws AnalysisException {

		//return caseAExternalChoice(node,node.getLeft(),node.getRight(), question);
		//if true this means that this is the first time here, so the Parallel Begin rule is invoked.
		if(!owner.hasChildren()){
			return newInspection(createSilentTransition(node, node,"Begin"), 
					new AbstractCalculationStep(owner, visitorAccess) {

				@Override
				public Pair<INode, Context> execute(CmlSupervisorEnvironment sve)
						throws AnalysisException {
					CmlBehaviour leftInstance = new ConcreteCmlBehaviour(node.getLeft(), question, new LexNameToken(name().getModule(),name().getIdentifier().getName() + "[]" ,node.getLeft().getLocation()) ,this.owner);
					setLeftChild(leftInstance);

					CmlBehaviour rightInstance = new ConcreteCmlBehaviour(node.getRight(), question, new LexNameToken(name().getModule(),"[]" + name().getIdentifier().getName(),node.getRight().getLocation()),this.owner); 
					setRightChild(rightInstance);
					//Now let this process wait for the children to get into a waitForEvent state
					return new Pair<INode, Context>(node, question);
				}
			});


		}
		//If this is true, the Skip rule is instantiated. This means that the entire choice evolves into Skip
		//with the state from the skip. After this all the children processes are terminated
		else if(CmlBehaviourUtility.finishedChildExists(owner))
		{
			return newInspection(createSilentTransition(node, node,"End"), 
					new AbstractCalculationStep(owner, visitorAccess) {

				@Override
				public Pair<INode, Context> execute(CmlSupervisorEnvironment sve)
						throws AnalysisException {
					CmlBehaviour theChoosenOne = findFinishedChild();
					setLeftChild(theChoosenOne.getLeftChild());
					setRightChild(theChoosenOne.getRightChild());
					return theChoosenOne.getNextState();
				}
			});
		}
		else
		{
			return newInspection(syncOnTockAndJoinChildren(), 
					new AbstractCalculationStep(owner, visitorAccess) {

				@Override
				public Pair<INode, Context> execute(CmlSupervisorEnvironment sve)
						throws AnalysisException {
					for(CmlBehaviour child : children())
					{
						if(child.inspect().containsImprecise(supervisor().selectedObservableEvent()))
						{
							if(supervisor().selectedObservableEvent() instanceof ChannelEvent)
							{
								//first we execute the child
								child.execute(supervisor());
								setLeftChild(child.getLeftChild());
								setRightChild(child.getRightChild());
								return child.getNextState();
							}
							else
							{
								child.execute(supervisor());
								return new Pair<INode, Context>(node, question);
							}
						}
					}

					throw new AnalysisException("Should not happen");
				}
			});
		}
	}

	@Override
	public Inspection caseAGeneralisedParallelismProcess(
			final AGeneralisedParallelismProcess node, final Context question)
					throws AnalysisException {

		return caseGeneralisedParallelismParallel(node,new parallelCompositionHelper() {

			@Override
			public void caseParallelBegin() throws AnalysisException {
				ProcessInspectionVisitor.this.caseParallelBegin(node,node.getLeft(),node.getRight(), question);
			}
		}, node.getChansetExpression(),question);
	}

	/**
	 * There are no actual transition rule for this. The rule for interleaving action is that they evolve 
	 * into Skip. However, this will just terminate successfully when all its children terminates successfully.
	 */
	@Override
	public Inspection caseAInterleavingProcess(
			final AInterleavingProcess node, final Context question)
					throws AnalysisException {

		//TODO: This only implements the "A ||| B (no state)" and not "A [|| ns1 | ns2 ||] B"

		//if true this means that this is the first time here, so the Parallel Begin rule is invoked.
		if(!owner.hasChildren()){

			return newInspection(createSilentTransition(node, node, "Begin"),
					new AbstractCalculationStep(owner, visitorAccess) {

				@Override
				public Pair<INode, Context> execute(CmlSupervisorEnvironment sve)
						throws AnalysisException {

					caseParallelBegin(node,node.getLeft(),node.getRight(),question);
					//We push the current state, since this process will control the child processes created by it
					return new Pair<INode,Context>(node, question);
				}
			});
		}
		//the process has children and must now handle either termination or event sync
		else if (CmlBehaviourUtility.isAllChildrenFinished(owner))
		{
			return newInspection(createSilentTransition(node, new ASkipAction(), "End"),caseParallelEnd(question));

		}
		else
		{
			return newInspection(syncOnTockAndJoinChildren(),
					new AbstractCalculationStep(owner, visitorAccess) {

				@Override
				public Pair<INode, Context> execute(CmlSupervisorEnvironment sve)
						throws AnalysisException {
					//At least one child is not finished and waiting for event, this will invoke the Parallel Non-sync 
					caseParallelNonSync();
					//We push the current state, 
					return new Pair<INode,Context>(node, question);
				}
			});

		}
	}

	private void caseParallelBegin(PProcess node, PProcess left, PProcess right, Context question) throws AnalysisException
	{
		if(left == null || right == null)
			throw new InterpreterRuntimeException(
					InterpretationErrorMessages.CASE_NOT_IMPLEMENTED.customizeMessage(node.getClass().getSimpleName()));

		ILexNameToken name = owner.name();

		//TODO: create a local copy of the question state for each of the actions
		CmlBehaviour leftInstance = 
				new ConcreteCmlBehaviour(left,question,new LexNameToken(name.getModule(),name.getIdentifier().getName() + "|||" ,left.getLocation()),owner);		
		CmlBehaviour rightInstance = 
				new ConcreteCmlBehaviour(right,question,new LexNameToken(name.getModule(),"|||" + name.getIdentifier().getName() ,right.getLocation()),owner);

		//add the children to the process graph
		visitorAccess.setLeftChild(leftInstance);
		visitorAccess.setRightChild(rightInstance);
	}

	@Override
	public Inspection caseAInternalChoiceProcess(
			final AInternalChoiceProcess node, final Context question)
					throws AnalysisException {

		if(rnd.nextInt(2) == 0)
		{
			return newInspection(createSilentTransition(node,node.getLeft()), 
					new AbstractCalculationStep(owner, visitorAccess) {

				@Override
				public Pair<INode, Context> execute(CmlSupervisorEnvironment sve)
						throws AnalysisException {
					return new Pair<INode,Context>(node.getLeft(), question);
				}
			});
		}
		else
		{
			return newInspection(createSilentTransition(node,node.getRight()), 
					new AbstractCalculationStep(owner, visitorAccess) {

				@Override
				public Pair<INode, Context> execute(CmlSupervisorEnvironment sve)
						throws AnalysisException {
					return new Pair<INode,Context>(node.getRight(), question);
				}
			});
		}

	}

	/**
	 * This implements the 7.5.10 Action Reference transition rule in D23.2. 
	 * (Even though this is a process I assume something similar will happen)
	 */
	@Override
	public Inspection caseAReferenceProcess(final AReferenceProcess node,
			final Context question) throws AnalysisException {

		//initials this process with the global context since this should see any of creators members
		//		CmlProcess childProcess = new CmlProcess(node.getProcessDefinition(), this, question.getGlobal());
		//		this.children.add(childProcess);
		//		return CmlBehaviourSignal.EXEC_SUCCESS;

		//ProcessObjectValue processValue = (ProcessObjectValue)question.lookup(node.getProcessName());
		//name = new LexNameToken(name.module,name.getIdentifier().getName() + " = " + processValue.getProcessDefinition().getName().getSimpleName(),name.location);

		//CmlStateContext processContext = new CmlStateContext(node.getLocation(), "Referenced Process context", question,null, processValue.getProcessDefinition());
		ProcessObjectValue processValue = (ProcessObjectValue)question.lookup(node.getProcessName());

		return newInspection(createSilentTransition(node,processValue.getProcessDefinition().getProcess()),
				new AbstractCalculationStep(owner, visitorAccess) {

			@Override
			public Pair<INode, Context> execute(CmlSupervisorEnvironment sve)
					throws AnalysisException {
				return new Pair<INode,Context>( node.getProcessDefinition().getProcess(), question); 
			}
		});

	}

	@Override
	public Inspection caseASequentialCompositionProcess(
			ASequentialCompositionProcess node, Context question)
					throws AnalysisException {

		return caseASequentialComposition(node,node.getLeft(),node.getRight(),question);
	}

	/**
	 * Parallel action
	 * 
	 * In general all the parallel action have three transition rules that can be invoked
	 * Parallel Begin:
	 * 	At this step the interleaving action are not yet created. So this will be a silent (tau) transition
	 * 	where they will be created and started. So the alphabet returned here is {tau}
	 * 
	 * Parallel Sync/Non-sync:
	 * 
	 * Parallel End:
	 *  At this step both child actions are in the FINISHED state and they will be removed from the running process network
	 *  and this will make a silent transition into Skip. So the alphabet returned here is {tau}
	 */

	/**
	 * Private helper methods
	 * @throws AnalysisException 
	 */

	private CmlAlphabet caseAGeneralisedParallelismInspectChildren(PVarsetExpression channelsetExp, Context question) throws AnalysisException
	{
		//convert the channel set of the current node to a alphabet
		CmlAlphabet cs =  ((CmlAlphabet)channelsetExp.apply(cmlExpressionVisitor,question)).union(new CmlTock());

		//Get all the child alphabets and add the events that are not in the channelset
		CmlBehaviour leftChild = owner.getLeftChild();
		CmlAlphabet leftChildAlphabet = leftChild.inspect();
		CmlBehaviour rightChild = owner.getRightChild();
		CmlAlphabet rightChildAlphabet = rightChild.inspect();

		//Find the intersection between the child alphabets and the channel set and join them.
		//Then if both left and right have them the next step will combine them.
		CmlAlphabet syncAlpha = leftChildAlphabet.intersectImprecise(cs).union(rightChildAlphabet.intersectImprecise(cs));

		//combine all the common events that are in the channel set 
		Set<CmlTransition> syncEvents = new HashSet<CmlTransition>();
		for(ObservableEvent ref : cs.getObservableEvents())
		{
			CmlAlphabet commonEvents = syncAlpha.intersectImprecise(ref.getAsAlphabet());
			if(commonEvents.getObservableEvents().size() == 2)
			{
				Iterator<ObservableEvent> it = commonEvents.getObservableEvents().iterator(); 
				syncEvents.add( it.next().synchronizeWith(it.next()));
			}
		}

		/*
		 *	Finally we create the returned alphabet by joining all the 
		 *  Synchronized events together with all the event of the children 
		 *  that are not in the channel set.
		 */
		CmlAlphabet resultAlpha = new CmlAlphabet(syncEvents).union(leftChildAlphabet.subtractImprecise(cs));
		resultAlpha = resultAlpha.union(rightChildAlphabet.subtractImprecise(cs));

		return resultAlpha;
	}

}
