package cop5556fa17;

import cop5556fa17.Scanner.Kind;
import cop5556fa17.Scanner.Token;
import cop5556fa17.TypeUtils.*;
import cop5556fa17.AST.*;

import java.util.*;


public class TypeCheckVisitor implements ASTVisitor {
	
	HashMap<String, Declaration> symbolTable = new HashMap<>();
	
		@SuppressWarnings("serial")
		public static class SemanticException extends Exception {
			Token t;

			public SemanticException(Token t, String message) {
				super("line " + t.line + " pos " + t.pos_in_line + ": "+  message);
				this.t = t;
			}

		}		

	
	/**
	 * The program name is only used for naming the class.  It does not rule out
	 * variables with the same name.  It is returned for convenience.
	 * 
	 * @throws Exception 
	 */
	@Override
	public Object visitProgram(Program program, Object arg) throws Exception {
		for (ASTNode node: program.decsAndStatements) {
			node.visit(this, arg);
		}
		return program.name;
	}

	@Override
	public Object visitDeclaration_Variable(
			Declaration_Variable declaration_Variable, Object arg)
			throws Exception {
		// TODO Auto-generated method stub
		Token token = declaration_Variable.firstToken;
		
		if( declaration_Variable.e != null){
			declaration_Variable.e.typeVar = (Type) declaration_Variable.e.visit(this, null);
		}
		
		if(lookUpType(declaration_Variable.name) == null){
			symbolTable.put(declaration_Variable.name, declaration_Variable);
			declaration_Variable.typeVar =  TypeUtils.getType(declaration_Variable.firstToken);
			if(declaration_Variable.e !=null){
				//System.out.println("Dec type: "+declaration_Variable.typeVar+" Dec E Type:"+declaration_Variable.e.typeVar);;

				if(declaration_Variable.typeVar == declaration_Variable.e.typeVar){
					return declaration_Variable.typeVar;
				}else{
					throw new SemanticException(token, "Dec Var Type != Ex Type");
				}
			}else{
				return declaration_Variable.typeVar;
			}
		}else{
			throw new SemanticException(token, "Dec Var");
		}
	}

	@Override
	public Object visitExpression_Binary(Expression_Binary expression_Binary,
			Object arg) throws Exception {
		// TODO Auto-generated method stub
		
		Token token = expression_Binary.firstToken;
		
		if( expression_Binary.e0 !=null){
			expression_Binary.e0.typeVar = (Type) expression_Binary.e0.visit(this, null);
			
		}
		if(expression_Binary.e1 != null){
			expression_Binary.e1.typeVar= (Type) expression_Binary.e1.visit(this, null);
		}
	    	
	    	if(expression_Binary.op == Kind.OP_EQ || expression_Binary.op == Kind.OP_NEQ){
	    		expression_Binary.typeVar = Type.BOOLEAN;
	    	}else if((expression_Binary.op == Kind.OP_GE || expression_Binary.op == Kind.OP_GT || expression_Binary.op == Kind.OP_LE ||expression_Binary.op == Kind.OP_LT ) && expression_Binary.e0.typeVar == Type.INTEGER){
	    		expression_Binary.typeVar = Type.BOOLEAN;
	    	}else if((expression_Binary.op == Kind.OP_AND || expression_Binary.op == Kind.OP_OR) && (expression_Binary.e0.typeVar == Type.INTEGER || expression_Binary.e0.typeVar == Type.BOOLEAN)){
	    		expression_Binary.typeVar = expression_Binary.e0.typeVar;
	    	}else if((expression_Binary.op == Kind.OP_DIV || expression_Binary.op == Kind.OP_MINUS || expression_Binary.op == Kind.OP_MOD || expression_Binary.op == Kind.OP_PLUS || expression_Binary.op == Kind.OP_POWER || expression_Binary.op == Kind.OP_TIMES) && expression_Binary.e0.typeVar == Type.INTEGER){
	    		expression_Binary.typeVar = Type.INTEGER;
	    	}else{
	    		expression_Binary.typeVar = null;
	    	}
	    if( (expression_Binary.e0.typeVar == expression_Binary.e1.typeVar) && expression_Binary.typeVar!= null){
	    		return expression_Binary.typeVar; 
	    		}else{
	    	throw new SemanticException(token, "Exp Binary");
	    }
	}

	@Override
	public Object visitExpression_Unary(Expression_Unary expression_Unary,
			Object arg) throws Exception {
		// TODO Auto-generated method stub
		Token token = expression_Unary.firstToken;
		if(expression_Unary.e != null) {
			expression_Unary.e.typeVar = (Type) expression_Unary.e.visit(this, null);
		}
		
	
		if( expression_Unary.op == Kind.OP_EXCL && ( expression_Unary.e.typeVar == Type.BOOLEAN || expression_Unary.e.typeVar== Type.INTEGER)){
			expression_Unary.typeVar = expression_Unary.e.typeVar;
		}else if((expression_Unary.op == Kind.OP_PLUS || expression_Unary.op == Kind.OP_MINUS) && (expression_Unary.e.typeVar == Type.INTEGER)){
			expression_Unary.typeVar = Type.INTEGER;
		}else{
			expression_Unary.typeVar = null;
		}
		if(expression_Unary.typeVar != null){
			return expression_Unary.typeVar;
		}else{
			throw new SemanticException(token, "Exp Unary");
		}
	}

	@Override

	public Object visitIndex(Index index, Object arg) throws Exception {
		// TODO Auto-generated method stub
		Token token = index.firstToken;
		
		index.e0.typeVar = (Type) index.e0.visit(this, null);
		index.e1.typeVar = (Type) index.e1.visit(this, null);
		
		if(index.e0.typeVar == Type.INTEGER && index.e1.typeVar == Type.INTEGER){
			index.setCartesian(!(index.e0.typeVar.equals(Kind.KW_r) && index.e1.typeVar.equals(Kind.KW_a)));
			return index.typeVar;
		}else{
			throw new SemanticException(token, "Visit Index");
		}
		
	}

	@Override
	public Object visitExpression_PixelSelector(
			Expression_PixelSelector expression_PixelSelector, Object arg)
			throws Exception {
		// TODO Auto-generated method stub
		Token token = expression_PixelSelector.firstToken;
		
		Type nameType = lookUpType(expression_PixelSelector.name);
		if( nameType == Type.IMAGE){
			expression_PixelSelector.typeVar = Type.INTEGER;
		}else if( expression_PixelSelector.index == null){
			expression_PixelSelector.typeVar = nameType;
		}else{
			expression_PixelSelector.typeVar = null;
		}
		
		if(expression_PixelSelector.typeVar !=null){
			return expression_PixelSelector.typeVar;
		}else{
			throw new SemanticException(token,"Exp PixelSelector");
		}
		
	}

	@Override
	public Object visitExpression_Conditional(
			Expression_Conditional expression_Conditional, Object arg)
			throws Exception {
		// TODO Auto-generated method stub
		Token token = expression_Conditional.firstToken;
		
		if(expression_Conditional.condition != null){
			expression_Conditional.condition.typeVar = (Type) expression_Conditional.condition.visit(this, null);
		}
		if(expression_Conditional.trueExpression != null){
			expression_Conditional.trueExpression.typeVar = (Type) expression_Conditional.trueExpression.visit(this, null);
		}
		if(expression_Conditional.falseExpression != null){
			expression_Conditional.falseExpression.typeVar = (Type) expression_Conditional.falseExpression.visit(this, null);
		}
		
		
		if( expression_Conditional.condition.typeVar == Type.BOOLEAN && expression_Conditional.trueExpression.typeVar == expression_Conditional.falseExpression.typeVar){
			expression_Conditional.typeVar = expression_Conditional.trueExpression.typeVar;
			return expression_Conditional.typeVar;
		}else{
			throw new SemanticException(token,"Exp Conditional");
		}
	}

	@Override
	public Object visitDeclaration_Image(Declaration_Image declaration_Image,
			Object arg) throws Exception {
		// TODO Auto-generated method stub
		Token token = declaration_Image.firstToken;
		
		if(declaration_Image.source != null){
			declaration_Image.source.typeVar = (Type) declaration_Image.source.visit(this, null);
		}
		
		if(declaration_Image.xSize != null){
			declaration_Image.xSize.typeVar = (Type) declaration_Image.xSize.visit(this, null);
		}
		if(declaration_Image.ySize != null){
			declaration_Image.ySize.typeVar = (Type) declaration_Image.ySize.visit(this, null);
		}
				
		if(lookUpType(declaration_Image.name) == null){
			symbolTable.put(declaration_Image.name, declaration_Image);
			declaration_Image.typeVar = Type.IMAGE;
		
		if(declaration_Image.xSize !=null){
			if( declaration_Image.ySize != null && (declaration_Image.xSize.typeVar == Type.INTEGER) && (declaration_Image.ySize.typeVar  == Type.INTEGER)){
				return declaration_Image.typeVar;
			}else{
				throw new SemanticException(token,"Dec Image:Size Type Fails");
			}
		}else{
			return declaration_Image.typeVar;
		  }
		}else{
			throw new SemanticException(token, "Dec Image: Lookup not null");
		}
	}

	@Override
	public Object visitSource_StringLiteral(
			Source_StringLiteral source_StringLiteral, Object arg)
			throws Exception {
		// TODO Auto-generated method stub
		try{
		java.net.URL url = new java.net.URL(source_StringLiteral.fileOrUrl);
			source_StringLiteral.typeVar = Type.URL;
		}catch(Exception e){
			source_StringLiteral.typeVar = Type.FILE;
		}
	
		return source_StringLiteral.typeVar;
	}

	@Override
	public Object visitSource_CommandLineParam(
			Source_CommandLineParam source_CommandLineParam, Object arg)
			throws Exception {
		// TODO Auto-generated method stub
		Token token = source_CommandLineParam.firstToken;
		Type paramType = (Type) source_CommandLineParam.paramNum.visit(this, null);
//		
//		source_CommandLineParam.typeVar = paramType;
//		if( source_CommandLineParam.typeVar == Type.INTEGER){
//			return source_CommandLineParam.typeVar;
//		}else{
//			throw new SemanticException(token, "CommandLine Params");
//		}
		source_CommandLineParam.typeVar = null;
		if(paramType == Type.INTEGER) {
			return source_CommandLineParam.typeVar;

		}else {
			throw new SemanticException(token, "CommandLine Params");

		}
	}

	@Override
	public Object visitSource_Ident(Source_Ident source_Ident, Object arg)
			throws Exception {
		// TODO Auto-generated method stub
		Token token = source_Ident.firstToken;
		source_Ident.typeVar = lookUpType(source_Ident.name);
		if( source_Ident.typeVar == Type.FILE || source_Ident.typeVar == Type.URL ){
			return source_Ident.typeVar;
		}else{
			throw new SemanticException(token,"Source Ident fails");
		}
		
	}

	@Override
	public Object visitDeclaration_SourceSink(
			Declaration_SourceSink declaration_SourceSink, Object arg)
			throws Exception {
		// TODO Auto-generated method stub
		Token token = declaration_SourceSink.firstToken;
		declaration_SourceSink.source.typeVar = (Type) declaration_SourceSink.source.visit(this, null);
		
		if(lookUpType(declaration_SourceSink.name) == null){
			symbolTable.put(declaration_SourceSink.name, declaration_SourceSink);
			declaration_SourceSink.typeVar = TypeUtils.getType(declaration_SourceSink.firstToken);
			if( (declaration_SourceSink.source.typeVar == declaration_SourceSink.typeVar) || (declaration_SourceSink.source.typeVar == null)){
				return declaration_SourceSink.typeVar;
			}else{
				throw new SemanticException(token,"Dec SourceSink: Type Mismatch");
			}
		}else{
			throw new SemanticException(token,"Dec Sourcesink:Lookup not null");
		}

	}

	@Override
	public Object visitExpression_IntLit(Expression_IntLit expression_IntLit,
			Object arg) throws Exception {
		// TODO Auto-generated method stub
		expression_IntLit.typeVar = Type.INTEGER;
		return expression_IntLit.typeVar;
	}

	@Override
	//Doubt in this function
	public Object visitExpression_FunctionAppWithExprArg(
			Expression_FunctionAppWithExprArg expression_FunctionAppWithExprArg,
			Object arg) throws Exception {
		// TODO Auto-generated method stub
		Token token = expression_FunctionAppWithExprArg.firstToken;
		if(expression_FunctionAppWithExprArg.arg != null){
			expression_FunctionAppWithExprArg.arg.typeVar = (Type) expression_FunctionAppWithExprArg.arg.visit(this, null);
		}
		
		if( expression_FunctionAppWithExprArg.arg.typeVar == Type.INTEGER){
			expression_FunctionAppWithExprArg.typeVar = Type.INTEGER;
			return expression_FunctionAppWithExprArg.typeVar;
		}else{
			throw new SemanticException(token, "Exp FuncWithExpArg");
		}
		
	}

	@Override
	public Object visitExpression_FunctionAppWithIndexArg(
			Expression_FunctionAppWithIndexArg expression_FunctionAppWithIndexArg,
			Object arg) throws Exception {
		// TODO Auto-generated method stub
		expression_FunctionAppWithIndexArg.typeVar = Type.INTEGER;
		return expression_FunctionAppWithIndexArg.typeVar;
	}

	@Override
	public Object visitExpression_PredefinedName(
			Expression_PredefinedName expression_PredefinedName, Object arg)
			throws Exception {
		// TODO Auto-generated method stub
		expression_PredefinedName.typeVar = Type.INTEGER;
		return expression_PredefinedName.typeVar;
	}

	@Override
	public Object visitStatement_Out(Statement_Out statement_Out, Object arg)
			throws Exception {
		// TODO Auto-generated method stub
		Token token = statement_Out.firstToken;
		if(statement_Out.sink != null){
			statement_Out.sink.typeVar = (Type) statement_Out.sink.visit(this, null);
		}
		Declaration dec = lookUpDec(statement_Out.name);
		statement_Out.setDec(dec);
		
		if(dec != null){
			if( ((dec.typeVar == Type.INTEGER ||dec.typeVar == Type.BOOLEAN)  && ( statement_Out.sink.typeVar == Type.SCREEN)) || (dec.typeVar == Type.IMAGE && (statement_Out.sink.typeVar == Type.FILE || statement_Out.sink.typeVar == Type.SCREEN)) ){
				return statement_Out.typeVar;
			}else{
				throw new SemanticException(token,"Stmt out: type mismatch");
			}
		}else{
			throw new SemanticException(token, "Stmt out:Dec null");
		}
		
	}

	@Override
	public Object visitStatement_In(Statement_In statement_In, Object arg)
			throws Exception {
		// TODO Auto-generated method stub
		Token token = statement_In.firstToken;
		if(statement_In.source != null) {
			statement_In.source.typeVar = (Type) statement_In.source.visit(this, null);
		}
		Declaration dec = lookUpDec(statement_In.name);
		statement_In.setDec(dec);
		
//		if(dec != null && (lookUpType(statement_In.name) == statement_In.source.typeVar)) {
//			return statement_In.typeVar;
//		}else {
//			throw new SemanticException(token, "Stmt In: type mismatch");
//		}
		return null;
	}

	@Override
	public Object visitStatement_Assign(Statement_Assign statement_Assign,
			Object arg) throws Exception {
		// TODO Auto-generated method stub
		Token token = statement_Assign.firstToken;
		if(statement_Assign.lhs != null){
			statement_Assign.lhs.typeVar = (Type) statement_Assign.lhs.visit(this, null);
		}
		if(statement_Assign.e != null){
			statement_Assign.e.typeVar = (Type) statement_Assign.e.visit(this, null);
		}
		
		if( (statement_Assign.lhs.typeVar == statement_Assign.e.typeVar) || (statement_Assign.lhs.typeVar == Type.IMAGE && statement_Assign.e.typeVar == Type.INTEGER)){
			statement_Assign.setCartesian(statement_Assign.lhs.isCartesian);
			return statement_Assign.isCartesian();
		}
		throw new SemanticException(token,"Stmt Assign:Type mismatch");
	}

	@Override
	public Object visitLHS(LHS lhs, Object arg) throws Exception {
		// TODO Auto-generated method stub
		Token token = lhs.firstToken;
		if( lhs.dec != null){
			lhs.dec.typeVar = (Type) lhs.dec.visit(this, null);
		}
		if( lhs.index != null){
			lhs.index.typeVar = (Type) lhs.index.visit(this, null);
		}
		
		lhs.dec = lookUpDec(lhs.name);
		if(lhs.dec == null){
			throw new SemanticException(token, "LHS: Lookup failed");
		}
		lhs.typeVar = lhs.dec.typeVar;
		if( lhs.index == null){
			return lhs.typeVar;
		}
		lhs.isCartesian = lhs.index.isCartesian();
		return lhs.typeVar;
	}

	@Override
	public Object visitSink_SCREEN(Sink_SCREEN sink_SCREEN, Object arg)
			throws Exception {
		// TODO Auto-generated method stub
		sink_SCREEN.typeVar = Type.SCREEN;		
		return sink_SCREEN.typeVar;
	}

	@Override
	public Object visitSink_Ident(Sink_Ident sink_Ident, Object arg)
			throws Exception {
		// TODO Auto-generated method stub
		Token token = sink_Ident.firstToken;
		
		sink_Ident.typeVar = lookUpType(sink_Ident.name);
		if( sink_Ident.typeVar == Type.FILE){
			return sink_Ident.typeVar;
		}else{
			throw new SemanticException(token,"Sink Ident: Type Mismatch");
		}
	}

	@Override
	public Object visitExpression_BooleanLit(
			Expression_BooleanLit expression_BooleanLit, Object arg)
			throws Exception {
		// TODO Auto-generated method stub
		expression_BooleanLit.typeVar = Type.BOOLEAN;
		return expression_BooleanLit.typeVar;
	}

	@Override
	public Object visitExpression_Ident(Expression_Ident expression_Ident,
			Object arg) throws Exception {
		// TODO Auto-generated method stub
		expression_Ident.typeVar = lookUpType(expression_Ident.name);
		return expression_Ident.typeVar;
	}
	
	public Type lookUpType(String name){
		
		if(symbolTable.containsKey(name)){
			return symbolTable.get(name).typeVar;
		}else{
			return null;
		}

	}
	public Declaration lookUpDec(String name){
		
		if(symbolTable.containsKey(name)){
			return symbolTable.get(name);
		}else{
			return null;
		}
	}
}
