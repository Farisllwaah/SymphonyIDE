package eu.compassresearch.core.interpreter.api.transitions;

import java.io.Serializable;
import java.util.Set;

import eu.compassresearch.core.interpreter.api.behaviour.CmlTransitionSet;
import eu.compassresearch.core.interpreter.api.behaviour.CmlBehaviour;


public interface CmlTransition extends Serializable {
	
	public abstract int hashCode();
	
	public abstract String toString();
	
	public abstract boolean equals(Object obj);
	
	/**
	 * Wraps the event in a CmlAlphabet
	 * @return
	 */
	public CmlTransitionSet getAsAlphabet();
	
	/**
	 * Returns the set of CmlBehaviourThreads that are prepared to engage in this event
	 * @return
	 */
	public Set<CmlBehaviour> getEventSources();
	
	public boolean isSourcesSubset(CmlTransition other);
}
