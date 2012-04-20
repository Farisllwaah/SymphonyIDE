%require "2.5"
//%define api.pure
%language "java"
%locations
%define parser_class_name "CmlParser"
//%define access "public"
%define package "eu.compassresearch.cml.compiler"
%code imports{

// ******************************
// *** required local imports ***
// ******************************

// required standard Java definitions
    import java.util.*;
    import java.io.File;
    import org.overture.ast.definitions.*;
    import org.overture.ast.declarations.*;
    import org.overture.ast.expressions.*;
    import org.overture.ast.patterns.*;
    import org.overture.ast.program.*;
    import org.overture.ast.types.*;
    import org.overturetool.vdmj.lex.*;
    import org.overture.ast.node.*;
    import org.overture.transforms.*;
    import org.overturetool.util.*;

    public
}


%code{
    // ************************
    // *** MEMBER VARIABLES ***
    // ************************

    //private List<PDefinition> documentDefs = new Vector<PDefinition>();
    private ASourcefileSourcefile currentSourceFile = null;

    // *************************
    // *** PRIVATE OPERATIONS ***
    // *************************
     
    private LexLocation extractLexLocation(CmlLexeme lexeme)
    {
	return new LexLocation(currentSourceFile.getFile(), "Default",
			       lexeme.getStartPos().line, lexeme.getStartPos().column, 
			       lexeme.getEndPos().line, lexeme.getEndPos().column,0,0);
    }

    private LexLocation extractLexLocation(CmlLexeme start, CmlLexeme end)
    {
	return new LexLocation(currentSourceFile.getFile(), "Default",
			       start.getStartPos().line, start.getStartPos().column, 
			       end.getEndPos().line, end.getEndPos().column,0,0);
    }

    private LexLocation extractLexLocation(CmlLexeme start, LexLocation end)
    {
	return new LexLocation(currentSourceFile.getFile(), "Default",
			       start.getStartPos().line, start.getStartPos().column, 
			       end.endLine, end.endPos,0,0);
    }

    private LexLocation combineLexLocation(LexLocation start, LexLocation end)
    {
      return new LexLocation(currentSourceFile.getFile(), "Default",
			     start.startLine, start.startPos, 
			     end.endLine, end.endPos,0,0);
    }

    
    private LexNameToken extractLexNameToken(CmlLexeme lexeme)
    {
      return new LexNameToken("Default",lexeme.getValue(), extractLexLocation(lexeme),false, true);
    }

    private LexIdentifierToken extractLexIdentifierToken(CmlLexeme lexeme)
    {
      return new LexIdentifierToken(lexeme.getValue(), false, extractLexLocation(lexeme));
    }

    // *************************
    // *** PUBLIC OPERATIONS ***
    // *************************

    public void setDocument(ASourcefileSourcefile doc)
    {
      currentSourceFile = doc;
    }
     
    public ASourcefileSourcefile getDocument()
    {
      return currentSourceFile;
    }
    
    public static void main(String[] args) throws Exception
    {
	if (args.length == 0) {
	    System.out.println("Usage : java CmlParser <inputfile>");
	}
	else {
      
	    CmlLexer scanner = null;
	    try {
	      String filePath = args[0];
	      ClonableFile file = new ClonableFile(filePath); 
	      ASourcefileSourcefile currentSourceFile = new ASourcefileSourcefile();
	      currentSourceFile.setName(file.getName());
	      currentSourceFile.setFile(file);
	      scanner = new CmlLexer( new java.io.FileReader(file) );
	      CmlParser cmlParser = new CmlParser(scanner);
	      cmlParser.setDocument(currentSourceFile);
	      //cmlParser.setDebugLevel(1);
	  
	      //do {
	      //System.out.println(scanner.yylex());
	      boolean result = cmlParser.parse();
	      if (result){
		System.out.println("parsed!");
				
		DotGraphVisitor dgv = new DotGraphVisitor();
		INode node = cmlParser.getDocument();

		node.apply(dgv,"");
				
		File dotFile = new File("generatedAST.gv");
		java.io.FileWriter fw = new java.io.FileWriter(dotFile);
		fw.write(dgv.getResultString());
		fw.close();

		System.out.println(dgv.getResultString());
	    
	      }
	      else
		System.out.println("Not parsed!");
		
	      //} while (!scanner.zzAtEOF);

	    }
	    catch (java.io.FileNotFoundException e) {
		System.out.println("File not found : \""+args[0]+"\"");
	    }
	    catch (java.io.IOException e) {
		System.out.println("IO error scanning file \""+args[0]+"\"");
		System.out.println(e);
	    }
	    catch (Exception e) {
		System.out.println("Unexpected exception:");
		e.printStackTrace();
	    }
	    
	}
    }
    
 }


/* General notes/FIXMEs:
 *
 * 1) At the moment there are a lot of shift/reduce conflicts "hidden"
 * by setting a left or right associativity.  For some this is surely
 * correct, for others it's a bit suspect
 *
 * 2) We need to go over the usage of [] and {} in the
 * CMLLanguageDef.pdf document and double-check their semantics.
 *
 */

%token CLASS END PROCESS INITIAL EQUALS AT BEGIN CSP_ACTIONS CSPSEQ CSPINTCH CSPEXTCH CSPLCHSYNC CSPRCHSYNC CSPINTERLEAVE CSPHIDE LPAREN RPAREN CSPRENAME LSQUARE RSQUARE CSPSKIP CSPSTOP CSPCHAOS RARROW LCURLY RCURLY CSPAND BAR DBAR CHANNELS CHANSETS TYPES SEMI VDMRECORDDEF VDMCOMPOSE OF VDMTYPEUNION STAR TO VDMINMAPOF VDMMAPOF VDMSEQOF VDMSEQ1OF VDMSETOF VDMPFUNCARROW VDMTFUNCARROW VDMUNITTYPE VDMTYPE VDMTYPENCMP DEQUALS VDMINV VALUES FUNCTIONS PRE POST MEASURE VDM_SUBCLASSRESP VDM_NOTYETSPEC OPERATIONS VDM_EXT VDM_RD VDM_WR STATE LET IN IF THEN ELSEIF ELSE CASES OTHERS PLUS MINUS ABS FLOOR NOT CARD POWER DUNION DINTER HD TL LEN ELEMS INDS REVERSE DCONC DOM RNG MERGE INVERSE ELLIPSIS MAPLETARROW MKUNDER DOT DOTHASH NUMERAL LAMBDA NEW SELF ISUNDER PREUNDER ISOFCLASS BACKTICK TILDE DCL ASSIGN ATOMIC OPERATIONARROW RETURN SKIP VDMDONTCARE IDENTIFIER
%token DIVIDE DIV REM MOD LT LTE GT GTE NEQ OR AND IMPLY BIMPLY INSET NOTINSET SUBSET PROPER_SUBSET UNION SETDIFF INTER CONC OVERWRITE MAPMERGE DOMRES VDM_MAP_DOMAIN_RESTRICT_BY RNGRES RNGSUB COMP ITERATE FORALL EXISTS EXISTS1 

%token NUMERAL HEX_LITERAL

%token AMP THREEBAR CSPBARGT CSPLSQUAREBAR CSPLSQUAREGT DLSQUARE DRSQUARE CSPBARRSQUARE COMMA CSPSAMEAS CSPLSQUAREDBAR CSPDBARRSQUARE CSPDBAR COLON CSP_CHANSET_BEGIN CSP_CHANSET_END CSP_CHANNEL_READ CSP_CHANNEL_WRITE CSP_VARDECL CSP_OPS_COM
%token TBOOL TNAT TNAT1 TINT TRAT TREAL TCHAR TTOKEN PRIVATE PROTECTED PUBLIC LOGICAL

%token VDMcommand nameset namesetExpr communication predicate chanset typeVarIdentifier quoteLiteral functionType localDef  implicitOperationBody

/* CSP ops and more */
%right CSPSEQ CSPINTCH CSPEXTCH CSPLCHSYNC CSPRCHSYNC CSPINTERLEAVE CSPHIDE CSPAND AMP THREEBAR RARROW DLSQUARE CSPBARGT CSPLSQUAREBAR CSPLSQUAREGT CSPBARRSQUARE LSQUARE RSQUARE CSPRENAME VDMTYPEUNION STAR VDMSETOF VDMSEQOF VDMSEQ1OF VDMMAPOF VDMINMAPOF VDMPFUNCARROW VDMTFUNCARROW TO OF NEW ASSIGN
%right ELSE ELSEIF

/* unary ops */
%right UPLUS UMINUS ABS FLOOR NOT CARD POWER DUNION DINTER HD TL LEN ELEMS INDS REVERSE DCONC DOM RNG MERGE INVERSE 
/* binary ops */
%left PLUS MINUS DIVIDE DIV REM MOD LT LTE GT GTE EQUALS NEQ OR AND IMPLY BIMPLY INSET NOTINSET SUBSET PROPER_SUBSET UNION SETDIFF INTER CONC OVERWRITE MAPMERGE DOMRES VDM_MAP_DOMAIN_RESTRICT_BY RNGRES RNGSUB COMP ITERATE IN DOT DOTHASH

 /* other hacks */
%right LPAREN

%start sourceFile

%%

/* 2 CML Grammar */

sourceFile
: programParagraphList                            
{
    List<PDeclaration> decls = (List<PDeclaration>) $1;  
    currentSourceFile.setDecls(decls);
}

| globalDecl programParagraphList                  
{
    List<PDeclaration> globalDecls = (List<PDeclaration>)$1;
    List<PDeclaration> decls = (List<PDeclaration>) $2;  
    decls.addAll(globalDecls);
    currentSourceFile.setDecls(decls);
}
| globalDecl                   
{
    List<PDeclaration> globalDecls = (List<PDeclaration>)$1;
    currentSourceFile.setDecls(globalDecls);
}
;

programParagraphList: 
  programParagraph                                
  {  
      List<PDeclaration> programParagraphList = 
	  new Vector<PDeclaration>();
      programParagraphList.add((PDeclaration)$1);
      $$ = programParagraphList;   
  }

| programParagraphList programParagraph           
{
    List<PDeclaration> programParagraphList = (List<PDeclaration>)$1;

    if (programParagraphList == null) 
	programParagraphList = new Vector<PDeclaration>();
	    
    programParagraphList.add((PDeclaration)$2);
    $$ = programParagraphList;
 }
;

programParagraph 
: classDecl                                       { $$ = $1; }
| processDecl                                     { $$ = $1; }
| channelDecl                                     { $$ = $1; }
| chansetDecl                                     { $$ = $1; }
//| globalDecl                                     { $$ = $1; }
 ;

/* 2.1 Classes */
classDecl 
: CLASS IDENTIFIER EQUALS classBody        
{ 
    Position classStartPos =  ((CmlLexeme)$1).getStartPos();
    Position classEndPos = ((CmlLexeme)$4).getEndPos();
    LexLocation loc = new LexLocation(null, "Default", classStartPos.line,classStartPos.column,classEndPos.line,classEndPos.column,0,0);
    LexNameToken lexName = extractLexNameToken((CmlLexeme)$2); 
    // $$ = new AClassClassDefinition(loc, lexName , /*NameScope nameScope_*/ null, /*Boolean used_*/ null, 
    // 				   /*AAccessSpecifierAccessSpecifier*/ null,/* List<? extends LexNameToken> supernames_*/ new Vector<LexNameToken>(), 
    // 				   null /*hasContructors_*/, /*ClassDefinitionSettings settingHierarchy_*/null, 
    // 				   null/*Boolean gettingInheritable_*/, null/*Boolean gettingInvDefs_*/, 
    // 				   /*Boolean isAbstract_*/null, /*Boolean isUndefined_*/null); 
    AClassClassDefinition c = new AClassClassDefinition();
    c.setLocation(loc);
    c.setName(lexName);
    c.setIsAbstract(false);
    c.setDefinitions((List)$3);
    $$ = c;
}
;

/* 2.2 Processes */

processDecl :
  PROCESS IDENTIFIER EQUALS processDef
  ;

processDef :
  declaration AT process
| process
  ;

process :
  BEGIN processPara AT action END
| process CSPSEQ process
| process CSPINTCH process
| process CSPEXTCH process
| process CSPLCHSYNC chansetExpr CSPRCHSYNC process
| process CSPINTERLEAVE process
| LPAREN declaration AT processDef RPAREN LPAREN expression RPAREN
| IDENTIFIER LPAREN expression RPAREN
| IDENTIFIER
| LPAREN process RPAREN LSQUARE identifierList CSPRENAME identifierList RSQUARE
| CSPSEQ LCURLY declaration AT process RCURLY
| CSPINTCH LCURLY declaration AT process RCURLY
| CSPEXTCH LCURLY declaration AT process RCURLY
| LSQUARE LCURLY chansetExpr RSQUARE declaration AT process RCURLY
| CSPINTERLEAVE LCURLY declaration AT process RCURLY
  ;

processPara :
  programParagraph
| IDENTIFIER EQUALS paragraphAction
| nameset IDENTIFIER EQUALS namesetExpr
  ;

paragraphAction :
  action
| declaration AT paragraphAction
  ;

action
: VDMcommand
| IDENTIFIER { /*new CMLIdentifier($1);*/ }
| cspAction
| action LSQUARE identifierList CSPRENAME identifierList RSQUARE
  ;

cspAction :
  CSPSKIP
| CSPSTOP
| CSPCHAOS
| communication RARROW cspAction
| cspAction CSPSEQ cspAction
| cspAction CSPAND cspAction
| cspAction CSPHIDE cspAction
| cspAction DLSQUARE renameList DRSQUARE
| cspAction CSPINTCH cspAction
| cspAction CSPEXTCH cspAction
| cspAction CSPLSQUAREGT cspAction
| cspAction CSPLSQUAREBAR IDENTIFIER CSPBARGT cspAction
| predicate AMP cspAction
| predicate THREEBAR cspAction
| cspAction CSPLSQUAREBAR namesetExpr BAR chansetExpr BAR namesetExpr CSPBARRSQUARE cspAction
| cspAction CSPLSQUAREBAR namesetExpr BAR chansetExpr DBAR chansetExpr BAR namesetExpr CSPBARRSQUARE cspAction
/* | cspAction LSQUARE renameList RSQUARE cspAction /\* FIXME shift/reduce because of rule 'action' case 4 *\/ */
| CSPSEQ LCURLY declaration AT cspAction RCURLY
| CSPINTCH LCURLY declaration AT cspAction RCURLY
| CSPEXTCH LCURLY declaration AT cspAction RCURLY
| CSPLSQUAREDBAR nameset CSPDBARRSQUARE LPAREN declaration AT cspAction RPAREN
| CSPLSQUAREBAR nameset BAR chanset CSPBARRSQUARE LPAREN declaration AT cspAction RPAREN
| CSPDBAR declaration AT LSQUARE nameset BAR chanset RSQUARE cspAction
| LSQUARE renameList RSQUARE LPAREN declaration AT cspAction RPAREN
  ;

renameList :
  IDENTIFIER CSPSAMEAS IDENTIFIER
| IDENTIFIER CSPSAMEAS IDENTIFIER COMMA renameList
  ;

/* 2.3 Channel Definitions */

channelDecl :
 CHANNELS channelDef                              
 {
     List<AChannelNameDeclaration> decls = (List<AChannelNameDeclaration>)$2;
     LexLocation start = decls.get(0).getLocation();
     LexLocation end = decls.get(decls.size()-1).getLocation();
     LexLocation location = combineLexLocation(start,end);

     AChannelDefinition channelDefinition = 
	 new AChannelDefinition(location,null,null,null,decls);
     AChannelDeclaration channelDecl = new AChannelDeclaration(location,channelDefinition);
     $$ = channelDecl;
 }
;

channelDef: 
  channelNameDecl
  {
      List<AChannelNameDeclaration> decls = new Vector<AChannelNameDeclaration>();
      decls.add((AChannelNameDeclaration)$1);
      AChannelDefinition channelDefinition = new AChannelDefinition();
      $$ = decls;
  }                                 
 |channelNameDecl SEMI channelDef
 {
     List<AChannelNameDeclaration> decls = (List<AChannelNameDeclaration>)$3;
     decls.add((AChannelNameDeclaration)$1);
     $$ = decls;
 }
;

channelNameDecl: 
  identifierList
  {
      List<LexIdentifierToken> ids = (List<LexIdentifierToken>)$1;

      LexLocation start = ids.get(0).getLocation();
      LexLocation end = ids.get(ids.size()-1).getLocation();
      LexLocation location = combineLexLocation(start,end);

      ASingleTypeDeclaration singleTypeDeclaration = new ASingleTypeDeclaration(location,ids,null);
            
      AChannelNameDeclaration channelNameDecl = new AChannelNameDeclaration(location,singleTypeDeclaration);
      
      $$ = channelNameDecl;
  }
 |singleTypeDecl
 {
     ASingleTypeDeclaration singleTypeDeclaration = (ASingleTypeDeclaration)$1;

     AChannelNameDeclaration channelNameDecl = 
	 new AChannelNameDeclaration(singleTypeDeclaration.getLocation(),singleTypeDeclaration);
      
      $$ = channelNameDecl; 
 }
;

declaration : 
singleTypeDecl
{


}

| singleTypeDecl SEMI declaration
{

}
;

singleTypeDecl :
IDENTIFIER COLON type
{
    LexIdentifierToken id = extractLexIdentifierToken((CmlLexeme)$1);
    List<LexIdentifierToken> ids = new Vector<LexIdentifierToken>();
    ids.add(id);
    ASingleTypeDeclaration singleTypeDeclaration = 
	new ASingleTypeDeclaration(id.getLocation(),ids,(PType)$3);
    $$ = singleTypeDeclaration;
}
| IDENTIFIER COMMA singleTypeDecl
{
    LexIdentifierToken id = extractLexIdentifierToken((CmlLexeme)$1);
    ASingleTypeDeclaration singleTypeDeclaration = (ASingleTypeDeclaration)$3;
    
    singleTypeDeclaration.getIdentifiers().add(id);
    $$ = singleTypeDeclaration;
}
;

/* 2.4 Chanset Definitions */

chansetDecl:
  CHANSETS IDENTIFIER EQUALS chansetExpr
  {
      //LexIdentifierToken id = extractLexIdentifierToken((CmlLexeme)$1);
      //AChansetDeclaration ChansetDeclaration = new AChansetDeclaration();
  }
  ;

chansetExpr : 
  CSP_CHANSET_BEGIN identifierList CSP_CHANSET_END
;

/* 2.5 Global Definitions */

globalDecl :
  globalDefinitionBlock
  ;

globalDefinitionBlock 
: globalDefinitionBlockAlternative
{
    List<SGlobalDeclaration> declBlockList = new Vector<SGlobalDeclaration>();
    SGlobalDeclaration globalDecl = (SGlobalDeclaration)$1;
    if (globalDecl != null) declBlockList.add(globalDecl);
    $$ = declBlockList;
}

| globalDefinitionBlock globalDefinitionBlockAlternative        
 { 
    List<SGlobalDeclaration> declBlockList = (List<SGlobalDeclaration>)$1;
    SGlobalDeclaration globalDecl = (SGlobalDeclaration)$2;
    if (declBlockList != null) if (globalDecl != null) declBlockList.add(globalDecl);
    $$ = declBlockList;
}
;

globalDefinitionBlockAlternative
: typeDefs             
{
    ATypeGlobalDeclaration typeGlobalDeclaration = new ATypeGlobalDeclaration();
    typeGlobalDeclaration.setTypeDefinitions((List<ATypeDefinition>) $1);
    $$ = typeGlobalDeclaration;
}
| valueDefs
{
    AValueGlobalDeclaration valueGlobalDeclaration = new AValueGlobalDeclaration();
    $$ = valueGlobalDeclaration;
}
| functionDefs
{
    AFunctionGlobalDeclaration functionGlobalDeclaration = new AFunctionGlobalDeclaration();
    $$ = functionGlobalDeclaration;

}
;

/* 3 Definitions */

classBody 
: classDefinitionBlock                       
{ 
  $$ = (List)$1; 
}
|
{
  $$ = new Vector<PDefinition>();
}
;

classDefinitionBlock 
: classDefinitionBlockAlternative
{
    List<PDefinition> defBlockList = new Vector<PDefinition>();
    List<PDefinition> defBlock = (List<PDefinition>)$1;
    if (defBlockList != null) if (defBlock != null) defBlockList.addAll(defBlock);
    $$ = defBlockList;
}

| classDefinitionBlock classDefinitionBlockAlternative        
{ 
    List<PDefinition> defBlockList = (List<PDefinition>)$1;
    List<PDefinition> defBlock = (List<PDefinition>)$2;
    if (defBlockList != null) if (defBlock != null) defBlockList.addAll(defBlock);
    $$ = defBlockList;
}
;

classDefinitionBlockAlternative
: typeDefs             
{
  $$ = $1;
}
| valueDefs
{
  $$ = $1;
}
| functionDefs
{
  $$ = $1;
}
| operationDefs
{
  $$ = $1;
}
| stateDefs
{
  $$ = $1;
}
| initialDef
{
  $$ = $1;
}
;

/* 3.1 Type Definitions */

typeDefs 
: TYPES
{ 
    $$ = new Vector<PDefinition>(); 
}
| TYPES typeDefList SEMI                          
{
    $$ = (List<PDefinition>)$2;
}
| TYPES typeDefList                          
{
    $$ = (List<PDefinition>)$2;
}
;

typeDefList
: typeDefList SEMI typeDef                   
{
    List<PDefinition> list = (List<PDefinition>)$1;
    list.add((PDefinition)$3);
    $$ = list;
}
| typeDef                                    
{
    List<PDefinition> list = new Vector<PDefinition>(); 
    list.add((PDefinition)$1);
    $$ = list;
} 
;

typeDef 
: qualifier IDENTIFIER EQUALS type invariant
{
    AAccessSpecifierAccessSpecifier access = (AAccessSpecifierAccessSpecifier)$1;
    LexNameToken name = extractLexNameToken((CmlLexeme)$2);
    LexLocation location = null;
    if (access.getLocation() != null)
	location = combineLexLocation(access.getLocation(),((PTypeBase)$4).getLocation());
    else
    {
	location = combineLexLocation(name.getLocation(),((PTypeBase)$4).getLocation());
    }
    
    $$ = new ATypeDefinition(location,null /*NameScope nameScope_*/, false, 
			     null/*SClassDefinition classDefinition_*/,access, 
			     (PType)$4, null, null, null, 
			     null, true, name); 
    
}
| qualifier IDENTIFIER EQUALS type                        
{ 
    AAccessSpecifierAccessSpecifier access = (AAccessSpecifierAccessSpecifier)$1;
    LexNameToken name = extractLexNameToken((CmlLexeme)$2);
    LexLocation location = null;
    if (access.getLocation() != null)
	location = combineLexLocation(access.getLocation(),((PTypeBase)$4).getLocation());
    else
    {
	location = combineLexLocation(name.getLocation(),((PTypeBase)$4).getLocation());
    }
        
    $$ = new ATypeDefinition(location,null /*NameScope nameScope_*/, false, 
			     null/*SClassDefinition classDefinition_*/,access, 
			     (PType)$4, null, null, null, 
			     null, true, name); 
}
| qualifier IDENTIFIER VDMRECORDDEF fieldList invariant
  ;

qualifier
: PRIVATE 
{ 
    LexLocation location = extractLexLocation((CmlLexeme)$1);
    $$ = new AAccessSpecifierAccessSpecifier(new APrivateAccess(),null,null,location);
}
| PROTECTED 
{ 
    LexLocation location = extractLexLocation((CmlLexeme)$1);
    $$ = new AAccessSpecifierAccessSpecifier(new AProtectedAccess(),null,null,location);
}
| PUBLIC  
{ 
    LexLocation location = extractLexLocation((CmlLexeme)$1);
    $$ = new AAccessSpecifierAccessSpecifier(new APublicAccess(),null,null,location);
}
| LOGICAL 
{ 
    LexLocation location = extractLexLocation((CmlLexeme)$1);
    $$ = new AAccessSpecifierAccessSpecifier(new ALogicalAccess(),null,null,location);
}
| /* empty */
{
    /*Default public?????*/
    $$ = new AAccessSpecifierAccessSpecifier(new APublicAccess(),null,null,null);
}
;

type 
: LPAREN type RPAREN
{ 
    $$ = $2;
}
| TBOOL
{ 
    $$ = new ABooleanBasicType(extractLexLocation((CmlLexeme)$1) , false);
}
| TNAT                                                
{ 
    $$ = new ANatNumericBasicType(extractLexLocation((CmlLexeme)$1) , false);
}                                         
| TNAT1
{ 
    $$ = new ANatOneNumericBasicType(extractLexLocation((CmlLexeme)$1) , false);
}
| TINT
{ 
    $$ = new AIntNumericBasicType(extractLexLocation((CmlLexeme)$1) , false);
}
| TRAT 
{ 
    $$ = new ARationalNumericBasicType(extractLexLocation((CmlLexeme)$1) , false);
}
| TREAL
{ 
    $$ = new ARealNumericBasicType(extractLexLocation((CmlLexeme)$1) , false);
}
| TCHAR
{ 
    $$ = new ACharBasicType(extractLexLocation((CmlLexeme)$1) , false);
}
| TTOKEN
{ 
    $$ = new ATokenBasicType(extractLexLocation((CmlLexeme)$1) , false);
}
| quoteLiteral /* replace me! */
| VDMCOMPOSE IDENTIFIER OF fieldList END
| type VDMTYPEUNION type
| type STAR type
| LSQUARE type RSQUARE
| VDMSETOF type
| VDMSEQOF type
| VDMSEQ1OF type
| VDMMAPOF type TO type
| VDMINMAPOF type TO type
| type VDMPFUNCARROW type
| VDMUNITTYPE VDMPFUNCARROW type
| type VDMTFUNCARROW type
| VDMUNITTYPE VDMTFUNCARROW type
| name
| typeVarIdentifier
  ;

fieldList : 
  field
| field fieldList
  ;

field :
  type
| IDENTIFIER VDMTYPE type
| IDENTIFIER VDMTYPENCMP type
  ;

invariant :
 VDMINV pattern DEQUALS expression
 {

 }
  ;

/* 3.2 Value Definitions */

valueDefs :
  VALUES valueDefList
  ;

valueDefList :
  pattern EQUALS expression SEMI valueDefList
| pattern VDMTYPE type EQUALS expression SEMI valueDefList
| /* empty */
  ;

/* FIXME the optional trailing semicolon in the values definitions is presently not optional */

/* 3.3 Function Definitions */

functionDefs :
  FUNCTIONS functionDefList
  ;

functionDefList :
  qualifier IDENTIFIER VDMTYPE functionType IDENTIFIER parameterList DEQUALS functionBody preExpr_opt postExpr_opt measureExpr
  ;

/* really? this is what a VDM function definition list looks like? */
parameterList :
  LPAREN patternList RPAREN
| LPAREN patternList RPAREN parameterList
  ;

functionBody :
  expression
| VDM_SUBCLASSRESP
| VDM_NOTYETSPEC
  ;

parameterTypes : 
LPAREN patternList COLON type RPAREN
| LPAREN patternList COLON type RPAREN COMMA parameterTypes
; 

identifierTypePairList_opt 
: /* empty */
| identifierTypePairList
;

identifierTypePairList :
IDENTIFIER COLON type
| IDENTIFIER COLON type COMMA identifierTypePairList
;

preExpr_opt :
  preExpr
| /* empty */
  ;

preExpr :
  PRE expression


postExpr_opt :
  postExpr
| /* empty */
  ;

postExpr :
POST expression
;

measureExpr :
  MEASURE expression
| /* empty */
  ;

/* 3.4 Operation Definitions */

operationDefs :
  OPERATIONS operationDefList
  ;
  
operationDefList :
  operationDef SEMI operationDefList
| /* empty */
  ;

/* FIXME the optional trailing semicolon in the operations definitions is presently not optional */

operationDef 
: implicitOperationDef
| explicitOperationDef
;

 explicitOperationDef
: qualifier IDENTIFIER VDMTYPE operationType IDENTIFIER parameterList DEQUALS operationBody externals preExpr_opt postExpr_opt
;

implicitOperationDef
: qualifier IDENTIFIER parameterTypes identifierTypePairList_opt externals_opt preExpr_opt postExpr
;

operationType :
  type OPERATIONARROW type
| VDMUNITTYPE OPERATIONARROW type
| type OPERATIONARROW VDMUNITTYPE
| VDMUNITTYPE OPERATIONARROW VDMUNITTYPE
  ;

operationBody :
  statement
| VDM_SUBCLASSRESP
| VDM_NOTYETSPEC
  ;

externals_opt:
externals
| /* empty */
;

externals :
  VDM_EXT varInformationList
  ;

/* FIXME this needs to be non-empty */
varInformationList :
  mode nameList varInformationList
| mode nameList VDMTYPE type varInformationList
| /* empty */
  ;

mode :
  VDM_RD
| VDM_WR
  ;

initialDef : 
INITIAL operationDef

/* 3.5 Instance Variable Definitions */

stateDefs :
  STATE stateDefList
  ;

/* FIXME this needs to be non-empty */
stateDefList :
  assignmentDef stateDefList
| invariantDef stateDefList
| /* empty */
  ;

invariantDef :
 VDMINV expression
  ;



/* 4 Expressions */

expressionList :
  expression
| expression COMMA expressionList
  ;

expression :
  LPAREN expression RPAREN
  {
      LexLocation loc = extractLexLocation((CmlLexeme)$1,(CmlLexeme)$3);
      $$ = new ABracketedExp(loc,(PExp)$2);
  }
| LET localDefList IN expression
| ifExpr
| casesExpr
| unaryExpr
| binaryExpr
{
    $$ = $1;
}
| quantifiedExpr
| setEnumeration
| setComprehension
| setRangeExpr
| sequenceEnumeration
| sequenceComprehension
| subsequence
| mapEnumeration
| mapComprehension
| tupleConstructor
| recordConstructor
| apply
| fieldSelect
| tupleSelect
| lambdaExpr
| newExpr
| SELF
| generalIsExpr
| preconditionExpr
| ISOFCLASS LPAREN name COMMA expression RPAREN
| name
{
    LexNameToken lnt = (LexNameToken)$1;
    $$ = new ANameExp(lnt.location,lnt);
}
| oldName
| symbolicLiteral
  ;

symbolicLiteral:
numericLiteral
{
    LexIntegerToken lit = (LexIntegerToken)$1;
    $$ = new AIntLiteralSymbolicLiteralExp(lit.location,lit);
}
/*| booleanLiteral
| nilLiteral
| characterLiteral
| textLiteral
| quoteLiteral*/
;

numericLiteral:
 NUMERAL 
 {
    CmlLexeme lexeme = (CmlLexeme)$1;
    LexLocation loc = extractLexLocation(lexeme);
    $$ = new LexIntegerToken(Long.decode(lexeme.getValue()),loc);   
 }
| HEX_LITERAL
{
    CmlLexeme lexeme = (CmlLexeme)$1;
    LexLocation loc = extractLexLocation(lexeme);
    $$ = new LexIntegerToken(Long.decode(lexeme.getValue()),loc);   
}
;

localDefList :
  localDef
| localDef COMMA localDefList
  ;

/* 4.3 Conditional Expressions */

ifExpr :
  IF expression THEN expression elseExprs
  ;

elseExprs :
  ELSE expression
| ELSEIF expression THEN expression elseExprs
  ;

casesExpr :
  CASES expression COLON casesExprAltList END
  ;

casesExprAltList :
  casesExprAlt
| casesExprAlt OTHERS RARROW expression
| casesExprAlt casesExprAltList
  ;

casesExprAlt :
  patternList RARROW expression
  ;

/* 4.4 Unary Expressions */

/* FIXME this hack makes me really tempted to see if I can use the lexer to define PLUS, MINUS, etc and just call the terminals UNARYOP and BINARYOP */
/* turns out that terminals embedded in a rule that only lists terminals ends up dropping precedence info */

/* unaryExpr : */
/*     unaryOperator expression */
/*   | INVERSE expression */
/*     ; */

unaryExpr :
  PLUS expression %prec UPLUS
| MINUS expression %prec UMINUS
| ABS expression
| FLOOR expression
| NOT expression
| CARD expression
| POWER expression
| DUNION expression
| DINTER expression
| HD expression
| TL expression
| LEN expression
| ELEMS expression
| INDS expression
| REVERSE expression
| DCONC expression
| DOM expression
| RNG expression
| MERGE expression
| INVERSE expression
  ;

/* 4.5 Binary Expressions */

/* binaryExpr : */
/*     expression binaryOperator expression */
/*     ; */

binaryExpr :
  expression PLUS expression
| expression MINUS expression
| expression DIVIDE expression
| expression DIV expression
| expression REM expression
| expression MOD expression
| expression LT expression
| expression LTE expression 
{
    LexLocation loc = combineLexLocation(((PExp)$1).getLocation(),((PExp)$3).getLocation());
    $$ = new ALessEqualNumericBinaryExp(loc,(PExp)$1,null,(PExp)$3);
}
| expression GT expression
| expression GTE expression
| expression EQUALS expression
| expression NEQ expression
| expression OR expression
| expression AND expression
| expression IMPLY expression
| expression BIMPLY expression
| expression INSET expression
| expression NOTINSET expression
| expression SUBSET expression
| expression PROPER_SUBSET expression
| expression UNION expression
| expression SETDIFF expression
| expression INTER expression
| expression CONC expression
| expression OVERWRITE expression
| expression MAPMERGE expression
| expression DOMRES expression
| expression VDM_MAP_DOMAIN_RESTRICT_BY expression
| expression RNGRES expression
| expression RNGSUB expression
| expression COMP expression
| expression ITERATE expression
  ;

/* 4.6 Quantified Expressions */

quantifiedExpr :
  FORALL bindList AMP expression
| EXISTS bindList AMP expression
| EXISTS1 bind AMP expression
  ;

/* 4.7 Set Expressions */

setEnumeration :
  LCURLY RCURLY
| LCURLY expressionList RCURLY
  ;

setComprehension :
  LCURLY expression BAR bindList RCURLY
| LCURLY expression BAR bindList AMP expression RCURLY
  ;

setRangeExpr : 
  LCURLY expression COMMA ELLIPSIS COMMA expression RCURLY
  ;

/* 4.8 Sequence Expression */

sequenceEnumeration :
  LSQUARE RSQUARE
| LSQUARE expressionList RSQUARE
  ;

sequenceComprehension :
  LSQUARE expression BAR setBind RSQUARE
| LSQUARE expression BAR setBind AMP expression RSQUARE
  ;

subsequence :
  expression LPAREN expression COMMA ELLIPSIS COMMA expression RPAREN
  ;

mapEnumeration :
  LCURLY MAPLETARROW RCURLY
| LCURLY mapletList RCURLY
  ;

mapletList :
  maplet
| maplet COMMA mapletList
  ;

maplet :
  expression MAPLETARROW expression
  ;

mapComprehension :
  LCURLY maplet BAR bindList RCURLY
| LCURLY maplet BAR bindList AMP expression RCURLY
  ;

/* 4.10 Tuple Constructor Expression */

tupleConstructor :
  MKUNDER LPAREN expression COMMA expression RPAREN
  ;

/* 4.11 Record Expressions */

recordConstructor :
  MKUNDER name LPAREN expressionList RPAREN
  ;

/* 4.12 Apply Expressions */

apply :
  expression LPAREN expressionList RPAREN
  ;

fieldSelect :
  expression DOT IDENTIFIER
  ;

tupleSelect :
  expression DOTHASH NUMERAL
  ;

/* 4.13 The Lambda Expression */

lambdaExpr :
  LAMBDA typeBindList AMP expression
  ;

/* 4.14 The New Expression */

/* FIXME there is a reduce/reduce with <apply> if you take out the PARENs */

newExpr :
  NEW LPAREN expression LPAREN expressionList RPAREN RPAREN
  ;

/* 4.16 The Is Expression */

generalIsExpr :
  ISUNDER name LPAREN expression RPAREN
| ISUNDER basicType LPAREN expression RPAREN
| ISUNDER LPAREN expression COMMA type RPAREN
  ;

basicType :
  TBOOL
| TNAT
| TNAT1
| TINT
| TRAT
| TREAL
| TCHAR
| TTOKEN
  ;

/* 4.17 The Precondition Expression */

preconditionExpr :
  PREUNDER LPAREN expressionList RPAREN
  ;

/* 4.19 Names */

name :
  IDENTIFIER
  {
      LexNameToken name = extractLexNameToken((CmlLexeme)$1);
      $$ = name;
  }
| IDENTIFIER BACKTICK IDENTIFIER
  ;

nameList :
  name
| name COMMA nameList
  ;

oldName :
  IDENTIFIER TILDE
  ;


/* 5 State Designators */

stateDesignator :
  name
| stateDesignator DOT IDENTIFIER
| stateDesignator LPAREN expression RPAREN
  ;

/* 6 Statements */

statement :
  SKIP
| LET localDefList IN statement
| blockStatement
| generalAssignStatement
| ifStatement
| casesStatement
| callStatement
| RETURN expression
| specificationStatement
  ;

statementList :
  statement
| statement SEMI statementList
  ;

/* 6.2 Block and Assignment Statements
 * to be clarified
 */

/* FIXME trailing semicolon not optional */
blockStatement :
  LPAREN statementList RPAREN
| LPAREN dclStatement statementList RPAREN
  ;

dclStatement :
  DCL assignmentDefList
  ;

assignmentDefList :
  assignmentDef
| assignmentDef COMMA assignmentDefList
  ;

assignmentDef :
  IDENTIFIER VDMTYPE type
| IDENTIFIER VDMTYPE type ASSIGN expression
  ;

generalAssignStatement :
  assignStatement
| multiAssignStatement
  ;

assignStatement :
  stateDesignator ASSIGN expression
  ;

assignStatementList :
  assignStatement
| assignStatement SEMI assignStatementList
  ;

multiAssignStatement :
  ATOMIC LPAREN assignStatement SEMI assignStatementList RPAREN
  ;

/* 6.3 Conditional Statements */

ifStatement :
  IF expression THEN statement elseStatements
  ;

elseStatements :
  ELSE statement
| ELSEIF expression THEN statement elseStatements
  ;

casesStatement :
  CASES expression COLON casesStatementAltList END
  ;

casesStatementAltList :
  casesStatementAlt
| casesStatementAlt OTHERS RARROW statement
| casesStatementAlt casesExprAltList
  ;

casesStatementAlt :
  patternList RARROW statement
  ;


/* 6.4 Call and Return Statements */

/* FIXME the CURLYs are there there to avoid several whatever/reduce conflicts with the assignment statement */

callStatement :
  LCURLY name RCURLY LPAREN expressionList RPAREN /* FIXME */
| LCURLY objectDesignator DOT name RCURLY LPAREN expressionList RPAREN /* FIXME */
  ;

objectDesignator :
  SELF
| name
| newExpr
| objectDesignator DOT IDENTIFIER
| objectDesignator LPAREN expressionList RPAREN
  ;

/* return inline above */


/* 6.5 The Specification Statement */

specificationStatement :
  LSQUARE implicitOperationBody RSQUARE
  ;


/* 7 Patterns and Bindings */

/* 7.1  */

pattern :
  patternIdentifier
| matchValue
| tuplePattern
| recordPattern
  ;

patternList :
  pattern
| pattern COMMA patternList
  ;

patternIdentifier :
  IDENTIFIER
  {
      CmlLexeme lexeme = (CmlLexeme)$1;
      LexNameToken lnt = extractLexNameToken(lexeme);
      $$ = new AIdentifierPattern(lnt.location,null,false,lnt);
  }
| VDMDONTCARE
  ;

/* FIXME shift/reduce conflict from a bracketed expression */
matchValue :
  symbolicLiteral
/* | LPAREN expression RPAREN */
  ;

/* FIXME not sure if if this is a minimum of one pattern or two; if the latter */
tuplePattern :
  MKUNDER LPAREN patternList RPAREN
  /* MKUNDER LPAREN pattern COMMA patternList RPAREN */
  ;

recordPattern :
  MKUNDER name LPAREN RPAREN
| MKUNDER name LPAREN patternList RPAREN
  ;


/* 7.2 Bindings */

/* FIXME where is patternBind used? */
/* patternBind : */
/*   pattern  */
/* | bind */
/*   ; */

bind :
  setBind
| typeBind
  ;

setBind :
  pattern INSET expression
  ;

typeBind :
  pattern VDMTYPE type
  ;

bindList :
  multipleBind
  | multipleBind COMMA multipleBind
  ;

multipleBind :
  multipleSetBind
| multipleTypeBind 
  ; 

multipleSetBind :
  patternList INSET expression 
  ; 

multipleTypeBind :
  patternList VDMTYPE type 
  ; 

typeBindList :
  typeBind
| typeBind COMMA typeBindList
  ;

/* Things not in the CML(-1) spec */

identifierList :
  IDENTIFIER
  {
      CmlLexeme cmlLex = (CmlLexeme) $1;
      LexLocation location = extractLexLocation(cmlLex);
      LexIdentifierToken lexIdToken = new LexIdentifierToken(cmlLex.getValue(),false,location);
      List<LexIdentifierToken> ids = new Vector<LexIdentifierToken>();
      ids.add(lexIdToken);
      $$ = ids;
  }

| IDENTIFIER COMMA identifierList
{

    List<LexIdentifierToken> ids = (List<LexIdentifierToken>)$3;

    if (ids == null)
	ids = new Vector<LexIdentifierToken>();

    CmlLexeme cmlLex = (CmlLexeme) $1;
    LexLocation location = extractLexLocation(cmlLex);
    LexIdentifierToken lexIdToken = 
	new LexIdentifierToken(cmlLex.getValue(),false,location);
    ids.add(lexIdToken);
    $$ = ids;
}
  ;

// **********************
// *** END OF GRAMMAR ***
// **********************
