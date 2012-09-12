package eu.compassresearch.core.typechecker;

import java.util.LinkedList;
import java.util.List;

import org.overture.parser.messages.VDMError;
import org.overture.typechecker.visitor.TypeCheckerExpVisitor;

import eu.compassresearch.ast.actions.SStatementAction;
import eu.compassresearch.ast.analysis.AnalysisException;
import eu.compassresearch.ast.analysis.QuestionAnswerAdaptor;
import eu.compassresearch.ast.declarations.AChannelNameDeclaration;
import eu.compassresearch.ast.definitions.AChannelParagraphDefinition;
import eu.compassresearch.ast.definitions.AClassParagraphDefinition;
import eu.compassresearch.ast.definitions.AExplicitFunctionDefinition;
import eu.compassresearch.ast.definitions.AExplicitOperationDefinition;
import eu.compassresearch.ast.definitions.AFunctionParagraphDefinition;
import eu.compassresearch.ast.definitions.ALocalDefinition;
import eu.compassresearch.ast.definitions.AOperationParagraphDefinition;
import eu.compassresearch.ast.definitions.AProcessDefinition;
import eu.compassresearch.ast.definitions.AProcessParagraphDefinition;
import eu.compassresearch.ast.definitions.ATypeDefinition;
import eu.compassresearch.ast.definitions.ATypesParagraphDefinition;
import eu.compassresearch.ast.definitions.AValueDefinition;
import eu.compassresearch.ast.definitions.AValueParagraphDefinition;
import eu.compassresearch.ast.definitions.PDefinition;
import eu.compassresearch.ast.expressions.PExp;
import eu.compassresearch.ast.patterns.AIdentifierPattern;
import eu.compassresearch.ast.patterns.PPattern;
import eu.compassresearch.ast.typechecker.NameScope;
import eu.compassresearch.ast.types.AChannelType;
import eu.compassresearch.ast.types.AClassType;
import eu.compassresearch.ast.types.AErrorType;
import eu.compassresearch.ast.types.AFunctionParagraphType;
import eu.compassresearch.ast.types.AFunctionType;
import eu.compassresearch.ast.types.AOperationType;
import eu.compassresearch.ast.types.AProcessParagraphType;
import eu.compassresearch.ast.types.ATypeParagraphType;
import eu.compassresearch.ast.types.AValueParagraphType;
import eu.compassresearch.ast.types.PType;
import eu.compassresearch.transformation.CmlAstToOvertureAst;
import eu.compassresearch.transformation.CopyTypesFromOvtToCmlAst;

@SuppressWarnings("serial")
public class TCDeclAndDefVisitor extends
    QuestionAnswerAdaptor<TypeCheckQuestion, PType>
  {
    
    // Errors and other things are recorded on this guy
    private VanillaCmlTypeChecker parentChecker;
    
    public TCDeclAndDefVisitor(VanillaCmlTypeChecker parent)
      {
        this.parentChecker = parent;
      }
    
    // -------------------------------------------------------
    // Cases
    // -------------------------------------------------------
    
    @Override
    public PType caseATypesParagraphDefinition(ATypesParagraphDefinition node,
        TypeCheckQuestion question) throws AnalysisException
      {
        
        LinkedList<ATypeDefinition> defs = node.getTypes();
        for (ATypeDefinition d : defs)
          {
            PType type = d.apply(this, question);
            question.addType(d.getName(), type);
          }
        node.setType(new ATypeParagraphType());
        return node.getType();
      }
    
    @Override
    public PType caseAValueParagraphDefinition(AValueParagraphDefinition node,
        TypeCheckQuestion question) throws AnalysisException
      {
        
        LinkedList<PDefinition> list = node.getValueDefinitions();
        for (PDefinition def : list)
          {
            PType defType = def.apply(parentChecker, question);
            question.addType(def.getName(), defType);
          }
        
        node.setType(new AValueParagraphType());
        return node.getType();
      }
    
    @Override
    public PType caseAValueDefinition(AValueDefinition node,
        TypeCheckQuestion question) throws AnalysisException
      {
        // Use Overture to get type for expression
        PExp exp = runOvertureTypeCheckerOnCmlExpression(node.getExpression(),
            question);
        
        PType declaredType = node.getType();
        PType expressionType = exp.getType();
        
        // Check type consistency
        if (!question.isFirstSubTypeOfSecond(expressionType, declaredType))
          parentChecker.addTypeError(exp,
              TypeErrorMessages.EXPECTED_SUBTYPE_RELATION.customizeMessage(
                  declaredType.toString(), expressionType.toString()));
        
        // No matter the declared type is the type of the definition
        node.setType(declaredType);
        return node.getType();
      }
    
    // ------------------------------------------------
    // Paragraphs
    // -------------------------------------------------
    /**
     * <pre>
     * C:AClassParagraphDefinition Def_1 Type Ok 
     * . 
     * . 
     * . 
     * Def_n Type Ok
     * -------------- C |- C Type OK
     * </pre>
     * 
     */
    @Override
    public PType caseAClassParagraphDefinition(AClassParagraphDefinition node,
        TypeCheckQuestion question) throws AnalysisException
      {
        
        for (PDefinition def : node.getDefinitions())
          {
            PType type = def.apply(parentChecker, question);
            if (type == null)
              throw new AnalysisException("Unable to determine type for: "
                  + def);
          }
        
        PType clzType = new AClassType();
        clzType.setDefinitions(node.getDefinitions());
        node.setType(clzType);
        
        return clzType;
      }
    
    /**
     * A Function Paragraph is well typed if all the type definitions it
     * contains are well typed.
     * 
     * 
     * 
     */
    @Override
    public PType caseAFunctionParagraphDefinition(
        AFunctionParagraphDefinition node, TypeCheckQuestion question)
        throws AnalysisException
      {
        for (PDefinition def : node.getFunctionDefinitions())
          {
            PType type = def.apply(parentChecker, question);
            if (type == null)
              throw new AnalysisException(
                  "Unable to determined type for function definition: " + def);
          }
        node.setType(new AFunctionParagraphType());
        
        return node.getType();
      }
    
    @Override
    public PType caseAOperationParagraphDefinition(
        AOperationParagraphDefinition node, TypeCheckQuestion question)
        throws AnalysisException
      {
        
        for (PDefinition def : node.getOperations())
          {
            PType defType = def.apply(this, question);
            if (defType == null)
              parentChecker.addTypeError(def,
                  TypeErrorMessages.COULD_NOT_DETERMINE_TYPE
                      .customizeMessage(def.toString()));
            question.addVariable(def.getName(), def);
          }
        
        return node.getType();
      }
    
    /**
     * A process paragraph is well typed if all its constituents are.
     * 
     * <pre>
     * |- P:AProcessParagraphDefinition 
     * |- Process_1 Type Ok 
     * . 
     * . 
     * . 
     * |- Process_N Type Ok 
     *    --------- 
     * |- P Type Ok
     * </pre>
     * 
     * 
     */
    @Override
    public PType caseAProcessParagraphDefinition(
        AProcessParagraphDefinition node, TypeCheckQuestion question)
        throws AnalysisException
      {
        
        AProcessDefinition pdef = node.getProcessDefinition();
        pdef.apply(this, question);
        
        // Marker type indicating paragraph type check ok
        node.setType(new AProcessParagraphType());
        question.addVariable(node.getName(), node);
        
        return node.getType();
      }
    
    @Override
    public PType caseAChannelParagraphDefinition(
        AChannelParagraphDefinition node, TypeCheckQuestion question)
        throws AnalysisException
      {
        
        LinkedList<AChannelNameDeclaration> cns = node.getChannelNames();
        for (AChannelNameDeclaration decl : cns)
          {
            PType typeBack = decl.apply(this, question);
            if (typeBack == null)
              parentChecker.addTypeError(decl,
                  TypeErrorMessages.COULD_NOT_DETERMINE_TYPE
                      .customizeMessage(decl.getIdentifier().name));
            else
              question.addChannel(decl.getIdentifier(), decl);
          }
        
        node.setType(new AChannelType());
        question.addType(node.getName(), node.getType());
        
        return node.getType();
      }
    
    /*
     * The Overture TypeCheckVisitor needs a parent visitor to invoke. At this
     * time we provide the empty one.
     */
    private static class FakeOvertureRootVisitor extends
        org.overture.typechecker.visitor.TypeCheckVisitor
      {
        
      }
    
    /*
     * Utility method to transform from CmlAst to OvtAst, type check OvtAst copy
     * the types froom OvtAst and Back to the CmlAst.
     * 
     * 
     * 
     * @param cml - cml subtree
     * 
     * @param nfo - the current environment
     * 
     * @return - cml subtree with types
     * 
     * @throws AnalysisException - if things goes wrong, like if some tree-node
     * is named differently in the OveAst than in the CmlAst.
     * 
     * @throws org.overture.ast.analysis.AnalysisException
     */
    @SuppressWarnings("unchecked")
    private <T extends PExp> T runOvertureTypeCheckerOnCmlExpression(T cml,
        TypeCheckQuestion nfo) throws AnalysisException
      {
        
        org.overture.typechecker.TypeChecker.clearErrors();
        
        CmlAstToOvertureAst transform = new CmlAstToOvertureAst();
        org.overture.ast.expressions.PExp ovtNode = (org.overture.ast.expressions.PExp) (cml
            .apply(transform));
        org.overture.typechecker.visitor.TypeCheckerExpVisitor exprCheckerExpVisitor = new TypeCheckerExpVisitor(
            new FakeOvertureRootVisitor());
        org.overture.typechecker.TypeCheckInfo ovtQuestion = new org.overture.typechecker.TypeCheckInfo(
            null); // TODO: fix null to be the right env
        
        try
          {
            ovtNode.apply(exprCheckerExpVisitor, ovtQuestion);
          } catch (org.overture.ast.analysis.AnalysisException ae)
          {
            throw new AnalysisException(ae.getMessage());
          }
        
        if (org.overture.typechecker.TypeChecker.getErrorCount() > 0)
          {
            for (VDMError vdme : org.overture.typechecker.TypeChecker
                .getErrors())
              parentChecker.addTypeError(cml, vdme.message);
            cml.setType(new AErrorType());
            return cml;
          }
        
        CopyTypesFromOvtToCmlAst copier = new CopyTypesFromOvtToCmlAst(
            transform.getNodeMap());
        
        PExp result = cml;
        try
          {
            result = (PExp) ovtNode.apply(copier);
          } catch (org.overture.ast.analysis.AnalysisException ae)
          {
            throw new AnalysisException(ae.getMessage());
          }
        
        return (T) result;
      }
    
    @Override
    public PType caseAExplicitOperationDefinition(
        AExplicitOperationDefinition node, TypeCheckQuestion question)
        throws AnalysisException
      {
        // add the state of the enclosing class to the Environment
        
        // add the parameter to the Environment
        
        // check the body
        TypeCheckQuestion newQuestion = createEnvironmentWithFormals(question,
            node);
        SStatementAction operationBody = node.getBody();
        PType bodyType = operationBody.apply(parentChecker, newQuestion);
        if (bodyType == null)
          throw new AnalysisException("Unable to type check operation body "
              + node.getName());
        
        // check constructor
        boolean isCtor = node.getIsConstructor();
        if (isCtor) // check type is of class type
          {
            AOperationType operType = node.getType();
            if (!operType.getResult().equals(
                node.getAncestor(AClassParagraphDefinition.class).getType()))
              {
                
              }
            
          }
        
        return node.getType();
      }
    
    private TypeCheckQuestion createEnvironmentWithFormals(
        TypeCheckQuestion current, PDefinition funDef) throws AnalysisException
      {
        
        List<PType> paramTypes = null;
        List<PPattern> patterns = null;
        
        if (funDef instanceof AExplicitFunctionDefinition)
          {
            // Flatten list of lists into one list.
            patterns = new LinkedList<PPattern>();
            for (List<PPattern> lp : AExplicitFunctionDefinition.class.cast(
                funDef).getParamPatternList())
              for (PPattern p : lp)
                patterns.add(p);
            
            paramTypes = AExplicitFunctionDefinition.class.cast(funDef)
                .getType().getParameters();
          } else if (funDef instanceof AExplicitOperationDefinition)
          {
            patterns = AExplicitOperationDefinition.class.cast(funDef)
                .getParameterPatterns();
          }
        
        // setup local environment
        TypeCheckQuestion functionBodyEnv = current.newScope();
        
        // add formal arguments to the environment
        int i = 0;
        for (PPattern p : patterns)
          {
            if (p instanceof AIdentifierPattern)
              {
                PType paramType = (i < paramTypes.size() ? paramTypes.get(i)
                    : new AErrorType(p.getLocation(), true));
                AIdentifierPattern idp = (AIdentifierPattern) p;
                ALocalDefinition local = new ALocalDefinition(
                    idp.getLocation(), idp.getName(), NameScope.LOCAL, false,
                    null, paramType, null);
                functionBodyEnv.addVariable(idp.getName(), local);
              } else
              throw new AnalysisException(
                  "Can only handle identifier patterns at this time.");
            i++;
          }
        
        /*
         * TODO: Question, are identifier patterns the only one we care about
         * for function parameters? If they are the AnalysisException thrown
         * above must be turned into a type error.
         */
        return functionBodyEnv;
      }
    
    @Override
    public PType caseAExplicitFunctionDefinition(
        AExplicitFunctionDefinition node, TypeCheckQuestion question)
        throws AnalysisException
      {
        
        // Type check the function body in an augmented environment
        TypeCheckQuestion newQuestion = createEnvironmentWithFormals(question,
            node);
        PExp body = node.getBody();
        runOvertureTypeCheckerOnCmlExpression(body, newQuestion);
        if (body.getType() == null)
          parentChecker.addTypeError(body,
              TypeErrorMessages.COULD_NOT_DETERMINE_TYPE.customizeMessage(node
                  .getName().name));
        
        // Check funcType <: bodyType in question
        AFunctionType funcType = node.getType();
        if (!question.isFirstSubTypeOfSecond(funcType, body.getType()))
          parentChecker.addTypeError(body,
              TypeErrorMessages.EXPECTED_SUBTYPE_RELATION.customizeMessage(
                  funcType.toString(), body.getType().toString()));
        
        // Nonetheless the function type will be the type its definition to
        // facilitate further type checking even in the presents of errors.
        return funcType;
      }
    
  }