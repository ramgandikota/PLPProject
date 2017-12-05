package cop5556fa17;

import java.util.ArrayList;
import java.util.HashMap;

import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import cop5556fa17.Scanner.Kind;
import cop5556fa17.TypeUtils.Type;
import cop5556fa17.AST.*;

//import cop5556fa17.image.ImageFrame;
//import cop5556fa17.image.ImageSupport;

public class CodeGenVisitor implements ASTVisitor, Opcodes {

	/**
	 * All methods and variable static.
	 */
	
	HashMap<Kind, Integer> opMap = new HashMap<>();
	final int DEF_X = 256;
	final int DEF_Y = 256;
	final int Z = 16777215;
	FieldVisitor fv;
	

	/**
	 * @param DEVEL
	 *            used as parameter to genPrint and genPrintTOS
	 * @param GRADE
	 *            used as parameter to genPrint and genPrintTOS
	 * @param sourceFileName
	 *            name of source file, may be null.
	 */
	public CodeGenVisitor(boolean DEVEL, boolean GRADE, String sourceFileName) {
		super();
		this.DEVEL = DEVEL;
		this.GRADE = GRADE;
		this.sourceFileName = sourceFileName;
	}

	ClassWriter cw;
	String className;
	String classDesc;
	String sourceFileName;

	MethodVisitor mv; // visitor of method currently under construction

	/** Indicates whether genPrint and genPrintTOS should generate code. */
	final boolean DEVEL;
	final boolean GRADE;
	
	public void initializeOpMap() {
		
		opMap.put(Kind.OP_PLUS, IADD);
		opMap.put(Kind.OP_MINUS, ISUB);
		opMap.put(Kind.OP_TIMES, IMUL);
		opMap.put(Kind.OP_DIV, IDIV);
		opMap.put(Kind.OP_MOD, IREM);
		opMap.put(Kind.OP_AND,IAND);
		opMap.put(Kind.OP_OR, IOR);
		
		opMap.put(Kind.OP_GT, IF_ICMPLE);
		opMap.put(Kind.OP_GE, IF_ICMPLT);
		opMap.put(Kind.OP_LT, IF_ICMPGE);
		opMap.put(Kind.OP_LE, IF_ICMPGT);
		opMap.put(Kind.OP_EQ, IF_ICMPNE);
		opMap.put(Kind.OP_NEQ, IF_ICMPEQ);
	}


	@Override
	public Object visitProgram(Program program, Object arg) throws Exception {
		cw = new ClassWriter(ClassWriter.COMPUTE_FRAMES);
		className = program.name;  
		classDesc = "L" + className + ";";
		String sourceFileName = (String) arg;
		cw.visit(52, ACC_PUBLIC + ACC_SUPER, className, null, "java/lang/Object", null);
		cw.visitSource(sourceFileName, null);
		// create main method
		mv = cw.visitMethod(ACC_PUBLIC + ACC_STATIC, "main", "([Ljava/lang/String;)V", null, null);
		// initialize
		mv.visitCode();		
		//add label before first instruction
		Label mainStart = new Label();
		mv.visitLabel(mainStart);		
		// if GRADE, generates code to add string to log
//		CodeGenUtils.genLog(GRADE, mv, "entering main");

		// visit decs and statements to add field to class
		//  and instructions to main method, respectivley
		ArrayList<ASTNode> decsAndStatements = program.decsAndStatements;
		for (ASTNode node : decsAndStatements) {
			node.visit(this, arg);
		}

		//generates code to add string to log
//		CodeGenUtils.genLog(GRADE, mv, "leaving main");
		
		//adds the required (by the JVM) return statement to main
		mv.visitInsn(RETURN);
		
		//adds label at end of code
		Label mainEnd = new Label();
		mv.visitLabel(mainEnd);
		
		//handles parameters and local variables of main. Right now, only args
		mv.visitLocalVariable("args", "[Ljava/lang/String;", null, mainStart, mainEnd, 0);
		mv.visitLocalVariable("x", "I", null, mainStart, mainEnd, 1);
		mv.visitLocalVariable("y", "I", null, mainStart, mainEnd, 2);
		mv.visitLocalVariable("X", "I", null, mainStart, mainEnd, 3);
		mv.visitLocalVariable("Y", "I", null, mainStart, mainEnd, 4);
		mv.visitLocalVariable("r", "I", null, mainStart, mainEnd, 5);
		mv.visitLocalVariable("R", "I", null, mainStart, mainEnd, 6);
		mv.visitLocalVariable("a", "I", null, mainStart, mainEnd, 7);
		mv.visitLocalVariable("A", "I", null, mainStart, mainEnd, 8);

		//Sets max stack size and number of local vars.
		//Because we use ClassWriter.COMPUTE_FRAMES as a parameter in the constructor,
		//asm will calculate this itself and the parameters are ignored.
		//If you have trouble with failures in this routine, it may be useful
		//to temporarily set the parameter in the ClassWriter constructor to 0.
		//The generated classfile will not be correct, but you will at least be
		//able to see what is in it.
		mv.visitMaxs(0, 0);
		
		//terminate construction of main method
		mv.visitEnd();
		
		//terminate class construction
		cw.visitEnd();

		//generate classfile as byte array and return
		return cw.toByteArray();
	}

	@Override
	public Object visitDeclaration_Variable(Declaration_Variable declaration_Variable, Object arg) throws Exception {
		// TODO 
		//throw new UnsupportedOperationException();
		String fieldType = "";
		if(declaration_Variable.typeVar == Type.INTEGER) {
			fieldType = "I";
			fv = cw.visitField(ACC_STATIC, declaration_Variable.name, fieldType, null, null);
		}else if(declaration_Variable.typeVar == Type.BOOLEAN) {
			fieldType = "Z";
			fv = cw.visitField(ACC_STATIC, declaration_Variable.name, fieldType, null, null);
		}
		fv.visitEnd();
		if(declaration_Variable.e != null) {
			declaration_Variable.e.visit(this, arg);
			mv.visitFieldInsn(PUTSTATIC,className, declaration_Variable.name, fieldType);
		}
		return null;
	}

	@Override
	public Object visitExpression_Binary(Expression_Binary expression_Binary, Object arg) throws Exception {
		// TODO 
		Label trueLabel = new Label();
		Label falseLabel = new Label();
		
		initializeOpMap();
		expression_Binary.e0.visit(this, arg);
		expression_Binary.e1.visit(this, arg);
		
		switch(expression_Binary.op) {
		case OP_PLUS: case OP_MINUS: case OP_MOD: case OP_DIV:
		case OP_TIMES: case OP_POWER: case OP_AND: case OP_OR:	
			mv.visitInsn(opMap.get(expression_Binary.op));
			break;
		case OP_GT: case OP_GE: case OP_LT:	case OP_LE: case OP_EQ: case OP_NEQ:
			
			mv.visitJumpInsn(opMap.get(expression_Binary.op), trueLabel);
			mv.visitInsn(ICONST_1);
			mv.visitJumpInsn(GOTO, falseLabel);
			mv.visitLabel(trueLabel);
			mv.visitInsn(ICONST_0);
			mv.visitLabel(falseLabel);
			break;
			
		default:
			break;		
		}

		return null;
	}

	@Override
	public Object visitExpression_Unary(Expression_Unary expression_Unary, Object arg) throws Exception {
		// TODO 

		expression_Unary.e.visit(this, arg);
		if(expression_Unary.e.typeVar == Type.INTEGER) {
			if(expression_Unary.op == Kind.OP_MINUS) {
				mv.visitInsn(INEG);
			}else if(expression_Unary.op == Kind.OP_EXCL) {
				mv.visitLdcInsn(Integer.MAX_VALUE);
				mv.visitInsn(IXOR);
			}
			
		}else if(expression_Unary.typeVar == Type.BOOLEAN) {
			Label trueLabel = new Label();
			Label falseLabel = new Label();
			mv.visitJumpInsn(IFNE, trueLabel);
			mv.visitInsn(ICONST_1);
			mv.visitJumpInsn(GOTO, falseLabel);
			mv.visitLabel(trueLabel);
			mv.visitInsn(ICONST_0);
			mv.visitLabel(falseLabel);
		}
		
		return null;
	}

	// generate code to leave the two values on the stack
	@Override
	public Object visitIndex(Index index, Object arg) throws Exception {
		// TODO HW6

		if(index.e0 != null) {
			index.e0.visit(this, arg);
		}
		if(index.e1 != null) {
			index.e1.visit(this, arg);
		}
		
		if(!index.isCartesian()) {
			mv.visitInsn(DUP2);
			mv.visitMethodInsn(INVOKESTATIC, RuntimeFunctions.className, "cart_y", RuntimeFunctions.cart_ySig,false);
			mv.visitInsn(DUP_X2);
			mv.visitInsn(POP);
			mv.visitMethodInsn(INVOKESTATIC, RuntimeFunctions.className, "cart_x", RuntimeFunctions.cart_xSig,false);
		}
		return null;
	}

	@Override
	public Object visitExpression_PixelSelector(Expression_PixelSelector expression_PixelSelector, Object arg)
			throws Exception {
		// TODO HW6

		mv.visitFieldInsn(GETSTATIC, className, expression_PixelSelector.name, ImageSupport.ImageDesc);
		if(expression_PixelSelector.index != null){
			expression_PixelSelector.index.visit(this, arg);
			mv.visitMethodInsn(INVOKESTATIC, ImageSupport.className, "getPixel", ImageSupport.getPixelSig,false);
		}
		return null;
	}

	@Override
	public Object visitExpression_Conditional(Expression_Conditional expression_Conditional, Object arg)
			throws Exception {
		// TODO 

		Label trueLabel = new Label();
		Label falseLabel = new Label();
		
		expression_Conditional.condition.visit(this, arg);
		mv.visitJumpInsn(IFNE, trueLabel);
		expression_Conditional.falseExpression.visit(this, arg);
		mv.visitJumpInsn(GOTO, falseLabel);
		mv.visitLabel(trueLabel);
		expression_Conditional.trueExpression.visit(this, arg);
		mv.visitLabel(falseLabel);
		
		return null;
	}


	@Override
	public Object visitDeclaration_Image(Declaration_Image declaration_Image, Object arg) throws Exception {
		// TODO HW6

		fv = cw.visitField(ACC_STATIC, declaration_Image.name, ImageSupport.ImageDesc, null, null);
		fv.visitEnd();
		if(declaration_Image.source != null) {
			declaration_Image.source.visit(this, arg);
			if(declaration_Image.xSize == null && declaration_Image.ySize == null) {
				mv.visitInsn(ACONST_NULL);
				mv.visitInsn(ACONST_NULL);
			}else {
				declaration_Image.xSize.visit(this, arg);
				mv.visitMethodInsn(INVOKESTATIC, "java/lang/Integer", "valueOf", "(I)Ljava/lang/Integer;", false);
				declaration_Image.ySize.visit(this, arg);
				mv.visitMethodInsn(INVOKESTATIC, "java/lang/Integer", "valueOf", "(I)Ljava/lang/Integer;", false);
			}
			mv.visitMethodInsn(INVOKESTATIC, ImageSupport.className, "readImage", ImageSupport.readImageSig, false);
			
		}

		if(declaration_Image.source == null) {
			if( declaration_Image.xSize == null && declaration_Image.ySize == null) {
				
				mv.visitLdcInsn(DEF_X);
				mv.visitLdcInsn(DEF_Y);
			}else {
				declaration_Image.xSize.visit(this, arg);				
				declaration_Image.ySize.visit(this, arg);
		
			}
			mv.visitMethodInsn(INVOKESTATIC, ImageSupport.className, "makeImage", ImageSupport.makeImageSig, false);
		}
		mv.visitFieldInsn(PUTSTATIC,className, declaration_Image.name, ImageSupport.ImageDesc);

		return null;
	}
	
  
	@Override
	public Object visitSource_StringLiteral(Source_StringLiteral source_StringLiteral, Object arg) throws Exception {
		// TODO HW6
//		if( source_StringLiteral.typeVar == Type.FILE) {
//			mv.visitFieldInsn(GETSTATIC, className, source_StringLiteral.fileOrUrl, "Ljava/io/File;");
//		}
//		else if( source_StringLiteral.typeVar == Type.URL){
//			mv.visitFieldInsn(GETSTATIC, className, source_StringLiteral.fileOrUrl, "Ljava/net/URL;");
//		}
	//	mv.visitFieldInsn(GETSTATIC, className, source_StringLiteral.fileOrUrl,ImageSupport.StringDesc);
		mv.visitLdcInsn(source_StringLiteral.fileOrUrl);
		return null;
	}

	

	@Override
	public Object visitSource_CommandLineParam(Source_CommandLineParam source_CommandLineParam, Object arg)
			throws Exception {
		// TODO 
		mv.visitVarInsn(ALOAD, 0);
		source_CommandLineParam.paramNum.visit(this, arg);
		mv.visitInsn(AALOAD);
		return null;
	}

	@Override
	public Object visitSource_Ident(Source_Ident source_Ident, Object arg) throws Exception {
		// TODO HW6
		
//		if( source_Ident.typeVar == Type.FILE) {
//			mv.visitFieldInsn(GETSTATIC, className, source_Ident.name, "Ljava/io/File;");
//		}
//		else if( source_Ident.typeVar == Type.URL){
//			mv.visitFieldInsn(GETSTATIC, className, source_Ident.name, "Ljava/net/URL;");
//		}
		mv.visitFieldInsn(GETSTATIC, className, source_Ident.name, ImageSupport.StringDesc);

		return null;	
	}


	@Override
	public Object visitDeclaration_SourceSink(Declaration_SourceSink declaration_SourceSink, Object arg)
			throws Exception {
		// TODO HW6
//		String fieldType  = "";
//		if(declaration_SourceSink.typeVar == Type.INTEGER) {
//			fieldType = "I";
//			fv = cw.visitField(ACC_STATIC, declaration_SourceSink.name, fieldType, null, null);
//			
//		}
//		else 
//			if(declaration_SourceSink.typeVar == Type.FILE) {
//			
//			fieldType = "Ljava/io/File;";
//			fv = cw.visitField(ACC_STATIC, declaration_SourceSink.name, fieldType, null, null);
//			
//		}else if(declaration_SourceSink.typeVar == Type.URL) {
//			
//			fieldType = "Ljava/net/URL;";
//			fv = cw.visitField(ACC_STATIC, declaration_SourceSink.name,fieldType, null, null);
//		}
		
		fv = cw.visitField(ACC_STATIC, declaration_SourceSink.name,ImageSupport.StringDesc, null, null);

		fv.visitEnd();
		if(declaration_SourceSink.source != null) {
			declaration_SourceSink.source.visit(this, arg);
			mv.visitFieldInsn(PUTSTATIC, className, declaration_SourceSink.name, ImageSupport.StringDesc);
		}
		
		return null;
	}
	


	@Override
	public Object visitExpression_IntLit(Expression_IntLit expression_IntLit, Object arg) throws Exception {
		// TODO 
		mv.visitLdcInsn(expression_IntLit.value);
		return null;
	}

	@Override
	public Object visitExpression_FunctionAppWithExprArg(
			Expression_FunctionAppWithExprArg expression_FunctionAppWithExprArg, Object arg) throws Exception {
		// TODO HW6
		if(expression_FunctionAppWithExprArg.arg != null) {
			expression_FunctionAppWithExprArg.arg.visit(this, arg);
		}
		if( expression_FunctionAppWithExprArg.function == Kind.KW_abs) {
			mv.visitMethodInsn(INVOKESTATIC, RuntimeFunctions.className,"abs",RuntimeFunctions.absSig,false);

		}else if(expression_FunctionAppWithExprArg.function == Kind.KW_log) {
			mv.visitMethodInsn(INVOKESTATIC, RuntimeFunctions.className,"log",RuntimeFunctions.logSig, false);
		}
		return null;
	}

	@Override
	public Object visitExpression_FunctionAppWithIndexArg(
			Expression_FunctionAppWithIndexArg expression_FunctionAppWithIndexArg, Object arg) throws Exception {
		// TODO HW6
		if(expression_FunctionAppWithIndexArg.arg.e0 != null) {
		expression_FunctionAppWithIndexArg.arg.e0.visit(this, arg);
		}
		if(expression_FunctionAppWithIndexArg.arg.e1 != null) {
			expression_FunctionAppWithIndexArg.arg.e1.visit(this, arg);
		}
		if( expression_FunctionAppWithIndexArg.function == Kind.KW_cart_x) {
			mv.visitMethodInsn(INVOKESTATIC, RuntimeFunctions.className,"cart_x",RuntimeFunctions.cart_xSig,false);

		}else if(expression_FunctionAppWithIndexArg.function == Kind.KW_cart_y) {
			
			mv.visitMethodInsn(INVOKESTATIC, RuntimeFunctions.className,"cart_y",RuntimeFunctions.cart_ySig, false);
		
		}else if(expression_FunctionAppWithIndexArg.function == Kind.KW_polar_a) {
			
			mv.visitMethodInsn(INVOKESTATIC, RuntimeFunctions.className,"polar_a",RuntimeFunctions.polar_aSig, false);
		
		}else if(expression_FunctionAppWithIndexArg.function == Kind.KW_polar_r) {
			
			mv.visitMethodInsn(INVOKESTATIC, RuntimeFunctions.className,"polar_r",RuntimeFunctions.polar_rSig, false);
		}
		
		return null;
	}

	@Override
	public Object visitExpression_PredefinedName(Expression_PredefinedName expression_PredefinedName, Object arg)
			throws Exception {
		// TODO HW6
		Kind kind = expression_PredefinedName.kind;
		switch(kind) {
		case KW_x:
			mv.visitVarInsn(ILOAD, 1);
			break;
		case KW_y:
			mv.visitVarInsn(ILOAD, 2);
			break;	
		case KW_X:
			mv.visitVarInsn(ILOAD, 3);
			break;
		case KW_Y:
			mv.visitVarInsn(ILOAD, 4);
			break;	
		case KW_r:
			mv.visitVarInsn(ILOAD, 1);
			mv.visitVarInsn(ILOAD, 2);
			mv.visitMethodInsn(INVOKESTATIC, RuntimeFunctions.className, "polar_r",RuntimeFunctions.polar_rSig, false);
			mv.visitVarInsn(ISTORE, 5);
			mv.visitVarInsn(ILOAD, 5);
			break;
		case KW_a:
			mv.visitVarInsn(ILOAD, 1);
			mv.visitVarInsn(ILOAD, 2);
			mv.visitMethodInsn(INVOKESTATIC, RuntimeFunctions.className, "polar_a",RuntimeFunctions.polar_aSig, false);
			mv.visitVarInsn(ISTORE, 7);
			mv.visitVarInsn(ILOAD, 7);
			break;
		case KW_R:
			mv.visitVarInsn(ILOAD, 6);
			break;
		case KW_A:
			mv.visitVarInsn(ILOAD, 8);
			break;
		case KW_Z:
			mv.visitLdcInsn(Z);
			break;
		case KW_DEF_X:
			mv.visitLdcInsn(DEF_X);
			break;
		case KW_DEF_Y:
			mv.visitLdcInsn(DEF_Y);
			break;
		default:
			break;
		}
		return null;
	}

	/** For Integers and booleans, the only "sink"is the screen, so generate code to print to console.
	 * For Images, load the Image onto the stack and visit the Sink which will generate the code to handle the image.
	 */
	@Override
	public Object visitStatement_Out(Statement_Out statement_Out, Object arg) throws Exception {
		// TODO in HW5:  only INTEGER and BOOLEAN
		// TODO HW6 remaining cases

		if( statement_Out.getDec().typeVar == Type.INTEGER) {
			mv.visitFieldInsn(GETSTATIC, "java/lang/System", "out", "Ljava/io/PrintStream;");
			mv.visitFieldInsn(GETSTATIC, className, statement_Out.name, "I");
			CodeGenUtils.genLogTOS(GRADE, mv, Type.INTEGER);
			mv.visitMethodInsn(INVOKEVIRTUAL, "java/io/PrintStream", "println", "(I)V",false);
			
		}else if(statement_Out.getDec().typeVar == Type.BOOLEAN) {
			mv.visitFieldInsn(GETSTATIC, "java/lang/System", "out", "Ljava/io/PrintStream;");
			mv.visitFieldInsn(GETSTATIC, className, statement_Out.name, "Z");
			CodeGenUtils.genLogTOS(GRADE, mv, Type.BOOLEAN);
			mv.visitMethodInsn(INVOKEVIRTUAL, "java/io/PrintStream", "println", "(Z)V",false);
			
		}else if(statement_Out.getDec().typeVar == Type.IMAGE ) {
			
			mv.visitFieldInsn(GETSTATIC, className, statement_Out.name, ImageSupport.ImageDesc);
		    CodeGenUtils.genLogTOS(GRADE, mv, Type.IMAGE);
			statement_Out.sink.visit(this, arg);
		}
		return null;
	}

	/**
	 * Visit source to load rhs, which will be a String, onto the stack
	 * 
	 *  In HW5, you only need to handle INTEGER and BOOLEAN
	 *  Use java.lang.Integer.parseInt or java.lang.Boolean.parseBoolean 
	 *  to convert String to actual type. 
	 *  
	 *  TODO HW6 remaining types
	 */
	@Override
	public Object visitStatement_In(Statement_In statement_In, Object arg) throws Exception {
		// TODO (see comment )

		statement_In.source.visit(this, arg);
	
		if(statement_In.getDec().typeVar == Type.INTEGER) {
			
			mv.visitMethodInsn(INVOKESTATIC, "java/lang/Integer", "parseInt", "(Ljava/lang/String;)I",false);
			mv.visitFieldInsn(PUTSTATIC, className, statement_In.name, "I");
			
		}else if(statement_In.getDec().typeVar == Type.BOOLEAN) {
			mv.visitMethodInsn(INVOKESTATIC, "java/lang/Boolean", "parseBoolean", "(Ljava/lang/String;)Z",false);
			mv.visitFieldInsn(PUTSTATIC, className, statement_In.name, "Z");
		}else if(statement_In.getDec().typeVar == Type.IMAGE) {
			Declaration_Image dec = (Declaration_Image) statement_In.getDec();
			if(dec.xSize == null && dec.ySize == null) {
				mv.visitInsn(ACONST_NULL);
				mv.visitInsn(ACONST_NULL);
			}else {
				dec.xSize.visit(this, arg);
				mv.visitMethodInsn(INVOKESTATIC, "java/lang/Integer", "valueOf", "(I)Ljava/lang/Integer;", false);
				dec.ySize.visit(this, arg);
				mv.visitMethodInsn(INVOKESTATIC, "java/lang/Integer", "valueOf", "(I)Ljava/lang/Integer;", false);
				
			}
			
			mv.visitMethodInsn(INVOKESTATIC, ImageSupport.className, "readImage", ImageSupport.readImageSig, false);
			mv.visitFieldInsn(PUTSTATIC, className, statement_In.name, ImageSupport.ImageDesc);
		}

		return null;
	}

	
	/**
	 * In HW5, only handle INTEGER and BOOLEAN types.
	 */
	@Override
	public Object visitStatement_Assign(Statement_Assign statement_Assign, Object arg) throws Exception {
		//TODO  (see comment)		
		
		if(statement_Assign.lhs.typeVar == Type.INTEGER || statement_Assign.lhs.typeVar == Type.BOOLEAN) {
			statement_Assign.e.visit(this, arg);
			statement_Assign.lhs.visit(this, arg);
		}
		if(statement_Assign.lhs.typeVar == Type.IMAGE) {
			
			if(statement_Assign.lhs.isCartesian) {
				    mv.visitFieldInsn(GETSTATIC, className, statement_Assign.lhs.name, ImageSupport.ImageDesc);
					mv.visitInsn(DUP);
				    mv.visitMethodInsn(INVOKESTATIC, ImageSupport.className, "getX", ImageSupport.getXSig, false);
					mv.visitVarInsn(ISTORE, 3);
					mv.visitMethodInsn(INVOKESTATIC, ImageSupport.className, "getY", ImageSupport.getYSig, false);
					mv.visitVarInsn(ISTORE, 4);
			}
				
//			Label l0 = new Label();
//			mv.visitLabel(l0);
			mv.visitInsn(ICONST_0);
			mv.visitVarInsn(ISTORE, 1);
			Label l1 = new Label();
			mv.visitLabel(l1);
			Label l2 = new Label();
			mv.visitJumpInsn(GOTO, l2);
			Label l3 = new Label();
			mv.visitLabel(l3);
			mv.visitFrame(Opcodes.F_APPEND,1, new Object[] {Opcodes.INTEGER}, 0, null);
			mv.visitInsn(ICONST_0);
			mv.visitVarInsn(ISTORE, 2);
			Label l4 = new Label();
			mv.visitLabel(l4);
			Label l5 = new Label();
			mv.visitJumpInsn(GOTO, l5);
			Label l6 = new Label();
			mv.visitLabel(l6);
			mv.visitFrame(Opcodes.F_APPEND,1, new Object[] {Opcodes.INTEGER}, 0, null);
			
			statement_Assign.e.visit(this, arg);
			statement_Assign.lhs.visit(this, arg);
			
			Label l7 = new Label();
			mv.visitLabel(l7);
			mv.visitIincInsn(2, 1);
			mv.visitLabel(l5);
			mv.visitFrame(Opcodes.F_SAME, 0, null, 0, null);
			
			mv.visitVarInsn(ILOAD, 2);
			mv.visitVarInsn(ILOAD, 4);
			
			
			mv.visitJumpInsn(IF_ICMPLT, l6);
			Label l8 = new Label();
			mv.visitLabel(l8);
			mv.visitIincInsn(1, 1);
			mv.visitLabel(l2);
			mv.visitFrame(Opcodes.F_CHOP,1, null, 0, null);
			
			mv.visitVarInsn(ILOAD, 1);
			mv.visitVarInsn(ILOAD, 3);
			mv.visitJumpInsn(IF_ICMPLT, l3);
		}
		
		return null;
	}

	/**
	 * In HW5, only handle INTEGER and BOOLEAN types.
	 */
	@Override
	public Object visitLHS(LHS lhs, Object arg) throws Exception {
		//TODO  (see comment)
		if(lhs.typeVar == Type.INTEGER) {
			mv.visitFieldInsn(PUTSTATIC, className, lhs.name, "I");
		}else if(lhs.typeVar == Type.BOOLEAN){
			mv.visitFieldInsn(PUTSTATIC, className, lhs.name, "Z");
		}
		if(lhs.typeVar == Type.IMAGE) {
			mv.visitFieldInsn(GETSTATIC, className, lhs.name, ImageSupport.ImageDesc);
			mv.visitVarInsn(ILOAD, 1);
			mv.visitVarInsn(ILOAD, 2);
			mv.visitMethodInsn(INVOKESTATIC, ImageSupport.className,"setPixel", ImageSupport.setPixelSig, false);
		}
		return null;
	}
	

	@Override
	public Object visitSink_SCREEN(Sink_SCREEN sink_SCREEN, Object arg) throws Exception {
		//TODO HW6
		
		mv.visitMethodInsn(INVOKESTATIC, ImageSupport.className, "makeFrame", ImageSupport.makeFrameSig, false);
		mv.visitInsn(POP);
		return null;
	}

	@Override
	public Object visitSink_Ident(Sink_Ident sink_Ident, Object arg) throws Exception {
		//TODO HW6
//		mv.visitFieldInsn(GETSTATIC, className, sink_Ident.name,"Ljava/io/File;");
		mv.visitFieldInsn(GETSTATIC, className, sink_Ident.name,ImageSupport.StringDesc);

		mv.visitMethodInsn(INVOKESTATIC, ImageSupport.className, "write", ImageSupport.writeSig, false);
		return null;
	}

	@Override
	public Object visitExpression_BooleanLit(Expression_BooleanLit expression_BooleanLit, Object arg) throws Exception {
		//TODO
		mv.visitLdcInsn(expression_BooleanLit.value);
		return null;
	}

	@Override
	public Object visitExpression_Ident(Expression_Ident expression_Ident,
			Object arg) throws Exception {
		//TODO
		if( expression_Ident.typeVar == Type.INTEGER) {
			mv.visitFieldInsn(GETSTATIC, className, expression_Ident.name, "I");
		}
		else if( expression_Ident.typeVar == Type.BOOLEAN){
			mv.visitFieldInsn(GETSTATIC, className, expression_Ident.name, "Z");
		}
		
		return null;
	}

}
