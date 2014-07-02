package eu.compassresearch.ide.refinementtool.handlers;

import java.util.LinkedList;
import java.util.List;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.handlers.HandlerUtil;
import org.eclipse.ui.part.FileEditorInput;
import org.overture.ast.analysis.AnalysisException;
import org.overture.ast.definitions.PDefinition;
import org.overture.ast.node.INode;

import eu.compassresearch.ast.actions.AStmAction;
import eu.compassresearch.ast.statements.AActionStm;
import eu.compassresearch.ide.core.resources.ICmlProject;
import eu.compassresearch.ide.core.resources.ICmlSourceUnit;
import eu.compassresearch.ide.refinementtool.INodeNearCaret;
import eu.compassresearch.ide.refinementtool.IRefineLaw;
import eu.compassresearch.ide.refinementtool.RefConstants;
import eu.compassresearch.ide.refinementtool.laws.ChoiceStopLeft;
import eu.compassresearch.ide.refinementtool.laws.ChoiceStopRight;
import eu.compassresearch.ide.refinementtool.laws.DummyRefineLaw;
import eu.compassresearch.ide.refinementtool.laws.ImplicitOperationRefineLaw;
import eu.compassresearch.ide.refinementtool.laws.LetIntroRefineLaw;
import eu.compassresearch.ide.refinementtool.laws.LetPreRefineLaw;
import eu.compassresearch.ide.refinementtool.laws.NullRefineLaw;
import eu.compassresearch.ide.refinementtool.laws.SpecIterRefineLaw;
import eu.compassresearch.ide.refinementtool.laws.SpecPostRefineLaw;
import eu.compassresearch.ide.refinementtool.laws.SpecPreRefineLaw;
import eu.compassresearch.ide.refinementtool.laws.SpecSeqRefineLaw;
import eu.compassresearch.ide.refinementtool.laws.SpecSkipRefineLaw;
import eu.compassresearch.ide.refinementtool.view.RefineLawView;
import eu.compassresearch.ide.ui.editor.core.CmlEditor;

/**
 * Our sample handler extends AbstractHandler, an IHandler base class.
 * @see org.eclipse.core.commands.IHandler
 * @see org.eclipse.core.commands.AbstractHandler
 */
public class RefineHandler extends AbstractHandler {
	/**
	 * The constructor.
	 */
	public RefineHandler() {
		
	}

	static IResource extractSelection(ISelection sel) {
		if (!(sel instanceof IStructuredSelection))
			return null;
		IStructuredSelection ss = (IStructuredSelection) sel;
		Object element = ss.getFirstElement();
		if (element instanceof IResource)
			return (IResource) element;
		if (!(element instanceof IAdaptable))
			return null;
		IAdaptable adaptable = (IAdaptable) element;
		Object adapter = adaptable.getAdapter(IResource.class);
		return (IResource) adapter;
	}
	
	/**
	 * the command has been executed, so extract extract the needed information
	 * from the application context.
	 */
	public Object execute(ExecutionEvent event) throws ExecutionException {
		IWorkbenchWindow window = HandlerUtil.getActiveWorkbenchWindowChecked(event);

		IStructuredSelection sel = (IStructuredSelection) window
				.getSelectionService().getSelection(
						"eu.compassresearch.ide.ui.CmlNavigator");
		IResource res = extractSelection(sel);

		IProject project = null;
		if (res != null ) {
			project = res.getProject();
		}
		
		CmlEditor editor = (CmlEditor) window.getActivePage().getActiveEditor();
		
		ICmlProject cmlProj = (ICmlProject) project
				.getAdapter(ICmlProject.class);

		List<IRefineLaw> laws = cmlProj.getModel().getAttribute(RefConstants.REF_LAWS_ID, List.class);
		
		if (laws == null) {
			laws = new LinkedList<IRefineLaw>();
			
			laws.add(new DummyRefineLaw());			
			laws.add(new NullRefineLaw());
			laws.add(new ChoiceStopLeft());
			laws.add(new ChoiceStopRight());
			laws.add(new ImplicitOperationRefineLaw());
			laws.add(new SpecSkipRefineLaw());
			laws.add(new SpecSeqRefineLaw());
			laws.add(new SpecIterRefineLaw());
			laws.add(new SpecPreRefineLaw());
			laws.add(new SpecPostRefineLaw());
			laws.add(new LetIntroRefineLaw());
			laws.add(new LetPreRefineLaw());
			
			cmlProj.getModel().setAttribute(RefConstants.REF_LAWS_ID, laws);  
		}
						
		ITextSelection selection = (ITextSelection) editor.getSelectionProvider().getSelection();
		
		INode node = null;

		FileEditorInput fei = (FileEditorInput) editor.getEditorInput();
		
		// FIXME get source unit, update implementation
		ICmlSourceUnit csu = (ICmlSourceUnit) fei.getFile().getAdapter(ICmlSourceUnit.class);
		if (csu == null || csu.hasParseErrors() || !csu.hasParseTree())
		{
			return null;
		}
		List<PDefinition> ast = csu.getParseListDefinitions();

		INodeNearCaret visitor = new INodeNearCaret(selection.getOffset(), ast.get(0));

		try
		{
			for (PDefinition def : ast)
			{
				def.apply(visitor);
			}
			// ast.apply(visitor);
			node = visitor.getBestCandidate();
		} catch (AnalysisException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		editor.selectAndReveal(node);
		
		selection = (ITextSelection) editor.getSelectionProvider().getSelection();
	
		RefineLawView rv = null;
		
		try {
			rv = (RefineLawView) window.getActivePage().showView(RefConstants.REF_LAW_VIEW);
		} catch (PartInitException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		} 

		rv.clearLaws();
		rv.setSelection(selection);
		rv.setNode(node);
		
		for (IRefineLaw l : laws) {
			while (node instanceof AActionStm || node instanceof AStmAction) {
				if (node instanceof AActionStm)
					node = ((AActionStm)node).getAction();
				if (node instanceof AStmAction)
					node = ((AStmAction)node).getStatement();

			}
			if (l.isApplicable(node)) {
				rv.addRefineLaw(l);				
			}
		}
				
		
		//IDocument doc = editor.getDocumentProvider().getDocument(editor.getEditorInput());
/*
		
		try {
			doc.replace(selection.getOffset(), selection.getLength(), "Hello!");
		} catch (BadLocationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
*/
		
		
	
/*		
		IRegion range = editor.getHighlightRange();
		
		int offset = range.getOffset();
		
		INode node = editor.getElementAt(offset);
		*/
		
		
		
		
		return null;
	}
}
