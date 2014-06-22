package eu.compassresearch.core.interpreter;

import java.util.List;

import org.overture.ast.definitions.PDefinition;

import eu.compassresearch.core.interpreter.api.CmlBehaviorFactory;
import eu.compassresearch.core.interpreter.api.CmlInterpreter;
import eu.compassresearch.core.interpreter.api.CmlInterpreterException;

public interface InterpreterFactory
{

	public abstract void setDefaultCmlBehaviourFactory(
			CmlBehaviorFactory factory);

	/**
	 * create an instance of the Vanilla interpreter.
	 * 
	 * @param definitions
	 *            - List of parsed and type-checked CML source to interpret
	 * @return 
	 * @throws CmlInterpreterException
	 */
	public abstract CmlInterpreter newInterpreter(List<PDefinition> definitions)
			throws CmlInterpreterException;

	/**
	 * create an instance of the Vanilla interpreter.
	 * 
	 * @param definitions
	 *            - List of parsed and type-checked CML source to interpret
	 * @param config
	 *            the configuration that the interpreter should use
	 * @return 
	 * @throws CmlInterpreterException
	 */
	public abstract CmlInterpreter newInterpreter(
			List<PDefinition> definitions, Config config)
			throws CmlInterpreterException;

	public abstract Config newDefaultConfig();

	public abstract Config newDefaultConfig(boolean filterTockEvents);

}