package org.eclipse.jdt.internal.compiler.ast;

import org.eclipse.jdt.internal.compiler.lookup.TypeBinding;

public class FormalSpecification {

	private static final char[] preconditionAssertionMessage = "Precondition does not hold".toCharArray(); //$NON-NLS-1$

	public final AbstractMethodDeclaration method;
	public Expression[] preconditions;
	public Expression[] postconditions;

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
//			if (this.postconditions != null)
//			for (Expression e : this.postconditions)
//				e.resolveTypeExpecting(this.method.scope, TypeBinding.BOOLEAN);
		} else {
			if (this.preconditions != null) {
				// Insert assert statements into method body.
				// FIXME(fsc4j): If this is a constructor without an explicit super()/this(), a super()/this() will incorrectly be inserted *before* the asserts.
				Statement[] statements = this.method.statements;
				if (statements == null) {
					statements = new Statement[this.preconditions.length];
				} else {
					int length = statements.length;
					System.arraycopy(statements, 0, statements = new Statement[this.preconditions.length + length], this.preconditions.length, length);
				}
				for (int i = 0; i < this.preconditions.length; i++) {
					Expression e = this.preconditions[i];
					statements[i] = new AssertStatement(new StringLiteral(preconditionAssertionMessage, e.sourceStart, e.sourceEnd, 0), e, e.sourceStart);
				}
				this.method.statements = statements;
				if (this.preconditions[0].sourceStart < this.method.bodyStart)
					this.method.bodyStart = this.preconditions[0].sourceStart;
				// The expressions will be resolved by the caller as part of method body resolution.
			}
		}
	}

}
