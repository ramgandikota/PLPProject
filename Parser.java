package cop5556fa17;




import cop5556fa17.Scanner.Kind;
import cop5556fa17.Scanner.Token;
import cop5556fa17.Parser.SyntaxException;

import static cop5556fa17.Scanner.Kind.*;

import java.util.ArrayList;

import cop5556fa17.AST.*;


@SuppressWarnings("unused")
public class Parser {

	@SuppressWarnings("serial")
	public class SyntaxException extends Exception {
		Token t;

		public SyntaxException(Token t, String message) {
			super(message);
			this.t = t;
		}

	}

	Scanner scanner;
	Token t;

	Parser(Scanner scanner) {
		this.scanner = scanner;
		t = scanner.nextToken();
	}

	/**
	 * Main method called by compiler to parser input.
	 * Checks for EOF
	 * 
	 * @throws SyntaxException
	 */
	public Program parse() throws SyntaxException {
		
	    Program p = program();
		matchEOF();
		return p;
	}
	

	/**
	 * Program ::=  IDENTIFIER   ( Declaration SEMI | Statement SEMI )*   
	 * 
	 * Program is start symbol of our grammar.
	 * @return 
	 * 
	 * @throws SyntaxException
	 */
	Program program() throws SyntaxException {
		Token firstToken  = t;
		ArrayList<ASTNode> arrList =  new ArrayList<>();
		matchAndConsume(IDENTIFIER);
		while( isVariableDecl() || isSourceSinkDecl() || t.kind == KW_image || t.kind == IDENTIFIER){
			if(isVariableDecl()  ||isSourceSinkDecl() || t.kind == KW_image ){
				arrList.add(decl());
			}else if(t.kind == Kind.IDENTIFIER){
				arrList.add(stmt());
			}
			matchAndConsume(SEMI);	
		}
		return new Program(firstToken, firstToken, arrList);
	}

	/*Declaration :: =  VariableDeclaration     |    ImageDeclaration   |   SourceSinkDeclaration  

	 * */
	Declaration decl() throws SyntaxException{
		
		if(isVariableDecl()){
			return variableDecl();
		}else if(isSourceSinkDecl()){
			return sourceSinkDecl();
		}else if( t.kind == KW_image){
			return imageDecl();
		}else{
			String message =  "Decl | Expected token "+t.kind+" at " + t.line + ":" + t.pos_in_line;
			throw new SyntaxException(t, message);
		}
		
	}
	/* Bool function for variableDecl condition check */
	
	boolean isVariableDecl()
	{
		if(t.kind == KW_int || t.kind == KW_boolean){
			
			return true;
		
		}
		return false;	
	}
	
	/* VariableDeclaration  ::=  VarType IDENTIFIER  (  OP_ASSIGN  Expression  | ε )*/
	
	Declaration_Variable variableDecl() throws SyntaxException{
		Token firstToken = t;
		varType();
		Token name = t;
		matchAndConsume(IDENTIFIER);
		if(t.kind == OP_ASSIGN){
			matchAndConsume(OP_ASSIGN);
			return new Declaration_Variable(firstToken, firstToken, name, expression());
		}
		return new Declaration_Variable(firstToken, firstToken, name, null);
	}
	
	/* VarType ::= KW_int | KW_boolean */
	void varType() throws SyntaxException{
		
		if(t.kind == KW_int){
			matchAndConsume(KW_int);
		}else if(t.kind == KW_boolean){
			matchAndConsume(KW_boolean);
		}else{
			String message =  "VarType | Expected token "+t.kind+" at " + t.line + ":" + t.pos_in_line;
			throw new SyntaxException(t, message);
		}
		
	}
	/* Bool function for checking sourceSinkDecl conditions*/
	
	boolean isSourceSinkDecl(){
		if(t.kind == KW_url || t.kind == KW_file){
			
			return true;
		}
		return false;
	}
	
	/* SourceSinkDeclaration ::= SourceSinkType IDENTIFIER  OP_ASSIGN  Source */
	
	Declaration_SourceSink sourceSinkDecl() throws SyntaxException{
		Token firstToken = t;
		sourceSinkType();
		Token name = t;
		matchAndConsume(IDENTIFIER);
		matchAndConsume(OP_ASSIGN);
		return new Declaration_SourceSink(firstToken,firstToken,name,source());
		
	}
	/* Source ::= STRING_LITERAL  Source ::= OP_AT Expression  Source ::= IDENTIFIER  */
	
	Source source() throws SyntaxException{
		Token firstToken = t;
		if(t.kind == STRING_LITERAL){
			matchAndConsume(t.kind);
			return new Source_StringLiteral(firstToken, firstToken.getText());
			
		}else if(t.kind == IDENTIFIER){
			matchAndConsume(t.kind);
			return new Source_Ident(firstToken, firstToken);
		}else if(t.kind == OP_AT){
			matchAndConsume(OP_AT);
			return new Source_CommandLineParam(firstToken, expression());
	
		}else{
			String message =  "Source | Expected token "+t.kind+" at " + t.line + ":" + t.pos_in_line;
			throw new SyntaxException(t, message);
		}
	}
	/* SourceSinkType := KW_url | KW_file */
	
	void sourceSinkType() throws SyntaxException{
		
		if(t.kind == KW_url){
			matchAndConsume(KW_url);
		}else if(t.kind == KW_file){
			matchAndConsume(KW_file);
		}else{
			String message =  "SS | Expected token "+t.kind+" at " + t.line + ":" + t.pos_in_line;
			throw new SyntaxException(t, message);
		}
	}
	/*ImageDeclaration ::=  KW_image  (LSQUARE Expression COMMA Expression RSQUARE | ε)
	  IDENTIFIER ( OP_LARROW Source | ε )     */
	
	Declaration_Image imageDecl() throws SyntaxException{
		Token firstToken = t;
		Expression xSize = null;
		Expression ySize = null;
		Source source = null;
		matchAndConsume(KW_image);
		if(t.kind == LSQUARE){
			matchAndConsume(LSQUARE);
			xSize = expression();
			matchAndConsume(COMMA);
			ySize = expression();
			matchAndConsume(RSQUARE);
		}
		Token name = t;
		matchAndConsume(IDENTIFIER);
		if(t.kind == OP_LARROW){
			matchAndConsume(OP_LARROW);
			source = source();
			
		}
		return new Declaration_Image(firstToken, xSize, ySize, name, source);
		
	}
	/* Sink ::= IDENTIFIER | KW_SCREEN  //ident must be file */
	Sink sink() throws SyntaxException{
		Token firstToken = t;
		Sink s = null;
		if(t.kind == IDENTIFIER){
			matchAndConsume(IDENTIFIER);
			s = new Sink_Ident(firstToken, firstToken);
		}else if(t.kind == KW_SCREEN){
		matchAndConsume(KW_SCREEN);
		 s= new Sink_SCREEN(firstToken);
		}
		return s;
	}
	
	/* Statement  ::= AssignmentStatement  | ImageOutStatement   | ImageInStatement   */
	
	Statement stmt() throws SyntaxException{
		Token firstToken = t;
		matchAndConsume(IDENTIFIER);
		switch(t.kind){
		case OP_RARROW:
			matchAndConsume(OP_RARROW);
			return new Statement_Out(firstToken,firstToken,sink());
		case OP_LARROW:
			matchAndConsume(OP_LARROW);
			return new Statement_In(firstToken, firstToken,source());
		case LSQUARE:
			matchAndConsume(LSQUARE);
			Index index = lhsSelector();
			matchAndConsume(RSQUARE);
			matchAndConsume(OP_ASSIGN);
			Expression e0 = expression();
			return new Statement_Assign(firstToken, new LHS(firstToken,firstToken,index), e0);
		case OP_ASSIGN:
			matchAndConsume(OP_ASSIGN);
			return new Statement_Assign(firstToken, new LHS(firstToken,firstToken,null), expression());
			
		default:
			String message =  "STMT | Expected token "+t.kind+" at " + t.line + ":" + t.pos_in_line;
			throw new SyntaxException(t, message);
		}
		
	}
	
//	void imageOutStmt() throws SyntaxException{
//		
//		matchAndConsume(IDENTIFIER);
//		matchAndConsume(OP_RARROW);
//		sink();
//		
//	}
//	
//	void imageInStmt() throws SyntaxException{
//		
//		matchAndConsume(IDENTIFIER);
//		matchAndConsume(OP_LARROW);
//		source();
//	}
//	
//	void assignmentStmt() throws SyntaxException{
//		
//		matchAndConsume(IDENTIFIER);
//		if(t.kind == LSQUARE){
//			matchAndConsume(LSQUARE);
//			lhsSelector();
//			matchAndConsume(RSQUARE);
//			matchAndConsume(OP_ASSIGN);
//			expression();
//		}
//	}

	/**
	 * Expression ::=  OrExpression  OP_Q  Expression OP_COLON Expression    | OrExpression
	 * 
	 * Our test cases may invoke this routine directly to support incremental development.
	 * 
	 * @throws SyntaxException
	 */
	Expression expression() throws SyntaxException {
		//TODO implement this.
		Token firstToken = t;
		
		Expression e0 = orExpression();
		if(t.kind == OP_Q){
			matchAndConsume(OP_Q);
			Expression trueExpression = expression();
			matchAndConsume(OP_COLON);
			Expression falseExpression = expression();
			return new Expression_Conditional(firstToken, e0, trueExpression, falseExpression);
		}
		return e0;
		
	}
    
	Expression orExpression() throws SyntaxException{
		
		Token firstToken = t;
		Expression e0 = andExpression();
		
		while(t.kind == OP_OR){
			Token op = t;
			matchAndConsume(t.kind);
			e0 = new Expression_Binary(firstToken,e0,op,andExpression());
		}
		return e0;
	}
	Expression andExpression() throws SyntaxException{
		Token firstToken = t;
		Expression e0 = eqExpression();
		
		while(t.kind == OP_AND){
			Token op = t;
			matchAndConsume(t.kind);
			e0 = new Expression_Binary(firstToken,e0,op,eqExpression());
		}
		return e0;
	}
	Expression eqExpression() throws SyntaxException{
		
		Token firstToken = t;
		Expression e0 = relExpression();
		
		while(t.kind == OP_EQ || t.kind ==OP_NEQ){
			Token op = t;
			matchAndConsume(t.kind);
			e0 = new Expression_Binary(firstToken,e0,op,relExpression());
		}
		return e0;
		
		
	}
	Expression relExpression() throws SyntaxException{
		Token firstToken = t;
		Expression e0 = addExpression();
		
		while(t.kind == OP_LT || t.kind == OP_GT || t.kind == OP_LE || t.kind == OP_GE){
			Token op = t;
			matchAndConsume(t.kind);
			e0 = new Expression_Binary(firstToken,e0,op,addExpression());
		}
		return e0;
	}
	Expression addExpression() throws SyntaxException{
		Token firstToken = t;
		Expression e0 = multExpression();
		while(t.kind == OP_PLUS || t.kind == OP_MINUS){
			Token op = t;
			matchAndConsume(t.kind);
			e0 = new Expression_Binary(firstToken,e0,op,multExpression());
		}
		return e0;
	}
	Expression multExpression() throws SyntaxException{
		Token firstToken = t;
		Expression e0 = unaryExpression();
		while( t.kind == OP_TIMES || t.kind == OP_DIV || t.kind == OP_MOD){
			Token op = t;
			matchAndConsume(t.kind);
			e0 = new Expression_Binary(firstToken,e0,op,unaryExpression());
			
		}
		return e0;
	}
	
	Expression unaryExpression() throws SyntaxException{
		
		Token firstToken = t;
		if(t.kind == OP_PLUS || t.kind == OP_MINUS){
			matchAndConsume(t.kind);
			
			return new Expression_Unary(firstToken, firstToken,unaryExpression());
		}
		else if(isUnaryExpressionNotPlusMinus()){
			return unaryExpressionNotPlusMinus();
		}else{
			String message =  "UE | Expected token "+t.kind+" at " + t.line + ":" + t.pos_in_line;
			throw new SyntaxException(t, message);
		}
		
	}
	boolean isUnaryExpressionNotPlusMinus(){
		
		if(t.kind == OP_EXCL || t.kind == Kind.IDENTIFIER || t.kind == KW_x || t.kind == KW_y || t.kind == KW_r
				|| t.kind == KW_a|| t.kind == KW_X || t.kind == KW_Y
				|| t.kind == KW_Z || t.kind == KW_A
				||t.kind == KW_R || t.kind == KW_DEF_X || t.kind == KW_DEF_Y || isPrimary()){
			
			return true;
		}
		return false;
		
	}
	Expression unaryExpressionNotPlusMinus() throws SyntaxException{
		Token firstToken = t;
		if(t.kind == OP_EXCL){
			matchAndConsume(OP_EXCL);
			return new Expression_Unary(firstToken, firstToken, unaryExpression());
		}else if(isPrimary()){
			return primary();
		}else if(t.kind == Kind.IDENTIFIER){
			return identOrPixelSelector();
		}else if(t.kind == KW_x || t.kind == KW_y || t.kind == KW_r
				|| t.kind == KW_a|| t.kind == KW_X || t.kind == KW_Y
				|| t.kind == KW_Z || t.kind == KW_A
				||t.kind == KW_R || t.kind == KW_DEF_X || t.kind == KW_DEF_Y){
			matchAndConsume(t.kind);
		return new Expression_PredefinedName(firstToken, firstToken.kind);	
		}else{
			String message =  "UEN | Expected token "+t.kind+" at " + t.line + ":" + t.pos_in_line;
			throw new SyntaxException(t, message);
		}
		
	
	}
	
	boolean isPrimary() {
		if( t.kind == INTEGER_LITERAL || t.kind == Kind.BOOLEAN_LITERAL || t.kind == LPAREN
				||t.kind == KW_sin || t.kind == KW_cos || t.kind == KW_atan
				|| t.kind == KW_abs|| t.kind == KW_cart_x || t.kind == KW_cart_y
				|| t.kind == KW_polar_a || t.kind == KW_polar_r){
			
			return true;
		}
		return false;
		
		
	}
	Expression primary() throws SyntaxException{
		
		Token firstToken = t;
		if( t.kind == INTEGER_LITERAL){
			
			matchAndConsume(t.kind);
			return new Expression_IntLit(firstToken, firstToken.intVal());
			
		}else if(t.kind == Kind.BOOLEAN_LITERAL){
			matchAndConsume(t.kind);
			return new Expression_BooleanLit(firstToken, firstToken.getText().equals("true"));
		}
		else if(t.kind == LPAREN){
			matchAndConsume(LPAREN);
			Expression e0 = expression();
			matchAndConsume(RPAREN);	
			return e0;
		}else if(t.kind == KW_sin || t.kind == KW_cos || t.kind == KW_atan
				|| t.kind == KW_abs|| t.kind == KW_cart_x || t.kind == KW_cart_y
				|| t.kind == KW_polar_a || t.kind == KW_polar_r){
			
			return functionApplication();
		}else{
			String message =  "Primary | Expected token "+t.kind+" at " + t.line + ":" + t.pos_in_line;
			throw new SyntaxException(t, message);
		}
		
		
	}
	Expression identOrPixelSelector() throws SyntaxException{
		Token firstToken = t;
		matchAndConsume(IDENTIFIER);
		if(t.kind == LSQUARE){
			matchAndConsume(LSQUARE);
			Index index = selector();
			matchAndConsume(RSQUARE);
			return new Expression_PixelSelector(firstToken,firstToken, index);
		}
			
		return new Expression_Ident(firstToken, firstToken);
	}

	Expression_FunctionApp functionApplication() throws SyntaxException{
		Token firstToken = t;
		functionName();
		if(t.kind == LPAREN){
			matchAndConsume(LPAREN);
			Expression_FunctionApp e0 = new Expression_FunctionAppWithExprArg(firstToken, firstToken.kind, expression());
			matchAndConsume(RPAREN);
			return e0;
		}else if(t.kind == LSQUARE){
			matchAndConsume(LSQUARE);
			Expression_FunctionApp e1 = new Expression_FunctionAppWithIndexArg(firstToken, firstToken.kind, selector());
			matchAndConsume(RSQUARE);
			return e1;
		}else{
			String message =  "FAPP | Expected token "+t.kind+" at " + t.line + ":" + t.pos_in_line;
			throw new SyntaxException(t, message);
		}
		

	}
	void functionName() throws SyntaxException{
		
		switch(t.kind){
		
		case KW_sin: case KW_cos: case KW_atan: case KW_abs: case KW_cart_x: case KW_cart_y: 
		case KW_polar_a:case KW_polar_r:
			matchAndConsume(t.kind);
			break;
		default:
			String message =  "FNAME | Expected token "+t.kind+" at " + t.line + ":" + t.pos_in_line;
			throw new SyntaxException(t, message);
			
		}
	}
	Index lhsSelector() throws SyntaxException{
		Index index = null;
		matchAndConsume(LSQUARE);
		if(t.kind == KW_x){
			index =  xySelector();
		}else if(t.kind == KW_r){
			index = raSelector();
		}else{
			String message =  "LHSS | Expected token "+t.kind+" at " + t.line + ":" + t.pos_in_line;
			throw new SyntaxException(t, message);
		}
		matchAndConsume(RSQUARE);
		return index;
	}
	Index xySelector() throws SyntaxException{
		Token firstToken = t;
		Expression e0 = new Expression_PredefinedName(t, t.kind);
		matchAndConsume(KW_x);
		matchAndConsume(COMMA);
		Expression e1 = new Expression_PredefinedName(t, t.kind);
		matchAndConsume(KW_y);
		return new Index(firstToken,e0 ,e1);
	}
	
	Index raSelector() throws SyntaxException{
		Token firstToken = t;
		Expression e0 = new Expression_PredefinedName(t, t.kind);
		matchAndConsume(KW_r);
		matchAndConsume(COMMA);
		Expression e1 = new Expression_PredefinedName(t, t.kind);
		matchAndConsume(KW_a);
		return new Index(firstToken, e0,e1);
	}
	
    Index selector() throws SyntaxException{
    	Token firstToken = t;
    	Expression e0 = expression();
    	matchAndConsume(COMMA);
    	Expression e1 = expression();
		return new Index(firstToken, e0, e1) ;
    }

	/**
	 * Only for check at end of program. Does not "consume" EOF so no attempt to get
	 * nonexistent next Token.
	 * 
	 * @return
	 * @throws SyntaxException
	 */
	private Token matchEOF() throws SyntaxException {
		if (t.kind == EOF) {
			return t;
		}
		String message =  "Expected EOL at " + t.line + ":" + t.pos_in_line;
		throw new SyntaxException(t, message);
	}
	

	private void matchAndConsume(Kind kind) throws SyntaxException{
		System.out.println("Matching - "+t.kind+" == "+kind);
		
		if(t.kind == kind){
			t = scanner.nextToken();
			
		}else{
			
			String message =  "MatchConsume | Expected token "+t.kind+" at " + t.line + ":" + t.pos_in_line;
			throw new SyntaxException(t, message);
		}
		
	}
}
