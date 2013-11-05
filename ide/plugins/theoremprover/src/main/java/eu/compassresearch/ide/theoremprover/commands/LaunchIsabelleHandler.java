package eu.compassresearch.ide.theoremprover.commands;

import java.util.HashMap;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.commands.IHandler;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.Platform;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunchConfigurationType;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.debug.ui.DebugUITools;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.handlers.HandlerUtil;

import eu.compassresearch.ide.theoremprover.isabellelaunch.Activator;
import eu.compassresearch.ide.theoremprover.isabellelaunch.IIsabelleConstants;

public class LaunchIsabelleHandler extends AbstractHandler implements IHandler
{

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException
	{
		ILaunchConfigurationType configType = getConfigurationType(IIsabelleConstants.LAUNCH_ID_MAC);//SWITCH ON THE OS 
		ILaunchConfigurationWorkingCopy wc;
		try
		{
			wc = configType.newInstance(null, getLaunchManager().generateLaunchConfigurationName("Isabelle"));
			wc.setAttribute(IIsabelleConstants.ATTR_LOCATION, Activator.getDefault().getPreferenceStore().getString(IIsabelleConstants.ATTR_LOCATION));

			HashMap<String, String> env = new HashMap<String, String>();
			if (Activator.getDefault().getPreferenceStore().getBoolean(IIsabelleConstants.Z3_NON_COMMERCIAL))
			{
				if (isWindowsPlatform())
				{
					env.put(IIsabelleConstants.Z3_NON_COMMERCIAL, "yes");
				} else
				{
					env.put(IIsabelleConstants.Z3_NON_COMMERCIAL, "true");
				}
				wc.setAttribute(ILaunchManager.ATTR_ENVIRONMENT_VARIABLES, env);
			}

			ISelection s = HandlerUtil.getCurrentSelection(event);
			if (s instanceof IStructuredSelection)
			{
				IStructuredSelection ss = (IStructuredSelection) s;
				if (ss.getFirstElement() instanceof IResource)
				{
					IProject project = ((IResource) ss.getFirstElement()).getProject();
					wc.setContainer(project);
					wc.doSave();
				}
			}

			//The line below will launch the launch config
			DebugUITools.launch(wc, "run"); //mode is run or debug
		} catch (CoreException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return null;
	}

	private ILaunchManager getLaunchManager()
	{
		return DebugPlugin.getDefault().getLaunchManager();
	}

	protected ILaunchConfigurationType getConfigurationType(String type)
	{
		return getLaunchManager().getLaunchConfigurationType(type);
	}

	public static boolean isMacPlatform()
	{
		return Platform.getOS().equalsIgnoreCase(Platform.OS_MACOSX);
	}

	public static boolean isWindowsPlatform()
	{
		return System.getProperty("os.name").toLowerCase().contains("win");
	}

}
