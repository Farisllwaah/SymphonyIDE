package eu.compassresearch.core.interpreter.cml.events;

import java.util.List;

import org.overture.interpreter.values.Value;

import eu.compassresearch.core.interpreter.cml.CmlChannel;

public interface ChannelEvent extends ObservableEvent{

	/**
	 * The channel of this involved in this events
	 * @return
	 */
	public CmlChannel getChannel();
	
	public Value getValue();
	
	public void setValue(Value value);
	
	public List<ChannelEvent> expand();
	
}
