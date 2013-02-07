/*******************************************************************************
 *
 *	Copyright (C) 2008 Fujitsu Services Ltd.
 *
 *	Author: Nick Battle
 *
 *	This file is part of VDMJ.
 *
 *	VDMJ is free software: you can redistribute it and/or modify
 *	it under the terms of the GNU General Public License as published by
 *	the Free Software Foundation, either version 3 of the License, or
 *	(at your option) any later version.
 *
 *	VDMJ is distributed in the hope that it will be useful,
 *	but WITHOUT ANY WARRANTY; without even the implied warranty of
 *	MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *	GNU General Public License for more details.
 *
 *	You should have received a copy of the GNU General Public License
 *	along with VDMJ.  If not, see <http://www.gnu.org/licenses/>.
 *
 ******************************************************************************
 *REUSE FOR NOW
 ******************************************************************************/

package eu.compassresearch.core.analysis.pog.obligations;

import org.overture.ast.definitions.AExplicitFunctionDefinition;
import org.overture.ast.definitions.AImplicitFunctionDefinition;
import org.overture.pog.obligation.POFunctionDefinitionContext;

public class CMLPOFunctionDefinitionContext extends POFunctionDefinitionContext implements CMLPOContext{

    public CMLPOFunctionDefinitionContext(
	    AExplicitFunctionDefinition definition, boolean precond) {
	super(definition, precond);
    }

    public CMLPOFunctionDefinitionContext(
	    AImplicitFunctionDefinition definition, boolean precond) {
	super(definition, precond);
    }

    public String getGUIContext()
    {
    	return getContext();
    }
    
	public String getIsabelleContext()
	{
		return "";
	}
}
