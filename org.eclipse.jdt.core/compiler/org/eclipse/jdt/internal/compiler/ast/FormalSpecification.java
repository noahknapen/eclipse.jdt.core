package org.eclipse.jdt.internal.compiler.ast;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

import org.eclipse.jdt.core.compiler.CharOperation;
import org.eclipse.jdt.internal.compiler.ASTVisitor;
import org.eclipse.jdt.internal.compiler.codegen.CodeStream;
import org.eclipse.jdt.internal.compiler.codegen.Opcodes;
import org.eclipse.jdt.internal.compiler.flow.ExceptionHandlingFlowContext;
import org.eclipse.jdt.internal.compiler.flow.FlowInfo;
import org.eclipse.jdt.internal.compiler.lookup.BlockScope;
import org.eclipse.jdt.internal.compiler.lookup.ClassScope;
import org.eclipse.jdt.internal.compiler.lookup.MethodBinding;
import org.eclipse.jdt.internal.compiler.lookup.MethodScope;
import org.eclipse.jdt.internal.compiler.lookup.TypeBinding;
import org.eclipse.jdt.internal.compiler.lookup.TypeIds;

public class FormalSpecification {

	private static final char[] preconditionAssertionMessage = "Precondition does not hold".toCharArray(); //$NON-NLS-1$
	private static final char[] postconditionAssertionMessage = "Postcondition does not hold".toCharArray(); //$NON-NLS-1$
	private static final char[] POSTCONDITION_VARIABLE_NAME = " $post".toCharArray(); //$NON-NLS-1$
	private static final char[] POSTCONDITION_METHOD_NAME_SUFFIX = "$post".toCharArray(); //$NON-NLS-1$
	static final char[] OLD_VARIABLE_PREFIX = "old$".toCharArray(); //$NON-NLS-1$
	private static final char[] LAMBDA_PARAMETER_NAME = " $result".toCharArray(); //$NON-NLS-1$
	private static final char[] RESULT_NAME = "result".toCharArray(); //$NON-NLS-1$
	
	private static QualifiedTypeReference getTypeReference(String name) {
		String[] components = name.split("\\."); //$NON-NLS-1$
		char[][] sources = new char[components.length][];
		long[] poss = new long[components.length];
		for (int i = 0; i < components.length; i++)
			sources[i] = components[i].toCharArray();
		return new QualifiedTypeReference(sources, poss);
	}
	
	private static final QualifiedTypeReference javaLangObject = getTypeReference("java.lang.Object"); //$NON-NLS-1$
	private static final QualifiedTypeReference javaLangRunnable = getTypeReference("java.lang.Runnable"); //$NON-NLS-1$
	private static final QualifiedTypeReference javaUtilFunctionConsumer = getTypeReference("java.util.function.Consumer"); //$NON-NLS-1$
	private static final HashMap<Integer, QualifiedTypeReference> boxedTypeReferences = new HashMap<>();
	
	private static void addBoxedTypeReference(int typeId, String typeName) {
		boxedTypeReferences.put(typeId, getTypeReference(typeName));
	}
	
	static {
		addBoxedTypeReference(TypeIds.T_boolean, "java.lang.Boolean"); //$NON-NLS-1$
		addBoxedTypeReference(TypeIds.T_byte, "java.lang.Byte"); //$NON-NLS-1$
		addBoxedTypeReference(TypeIds.T_char, "java.lang.Character"); //$NON-NLS-1$
		addBoxedTypeReference(TypeIds.T_double, "java.lang.Double"); //$NON-NLS-1$
		addBoxedTypeReference(TypeIds.T_float, "java.lang.Float"); //$NON-NLS-1$
		addBoxedTypeReference(TypeIds.T_int, "java.lang.Integer"); //$NON-NLS-1$
		addBoxedTypeReference(TypeIds.T_long, "java.lang.Long"); //$NON-NLS-1$
		addBoxedTypeReference(TypeIds.T_short, "java.lang.Short"); //$NON-NLS-1$
	}
	
	private static TypeReference getBoxedType(TypeBinding binding, TypeReference reference) {
		TypeReference r = boxedTypeReferences.get(binding.id);
		if (r == null)
			return reference;
		return r;
	}
	
	private static QualifiedTypeReference getJavaUtilConsumerOf(TypeReference typeArgument) {
		TypeReference[][] typeArguments = new TypeReference[][] { null, null, null, {typeArgument}};
		return new ParameterizedQualifiedTypeReference(javaUtilFunctionConsumer.tokens, typeArguments, 0, javaUtilFunctionConsumer.sourcePositions);
	}
	
	private static QualifiedTypeReference getPostconditionLambdaType(TypeBinding returnTypeBinding, TypeReference returnType) {
		if (returnType == null) // constructor
			return getJavaUtilConsumerOf(javaLangObject);
		switch (returnTypeBinding.id) {
			case TypeIds.T_void: return javaLangRunnable;
			default: return getJavaUtilConsumerOf(getBoxedType(returnTypeBinding, returnType));
		}
	}

	public final AbstractMethodDeclaration method;
	public Expression[] preconditions;
	public ArrayList<OldExpression> oldExpressions;
	public Expression[] postconditions;
	
	public Block block;
	public LocalDeclaration postconditionVariableDeclaration;
	public MessageSend postconditionMethodCall;
	public ArrayList<Statement> statementsForMethodBody;

	public FormalSpecification(AbstractMethodDeclaration method) {
		this.method = method;
	}

	public void print(int tab, StringBuffer output) {
		if (this.preconditions != null) {
			for (int i = 0; i < this.preconditions.length; i++) {
				output.append("/** @pre | "); //$NON-NLS-1$
				this.preconditions[i].printExpression(tab, output);
				output.append(" */"); //$NON-NLS-1$
			}
		}
		if (this.postconditions != null) {
			for (int i = 0; i < this.postconditions.length; i++) {
				output.append("/** @post | "); //$NON-NLS-1$
				this.postconditions[i].printExpression(tab, output);
				output.append(" */"); //$NON-NLS-1$
			}
		}
	}

	public void resolve() {
		if (this.method.isAbstract() || this.method.isNative()) {
			if (this.preconditions != null)
				for (Expression e : this.preconditions)
					e.resolveTypeExpecting(this.method.scope, TypeBinding.BOOLEAN);
			if (this.postconditions != null)
				for (Expression e : this.postconditions)
					e.resolveTypeExpecting(this.method.scope, TypeBinding.BOOLEAN);
		} else {
			ArrayList<Statement> statementsForBlock = new ArrayList<>();
			HashMap<String, LocalDeclaration> oldExpressions = new HashMap<>();
			int blockDeclarationsCount = 0;
			this.statementsForMethodBody = new ArrayList<>();
			if (this.preconditions != null) {
				// Insert assert statements into method body.
				// FIXME(fsc4j): If this is a constructor without an explicit super()/this(), a super()/this() will incorrectly be inserted *before* the asserts.
				for (int i = 0; i < this.preconditions.length; i++) {
					Expression e = this.preconditions[i];
					statementsForBlock.add(new AssertStatement(new StringLiteral(preconditionAssertionMessage, e.sourceStart, e.sourceEnd, 0), e, e.sourceStart));
				}
			}
			if (this.postconditions != null) {
				for (int i = 0; i < this.postconditions.length; i++) {
					this.postconditions[i].traverse(new ASTVisitor() {

						@Override
						public boolean visit(OldExpression oldExpression, BlockScope blockScope) {
							char[] name = Arrays.copyOf(oldExpression.source, oldExpression.source.length);
							for (int i = 0; i < name.length; i++) { // JVMS 4.2.2 field names cannot contain . ; [ / .
								switch (name[i]) {
									case '.': name[i] = '\u2024'; break; // ONE DOT LEADER
									case ';': name[i] = '\u204f'; break; // REVERSED SEMICOLON 
									case '[': name[i] = '\u298b'; break; // LEFT SQUARE BRACKET WITH UNDERBAR
									case ']': name[i] = '\u298c'; break; // RIGHT SQUARE BRACKET WITH UNDERBAR
									case '/': name[i] = '\u2afd'; break; // DOUBLE SOLIDUS OPERATOR
								}
							}
							String nameString = String.valueOf(name);
							LocalDeclaration declaration = oldExpressions.get(nameString);
							long pos = (oldExpression.sourceStart << 32) + oldExpression.sourceEnd;
							if (declaration == null) {
								declaration = new LocalDeclaration(name, oldExpression.sourceStart, oldExpression.sourceEnd);
								declaration.type = new SingleTypeReference("var".toCharArray(), pos); //$NON-NLS-1$
								declaration.initialization = oldExpression.expression;
								statementsForBlock.add(declaration);
								oldExpressions.put(nameString, declaration);
							}
							oldExpression.declaration = declaration;
							oldExpression.reference = new SingleNameReference(name, pos);
							return false;
						}
						
					}, this.method.scope);
				}
				blockDeclarationsCount += oldExpressions.size();
				
				ArrayList<Statement> postconditionBlockStatements = new ArrayList<>();
				int postconditionBlockDeclarationsCount = 0;
				LocalDeclaration resultDeclaration = null;
				if (this.method instanceof MethodDeclaration) {
					MethodDeclaration md = (MethodDeclaration)this.method;
					if (md.binding.returnType.id != TypeIds.T_void) {
						resultDeclaration = new LocalDeclaration(RESULT_NAME, this.method.bodyStart, this.method.bodyStart);
						resultDeclaration.type = md.returnType;
						resultDeclaration.initialization = new SingleNameReference(LAMBDA_PARAMETER_NAME, (this.method.bodyStart << 32) + this.method.bodyStart);
						postconditionBlockStatements.add(resultDeclaration);
						postconditionBlockDeclarationsCount++;
					}
				}
				for (int i = 0; i < this.postconditions.length; i++) {
					Expression e = this.postconditions[i];
					postconditionBlockStatements.add(new AssertStatement(new StringLiteral(postconditionAssertionMessage, e.sourceStart, e.sourceEnd, 0), e, e.sourceStart));
				}
				Block postconditionBlock = new Block(postconditionBlockDeclarationsCount);
				postconditionBlock.statements = new Statement[postconditionBlockStatements.size()];
				postconditionBlockStatements.toArray(postconditionBlock.statements);
				postconditionBlock.sourceStart = this.postconditions[0].sourceStart;
				postconditionBlock.sourceEnd = this.method.bodyEnd;
				LambdaExpression postconditionLambda = new LambdaExpression(this.method.compilationResult, false);
				postconditionLambda.allowReferencesToNonEffectivelyFinalOuterLocals = true;
				if (this.method instanceof ConstructorDeclaration)
					postconditionLambda.lateBindReceiver = true;
				postconditionLambda.lambdaMethodSelector = CharOperation.concat(this.method.selector, POSTCONDITION_METHOD_NAME_SUFFIX);
				if (this.method.binding.returnType.id != TypeIds.T_void || this.method instanceof ConstructorDeclaration)
					postconditionLambda.setArguments(new Argument[] {new Argument(LAMBDA_PARAMETER_NAME, (this.method.bodyStart << 32) + this.method.bodyStart, null, 0, true)});
				postconditionLambda.setBody(postconditionBlock);
				postconditionLambda.sourceStart = this.method.bodyStart;
				postconditionLambda.sourceEnd = this.method.bodyEnd;
				this.postconditionVariableDeclaration = new LocalDeclaration(POSTCONDITION_VARIABLE_NAME, this.method.bodyStart, this.method.bodyStart);
				this.postconditionVariableDeclaration.type = getPostconditionLambdaType(this.method.binding.returnType, this.method instanceof MethodDeclaration ? ((MethodDeclaration)this.method).returnType : null);
				this.statementsForMethodBody.add(this.postconditionVariableDeclaration);
				this.method.explicitDeclarations++;
				statementsForBlock.add(new Assignment(new SingleNameReference(this.postconditionVariableDeclaration.name, (this.method.bodyStart << 32) + this.method.bodyStart), postconditionLambda, this.method.bodyStart));
				
				this.postconditionMethodCall = new MessageSend();
				this.postconditionMethodCall.receiver = new SingleNameReference(POSTCONDITION_VARIABLE_NAME, (this.method.bodyStart<< 32) + this.method.bodyStart);
				if (this.method.binding.returnType.id == TypeIds.T_void && !(this.method instanceof ConstructorDeclaration))
					this.postconditionMethodCall.selector = "run".toCharArray(); //$NON-NLS-1$
				else {
					this.postconditionMethodCall.selector = "accept".toCharArray(); //$NON-NLS-1$
					this.postconditionMethodCall.arguments = new Expression[] {new NullLiteral(0, 0)};
				}
			}
			this.block = new Block(blockDeclarationsCount);
			this.block.statements = new Statement[statementsForBlock.size()];
			statementsForBlock.toArray(this.block.statements);
			this.statementsForMethodBody.add(this.block);
			
			for (Statement s : this.statementsForMethodBody)
				s.resolve(this.method.scope);
			
			ASTVisitor checker = new ASTVisitor() {
				
				private boolean isVisible(MethodBinding binding) {
					if (!isVisible(binding.declaringClass))
						return false;
					if (binding.isPublic())
						return true;
					if (FormalSpecification.this.method.binding.isPublic())
						return false;
					if (FormalSpecification.this.method.binding.isPrivate())
						return true;
					if (binding.isPrivate())
						return false;
					// Here, both elements are either protected or package-accessible
					// TODO(fsc4j): Deal with 'protected' case
					// Here, we assume both elements are package-accessible
					return binding.declaringClass.fPackage == FormalSpecification.this.method.binding.declaringClass.fPackage;
				}

				@Override
				public boolean visit(AllocationExpression allocationExpression, BlockScope scope) {
					MethodBinding binding = allocationExpression.binding;
					if (!isVisible(binding))
						FormalSpecification.this.method.scope.problemReporter().notVisibleConstructor(allocationExpression, binding);
					return true;
				}

				@Override
				public boolean visit(ArrayAllocationExpression arrayAllocationExpression, BlockScope scope) {
					// TODO Auto-generated method stub
					return super.visit(arrayAllocationExpression, scope);
				}

				@Override
				public boolean visit(ArrayQualifiedTypeReference arrayQualifiedTypeReference, BlockScope scope) {
					// TODO Auto-generated method stub
					return super.visit(arrayQualifiedTypeReference, scope);
				}

				@Override
				public boolean visit(ArrayQualifiedTypeReference arrayQualifiedTypeReference, ClassScope scope) {
					// TODO Auto-generated method stub
					return super.visit(arrayQualifiedTypeReference, scope);
				}

				@Override
				public boolean visit(ArrayTypeReference arrayTypeReference, BlockScope scope) {
					// TODO Auto-generated method stub
					return super.visit(arrayTypeReference, scope);
				}

				@Override
				public boolean visit(ArrayTypeReference arrayTypeReference, ClassScope scope) {
					// TODO Auto-generated method stub
					return super.visit(arrayTypeReference, scope);
				}

				@Override
				public boolean visit(Assignment assignment, BlockScope scope) {
					// TODO Auto-generated method stub
					return super.visit(assignment, scope);
				}

				@Override
				public boolean visit(ClassLiteralAccess classLiteral, BlockScope scope) {
					// TODO Auto-generated method stub
					return super.visit(classLiteral, scope);
				}

				@Override
				public boolean visit(CompoundAssignment compoundAssignment, BlockScope scope) {
					// TODO Auto-generated method stub
					return super.visit(compoundAssignment, scope);
				}

				@Override
				public boolean visit(FieldReference fieldReference, BlockScope scope) {
					// TODO Auto-generated method stub
					return super.visit(fieldReference, scope);
				}

				@Override
				public boolean visit(FieldReference fieldReference, ClassScope scope) {
					// TODO Auto-generated method stub
					return super.visit(fieldReference, scope);
				}

				@Override
				public boolean visit(MessageSend messageSend, BlockScope scope) {
					// TODO Auto-generated method stub
					return super.visit(messageSend, scope);
				}

				@Override
				public boolean visit(ParameterizedQualifiedTypeReference parameterizedQualifiedTypeReference,
						BlockScope scope) {
					// TODO Auto-generated method stub
					return super.visit(parameterizedQualifiedTypeReference, scope);
				}

				@Override
				public boolean visit(ParameterizedQualifiedTypeReference parameterizedQualifiedTypeReference,
						ClassScope scope) {
					// TODO Auto-generated method stub
					return super.visit(parameterizedQualifiedTypeReference, scope);
				}

				@Override
				public boolean visit(ParameterizedSingleTypeReference parameterizedSingleTypeReference,
						BlockScope scope) {
					// TODO Auto-generated method stub
					return super.visit(parameterizedSingleTypeReference, scope);
				}

				@Override
				public boolean visit(ParameterizedSingleTypeReference parameterizedSingleTypeReference,
						ClassScope scope) {
					// TODO Auto-generated method stub
					return super.visit(parameterizedSingleTypeReference, scope);
				}

				@Override
				public boolean visit(QualifiedAllocationExpression qualifiedAllocationExpression, BlockScope scope) {
					// TODO Auto-generated method stub
					return super.visit(qualifiedAllocationExpression, scope);
				}

				@Override
				public boolean visit(QualifiedNameReference qualifiedNameReference, BlockScope scope) {
					// TODO Auto-generated method stub
					return super.visit(qualifiedNameReference, scope);
				}

				@Override
				public boolean visit(QualifiedNameReference qualifiedNameReference, ClassScope scope) {
					// TODO Auto-generated method stub
					return super.visit(qualifiedNameReference, scope);
				}

				@Override
				public boolean visit(QualifiedSuperReference qualifiedSuperReference, BlockScope scope) {
					// TODO Auto-generated method stub
					return super.visit(qualifiedSuperReference, scope);
				}

				@Override
				public boolean visit(QualifiedSuperReference qualifiedSuperReference, ClassScope scope) {
					// TODO Auto-generated method stub
					return super.visit(qualifiedSuperReference, scope);
				}

				@Override
				public boolean visit(QualifiedTypeReference qualifiedTypeReference, BlockScope scope) {
					// TODO Auto-generated method stub
					return super.visit(qualifiedTypeReference, scope);
				}

				@Override
				public boolean visit(QualifiedTypeReference qualifiedTypeReference, ClassScope scope) {
					// TODO Auto-generated method stub
					return super.visit(qualifiedTypeReference, scope);
				}

				@Override
				public boolean visit(SingleNameReference singleNameReference, BlockScope scope) {
					// TODO Auto-generated method stub
					return super.visit(singleNameReference, scope);
				}

				@Override
				public boolean visit(SingleNameReference singleNameReference, ClassScope scope) {
					// TODO Auto-generated method stub
					return super.visit(singleNameReference, scope);
				}

				@Override
				public boolean visit(SingleTypeReference singleTypeReference, BlockScope scope) {
					// TODO Auto-generated method stub
					return super.visit(singleTypeReference, scope);
				}

				@Override
				public boolean visit(SingleTypeReference singleTypeReference, ClassScope scope) {
					// TODO Auto-generated method stub
					return super.visit(singleTypeReference, scope);
				}

				@Override
				public boolean visit(ThrowStatement throwStatement, BlockScope scope) {
					// TODO Auto-generated method stub
					return super.visit(throwStatement, scope);
				}

				@Override
				public boolean visit(TryStatement tryStatement, BlockScope scope) {
					// TODO Auto-generated method stub
					return super.visit(tryStatement, scope);
				}
				
			};
			
			if (this.preconditions != null)
				for (Expression e : this.preconditions)
					e.traverse(checker, this.method.scope);
			if (this.postconditions != null)
				for (Expression e : this.postconditions)
					e.traverse(checker, this.method.scope);
			
			if (this.preconditions != null)
				this.method.bodyStart = this.preconditions[0].sourceStart;
			else
				this.method.bodyStart = this.postconditions[0].sourceStart;
		}
	}

	public void generatePostconditionCheck(CodeStream codeStream) {
		if (this.postconditions != null) {
			int returnType = this.method.binding.returnType.id;
			if (returnType == TypeIds.T_void) {
				codeStream.load(this.postconditionVariableDeclaration.binding);
				if (this.method instanceof ConstructorDeclaration)
					codeStream.aload_0();
			} else {
				if (returnType == TypeIds.T_long || returnType == TypeIds.T_double) {
					codeStream.dup2();
					codeStream.load(this.postconditionVariableDeclaration.binding);
					codeStream.dup_x2();
					codeStream.pop();
				} else {
					codeStream.dup();
					codeStream.load(this.postconditionVariableDeclaration.binding);
					codeStream.swap();
				}
				if (this.method.binding.returnType.isPrimitiveType())
					codeStream.generateBoxingConversion(returnType);
			}
			MethodBinding method = this.postconditionMethodCall.binding.original();
			TypeBinding constantPoolDeclaringClass = CodeStream.getConstantPoolDeclaringClass(this.method.scope, method, method.declaringClass, false);
			codeStream.invoke(Opcodes.OPC_invokeinterface, method, constantPoolDeclaringClass);
		}
		
	}

	public FlowInfo analyseCode(MethodScope scope, ExceptionHandlingFlowContext methodContext, FlowInfo flowInfo) {
		for (Statement s : this.statementsForMethodBody)
			flowInfo = s.analyseCode(scope, methodContext, flowInfo);
		return flowInfo;
	}

	public void generateCode(MethodScope scope, CodeStream codeStream) {
		for (Statement s : this.statementsForMethodBody)
			s.generateCode(scope, codeStream);
	}

}
