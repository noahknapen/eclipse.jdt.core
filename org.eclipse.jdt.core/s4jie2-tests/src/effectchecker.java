class Main {
	public static void main(String[] args) throws Exception {
		System.out.println(Class.forName("fsc4j.EffectChecker").getDeclaredMethod("assertCanCreate", Object.class).toString());
	}
}