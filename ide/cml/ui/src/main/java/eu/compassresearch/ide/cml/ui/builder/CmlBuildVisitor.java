package eu.compassresearch.ide.cml.ui.builder;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileReader;
import java.io.InputStream;
import java.io.PrintWriter;
import java.util.LinkedList;
import java.util.List;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceVisitor;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.Path;

import eu.compassresearch.ast.lex.LexLocation;
import eu.compassresearch.ast.program.AFileSource;
import eu.compassresearch.core.lexer.CmlLexer;
import eu.compassresearch.core.lexer.ParserError;
import eu.compassresearch.core.parser.CmlParser;
import eu.compassresearch.core.typechecker.CmlTypeChecker;
import eu.compassresearch.core.typechecker.CmlTypeChecker.CMLTypeError;
import eu.compassresearch.core.typechecker.VanillaCmlTypeChecker;
import eu.compassresearch.ide.cml.ui.editor.core.dom.CmlSourceUnit;

public class CmlBuildVisitor implements IResourceVisitor
  {
    
    @Override
    public boolean visit(IResource resource) throws CoreException
      {
            	
        // Resource for this build
        if (!shouldBuild(resource))
          return true;

        // This visitor only builds files.
        IFile file = (IFile) resource;
        
        // Generate Isabelle theory file
        if ("cml".equalsIgnoreCase(file.getFileExtension())) {
        	String thyName = Path.fromPortableString(file.getName()).removeFileExtension().toString();
        	
        	IFile thyFile = file.getProject().getFile(thyName + ".thy");
        	
        	if (!thyFile.exists()) {
        		String theory = "theory "+thyName+"\nimports Main\nbegin\n\nend";
        		InputStream thysource = new ByteArrayInputStream(theory.getBytes());
        		thyFile.create(thysource, false, null);
        	}
        }
                
        // Parse the source
        AFileSource source = new AFileSource();
        if (!parse(file, source))
          return false;
        
        
        // Lets run the type checker
        if (!typeCheck(file, source))
          return false;
        
        // Set the AST on the source unit
        CmlSourceUnit dom = CmlSourceUnit.getFromFileResource(file);
        dom.setSourceAst(source, new LinkedList<ParserError>());
        
        return false;
      }
    
    /*
     * Run the type checker.
     */
    private static boolean typeCheck(IFile file, AFileSource source)
        throws CoreException
      {
        try
          {
            CmlTypeChecker cmlTC = new VanillaCmlTypeChecker(source);
            boolean tcSuccess = cmlTC.typeCheck();
            if (!tcSuccess)
              {
                
                List<CMLTypeError> tcerrors = cmlTC.getTypeErrors();
                for (CMLTypeError tcError : tcerrors)
                  {
                    LexLocation loc = tcError.getLocation();
                    IMarker marker = file.createMarker(IMarker.PROBLEM);
                    setProblem(marker, tcError.getDescription(), loc.startLine);
                  }
              }
            return tcSuccess;
          } catch (Exception tcException)
          {
            IMarker tcMarker = file.createMarker(IMarker.PROBLEM);
            setExceptionInfo(
                tcMarker,
                tcException,
                "An exception occurred while type checking file \""
                    + file.getName() + "\".");
            
          }
        return false;
      }
    
    private static void setProblem(IMarker marker, String text, int line)
        throws CoreException
      {
        marker.setAttribute(IMarker.MESSAGE, text);
        marker.setAttribute(IMarker.SEVERITY, IMarker.SEVERITY_ERROR);
        marker.setAttribute(IMarker.LINE_NUMBER, line);
      }
    
    private static void setInfo(IMarker marker, String shortText, String text)
        throws CoreException
      {
        marker.setAttribute(IMarker.MESSAGE, shortText);
        marker.setAttribute(IMarker.SEVERITY, IMarker.SEVERITY_INFO);
        marker.setAttribute(IMarker.TEXT, text);
      }
    
    private static void setExceptionInfo(IMarker marker, Exception e,
        String shortText) throws CoreException
      {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PrintWriter pw = new PrintWriter(baos);
        e.printStackTrace(pw);
        pw.flush();
        pw.close();
        setInfo(marker, shortText, new String(baos.toByteArray()));
      }
    
    /*
     * Run the parser and lexer on the file-resource
     */
    private static boolean parse(IFile file, AFileSource source)
        throws CoreException
      {
        // Create parser and lexer to handle the given cml source
        String localPathToFile = file.getLocation().toString();
        source.setFile(new File(localPathToFile));
        CmlLexer lexer = null;
        
        // Clear markers for this file
        file.deleteMarkers(IMarker.PROBLEM, true, IResource.DEPTH_INFINITE);
        try
          {
            
            lexer = new CmlLexer(new FileReader(localPathToFile));
            CmlParser parser = new CmlParser(lexer);
            parser.setDocument(source);
            
            // parse
            if (!parser.parse())
              {
                
                // report errors
                for (ParserError e : lexer.parseErrors)
                  {
                    IMarker marker = file.createMarker(IMarker.PROBLEM);
                    setProblem(marker, e.message, e.line);
                  }
              }
          } catch (Exception e1)
          {
            IMarker m = file.createMarker(IMarker.PROBLEM);
            setExceptionInfo(m, e1,
                "An exception occurred while parsing file: \"" + file.getName()
                    + "\"");
            return false;
          }
        // Error reporting
        
        return lexer.parseErrors.size() == 0;
      }
    
    /*
     * Return true of this build visitor should continue to build the given
     * resource.
     */
    private static boolean shouldBuild(IResource resource)
      {
        return resource instanceof IFile
            && "cml".equalsIgnoreCase(((IFile) resource).getFileExtension());
      }
  }
