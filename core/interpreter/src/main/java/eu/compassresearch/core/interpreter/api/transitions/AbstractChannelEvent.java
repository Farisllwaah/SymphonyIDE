package eu.compassresearch.core.interpreter.api.transitions;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.overture.interpreter.values.Value;

import eu.compassresearch.core.interpreter.api.CmlChannel;
import eu.compassresearch.core.interpreter.api.behaviour.CmlBehaviour;

/**
 * This represents an observable channel event from a set of CmlBehaviourThread objects
 * @author akm
 *
 */
abstract class AbstractChannelEvent extends AbstractCmlTransition implements ChannelEvent{

	final protected CmlChannel channel;
	
	
	public AbstractChannelEvent(CmlBehaviour eventSource, CmlChannel channel)
	{
		super(eventSource);
		this.channel = channel;
	}
	
	public AbstractChannelEvent(CmlChannel channel)
	{
		super(new HashSet<CmlBehaviour>());
		this.channel = channel;
	}
	
	protected AbstractChannelEvent(Set<CmlBehaviour> sources, CmlChannel channel)
	{
		super(sources);
		this.channel = channel;
	}
	
	/**
	 * The channel of this involved in this events
	 * @return
	 */
	public CmlChannel getChannel()
	{
		return channel;
	}

	/**
	 * Two Observable events are comparable if the are occurring on the same channel and
	 * the sources of one must either be a subset of the other or equal to.
	 * 
	 * Values do not have to be identical
	 * @param other
	 * @return
	 */
	public boolean isComparable(ObservableEvent other) {

		return this.equals(other);
	}
	
	public abstract Value getValue();
	
	public abstract void setValue(Value value);
	
	public abstract boolean isPrecise();
	
	public abstract List<ChannelEvent> expand();
	
	/**
	 * return the most precise of this and other
	 * @param other
	 * @return
	 */
	public abstract ObservableEvent meet(ObservableEvent other); 
	
	@Override
	public boolean equals(Object obj) {

		ChannelEvent other = null;
		
		if(!(obj instanceof ChannelEvent))
			return false;
		
		other = (ChannelEvent)obj;
		
		return other.getChannel().equals(getChannel()) && 
				super.equals(obj);
	}
	
}
