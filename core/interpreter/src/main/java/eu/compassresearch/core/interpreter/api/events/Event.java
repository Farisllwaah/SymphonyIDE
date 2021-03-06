package eu.compassresearch.core.interpreter.api.events;

import java.io.Serializable;
import java.util.LinkedList;
import java.util.List;

public abstract class Event<T> implements Serializable
{

	/**
	 * 
	 */
	private static final long serialVersionUID = -8906502921055581670L;
	protected final List<T> sources;

	public Event(T source)
	{
		sources = new LinkedList<T>();
		this.sources.add(0, source);
	}

	public Event(List<T> sourceList)
	{
		this.sources = sourceList;
	}

	public T getSource()
	{
		return sources.get(0);
	}

	public List<T> getSources()
	{
		return new LinkedList<T>(sources);
	}

	public boolean isRedirectedEvent()
	{
		return sources.size() > 1;
	}
}
