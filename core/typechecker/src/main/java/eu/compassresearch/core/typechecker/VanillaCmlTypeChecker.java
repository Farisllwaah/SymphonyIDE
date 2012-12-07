package eu.compassresearch.core.typechecker;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.util.LinkedList;
import java.util.List;

import org.overture.ast.analysis.AnalysisException;
import org.overture.ast.analysis.intf.IQuestionAnswer;
import org.overture.ast.definitions.AClassClassDefinition;
import org.overture.ast.definitions.ATypeDefinition;
import org.overture.ast.definitions.AValueDefinition;
import org.overture.ast.definitions.PDefinition;
import org.overture.ast.expressions.PExp;
import org.overture.ast.factory.AstFactory;
import org.overture.ast.node.INode;
import org.overture.ast.patterns.PBind;
import org.overture.ast.patterns.PMultipleBind;
import org.overture.ast.typechecker.NameScope;
import org.overture.ast.types.PType;
import org.overture.typechecker.FlatEnvironment;
import org.overture.typechecker.TypeCheckInfo;

import eu.compassresearch.ast.actions.PAction;
import eu.compassresearch.ast.declarations.PDeclaration;
import eu.compassresearch.ast.definitions.AClassParagraphDefinition;
import eu.compassresearch.ast.definitions.AProcessParagraphDefinition;
import eu.compassresearch.ast.definitions.SParagraphDefinition;
import eu.compassresearch.ast.process.PProcess;
import eu.compassresearch.ast.program.AFileSource;
import eu.compassresearch.ast.program.AInputStreamSource;
import eu.compassresearch.ast.program.PSource;
import eu.compassresearch.ast.types.AErrorType;
import eu.compassresearch.core.parser.CmlParser;
import eu.compassresearch.core.typechecker.api.TypeComparator;
import eu.compassresearch.core.typechecker.api.TypeErrorMessages;
import eu.compassresearch.core.typechecker.api.TypeIssueHandler;
import eu.compassresearch.core.typechecker.api.TypeIssueHandler.CMLTypeError;
import eu.compassresearch.core.typechecker.api.TypeIssueHandler.CMLTypeWarning;

@SuppressWarnings("serial")
class VanillaCmlTypeChecker extends AbstractTypeChecker {

	
	// ---------------------------------------------
	// -- Type Checker State
	// ---------------------------------------------m
	// subcheckers
	private IQuestionAnswer<org.overture.typechecker.TypeCheckInfo, PType> exp;
	private IQuestionAnswer<org.overture.typechecker.TypeCheckInfo, PType> act;
	private IQuestionAnswer<org.overture.typechecker.TypeCheckInfo, PType> dad;
	private IQuestionAnswer<org.overture.typechecker.TypeCheckInfo, PType> typ; // basic
	private IQuestionAnswer<org.overture.typechecker.TypeCheckInfo, PType> prc;
	private IQuestionAnswer<org.overture.typechecker.TypeCheckInfo, PType> bnd; // bind
	// type
	// checker
	private boolean lastResult;
	private final TypeComparator typeComparator;
	private AClassClassDefinition globalRoot;

	private void initialize(TypeIssueHandler issueHandler) {
		if (issueHandler != null)
			this.issueHandler = issueHandler;
		else
			this.issueHandler = new CollectingIssueHandler();

		exp = new TCExpressionVisitor(this, this.issueHandler);
		act = new TCActionVisitor(this, this.issueHandler, typeComparator);
		dad = new TCDeclAndDefVisitor(this, typeComparator, this.issueHandler);
		typ = new TCTypeVisitor(this, this.issueHandler);
		prc = new TCProcessVisitor(this, this.issueHandler, typeComparator);
		bnd = new TCBindVisitor(this);

	}

	// ---------------------------------------------
	// -- Dispatch to sub-checkers
	// ---------------------------------------------

	private PType addErrorForMissingType(INode node, PType type) {
		if (type == null) {
			// addTypeError(node, "Insufficient type checker implementation.");
			return new AErrorType();
		} else
			return type;

	}

	@Override
	public PType defaultPMultipleBind(PMultipleBind node, TypeCheckInfo question)
			throws AnalysisException {
		return addErrorForMissingType(node, node.apply(bnd, question));
	}

	@Override
	public PType defaultPBind(PBind node, TypeCheckInfo question)
			throws AnalysisException {
		return addErrorForMissingType(node, node.apply(bnd, question));
	}

	@Override
	public PType defaultPType(PType node,
			org.overture.typechecker.TypeCheckInfo question)
			throws AnalysisException {
		return addErrorForMissingType(node, node.apply(typ, question));
	}

	@Override
	public PType defaultINode(INode node,
			org.overture.typechecker.TypeCheckInfo question)
			throws AnalysisException {
		return addErrorForMissingType(node, super.defaultINode(node, question));
	}

	@Override
	public PType defaultPDeclaration(PDeclaration node,
			org.overture.typechecker.TypeCheckInfo question)
			throws AnalysisException {
		return addErrorForMissingType(node, node.apply(this.dad, question));
	}

	@Override
	public PType defaultPDefinition(PDefinition node,
			org.overture.typechecker.TypeCheckInfo question)
			throws AnalysisException {
		return addErrorForMissingType(node, node.apply(this.dad, question));
	}

	@Override
	public PType defaultPExp(PExp node,
			org.overture.typechecker.TypeCheckInfo question)
			throws AnalysisException {
		return addErrorForMissingType(node, node.apply(exp, question));
	}

	@Override
	public PType defaultPProcess(PProcess node,
			org.overture.typechecker.TypeCheckInfo question)
			throws AnalysisException {
		return node.apply(prc, question);
	}

	@Override
	public PType defaultPAction(PAction node,
			org.overture.typechecker.TypeCheckInfo question)
			throws AnalysisException {
		return node.apply(act, question);
	}

	// ---------------------------------------------
	// -- Public API to CML Type Checker
	// ---------------------------------------------
	/**
	 * This method is invoked by the command line tool when pretty printing the
	 * analysis name.
	 * 
	 * @return Pretty short name for this analysis.
	 */
	public String getAnalysisName() {
		return "The CML Type Checker";
	}

	/**
	 * Construct a CmlTypeChecker with the intension of checking a list of
	 * PSources. These source may refer to each other.
	 * 
	 * 
	 * @param cmlSources
	 *            - Source containing CML Paragraphs for type checking.
	 */
	public VanillaCmlTypeChecker(List<PSource> cmlSources,
			TypeIssueHandler issueHandler) {

		this.sourceForest = cmlSources;
		typeComparator = SimpleTypeComparator.newInstance();
		initialize(issueHandler);
	}

	void clear() {
		cleared = true;
		sourceForest = null;

	}

	public VanillaCmlTypeChecker(List<PSource> cmlSource,
			TypeComparator typeComparator, TypeIssueHandler issueHandler) {
		this.sourceForest = new LinkedList<PSource>();
		sourceForest.addAll(cmlSource);
		this.typeComparator = typeComparator;
		initialize(issueHandler);
	}

	/**
	 * Construct a CmlTypeChecker with the intension of checking a single
	 * source.
	 * 
	 * @param singleSource
	 */
	public VanillaCmlTypeChecker(PSource singleSource,
			TypeIssueHandler issueHandler) {

		this.sourceForest = new LinkedList<PSource>();
		this.sourceForest.add(singleSource);
		typeComparator = SimpleTypeComparator.newInstance();
		initialize(issueHandler);

	}

	/**
	 * Run the type checker. This will update the source(s) this type checker
	 * instance was constructed with.
	 * 
	 * @return - Returns true if the entire tree could be type checked without
	 *         errors.
	 */
	public boolean typeCheck() {

		eu.compassresearch.core.typechecker.TypeCheckInfo info = eu.compassresearch.core.typechecker.TypeCheckInfo
				.getNewTopLevelInstance(this.issueHandler, globalRoot);

		if (!cleared)
			return lastResult;

		try {
			globalRoot = CollectGlobalStateClass.getGlobalRoot(
					this.sourceForest, issueHandler, info);

			// Add all global definitions to the environment
			for (PDefinition d : globalRoot.getDefinitions()) {
				PDefinition defToAdd = null;
				if (d instanceof AValueDefinition) {
					AValueDefinition vdef = (AValueDefinition) d;
					defToAdd = AstFactory.newALocalDefinition(vdef.getLocation(),
							vdef.getName(), vdef.getNameScope(), vdef.getType());
				}

				if (d instanceof ATypeDefinition)
					defToAdd = d;

				if (defToAdd != null)
					((FlatEnvironment) info.env).add(defToAdd);
			}
			info.env.setEnclosingDefinition(globalRoot);
			info.scope = NameScope.GLOBAL;
			PType globalRootType = ((TCDeclAndDefVisitor) dad)
					.typeCheckOvertureClass(globalRoot, info);
			if (!TCDeclAndDefVisitor.successfulType(globalRootType)) {
				issueHandler.addTypeError(globalRoot,
						TypeErrorMessages.PARAGRAPH_HAS_TYPES_ERRORS
								.customizeMessage("Global Definitions"));
				return false;
			}

		} catch (AnalysisException e) {
			e.printStackTrace();
		}


		// for each source
		for (PSource s : sourceForest) {
			for (SParagraphDefinition paragraph : s.getParagraphs()) {
				if (paragraph instanceof AClassParagraphDefinition
						|| paragraph instanceof AProcessParagraphDefinition)

					try {
						PType topType = paragraph.apply(this, info);
						if (topType == null || topType instanceof AErrorType) {
							issueHandler
									.addTypeError(
											paragraph,
											TypeErrorMessages.PARAGRAPH_HAS_TYPES_ERRORS
													.customizeMessage(paragraph
															.getName()
															.toString()));
						}
					} catch (AnalysisException ae) {
						ByteArrayOutputStream baos = new ByteArrayOutputStream();
						ae.printStackTrace(new PrintStream(baos));
						issueHandler
								.addTypeError(
										s,
										"The COMPASS Type checker failed on this cml-source. Please submit it for investigation to rala@iha.dk.\n"
												+ new String(baos.toByteArray()));
						// This means we have a bug in the type checker
						return false;
					} catch (ClassCastException e) {
						ByteArrayOutputStream baos = new ByteArrayOutputStream();
						PrintWriter out = new PrintWriter(baos);
						e.printStackTrace(out);
						out.flush();
						issueHandler
								.addTypeError(
										paragraph,
										"Ill defined ast definition. Check that the implied AST-node is not defined in both cml.ast and in overtureII.astv2. Naturally, if this is the case the visitor has an ambigouos choice.\n"
												+ e.getMessage()
												+ "\n"
												+ new String(baos.toByteArray()));
					}
			}
		}
		super.cleared = false;

		return !super.issueHandler.hasErrors();
	}

	/**
	 * Get errors that occurred while type checking.
	 * 
	 * @return list of CMLTypeErrors
	 */
	public List<CMLTypeError> getTypeErrors() {
		return issueHandler.getTypeErrors();
	}

	/**
	 * Get warnings that occurred while type checking. The type check method
	 * will return true even though this returns an non-empty list.
	 * 
	 * @return list of CMLTypeWarnings
	 */
	public List<CMLTypeWarning> getTypeWarnings() {
		return issueHandler.getTypeWarnings();
	}

	// ---------------------------------------
	// Static stuff for running the TypeChecker from Eclipse
	// ---------------------------------------

	// setting the file on AFileSource allows the CmlParser factory method
	// to create both parser and lexer.
	private static PSource prepareSource(File f) {
		if (f == null) {
			AInputStreamSource iss = new AInputStreamSource();
			iss.setStream(System.in);
			iss.setOrigin("stdin");
			return iss;
		} else {
			AFileSource fs = new AFileSource();
			fs.setName(f.getName());
			fs.setFile(f);
			return fs;
		}
	}

	private static void runOnFile(File f) throws IOException {
		// set file name
		PSource source = prepareSource(f);

		// Call factory method to build parser and lexer
		CmlParser parser = CmlParser.newParserFromSource(source);

		// Run the parser and lexer and report errors if any
		if (!parser.parse()) {
			System.out.println("Failed to parse: " + source.toString());
			return;
		}

		// Type check
		VanillaCmlTypeChecker cmlTC = new VanillaCmlTypeChecker(source,
				VanillaFactory.newCollectingIssueHandle());

		// Print result and report errors if any
		if (!cmlTC.typeCheck()) {
			System.out.println("Failed to type check" + source.toString());
		}
		;

		// Report success
		System.out.println("The given CML Program is type checked.");
	}

	public static void main(String[] args) throws IOException {
		File cml_examples = new File("../../docs/cml-examples");
		int failures = 0;
		int successes = 0;
		// runOnFile(null);

		if (cml_examples.isDirectory()) {
			for (File example : cml_examples.listFiles()) {
				System.out.print("Typechecking example: " + example.getName()
						+ " \t\t...: ");
				System.out.flush();
				try {
					runOnFile(example);
					System.out.println("done");
					successes++;
				} catch (Exception e) {
					System.out.println("exception");
					failures++;
				}
			}
		}

		System.out.println(successes + " was successful, " + failures
				+ " was failures.");

	}

	public boolean hasErrors() {
		return issueHandler.hasErrors();
	}

	public boolean hasWarnings() {
		return issueHandler.hasWarnings();
	}

	public boolean hasIssues() {
		return issueHandler.hasIssues();
	}

}