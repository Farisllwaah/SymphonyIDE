package eu.compassresearch.core.analysis.modelchecker.ast.expressions;

import java.util.LinkedList;

public class MCAFatEnumVarsetExpression implements MCPVarsetExpression {

	private LinkedList<MCANameChannelExp> channelNames = new LinkedList<MCANameChannelExp>();

	
	public MCAFatEnumVarsetExpression(LinkedList<MCANameChannelExp> channelNames) {
		super();
		this.channelNames = channelNames;
	}


	@Override
	public String toFormula(String option) {
		// TODO Auto-generated method stub
		return null;
	}


	public LinkedList<MCANameChannelExp> getChannelNames() {
		return channelNames;
	}


	public void setChannelNames(LinkedList<MCANameChannelExp> channelNames) {
		this.channelNames = channelNames;
	}

}
