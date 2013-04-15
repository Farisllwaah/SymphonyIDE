package eu.compassresearch.core.interpreter.cml.events;

import java.util.List;
import java.util.Set;

import org.overture.ast.node.INode;

import eu.compassresearch.core.interpreter.cml.CmlBehaviour;
import eu.compassresearch.core.interpreter.cml.CmlChannel;

public class CmlEventFactory {

	protected static CmlTauEvent instance = null;
	
	/*
	 * Tau event factory methods
	 */
	
	public static CmlSpecialEvent referenceTauEvent()
	{
		if(instance == null)
			instance = new CmlTauEvent(null,null,null,"referenceTau");
		
		return instance;
	}
	
	public static CmlSpecialEvent newTauEvent(CmlBehaviour source, INode transitionSrcNode, INode transitionDstNode, String transitionMessage)
	{
		return new CmlTauEvent(source, transitionSrcNode,transitionDstNode,transitionMessage);
	}
	
	
	/*
	 * prefix event factory methods 
	 */
	
	public static AbstractObservableEvent newPrefixEvent(CmlBehaviour eventSource, CmlChannel channel) {
		return new PrefixEvent(eventSource,channel);
	}
	
	public static AbstractObservableEvent newPrefixEvent(Set<CmlBehaviour> eventSources, CmlChannel channel) {
		return new PrefixEvent(eventSources,channel);
	}
	
	public static AbstractObservableEvent newPrefixEvent(CmlChannel channel) {
		return new PrefixEvent(channel);
	}
	
	/*
	 * communication event factory methods 
	 */
	
	public static AbstractObservableEvent newCmlCommunicationEvent(CmlBehaviour source, CmlChannel channel, List<CommunicationParameter> params)
	{
		return new CmlCommunicationEvent(source, channel, params);
	}
	
	public static AbstractObservableEvent newCmlCommunicationEvent(CmlChannel channel, List<CommunicationParameter> params)
	{
		return new CmlCommunicationEvent(channel, params);
	}
	
}
