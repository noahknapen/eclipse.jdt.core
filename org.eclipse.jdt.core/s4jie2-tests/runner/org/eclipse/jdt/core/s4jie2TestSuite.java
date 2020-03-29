package org.eclipse.jdt.core;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.FileSystems;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;

import org.eclipse.jdt.internal.compiler.batch.Main;

/**
 * @since 3.24
 */
@SuppressWarnings("nls")
public class s4jie2TestSuite {

	public static void deleteFileTree(String path) throws IOException {
	     Files.walkFileTree(FileSystems.getDefault().getPath(path), new SimpleFileVisitor<Path>() {
	         @Override
	         public FileVisitResult visitFile(Path file, BasicFileAttributes attrs)
	             throws IOException
	         {
	             Files.delete(file);
	             return FileVisitResult.CONTINUE;
	         }
	         @Override
	         public FileVisitResult postVisitDirectory(Path dir, IOException e)
	             throws IOException
	         {
	             if (e == null) {
	                 Files.delete(dir);
	                 return FileVisitResult.CONTINUE;
	             } else {
	                 // directory iteration failed
	                 throw e;
	             }
	         }
	     });
	}

	public static void assertTrue(boolean b, String message) {
		if (!b) {
			System.err.println("FAIL " + message);
			System.exit(1);
		}
	}

	public static void assertEquals(boolean actual, boolean expected, String message) {
		if (actual != expected) {
			System.err.println("FAIL " + message + ": expected: " + expected + "; actual: " + actual);
			System.exit(1);
		}
	}
	public static String normalize(String text) { return text.replace("\r\n", "\n"); }
	public static void assertEquals(String actual, String expected, String msg) {
		if (!normalize(actual).equals(normalize(expected))) {
			System.err.println("FAIL " + msg + " is not as expected");
			System.err.println("=== expected START ===");
			System.err.println(expected);
			System.err.println("=== expected END ===");
			System.err.println("=== actual START ===");
			System.err.println(actual);
			System.err.println("=== actual END ===");
			System.exit(1);
		}
	}

	private static final String binPath = "s4jie2-tests/bin";

	public static void testCompile(String filename, boolean expectedSuccess, String outExpected, String errExpected) {
		testCompile(false, filename, expectedSuccess, outExpected, errExpected);
	}
	
	@SuppressWarnings("deprecation")
	public static void testCompile(boolean asModule, String filename, boolean expectedSuccess, String outExpected, String errExpected) {
		System.out.println("     Test " + filename + " start");
		StringWriter outWriter = new StringWriter();
		StringWriter errWriter = new StringWriter();
		String path = "s4jie2-tests/src/" + filename + ".java";
		String fullPath = new File(path).getAbsolutePath();
		String moduleArgs = asModule ? "--module-source-path s4jie2-tests/src s4jie2-tests/src/module-info.java" : "";
		String args = "-source 10 -proc:none " + moduleArgs + " " + path + " -g -d " + binPath + "/" + filename;
		if (Main.compile(args, new PrintWriter(outWriter), new PrintWriter(errWriter)) != expectedSuccess) {
			System.err.println("FAIL compiler success: expected: " + expectedSuccess + "; actual: " + !expectedSuccess);
			System.err.println("=== standard output start ===");
			System.err.println(outWriter.toString());
			System.err.println("=== standard output end ===");
			System.err.println("=== standard error start ===");
			System.err.println(errWriter.toString());
			System.err.println("=== standard error end ===");
			System.exit(1);
		}
		assertEquals(outWriter.toString(), outExpected.replace("SOURCE_FILE_FULL_PATH", fullPath), "standard output");
		assertEquals(errWriter.toString(), errExpected.replace("SOURCE_FILE_FULL_PATH", fullPath), "standard error");
		System.out.println("PASS Test " + filename + " compile success");
	}

	public static void readFullyInto(InputStream stream, StringBuilder builder) {
		InputStreamReader reader = new InputStreamReader(stream);
		char[] buffer = new char[65536];
		try {
			for (;;) {
				int result = reader.read(buffer);
				if (result < 0) break;
				builder.append(buffer, 0, result);
			}
		} catch (IOException e) {
			e.printStackTrace();
			builder.append("<exception while reading from subprocess>");
		}
	}

	public static void testCompileAndRun(boolean enableAssertions, String filename, boolean expectedSuccess, String outExpected, String errExpected) throws IOException {
		testCompile(filename, true, "", "");

		String classpath = binPath+"/"+filename;
		String java10Home = System.getenv("JAVA_10_HOME");
		if (java10Home == null)
			throw new AssertionError("Please make JAVA_10_HOME point to a Java 10 or later JRE or JDK");
		Process process = new ProcessBuilder(java10Home + "/bin/java", "-classpath", classpath, enableAssertions ? "-ea" : "-da", "Main").start();
		StringBuilder stdoutBuffer = new StringBuilder();
		Thread stdoutThread = new Thread(() -> readFullyInto(process.getInputStream(), stdoutBuffer));
		stdoutThread.start();
		StringBuilder stderrBuffer = new StringBuilder();
		Thread stderrThread = new Thread(() -> readFullyInto(process.getErrorStream(), stderrBuffer));
		stderrThread.start();
		int exitCode;
		try {
			exitCode = process.waitFor();
			stdoutThread.join();
			stderrThread.join();
		} catch (InterruptedException e) {
			throw new AssertionError(e);
		}
		String stdout = stdoutBuffer.toString();
		String stderr = stderrBuffer.toString();

		if ((exitCode == 0) != expectedSuccess) {
			System.err.println("FAIL execution success: expected: " + expectedSuccess + "; actual: exit code " + exitCode);
			System.err.println("=== standard output start ===");
			System.err.println(stdout);
			System.err.println("=== standard output end ===");
			System.err.println("=== standard error start ===");
			System.err.println(stderr);
			System.err.println("=== standard error end ===");
			System.exit(1);
		}
		assertEquals(stdout, outExpected, "standard output");
		assertEquals(stderr, errExpected, "standard error");
		System.out.println("PASS Test "+ filename + " execution success");
	}

	public static void main(String[] args) throws IOException {
		if (new File(binPath).exists())
			deleteFileTree(binPath);

		testCompile("Minimal", true, "", "");

		testCompileAndRun(false, "GameCharacter_pre", true, "",
				"No exception was thrown! :-(\n" +
				"No exception was thrown! :-(\n");
		testCompileAndRun(true, "GameCharacter_pre", true, "",
				"java.lang.AssertionError: Precondition does not hold\n" +
				"	at GameCharacter.takeDamage(GameCharacter_pre.java:20)\n" +
				"	at Main.main(GameCharacter_pre.java:36)\n" +
				"java.lang.AssertionError: Precondition does not hold\n" +
				"	at GameCharacter.<init>(GameCharacter_pre.java:9)\n" +
				"	at Main.main(GameCharacter_pre.java:44)\n");
		testCompile("GameCharacter_pre_fail", false, "",
				"----------\n" +
				"1. ERROR in SOURCE_FILE_FULL_PATH (at line 10)\n" +
				"	*    | 0 <=\n" +
				"	         ^^\n" +
				"Syntax error on token \"<=\", Expression expected after this token\n" +
				"----------\n" +
				"1 problem (1 error)\n");
		testCompile("GameCharacter_pre_type_error", false, "",
				"----------\n" +
				"1. ERROR in SOURCE_FILE_FULL_PATH (at line 19)\n" +
				"	*    | amount\n" +
				"	       ^^^^^^\n" +
				"Type mismatch: cannot convert from int to boolean\n" +
				"----------\n" +
				"2. ERROR in SOURCE_FILE_FULL_PATH (at line 20)\n" +
				"	* @pre | new Foo().isOk()\n" +
				"	         ^^^^^^^^^\n" +
				"The constructor Foo() is not visible\n" +
				"----------\n" +
				"3. ERROR in SOURCE_FILE_FULL_PATH (at line 20)\n" +
				"	* @pre | new Foo().isOk()\n" +
				"	             ^^^\n" +
				"The type Foo is not visible\n" +
				"----------\n" +
				"4. ERROR in SOURCE_FILE_FULL_PATH (at line 20)\n" +
				"	* @pre | new Foo().isOk()\n" +
				"	                   ^^^^\n" +
				"The method isOk() from the type Foo is not visible\n" +
				"----------\n" +
				"5. ERROR in SOURCE_FILE_FULL_PATH (at line 21)\n" +
				"	* @pre | health == 0\n" +
				"	         ^^^^^^\n" +
				"The field GameCharacter_pre_type_error.health is not visible\n" +
				"----------\n" +
				"6. ERROR in SOURCE_FILE_FULL_PATH (at line 22)\n" +
				"	* @pre | this.health == 0\n" +
				"	         ^^^^^^^^^^^\n" +
				"The field GameCharacter_pre_type_error.health is not visible\n" +
				"----------\n" +
				"7. ERROR in SOURCE_FILE_FULL_PATH (at line 23)\n" +
				"	* @pre | helper()\n" +
				"	         ^^^^^^\n" +
				"The method helper() from the type GameCharacter_pre_type_error is not visible\n" +
				"----------\n" +
				"8. ERROR in SOURCE_FILE_FULL_PATH (at line 24)\n" +
				"	* @pre | Foo.class.getName() == \"Foo\"\n" +
				"	         ^^^\n" +
				"The type Foo is not visible\n" +
				"----------\n" +
				"9. ERROR in SOURCE_FILE_FULL_PATH (at line 25)\n" +
				"	* @pre | (bazz += 1) + (bazz = 1) + (bazz++) == 42\n" +
				"	         ^^^^^^^^^^^\n" +
				"Assignments are not allowed inside Javadoc comments\n" +
				"----------\n" +
				"10. ERROR in SOURCE_FILE_FULL_PATH (at line 25)\n" +
				"	* @pre | (bazz += 1) + (bazz = 1) + (bazz++) == 42\n" +
				"	                       ^^^^^^^^^^\n" +
				"Assignments are not allowed inside Javadoc comments\n" +
				"----------\n" +
				"11. ERROR in SOURCE_FILE_FULL_PATH (at line 25)\n" +
				"	* @pre | (bazz += 1) + (bazz = 1) + (bazz++) == 42\n" +
				"	                                    ^^^^^^^^\n" +
				"Assignments are not allowed inside Javadoc comments\n" +
				"----------\n" +
				"11 problems (11 errors)\n");
		testCompile("GameCharacter_pre_post_syntax_error", false, "",
				"----------\n" +
				"1. ERROR in SOURCE_FILE_FULL_PATH (at line 10)\n" +
				"	*    | 0 <= amount +\n" +
				"	                   ^\n" +
				"Syntax error on token \"+\", ++ expected\n" +
				"----------\n" +
				"2. ERROR in SOURCE_FILE_FULL_PATH (at line 23)\n" +
				"	*    | getHealth() == old(getHealth()) - amount +\n" +
				"	                                                ^\n" +
				"Syntax error on token \"+\", ++ expected\n" +
				"----------\n" +
				"3. ERROR in SOURCE_FILE_FULL_PATH (at line 29)\n" +
				"	/** @post | result == (getHealth() ** 3 > 0) */\n" +
				"	                                    ^\n" +
				"Syntax error on token \"*\", delete this token\n" +
				"----------\n" +
				"4. ERROR in SOURCE_FILE_FULL_PATH (at line 32)\n" +
				"	/** @post | nested.comment == /* */\n" +
				"	                              ^^\n" +
				"Syntax error on tokens, Expression expected instead\n" +
				"----------\n" +
				"4 problems (4 errors)\n");
	    testCompile("GameCharacter_pre_post_type_error", false, "",
	    		"----------\n" +
	    		"1. ERROR in SOURCE_FILE_FULL_PATH (at line 9)\n" +
	    		"	*     | getHealth()\n" +
	    		"	        ^^^^^^^^^^^\n" +
	    		"Type mismatch: cannot convert from int to boolean\n" +
	    		"----------\n" +
	    		"1 problem (1 error)\n");
		testCompileAndRun(true, "GameCharacter_pre_post", true, "",
				"java.lang.AssertionError: Postcondition does not hold\n" +
				"	at GameCharacter.takeDamage$post(GameCharacter_pre_post.java:28)\n" +
				"	at GameCharacter.takeDamage(GameCharacter_pre_post.java:36)\n" +
				"	at Main.main(GameCharacter_pre_post.java:98)\n" +
				"java.lang.AssertionError: Postcondition does not hold\n" +
				"	at GameCharacter.setHealth$post(GameCharacter_pre_post.java:13)\n" +
				"	at GameCharacter.setHealth(GameCharacter_pre_post.java:17)\n" +
				"	at Main.main(GameCharacter_pre_post.java:105)\n" +
				"java.lang.AssertionError: Postcondition does not hold\n" +
				"	at GameCharacter.simpleReturnTest$post(GameCharacter_pre_post.java:54)\n" +
				"	at GameCharacter.simpleReturnTest(GameCharacter_pre_post.java:58)\n" +
				"	at Main.main(GameCharacter_pre_post.java:112)\n" +
				"java.lang.AssertionError: Postcondition does not hold\n" +
				"	at GameCharacter.returnInsideIfTest$post(GameCharacter_pre_post.java:62)\n" +
				"	at GameCharacter.returnInsideIfTest(GameCharacter_pre_post.java:67)\n" +
				"	at Main.main(GameCharacter_pre_post.java:119)\n" +
				"java.lang.AssertionError: Postcondition does not hold\n" +
				"	at GameCharacter.returnInsideIfTest$post(GameCharacter_pre_post.java:62)\n" +
				"	at GameCharacter.returnInsideIfTest(GameCharacter_pre_post.java:70)\n" +
				"	at Main.main(GameCharacter_pre_post.java:126)\n" +
				"java.lang.AssertionError: Postcondition does not hold\n" +
				"	at Main.booleanResult$post(GameCharacter_pre_post.java:211)\n" +
				"	at Main.booleanResult(GameCharacter_pre_post.java:213)\n" +
				"	at Main.main(GameCharacter_pre_post.java:146)\n" +
				"java.lang.AssertionError: Postcondition does not hold\n" +
				"	at Main.byteResult$post(GameCharacter_pre_post.java:216)\n" +
				"	at Main.byteResult(GameCharacter_pre_post.java:218)\n" +
				"	at Main.main(GameCharacter_pre_post.java:153)\n" +
				"java.lang.AssertionError: Postcondition does not hold\n" +
				"	at Main.charResult$post(GameCharacter_pre_post.java:221)\n" +
				"	at Main.charResult(GameCharacter_pre_post.java:223)\n" +
				"	at Main.main(GameCharacter_pre_post.java:160)\n" +
				"java.lang.AssertionError: Postcondition does not hold\n" +
				"	at Main.doubleResult$post(GameCharacter_pre_post.java:226)\n" +
				"	at Main.doubleResult(GameCharacter_pre_post.java:228)\n" +
				"	at Main.main(GameCharacter_pre_post.java:167)\n" +
				"java.lang.AssertionError: Postcondition does not hold\n" +
				"	at Main.floatResult$post(GameCharacter_pre_post.java:231)\n" +
				"	at Main.floatResult(GameCharacter_pre_post.java:233)\n" +
				"	at Main.main(GameCharacter_pre_post.java:174)\n" +
				"java.lang.AssertionError: Postcondition does not hold\n" +
				"	at Main.intResult$post(GameCharacter_pre_post.java:236)\n" +
				"	at Main.intResult(GameCharacter_pre_post.java:238)\n" +
				"	at Main.main(GameCharacter_pre_post.java:181)\n" +
				"java.lang.AssertionError: Postcondition does not hold\n" +
				"	at Main.longResult$post(GameCharacter_pre_post.java:241)\n" +
				"	at Main.longResult(GameCharacter_pre_post.java:243)\n" +
				"	at Main.main(GameCharacter_pre_post.java:188)\n" +
				"java.lang.AssertionError: Postcondition does not hold\n" +
				"	at Main.shortResult$post(GameCharacter_pre_post.java:246)\n" +
				"	at Main.shortResult(GameCharacter_pre_post.java:248)\n" +
				"	at Main.main(GameCharacter_pre_post.java:195)\n" +
				"java.lang.AssertionError: Postcondition does not hold\n" +
				"	at Main.genericResult$post(GameCharacter_pre_post.java:251)\n" +
				"	at Main.genericResult(GameCharacter_pre_post.java:259)\n" +
				"	at Main.main(GameCharacter_pre_post.java:202)\n");
		testCompileAndRun(true, "GameCharacter_ctor_post", true, "",
				"java.lang.AssertionError: Postcondition does not hold\n" +
				"	at GameCharacter.GameCharacter$post(GameCharacter_ctor_post.java:21)\n" +
				"	at GameCharacter.<init>(GameCharacter_ctor_post.java:36)\n" +
				"	at Main.main(GameCharacter_ctor_post.java:51)\n" +
				"java.lang.AssertionError: Postcondition does not hold\n" +
				"	at GameCharacter.GameCharacter$post(GameCharacter_ctor_post.java:21)\n" +
				"	at GameCharacter.<init>(GameCharacter_ctor_post.java:31)\n" +
				"	at Main.main(GameCharacter_ctor_post.java:59)\n" +
				"java.lang.AssertionError: Postcondition does not hold\n" +
				"	at GameCharacter.GameCharacter$post(GameCharacter_ctor_post.java:23)\n" +
				"	at GameCharacter.<init>(GameCharacter_ctor_post.java:36)\n" +
				"	at Main.main(GameCharacter_ctor_post.java:67)\n" +
				"java.lang.AssertionError: Postcondition does not hold\n" +
				"	at GameCharacter.GameCharacter$post(GameCharacter_ctor_post.java:25)\n" +
				"	at GameCharacter.<init>(GameCharacter_ctor_post.java:36)\n" +
				"	at Main.main(GameCharacter_ctor_post.java:75)\n");
		testCompile(true, "testpackage/unresolved_type", false, "",
				"----------\n" + 
				"1. ERROR in SOURCE_FILE_FULL_PATH (at line 8)\n" + 
				"	* @post | Arrays.equals(getElements(), 0, getElements().length, old(getElements()), 0, old(getElements()).length)\n" + 
				"	          ^^^^^^\n" + 
				"Arrays cannot be resolved\n" + 
				"----------\n" + 
				"1 problem (1 error)\n");
		testCompileAndRun(true, "multiline_lambdas", false, "",
				"Exception in thread \"main\" java.lang.AssertionError: Postcondition does not hold\n" + 
				"	at Main.main$post(multiline_lambdas.java:4)\n" + 
				"	at Main.main(multiline_lambdas.java:8)\n");
		testCompile("old_resolvedType", true, "", "");
		testCompile("bad_return_type", false, "",
				"----------\n" + 
				"1. ERROR in SOURCE_FILE_FULL_PATH (at line 6)\n" + 
				"	static Foo<Object> foo() {}\n" + 
				"	       ^^^\n" + 
				"Incorrect number of arguments for type Foo<A,B>; it cannot be parameterized with arguments <Object>\n" + 
				"----------\n" + 
				"1 problem (1 error)\n");
	    testCompile("qualified_name_visibility_check", false, "",
	    		"----------\n" + 
	    		"1. ERROR in SOURCE_FILE_FULL_PATH (at line 6)\n" + 
	    		"	* @pre | bar.x != 0\n" + 
	    		"	         ^^^^^\n" + 
	    		"The field Foo.x is not visible\n" + 
	    		"----------\n" + 
	    		"1 problem (1 error)\n");
	    testCompile("bad_call", false, "",
	    		"----------\n" + 
	    		"1. ERROR in SOURCE_FILE_FULL_PATH (at line 4)\n" + 
	    		"	* @pre | xs.baz()\n" + 
	    		"	         ^^^^^^^^\n" + 
	    		"Cannot invoke baz() on the array type int[]\n" + 
	    		"----------\n" + 
	    		"1 problem (1 error)\n");
	    testCompile("nested_lambda", false, "",
	    		"----------\n" + 
	    		"1. ERROR in /Users/bartj/s4jie2/eclipse.jdt.core/org.eclipse.jdt.core/s4jie2-tests/src/nested_lambda.java (at line 9)\n" + 
	    		"	* @post | \n" + 
	    		"	  ^^^^^\n" + 
	    		"Expression expected in formal part\n" + 
	    		"----------\n" + 
	    		"1 problem (1 error)\n");
	    testCompile("throws_may_throw_syntax_error", false, "",
	    		"----------\n" + 
	    		"1. ERROR in SOURCE_FILE_FULL_PATH (at line 6)\n" + 
	    		"	*    | 10000 <=\n" + 
	    		"	             ^^\n" + 
	    		"Syntax error on token \"<=\", Expression expected after this token\n" + 
	    		"----------\n" + 
	    		"2. ERROR in SOURCE_FILE_FULL_PATH (at line 12)\n" + 
	    		"	*    | 5000 <=\n" + 
	    		"	            ^^\n" + 
	    		"Syntax error on token \"<=\", Expression expected after this token\n" + 
	    		"----------\n" + 
	    		"2 problems (2 errors)\n");
	    testCompile("throws_may_throw_resolve_error", false, "",
	    		"----------\n" + 
	    		"1. ERROR in SOURCE_FILE_FULL_PATH (at line 8)\n" + 
	    		"	* @throws IllegalArgumentException | 10000 <= y\n" + 
	    		"	                                              ^\n" + 
	    		"y cannot be resolved to a variable\n" + 
	    		"----------\n" + 
	    		"2. ERROR in SOURCE_FILE_FULL_PATH (at line 10)\n" + 
	    		"	*    | 10000 <= z\n" + 
	    		"	                ^\n" + 
	    		"The field Foo.z is not visible\n" + 
	    		"----------\n" + 
	    		"3. ERROR in SOURCE_FILE_FULL_PATH (at line 14)\n" + 
	    		"	*    | 5000 <= y\n" + 
	    		"	               ^\n" + 
	    		"y cannot be resolved to a variable\n" + 
	    		"----------\n" + 
	    		"4. ERROR in SOURCE_FILE_FULL_PATH (at line 15)\n" + 
	    		"	* @may_throw IllegalArgumentException | 5000 <= z \n" + 
	    		"	                                                ^\n" + 
	    		"The field Foo.z is not visible\n" + 
	    		"----------\n" + 
	    		"4 problems (4 errors)\n");
	    testCompileAndRun(true, "throws_may_throw_success", true,
	    		"Caught the IAE\n" + 
	    		"Caught the IAE\n", "");
	    testCompile("invariants_syntax_error", false, "",
	    		"----------\n" + 
	    		"1. ERROR in SOURCE_FILE_FULL_PATH (at line 2)\n" + 
	    		"	* @invar | 10 <\n" + 
	    		"	              ^\n" + 
	    		"Syntax error on token \"<\", Expression expected after this token\n" + 
	    		"----------\n" + 
	    		"2. ERROR in SOURCE_FILE_FULL_PATH (at line 7)\n" + 
	    		"	* @invar | 10 <\n" + 
	    		"	              ^\n" + 
	    		"Syntax error on token \"<\", Expression expected after this token\n" + 
	    		"----------\n" + 
	    		"3. ERROR in SOURCE_FILE_FULL_PATH (at line 12)\n" + 
	    		"	* @invar | 10 <\n" + 
	    		"	              ^\n" + 
	    		"Syntax error on token \"<\", Expression expected after this token\n" + 
	    		"----------\n" + 
	    		"3 problems (3 errors)\n");
	    testCompile("invariants_resolve_error", false, "",
	    		"----------\n" + 
	    		"1. ERROR in SOURCE_FILE_FULL_PATH (at line 2)\n" + 
	    		"	* @invar | 10 < true\n" + 
	    		"	           ^^^^^^^^^\n" + 
	    		"The operator < is undefined for the argument type(s) int, boolean\n" + 
	    		"----------\n" + 
	    		"2. ERROR in SOURCE_FILE_FULL_PATH (at line 3)\n" + 
	    		"	* @invar | 10 < y\n" + 
	    		"	                ^\n" + 
	    		"y cannot be resolved to a variable\n" + 
	    		"----------\n" + 
	    		"3. ERROR in SOURCE_FILE_FULL_PATH (at line 4)\n" + 
	    		"	* @invar | 10 < x\n" + 
	    		"	                ^\n" + 
	    		"The field invariants_resolve_error.x is not visible\n" + 
	    		"----------\n" + 
	    		"4. ERROR in SOURCE_FILE_FULL_PATH (at line 5)\n" + 
	    		"	* @invar | 10 < getX()\n" + 
	    		"	                ^^^^\n" + 
	    		"The method getX() from the type invariants_resolve_error is not visible\n" + 
	    		"----------\n" + 
	    		"5. ERROR in SOURCE_FILE_FULL_PATH (at line 10)\n" + 
	    		"	* @invar | 10 < true\n" + 
	    		"	           ^^^^^^^^^\n" + 
	    		"The operator < is undefined for the argument type(s) int, boolean\n" + 
	    		"----------\n" + 
	    		"6. ERROR in SOURCE_FILE_FULL_PATH (at line 11)\n" + 
	    		"	* @invar | 10 < y\n" + 
	    		"	                ^\n" + 
	    		"y cannot be resolved to a variable\n" + 
	    		"----------\n" + 
	    		"7. ERROR in SOURCE_FILE_FULL_PATH (at line 16)\n" + 
	    		"	* @invar | 10 < true\n" + 
	    		"	           ^^^^^^^^^\n" + 
	    		"The operator < is undefined for the argument type(s) int, boolean\n" + 
	    		"----------\n" + 
	    		"8. ERROR in SOURCE_FILE_FULL_PATH (at line 17)\n" + 
	    		"	* @invar | 10 < x\n" + 
	    		"	                ^\n" + 
	    		"The field invariants_resolve_error.x is not visible\n" + 
	    		"----------\n" + 
	    		"8 problems (8 errors)\n");
	    testCompileAndRun(true, "invariants_success", true, "", "");
	    testCompile("effect_clauses_syntax_error", false, "",
	    		"----------\n" + 
	    		"1. ERROR in SOURCE_FILE_FULL_PATH (at line 6)\n" + 
	    		"	* @inspects | this, ...stuff, other,\n" + 
	    		"	                                   ^\n" + 
	    		"Syntax error on token \",\", JavadocFormalPartExpression expected after this token\n" + 
	    		"----------\n" + 
	    		"2. ERROR in SOURCE_FILE_FULL_PATH (at line 11)\n" + 
	    		"	* @mutates | quux, bar(\n" + 
	    		"	                      ^\n" + 
	    		"Syntax error, insert \")\" to complete JavadocFormalPartExpression\n" + 
	    		"----------\n" + 
	    		"3. ERROR in SOURCE_FILE_FULL_PATH (at line 16)\n" + 
	    		"	* @mutates_properties | bar)\n" + 
	    		"	                           ^\n" + 
	    		"Syntax error on token \")\", delete this token\n" + 
	    		"----------\n" + 
	    		"4. ERROR in SOURCE_FILE_FULL_PATH (at line 21)\n" + 
	    		"	* @creates | result -\n" + 
	    		"	                    ^\n" + 
	    		"Syntax error on token \"-\", -- expected\n" + 
	    		"----------\n" + 
	    		"4 problems (4 errors)\n");
	    testCompile("effect_clauses_resolve_error", false, "",
	    		"----------\n" + 
	    		"1. ERROR in SOURCE_FILE_FULL_PATH (at line 8)\n" + 
	    		"	* @inspects | this, ...stuff, other, zazz, x\n" + 
	    		"	                                     ^^^^\n" + 
	    		"zazz cannot be resolved to a variable\n" + 
	    		"----------\n" + 
	    		"2. ERROR in SOURCE_FILE_FULL_PATH (at line 8)\n" + 
	    		"	* @inspects | this, ...stuff, other, zazz, x\n" + 
	    		"	                                           ^\n" + 
	    		"The field Foo.x is not visible\n" + 
	    		"----------\n" + 
	    		"3. ERROR in SOURCE_FILE_FULL_PATH (at line 9)\n" + 
	    		"	* @mutates | quux, bar(3), x\n" + 
	    		"	                   ^^^\n" + 
	    		"The method bar() in the type Foo is not applicable for the arguments (int)\n" + 
	    		"----------\n" + 
	    		"4. ERROR in SOURCE_FILE_FULL_PATH (at line 9)\n" + 
	    		"	* @mutates | quux, bar(3), x\n" + 
	    		"	                           ^\n" + 
	    		"The field Foo.x is not visible\n" + 
	    		"----------\n" + 
	    		"5. ERROR in SOURCE_FILE_FULL_PATH (at line 10)\n" + 
	    		"	* @mutates_properties | bar(), other, x\n" + 
	    		"	                               ^^^^^\n" + 
	    		"Method call expected\n" + 
	    		"----------\n" + 
	    		"6. ERROR in SOURCE_FILE_FULL_PATH (at line 10)\n" + 
	    		"	* @mutates_properties | bar(), other, x\n" + 
	    		"	                                      ^\n" + 
	    		"Method call expected\n" + 
	    		"----------\n" + 
	    		"7. ERROR in SOURCE_FILE_FULL_PATH (at line 10)\n" + 
	    		"	* @mutates_properties | bar(), other, x\n" + 
	    		"	                                      ^\n" + 
	    		"The field Foo.x is not visible\n" + 
	    		"----------\n" + 
	    		"7 problems (7 errors)\n");
	    testCompileAndRun(true, "effect_clauses_success", true, "", "");
	    testCompileAndRun(true, "abstract_methods", true, "Success!\n", "");
	    testCompileAndRun(true, "old_exception", true, "Success\nSuccess\n", "");
	    
		System.out.println("s4jie2TestSuite: All tests passed.");
	}
	
}
