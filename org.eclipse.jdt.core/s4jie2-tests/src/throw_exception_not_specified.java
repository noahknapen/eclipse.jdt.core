class Foo {
	
	/**
	 * @throws ArithmeticException | false
	 * Problem is that logging message contains date and time of time of error, which thus constantly changes on each run
	 */
	public void bar() {
		throw new IllegalArgumentException();
	}
}

class Main {
	public static void main(String[] args) {
		Foo foo = new Foo();
		foo.bar();
	}
}