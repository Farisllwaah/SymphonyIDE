package eu.compassresearch.core.interpreter.cml.events;

import java.util.List;
import java.util.Set;

import org.overture.ast.node.INode;

import eu.compassresearch.core.interpreter.cml.CmlBehaviour;
import eu.compassresearch.core.interpreter.cml.CmlChannel;

public class CmlEventFactory {

	protected static CmlTau instance = null;
	
	/*
	 * Tau event factory methods
	 */
	
	public static CmlTau referenceTauEvent()
	{
		if(instance == null)
			instance = new SilentEvent(null,null,null,"referenceTau");
		
		return instance;
	}
	
	/*
	 * prefix event factory methods 
	 */
	
	public static ObservableEvent newPrefixEvent(CmlBehaviour eventSource, CmlChannel channel) {
		return new PrefixEvent(eventSource,channel);
	}
	
	public static ObservableEvent newPrefixEvent(Set<CmlBehaviour> eventSources, CmlChannel channel) {
		return new PrefixEvent(eventSources,channel);
	}
	
	public static ObservableEvent newPrefixEvent(CmlChannel channel) {
		return new PrefixEvent(channel);
	}
	
	/*
	 * communication event factory methods 
	 */
	
	public static ObservableEvent newCmlCommunicationEvent(CmlBehaviour source, CmlChannel channel, List<CommunicationParameter> params)
	{
		return new CmlCommunicationEvent(source, channel, params);
	}
	
	public static ObservableEvent newCmlCommunicationEvent(CmlChannel channel, List<CommunicationParameter> params)
	{
		return new CmlCommunicationEvent(channel, params);
	}
	
}
