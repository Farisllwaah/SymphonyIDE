package eu.compassresearch.ide.rttmbt;

import java.io.File;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.ui.dialogs.WizardNewProjectCreationPage;
import org.eclipse.ui.wizards.newresource.BasicNewProjectResourceWizard;

import eu.compassresearch.rttMbtTmsClientApi.IRttMbtProgressBar;
import eu.compassresearch.rttMbtTmsClientApi.RttMbtClient;

public class RttMbtProjectWizard extends BasicNewProjectResourceWizard {
	@Override
	public boolean performFinish() {

		// create RTT-MBT TMS client
		RttMbtClient client = Activator.getClient();

		// get workspace
		IWorkspace workspace = ResourcesPlugin.getWorkspace();
		File workspaceDirectory = workspace.getRoot().getLocation().toFile();

		// pass workspace information to client
		client.setWorkspacePath(workspaceDirectory.getAbsolutePath());

		// Project are NOT embedded in other projects, so the selection is not evaluated
		client.setWorkspaceProjectPrefix(null);

		// test connection to rtt-mbt-tms server
		if (client.testConenction()) {
			if (client.getVerboseLogging()) {
				client.addLogMessage("[PASS]: test RTT-MBT server connection");
			}
		} else {
			client.addErrorMessage("[FAIL]: test RTT-MBT server connection");
			client.setProgress(IRttMbtProgressBar.Tasks.Global, 100);
			return false;
		}
		client.setProgress(IRttMbtProgressBar.Tasks.Global, 10);

		// create folder
		WizardNewProjectCreationPage newFolderPage = (WizardNewProjectCreationPage) getPages()[0];
		IProject newProject = newFolderPage.getProjectHandle();
		try {
			newProject.create(null);
			newProject.open(null);
		} catch (CoreException e) {
			client.addErrorMessage("[FAIL]: creating project resource failed!");
			client.setProgress(IRttMbtProgressBar.Tasks.Global, 100);
			return false;
		}

		// get folder name
		String projectName = newProject.getName();
		client.setRttProjectName(projectName);
		client.setWorkspaceProjectName(projectName);
		client.setRttProjectPath(client.getWorkspacePath() + File.separator + projectName);
		client.addLogMessage("creating RTT-MBT project " + projectName + "... please wait for the task to be finished.");

		// start RTT-MBT-TMS session
		if (client.beginRttMbtSession()) {
			if (client.getVerboseLogging()) {
				client.addLogMessage("[PASS]: begin RTT-MBT session");
			}
		} else {
			client.addErrorMessage("[FAIL]: begin RTT-MBT session");
			client.setProgress(IRttMbtProgressBar.Tasks.Global, 100);
			return false;
		}
		client.setProgress(IRttMbtProgressBar.Tasks.Global, 15);

		// download templates
		if (client.downloadDirectory("templates")) {
			client.addLogMessage("[PASS]: downloading templates");
			client.setProgress(IRttMbtProgressBar.Tasks.Global, 75);
		} else {
			client.addErrorMessage("[FAIL]: downloading templates");
			client.setProgress(IRttMbtProgressBar.Tasks.Global, 100);
			return false;
		}

		// create/select a new project
		if (client.createProject(projectName)) {
			client.addLogMessage("[PASS]: create initial project structure");
		} else {
			client.addErrorMessage("[FAIL]: create initial project structure");
			client.setProgress(IRttMbtProgressBar.Tasks.Global, 100);
			return false;
		}
		
		// update progress bar
		client.setProgress(IRttMbtProgressBar.Tasks.Global, 100);
		return true;
	}
}
