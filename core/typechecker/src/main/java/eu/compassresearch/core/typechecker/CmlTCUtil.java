package eu.compassresearch.core.typechecker;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.LinkedList;
import java.util.List;

import org.overture.ast.definitions.AExplicitFunctionDefinition;
import org.overture.ast.definitions.ATypeDefinition;
import org.overture.ast.definitions.PDefinition;
import org.overture.ast.expressions.PExp;
import org.overture.ast.factory.AstFactory;
import org.overture.ast.lex.LexIdentifierToken;
import org.overture.ast.lex.LexLocation;
import org.overture.ast.lex.LexNameToken;
import org.overture.ast.patterns.APatternListTypePair;
import org.overture.ast.patterns.PPattern;
import org.overture.ast.typechecker.NameScope;
import org.overture.ast.types.AFunctionType;
import org.overture.ast.types.AOperationType;
import org.overture.ast.types.PType;
import org.overture.typechecker.Environment;
import org.overture.typechecker.assistant.definition.PDefinitionAssistantTC;

import eu.compassresearch.ast.definitions.AClassDefinition;


public class CmlTCUtil {

	
	public static String getErrorMessages(RuntimeException e)
	{
		ByteArrayOutputStream b = new ByteArrayOutputStream();
		PrintWriter pw = new PrintWriter(b);
		e.printStackTrace(pw);
		pw.flush();
		try {
			b.flush();
		} catch (IOException e1) {
		}
		return new String(b.toByteArray());
	}
	
	/**
	 * Create a pre- or post-condition.
	 * 
	 * The result of this function is appropriate to assign to the predef or postdef 
	 * fields of A*CmlOperationDefinition or A*FunctionDefinition
	 * 
	 *  Post conditions allows access to old-name and hence these must be available in the
	 *  environment when checking the post condition.
	 * 
	 * @param prefix - typically the prefix is "pre" for preconditions and "post" for post conditions however it is up to the client. The resulting function name is prefix_target.getName().
	 * @param target - The function or operation for which we are creating a predef
	 * @param type - The of the function or operation in target
	 * @param parameters - The parameter list for the function
	 * @param condition - The pre/post condition field of target.
	 * @return An AExplicitFunctionDefinition taking same arguments as target (parameters) and returns boolean with condition as body.
	 * 
	 * NOTICE! The type of condition must be boolean for the result to be well typed. This is *NOT* checked 
	 */
	public static AExplicitFunctionDefinition buildCondition(String prefix, PDefinition target, PType type, List<APatternListTypePair> parameters, PExp condition)
	{
		// create new with pre_ before the name
		LexNameToken name = new LexNameToken("", new LexIdentifierToken(prefix+"_"+target.getName().getName(), false, target.getLocation()));

		// pre/post conditions are local scope
		NameScope scope = NameScope.LOCAL;

		// TODO: RWL Figure out what this is an why it is there
		List<LexNameToken> typeParams = new LinkedList<LexNameToken>();

		// Extract parameterTypes from the given type
		LinkedList<PType> parameterTypes = null;

		if (type instanceof AFunctionType)
			parameterTypes = ((AFunctionType) type).getParameters();

		if (type instanceof AOperationType)
			parameterTypes = ((AOperationType) type).getParameters();

		if (parameterTypes == null)
			return null;

		// Clone parameters as they are tree nodes and transform from cml operations params to Overture function params :S
		List<List<PPattern>> newParameters = new LinkedList<List<PPattern>>();
		for(APatternListTypePair p : parameters)
		{
			List<PPattern> pList = new LinkedList<PPattern>();
			for(PPattern pptrn : p.getPatterns())
				pList.add(pptrn.clone());
			newParameters.add(pList);
		}


		// The body is the given condition, we assume ot has type boolean
		PExp body = condition;

		// pre/post-condition on a post or pre condition is not used.
		PExp precondition = null;
		PExp postcondition = null;

		// TODO RWL: This is false as we are not generating this for a type invariant.
		// Client code for type invariants could use this function but must explicitely 
		// set the type Invariant flag afterwards on the resulting definition.
		boolean typeInvariant = false;

		// Recursive pre/post-condition, we don't do it !
		LexNameToken measuref = null;

		// Alright create the result
		AFunctionType preDefType = AstFactory.newAFunctionType(target.getLocation(), false, parameterTypes, AstFactory.newABooleanBasicType(target.getLocation()));
		AExplicitFunctionDefinition preDef = AstFactory.newAExplicitFunctionDefinition(name, scope, typeParams, preDefType, newParameters, body, precondition, postcondition, typeInvariant, measuref);
		return preDef;
	}

	/**
	 * 
	 * Type Checking relies on every PDefinition has a non-null name. This method allows us
	 * to generate a LexNameToken with an empty string and only a location quickly for setting the
	 * name of definitions that lack a name.
	 * 
	 * @param loc
	 * @return
	 */
	public static LexNameToken newEmptyStringLexName(LexLocation loc)
	{
		return new LexNameToken("", new LexIdentifierToken("",false,loc));
	}

	/**
	 * Given an overture env find the above cml env
	 * @param question
	 * @return
	 */
	public static CmlTypeCheckInfo getCmlEnv(org.overture.typechecker.TypeCheckInfo question)
	{
		if (question instanceof CmlTypeCheckInfo)
			return (CmlTypeCheckInfo)question;
		return question.contextGet(CmlTypeCheckInfo.class);
	}


	/**
	 * Search from the given environment and outwards towards the top-level
	 * environment. The first definition in the nearest environment is returned.
	 * 
	 * 
	 * @param name
	 * @param overtureEnv
	 * @return
	 */
	public static PDefinition findNearestFunctionOrOperationInEnvironment(LexNameToken name, Environment overtureEnv)
	{
		PDefinition result = null;
		org.overture.typechecker.Environment cur = overtureEnv;
		while(cur != null && result == null)
		{
			List<PDefinition> defs = cur.getDefinitions();
			if (defs != null)
			{
				for(PDefinition def : defs)
				{
					LexNameToken defName = def.getName();
					if (defName != null && defName.getName() != null && defName.getName().startsWith(name.getName()))
					{
						if (PDefinitionAssistantTC.isFunctionOrOperation(def))
						{
							return result = def;
						}

					} 
				}
			}
			cur = cur.getOuter();
		}
		return result;
	}

	/**
	 * Look everywhere for the given name in order:
	 * 1) Locally in the present questions's definition list (env.definitions)
	 * 2) The anywhere in the enclosingDefinition 
	 * 3) If still not found expand to global scope if we have an Cml environemt
	 * 
	 * 
	 * @param question
	 * @return
	 */
	public static PDefinition findDefByAllMeans(org.overture.typechecker.TypeCheckInfo question, LexIdentifierToken id)
	{
		PDefinition res = null;
		LexNameToken sought = null;
		if (id instanceof LexNameToken)
			sought = (LexNameToken)id;
		else
			sought = new LexNameToken("",id);

		// search locally names
		res = question.env.findName(sought, NameScope.NAMESANDANYSTATE);
		if (res != null) return res;

		// search locally types
		res = question.env.findType(sought,"");
		if (res != null) return res;

		// search enclosing definition
		PDefinition enclosingDef = question.env.getEnclosingDefinition();
		for(NameScope scope : NameScope.values())
		{
			try {
				res = PDefinitionAssistantTC.findName(enclosingDef, sought, scope);
				if (res != null) return res;
			} catch (Exception e)
			{
				// silently ignore.
			}
		}

		// search globally
		CmlTypeCheckInfo cmlEnv = getCmlEnv(question);
		if (cmlEnv == null) return null; // no global scope :(

		PDefinition globalDef = cmlEnv.getGlobalClassDefinitions();
		for(NameScope scope : NameScope.values())
		{
			try {
				res = PDefinitionAssistantTC.findName(globalDef, sought, scope);
				if (res != null) return res;
			} catch (Exception e)
			{
				// silently ignore.
			}
		}


		return null;
	}


	/**
	 * Run through the definitions constituting the given class node and flatten paragraph sections
	 * and add the definitions to the environment.
	 * 
	 * @param info
	 * @param node
	 * @return
	 */
	static CmlTypeCheckInfo createCmlClassEnvironment(CmlTypeCheckInfo info,
			AClassDefinition node) {

		CmlTypeCheckInfo cmlClassEnv = info.newScope();
		info.addType(node.getName(), node);

		for(PDefinition def : node.getBody())
		{
			List<PDefinition> l = TCDeclAndDefVisitor.handleDefinitionsForOverture(def);
			if (l != null)
				for(PDefinition dd : l)
				{
					if (dd instanceof ATypeDefinition)
						info.addType(dd.getName(), dd);
					else
						info.addVariable(dd.getName(),dd);
				}		
		}

		return cmlClassEnv;

	}

}
