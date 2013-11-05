package eu.compassresearch.core.typechecker;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Vector;

import org.overture.ast.definitions.PDefinition;
import org.overture.typechecker.TypeCheckException;

import eu.compassresearch.core.parser.CmlParserError;
import eu.compassresearch.core.parser.ParserUtil;
import eu.compassresearch.core.parser.ParserUtil.ParserResult;
import eu.compassresearch.core.typechecker.api.ICmlTypeChecker;
import eu.compassresearch.core.typechecker.api.ITypeIssueHandler;

public class TestUtil
{
	public static class TypeCheckerResult
	{
		public ITypeIssueHandler issueHandler;
		public boolean parsedOk;
		public boolean tcOk;
		public List<String> parseErrors = new Vector<String>();
		public List<PDefinition> sources;
	}

	public static TypeCheckerResult runTypeChecker(String... fileNames)
			throws IOException
	{
		TypeCheckerResult res = new TypeCheckerResult();

		List<File> files = new Vector<File>();
		for (String f : fileNames)
		{
			files.add(new File(f));
		}

		ParserResult parserRes = ParserUtil.parse(files);

		res.parsedOk = parserRes.errors.isEmpty();
		for (CmlParserError err : parserRes.errors)
		{
			res.parseErrors.add(err.toString());
		}

		if (res.parsedOk)
		{
			ITypeIssueHandler issueHandler = new CollectingIssueHandler();
			res.issueHandler = issueHandler;
			ICmlTypeChecker checker = new VanillaCmlTypeChecker(parserRes.definitions, issueHandler);

			try
			{
				res.tcOk = checker.typeCheck();
			} catch (TypeCheckException e)
			{
				issueHandler.addTypeError(e.node, e.location, e.getMessage());
			}
		}
		res.sources = parserRes.definitions;
		;

		return res;
	}

	// static List<String> parse(AFileSource... files)
	// {
	// // boolean ok = true;
	// List<String> errors = new Vector<String>();
	// for (AFileSource fileSource : files)
	// {
	// CmlLexer lexer = null;
	// CmlParser parser = null;
	// try
	// {
	// ANTLRInputStream in = new ANTLRInputStream(new FileInputStream(fileSource.getFile()));
	// lexer = new CmlLexer(in);
	// lexer.sourceFileName = fileSource.getFile().getName();
	// CommonTokenStream tokens = new CommonTokenStream(lexer);
	// parser = new CmlParser(tokens);
	// parser.sourceFileName = lexer.sourceFileName;
	// fileSource.setParagraphs(parser.source());
	//
	// // ok &= true;
	// } catch (Exception e)
	// {
	// // e.printStackTrace();
	// // ok &= false;
	// }
	//
	// for (CmlParserError string : lexer.getErrors())
	// {
	// errors.add(string.toString());
	// }
	// for (CmlParserError string : parser.getErrors())
	// {
	// errors.add(string.toString());
	// }
	// }
	//
	// return errors;
	//
	// }
}
