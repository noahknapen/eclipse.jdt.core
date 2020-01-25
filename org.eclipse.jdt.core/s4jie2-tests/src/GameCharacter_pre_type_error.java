class Foo {
	public Foo() {}
	boolean isOk() { return true; }
}

class GameCharacter {

	private int health;

	public int getHealth() { return this.health; }

	/**
	 * Reduces this game character's health by the given amount.
	 * @pre The given amount is nonnegative.
	 *    | amount
	 * @pre | new Foo().isOk()
	 */
	public void takeDamage(int amount) {
		this.health -= amount;
	}
}