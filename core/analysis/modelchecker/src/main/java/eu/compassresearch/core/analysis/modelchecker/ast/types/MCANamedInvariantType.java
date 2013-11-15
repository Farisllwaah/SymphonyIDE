package eu.compassresearch.core.analysis.modelchecker.ast.types;

import java.util.LinkedList;

import eu.compassresearch.core.analysis.modelchecker.ast.definitions.MCAValueDefinition;
import eu.compassresearch.core.analysis.modelchecker.visitors.NewCMLModelcheckerContext;

public class MCANamedInvariantType implements MCPCMLType {

	private String name;
		
	public MCANamedInvariantType(String name) {
		this.name = name;
	}

	@Override
	public String toFormula(String option) {
		StringBuilder result = new StringBuilder();
		
		NewCMLModelcheckerContext context = NewCMLModelcheckerContext.getInstance();
		MCAValueDefinition valueDef = context.getValueDefinition(name);
		if(valueDef != null){
			result.append(valueDef.getExpression().toFormula(option));
		}else{
			result.append(this.name);
		}
		return result.toString();
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}
	

	@Override
	public MCPCMLType copy() {
		// TODO Auto-generated method stub
		return null;
	}

	
}
