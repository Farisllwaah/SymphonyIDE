package eu.compassresearch.core.analysis.modelchecker.visitors;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Stack;

import org.overture.ast.node.INode;

import eu.compassresearch.ast.definitions.AActionDefinition;
import eu.compassresearch.ast.definitions.AProcessDefinition;
import eu.compassresearch.core.analysis.modelchecker.ast.auxiliary.ActionChannelDependency;
import eu.compassresearch.core.analysis.modelchecker.ast.auxiliary.Binding;
import eu.compassresearch.core.analysis.modelchecker.ast.auxiliary.Domain;
import eu.compassresearch.core.analysis.modelchecker.ast.auxiliary.IntroduceCommand;
import eu.compassresearch.core.analysis.modelchecker.ast.auxiliary.MCAssignDef;
import eu.compassresearch.core.analysis.modelchecker.ast.auxiliary.MCCondition;
import eu.compassresearch.core.analysis.modelchecker.ast.auxiliary.MCGuardDef;
import eu.compassresearch.core.analysis.modelchecker.ast.auxiliary.MCIOCommDef;
import eu.compassresearch.core.analysis.modelchecker.ast.auxiliary.MCLieInFact;
import eu.compassresearch.core.analysis.modelchecker.ast.auxiliary.NameValue;
import eu.compassresearch.core.analysis.modelchecker.ast.auxiliary.NewMCGuardDef;
import eu.compassresearch.core.analysis.modelchecker.ast.auxiliary.NullBinding;
import eu.compassresearch.core.analysis.modelchecker.ast.definitions.MCAActionDefinition;
import eu.compassresearch.core.analysis.modelchecker.ast.definitions.MCAChannelDefinition;
import eu.compassresearch.core.analysis.modelchecker.ast.definitions.MCAChansetDefinition;
import eu.compassresearch.core.analysis.modelchecker.ast.definitions.MCAExplicitCmlOperationDefinition;
import eu.compassresearch.core.analysis.modelchecker.ast.definitions.MCAProcessDefinition;
import eu.compassresearch.core.analysis.modelchecker.ast.definitions.MCATypeDefinition;
import eu.compassresearch.core.analysis.modelchecker.ast.definitions.MCAValueDefinition;
import eu.compassresearch.core.analysis.modelchecker.ast.definitions.MCSCmlOperationDefinition;
import eu.compassresearch.core.analysis.modelchecker.ast.definitions.MCSFunctionDefinition;
import eu.compassresearch.core.analysis.modelchecker.ast.expressions.MCAIntLiteralExp;
import eu.compassresearch.core.analysis.modelchecker.ast.expressions.MCASBinaryExp;
import eu.compassresearch.core.analysis.modelchecker.ast.expressions.MCAVariableExp;
import eu.compassresearch.core.analysis.modelchecker.ast.expressions.MCPCMLExp;
import eu.compassresearch.core.analysis.modelchecker.ast.expressions.MCPVarsetExpression;
import eu.compassresearch.core.analysis.modelchecker.ast.types.MCAIntNumericBasicType;
import eu.compassresearch.core.analysis.modelchecker.ast.types.MCANatNumericBasicType;

public class NewCMLModelcheckerContext {
	
	public static int ASSIGN_COUNTER;
	public static int GUARD_COUNTER;
	public static int IOCOMM_COUNTER;
	
	public String mainProcessName;
	public Domain propertiesDomain;
	public String propertyToCheck = Utilities.DEADLOCK_PROPERTY;
	public NewSetStack<MCPVarsetExpression> setStack;
	public ArrayListSet<MCLieInFact> lieIn;
	public ArrayListSet<MCLieInFact> realLieInFacts;
	public ArrayListSet<MCAActionDefinition> localActions;
	public ArrayListSet<MCCondition> conditions;
	public Binding maximalBinding = new NullBinding();
	public HashMap<MCPCMLExp, LinkedList<MCGuardDef>> guardDefs;
	public HashMap<MCPCMLExp, LinkedList<NewMCGuardDef>> stmGuardDefs;
	public ArrayListSet<MCAssignDef> assignDefs;
	public ArrayListSet<MCAChannelDefinition> channelDefs;
	public ArrayListSet<MCSCmlOperationDefinition> operations;
	public ArrayList<MCSFunctionDefinition> functions;
	public LinkedList<MCAValueDefinition> valueDefinitions;
	public LinkedList<MCATypeDefinition> typeDefinitions;
	public ArrayListSet<MCAProcessDefinition> processDefinitions;
	public ArrayList<MCIOCommDef> ioCommDefs;
	public Stack<INode> actionOrProcessDefStack;
	public ArrayList<ActionChannelDependency> channelDependencies;
	public ArrayListSet<MCPVarsetExpression> globalChanSets;
	public ArrayListSet<NameValue> localVariablesMapping;
	public Stack<NameValue> localIndexedVariablesMapping;
	public ArrayListSet<String> localIndexedVariablesDiscarded;
	public ArrayList<MCASBinaryExp> setExpressioFacts;
	public ArrayListSet<IntroduceCommand> introduceFacts;
	public LinkedList<MCAChansetDefinition> chansetDefs;
	public int maxClock;
	public static int numberOfInstances;
	//a mapping containing new names -> old names. It is necessary to allow 
	//using variables with same names in CML models when translating to FORMULA
	public HashMap<String,String> directNameMapping;
	
	//a mapping from old names to new names (volatile to be used in reference calls).
	public HashMap<String,String> reverseNameMapping;
	
	public Stack<AProcessDefinition> processStack;
	public Stack<AActionDefinition> actionStack;
	
	
	
	
	public static int CHANTYPE_COUNTER;
	protected int numberOfFetchFacts = 1;
	protected int numberOfUpdFacts = 1;
	protected int numberOfDelFacts = 1;
	
	private static NewCMLModelcheckerContext instance;
	
	public synchronized static NewCMLModelcheckerContext getInstance(){
		if(instance == null){
			instance = new NewCMLModelcheckerContext();
		}
		
		return instance;
	}
		
	public synchronized static void resetInstance(){
		int oldNumberOfInstances = numberOfInstances; 
		instance = new NewCMLModelcheckerContext();
		instance.setNumberOfInstances(oldNumberOfInstances);
	}
	
	
	public void setNumberOfInstances(int numberOfInstances) {
		this.numberOfInstances = numberOfInstances;
	}
	

	public int getNumberOfInstances() {
		return numberOfInstances;
	}

	public void addIntroduce(IntroduceCommand command){
		command.setNumberOfInstances(this.numberOfInstances);
		this.introduceFacts.add(command);
	}
	public MCCondition getConditionByExpression(MCPCMLExp expression){
		
		MCCondition result = null;
		for (MCCondition condition : this.conditions) {
			if(condition.getExpression().equals(expression)){
				result = condition;
				break;
			}
		}
		
		return result;
	}
	
	public MCAProcessDefinition getMainProcessDefinition(){
		MCAProcessDefinition result = null;
		if(this.processDefinitions.size() == 1){
			result = this.processDefinitions.get(0);
		}else{
			if(this.mainProcessName != null){
				result = this.getProcessByName(this.mainProcessName);
			}
		}
		return result;
	}
	
	public MCAActionDefinition getActionByName(String name){
		MCAActionDefinition result = null;
		for (MCAActionDefinition action : localActions) {
			if (action.getName().equals(name)) {
				result = action;
				break;
			}
		}
		
		return result;
	}
	
	public MCAProcessDefinition getProcessByName(String name){
		MCAProcessDefinition result = null;
		NewCMLModelcheckerContext context = NewCMLModelcheckerContext.getInstance();
		
		if(context.processDefinitions.size() > 1){
			for (MCAProcessDefinition proc : context.processDefinitions) {
				if (proc.getName().equals(name)) {
					result = proc;
				}
			}
		}else{
			result = context.processDefinitions.get(0);
		}
		
		return result;
	} 
	
	public LinkedList<ActionChannelDependency> getActionChannelDependendies(String actionName){
		LinkedList<ActionChannelDependency> result = new LinkedList<ActionChannelDependency>();
		for (ActionChannelDependency actionChannelDependency : this.channelDependencies) {
			if(actionChannelDependency.getActionName().equals(actionName)){
				result.add(actionChannelDependency);
			}
		}
		return result;
	}
	
	public LinkedList<ActionChannelDependency> getActionChannelDependendiesByChannelName(String channelName){
		LinkedList<ActionChannelDependency> result = new LinkedList<ActionChannelDependency>();
		for (ActionChannelDependency actionChannelDependency : this.channelDependencies) {
			if(actionChannelDependency.getChannelName().equals(channelName)){
				result.add(actionChannelDependency);
			}
		}
		return result;
	}
	
	public MCAChannelDefinition getChannelDefinition(String channelName){
		MCAChannelDefinition result = null;
		for (MCAChannelDefinition chanDef : this.channelDefs) {
			if(chanDef.getName().equals(channelName)){
				result = chanDef;
				break;
			}
		}
		return result;
	}
	
	public MCAChansetDefinition getChansetDefinition(String name){
		MCAChansetDefinition result = null;
		for (MCAChansetDefinition chansetDef : this.chansetDefs) {
			if(chansetDef.getIdentifier().equals(name)){
				result = chansetDef;
				break;
			}
		}
		return result;
	}
	
	public MCSCmlOperationDefinition getOperationDefinition(String name){
		MCSCmlOperationDefinition result = null;
		for (MCSCmlOperationDefinition opDef : this.operations) {
			if(opDef instanceof MCAExplicitCmlOperationDefinition){
				if(((MCAExplicitCmlOperationDefinition) opDef).getName().equals(name)){
					result = opDef;
				}
			}
		}
		return result;
	}
	
	public NameValue getNameValue(String variableName){
		NameValue result = null;
		for (NameValue item : this.localVariablesMapping) {
			if(item.getVariableName().equals(variableName)){
				result = item;
				break;
			}
		}
		
		return result;
	}
	
	public NameValue getNameValueInIndexedVariables(String variableName){
		NameValue result = null;
		Stack<NameValue> copy = new Stack<NameValue>();
		copy.addAll(localIndexedVariablesMapping);
		while(!copy.isEmpty()){
			NameValue item = copy.pop();
			if(item.getVariableName().equals(variableName)){
				result = item;
				break;
			}
		}
		
		return result;
	}
	
	
	public NewCMLModelcheckerContext() {
		setStack = new NewSetStack<MCPVarsetExpression>();
		lieIn = new ArrayListSet<MCLieInFact>();
		operations = new ArrayListSet<MCSCmlOperationDefinition>(); 
		localActions = new ArrayListSet<MCAActionDefinition>();
		conditions = new ArrayListSet<MCCondition>();
		channelDependencies = new ArrayList<ActionChannelDependency>();
		ioCommDefs = new ArrayList<MCIOCommDef>();
		valueDefinitions = new LinkedList<MCAValueDefinition>();
		typeDefinitions = new LinkedList<MCATypeDefinition>();
		guardDefs = new HashMap<MCPCMLExp, LinkedList<MCGuardDef>>();
		stmGuardDefs = new HashMap<MCPCMLExp, LinkedList<NewMCGuardDef>>();
		assignDefs = new ArrayListSet<MCAssignDef>();
		channelDefs = new ArrayListSet<MCAChannelDefinition>();
		processDefinitions = new ArrayListSet<MCAProcessDefinition>();
		actionOrProcessDefStack = new Stack<INode>(); 
		functions = new ArrayList<MCSFunctionDefinition>();
		globalChanSets = new ArrayListSet<MCPVarsetExpression>();
		realLieInFacts = new ArrayListSet<MCLieInFact>();
		localVariablesMapping = new ArrayListSet<NameValue>();
		localIndexedVariablesMapping = new Stack<NameValue>();
		localIndexedVariablesDiscarded = new ArrayListSet<String>();
		setExpressioFacts = new ArrayList<MCASBinaryExp>(); 
		chansetDefs = new LinkedList<MCAChansetDefinition>();
		directNameMapping = new HashMap<String,String>();
		reverseNameMapping = new HashMap<String,String>();
		processStack = new Stack<AProcessDefinition>();
		actionStack = new Stack<AActionDefinition>();
		introduceFacts = new ArrayListSet<IntroduceCommand>();
		maxClock = 0;
		numberOfInstances = 1;
		ASSIGN_COUNTER = 0;
		GUARD_COUNTER = 0;
		IOCOMM_COUNTER = 0;
		CHANTYPE_COUNTER = 0;
	}
	
	public void updateMaxClock(MCPCMLExp timeExpression){
		if(timeExpression instanceof MCAIntLiteralExp){
			String value = ((MCAIntLiteralExp) timeExpression).getValue();
			int clockValue = Integer.parseInt(value);
			if(clockValue > this.maxClock){
				this.maxClock = clockValue;
			}
		} else if (timeExpression instanceof MCAVariableExp){
			String name = ((MCAVariableExp) timeExpression).getName();
			MCAValueDefinition valueDef = this.getValueDefinition(name);
			if(valueDef != null){
				MCPCMLExp value = valueDef.getExpression();
				if(value instanceof MCAIntLiteralExp){
					int clockValue = Integer.parseInt(((MCAIntLiteralExp) value).getValue());
					if(clockValue > this.maxClock){
						this.maxClock = clockValue;
					}
				}
			}
		}
	}
	
	public String generateName(String originalName){
		StringBuilder result = new StringBuilder();
		result.append(originalName);
		if(!this.processStack.isEmpty()){
			AProcessDefinition procDef = this.processStack.peek();
			result.append(procDef.getName().toString());
		}
		
		this.directNameMapping.put(result.toString(), originalName);
		this.reverseNameMapping.put(originalName, result.toString());
		
		return result.toString();
	}
	
	public String generateVariableName(String originalName){
		StringBuilder result = new StringBuilder();
		result.append(originalName);
		if(!this.processStack.isEmpty()){
			AProcessDefinition procDef = this.processStack.peek();
			result.append(procDef.getName().toString());
			if(!this.actionStack.isEmpty()){
				AActionDefinition actionDef = actionStack.peek();
				result.append(Utilities.extractFunctionName(actionDef.getName().toString()));
				
			}
			this.directNameMapping.put(result.toString(), originalName);
			this.reverseNameMapping.put(originalName, result.toString());
		}
		
		this.directNameMapping.put(result.toString(), originalName);
		
		return result.toString();
	}
	
	public MCAValueDefinition getValueDefinition(String valueName){
		MCAValueDefinition result = null;
		
		for (MCAValueDefinition valueDef : valueDefinitions) {
			if(valueDef.getName().equals(valueName)){
				result = valueDef;
				break;
			}
		}
		
		return result;
	}
	
	public MCATypeDefinition getTypeDefinition(String typeName){
		MCATypeDefinition result = null;
		for (MCATypeDefinition typeDef : typeDefinitions) {
			if(typeDef.getName().equals(typeName)){
				result = typeDef;
				break;
			}
		}
		return result;
	}

	public String getPropertyToCheck() {
		return propertyToCheck;
	}

	public void setPropertyToCheck(String propertyToCheck) {
		this.propertyToCheck = propertyToCheck;
	}

	public static void main(String[] args) {
		ArrayListSet<IntroduceCommand> list = new ArrayListSet<IntroduceCommand>();
		for (int i = 0; i < 3; i++) {
			MCAIntNumericBasicType type = new MCAIntNumericBasicType("4");
			list.add(new IntroduceCommand(type,2));
		}
		StringBuilder r = new StringBuilder();
		r.append(list.toString());
		System.out.println(r);
	}
}
