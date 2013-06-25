package eu.compassresearch.ide.cml.rttplugin;

import java.io.File;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.TreeSelection;
import org.eclipse.ui.IMemento;
import org.eclipse.ui.ISelectionListener;
import org.eclipse.ui.IViewSite;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.services.ISourceProviderService;

import eu.compassresearch.rttMbtTmsClientApi.RttMbtClient;

public class RttMbtProjectExplorer extends org.eclipse.ui.navigator.CommonNavigator {

	protected String selectedObject = null;
	protected String selectedObjectPath = null;
	protected Boolean isFolderSelected = false;
	protected Boolean isFileSelected = false;
	protected RttMbtClient client = null;

	@Override
	public void init(IViewSite aSite, IMemento aMemento) throws PartInitException {
		// perform stuff from org.eclipse.ui.navigator.CommonNavigator
		super.init(aSite, aMemento);

		// get RTT-MBT client
		client = Activator.getClient();

		// set selection changed listener
		getSite().getWorkbenchWindow().getSelectionService().addSelectionListener(commandStateListener);
	}

	private ISelectionListener commandStateListener = new ISelectionListener() {
		@Override
		public void selectionChanged(IWorkbenchPart part, ISelection selection) {
	        if (selection instanceof TreeSelection) {
	        	evaluteSelection((IStructuredSelection) selection);
	        }
		}
    };

    private void evaluteSelection(IStructuredSelection selection) {
	    // evaluate selection
		selectedObject = null;
		selectedObjectPath = null;
		isFolderSelected = false;
		isFileSelected = false;
		if (selection instanceof TreeSelection) {		    
			TreeSelection treeSelection = (TreeSelection)selection;
			if (treeSelection.size() == 0) {
				setAllKeysFlase();
				return;
			}
			if (treeSelection.size() > 1) {
				setAllKeysFlase();
				return;
			}
			if (treeSelection.getFirstElement() instanceof IFolder) {
				IFolder folder = (IFolder)treeSelection.getFirstElement();
				selectedObject = folder.getName();
				selectedObjectPath = folder.getFullPath().toString();
				isFolderSelected = true;
			} else if ((treeSelection.getFirstElement() != null) &&
				(treeSelection.getFirstElement() instanceof IFile)) {
				IFile file = (IFile)treeSelection.getFirstElement();
				selectedObject = file.getName();
				selectedObjectPath = file.getFullPath().toString();
				isFileSelected = true;
			}
		}
		if ((selectedObject == null) ||
			(selectedObjectPath == null)) {
			setAllKeysFlase();
			return;
		}
		if (isGenerationContextSelected()) {
			setService(RttMbtCommandState.keyIsGenerationContextTP,
                       RttMbtCommandState.TRUE);
			setService(RttMbtCommandState.keyIsExecutionContextTP,
                       RttMbtCommandState.FALSE);
		} else if (isExecutionContextSelected()) {
			setService(RttMbtCommandState.keyIsGenerationContextTP,
                       RttMbtCommandState.FALSE);
			setService(RttMbtCommandState.keyIsExecutionContextTP,
                       RttMbtCommandState.TRUE);
		} else {
			setAllKeysFlase();
		}
		return;
	}

    private void setAllKeysFlase() {
		setService(RttMbtCommandState.keyIsGenerationContextTP, RttMbtCommandState.FALSE);
		setService(RttMbtCommandState.keyIsExecutionContextTP, RttMbtCommandState.FALSE);
    }

    private void setService(String key, String value) {
	    // get service provider
	    ISourceProviderService sourceProviderService = 
	    		(ISourceProviderService) getSite().getService(ISourceProviderService.class);
	    RttMbtCommandState commandStateService = 
	    		(RttMbtCommandState) sourceProviderService.getSourceProvider(RttMbtCommandState.keyIsGenerationContextTP);
	    // set value
		commandStateService.setValue(key,value);    	
    }
    
	// check if the selected item is a test procedure generation context
    public Boolean isGenerationContextSelected() {
		if (selectedObjectPath == null) {
			return false;
		}
		int idx = selectedObjectPath.lastIndexOf(selectedObject);
		if (idx < 1) { return false; }
		String path = selectedObjectPath.substring(0, idx - 1);
		String TProcGenCtx = client.getRttMbtTProcGenCtxFolderName();
		idx = path.lastIndexOf(TProcGenCtx);
		if (idx < 1) { return false; }
		if ((path.substring(idx - 1, idx).compareTo("/") != 0) &&
			(path.substring(idx - 1, idx).compareTo(File.separator) != 0)) { return false; }
		String objectContainer = path.substring(idx, path.length());
		return objectContainer.compareTo(TProcGenCtx) == 0;
	}
	
	// check if the selected item is an rtt-test-procedure
	public Boolean isExecutionContextSelected() {
		if (selectedObjectPath == null) {
			return false;
		}
		int idx = selectedObjectPath.lastIndexOf(selectedObject);
		if (idx < 1) { return false; }
		String path = selectedObjectPath.substring(0, idx - 1);
		String RttTestProc = client.getRttMbtTestProcFolderName();
		idx = path.lastIndexOf(RttTestProc);
		if (idx < 1) { return false; }
		if ((path.substring(idx - 1, idx).compareTo("/") != 0) &&
			(path.substring(idx - 1, idx).compareTo(File.separator) != 0)) { return false; }
		String objectContainer = path.substring(idx, path.length());
		return objectContainer.compareTo(RttTestProc) == 0;
	}
}