package eu.compassresearch.ide.cml.interpreter_plugin;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.SynchronousQueue;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.ListViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.ui.part.ViewPart;

public class CmlEventOptionView extends ViewPart
  {
	ListViewer viewer;
	List<String> options = new LinkedList<String>();
    SynchronousQueue<String> selectSync = new SynchronousQueue<String>();
    
	
    @Override
    public String getTitle()
      {
        return "Event Options";
      }
    
    @Override
    public void setFocus()
    {
    }
    
    public void setOptions(List<String> options)
    {
    	viewer.setInput(options);
    }
    
    public String waitForSelectedOption()
    {
    	try {
			return selectSync.take();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
    	
    	return null;
    }
    
    @Override
    public void createPartControl(final org.eclipse.swt.widgets.Composite parent)
      {
    	//Composite composite = new Composite(parent, SWT.NONE);
    	viewer = new ListViewer(parent);
    	viewer.setContentProvider(new IStructuredContentProvider() {
			
    		
    		
			@Override
			public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
				System.out.println("Input changed: old=" + oldInput + ", new=" + newInput);
			}
			
			@Override
			public void dispose() {
				
			}

			@Override
			public Object[] getElements(Object inputElement) {
				List<String> strs = (List<String>)inputElement;
				return strs.toArray();
			}
		});
    	
    	viewer.addDoubleClickListener(new IDoubleClickListener() {
			
			@Override
			public void doubleClick(DoubleClickEvent event) {
				selectSync.offer(event.getSelection().toString());
				//MessageDialog.openInformation(null, "New selction", event.getSelection().toString());
			}
		});
    	
    	//viewer.setContentProvider(new ListCon)
    	
//        org.eclipse.swt.widgets.Canvas canvas = new Canvas(parent, SWT.NONE);
//        canvas.addPaintListener(new PaintListener()
//          {
//            @Override
//            public void paintControl(PaintEvent e)
//              {
//                Canvas canvas = ((Canvas) e.widget);
//                Rectangle rect = canvas.getBounds();
//                e.gc.setForeground(e.display.getSystemColor(SWT.COLOR_RED));
//                e.gc.drawFocus(5, 5, rect.width - 10, rect.height - 10);
//                e.gc.drawText("You can draw text directly on a canvas", 60, 60);
//                e.gc.drawText("Visit: http://www.compassresearch.eu/", 60, 90);
//                canvas.setBackground(new Color(parent.getDisplay(), 0, 213, 220));
//              }
//          });
      }
  }
