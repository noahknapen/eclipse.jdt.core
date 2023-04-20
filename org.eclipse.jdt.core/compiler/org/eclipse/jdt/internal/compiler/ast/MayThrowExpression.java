package org.eclipse.jdt.internal.compiler.ast;

public class MayThrowExpression extends OldExpression {

	public MayThrowExpression(int sourceStart, Expression expression, int sourceEnd, char[] source) {
		super(sourceStart, expression, sourceEnd, source);
		this.source = new char[sourceEnd - sourceStart + 6];
		char[] oldCharArray = "old(".toCharArray(); //$NON-NLS-1$
		char[] rparenCharArray = ")".toCharArray(); //$NON-NLS-1$
		System.arraycopy(oldCharArray, 0, this.source, 0, oldCharArray.length);
		System.arraycopy(source, sourceStart, this.source, oldCharArray.length, sourceEnd - sourceStart + 1);
		System.arraycopy(rparenCharArray, 0, this.source, oldCharArray.length + sourceEnd - sourceStart + 1, rparenCharArray.length);
	}

}
