class Main {
	
	/**
	 * @may_throw IllegalArgumentException | false
	 * @may_throw ArithmeticException | false
	 */
	public static void foo() {
		throw new ArithmeticException();
	}
	
	public static void main(String[] args) {
		Main.foo();
	}
}