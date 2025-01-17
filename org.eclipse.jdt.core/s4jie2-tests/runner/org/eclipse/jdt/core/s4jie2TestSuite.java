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
import java.util.ArrayList;

import org.eclipse.jdt.internal.compiler.batch.Main;

/**
 * @since 3.28
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
	
	public static void assertContainsSubstring(String str, String substr, String msg) {
		if (!normalize(str).contains(normalize(substr))) {
			System.err.println("FAIL " + msg + " is not as expected. The expected string is not a substring of the actual string");
			System.err.println("=== expected START ===");
			System.err.println(substr);
			System.err.println("=== expected END ===");
			System.err.println("=== actual START ===");
			System.err.println(str);
			System.err.println("=== actual END ===");
			System.exit(1);
		}
	}

	private static final String binPath = "s4jie2-tests/bin";
	private static final String multifileBinPath = "s4jie2-tests/bin_multifile";
	private static final String junitPath = System.getenv("JUNIT_PATH");
	private static final String junitPlatformConsoleStandalonePath = junitPath;
	private static final String pathSeparator = System.getProperty("path.separator");

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
			System.err.println(outWriter.toString().replace(fullPath, "SOURCE_FILE_FULL_PATH"));
			System.err.println("=== standard output end ===");
			System.err.println("=== standard error start ===");
			System.err.println(errWriter.toString().replace(fullPath, "SOURCE_FILE_FULL_PATH"));
			System.err.println("=== standard error end ===");
			System.exit(1);
		}
		assertEquals(outWriter.toString().replace(fullPath, "SOURCE_FILE_FULL_PATH"), outExpected, "standard output");
		assertEquals(errWriter.toString().replace(fullPath, "SOURCE_FILE_FULL_PATH"), errExpected, "standard error");
		System.out.println("PASS Test " + filename + " compile success");
	}

	@SuppressWarnings("deprecation")
	public static void testCompileMultifile(String rootDirectory, boolean expectedSuccess, String outExpected, String errExpected) {
		System.out.println("     Multifile test " + rootDirectory + " start");
		StringWriter outWriter = new StringWriter();
		StringWriter errWriter = new StringWriter();
		String path = "s4jie2-tests/src_multifile/" + rootDirectory;
		String fullPath = new File(path).getAbsolutePath();
		ArrayList<String> classPath = new ArrayList<>();
		classPath.add(junitPath);
		if (!rootDirectory.equals("logicalcollections"))
			classPath.add(multifileBinPath + "/logicalcollections");
		String classPathString = String.join(pathSeparator, classPath);
		String args = "-11 -cp " + classPathString + " -proc:none " + path + " -g -d " + multifileBinPath + "/" + rootDirectory;
		if (Main.compile(args, new PrintWriter(outWriter), new PrintWriter(errWriter)) != expectedSuccess) {
			System.err.println("FAIL compiler success: expected: " + expectedSuccess + "; actual: " + !expectedSuccess);
			System.err.println("=== standard output start ===");
			System.err.println(outWriter.toString().replace(fullPath, "SOURCE_ROOT_PATH"));
			System.err.println("=== standard output end ===");
			System.err.println("=== standard error start ===");
			System.err.println(errWriter.toString().replace(fullPath, "SOURCE_ROOT_PATH"));
			System.err.println("=== standard error end ===");
			System.exit(1);
		}
		assertEquals(outWriter.toString().replace(fullPath, "SOURCE_ROOT_PATH"), outExpected, "standard output");
		assertEquals(errWriter.toString().replace(fullPath, "SOURCE_ROOT_PATH"), errExpected, "standard error");
		System.out.println("PASS Multifile test " + rootDirectory + " compile success");
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
		Process process = new ProcessBuilder(System.getProperty("java.home") + "/bin/java", "-classpath", classpath, enableAssertions ? "-ea" : "-da", "Main").start();
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
	
	public static void testPartOfStringCompileAndRun(boolean enableAssertions, String filename, boolean expectedSuccess, String partOfOutExpected, String partOfErrExpected) throws IOException {
		testCompile(filename, true, "", "");

		String classpath = binPath+"/"+filename;
		Process process = new ProcessBuilder(System.getProperty("java.home") + "/bin/java", "-classpath", classpath, enableAssertions ? "-ea" : "-da", "Main").start();
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
		assertContainsSubstring(stdout, partOfOutExpected, "standard output");
		assertContainsSubstring(stderr, partOfErrExpected, "standard error");
		System.out.println("PASS Test "+ filename + " execution success");
	}

	public static void testCompileAndRunMultifile(String rootDirectory, boolean expectedSuccess, String outExpected, String errExpected) throws IOException {
		testCompileMultifile(rootDirectory, true, "", "");

		String classpath =
				//junitPath + pathSeparator +
				junitPlatformConsoleStandalonePath + pathSeparator +
				multifileBinPath + "/logicalcollections" + pathSeparator +
				multifileBinPath + "/" + rootDirectory;
		Process process = new ProcessBuilder(System.getProperty("java.home") + "/bin/java",
					"-classpath", classpath,
					"-ea",
					"org.junit.platform.console.ConsoleLauncher",
					"--exclude-engine=junit-platform-suite",
					"--config=junit.jupiter.testclass.order.default=org.junit.jupiter.api.ClassOrderer$DisplayName",
					"--config=junit.jupiter.testmethod.order.default=org.junit.jupiter.api.MethodOrderer$MethodName",
					"--fail-if-no-tests",
					"--disable-banner",
					"-details-theme=ascii",
					"--disable-ansi-colors",
					"--include-classname=.*",
					"--scan-classpath=" + multifileBinPath + "/" + rootDirectory
				).start();
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
		String stdout = stdoutBuffer.toString().replace("Thanks for using JUnit! Support its development at https://junit.org/sponsoring\n\n", "").trim().replaceFirst("Test run finished after [0-9]+ ms", "Test run finished after XX ms");
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
		System.out.println("PASS Multifile test "+ rootDirectory + " execution success");
	}

	public static void main(String[] args) throws IOException {
		if (new File(binPath).exists())
			deleteFileTree(binPath);
		if (new File(multifileBinPath).exists())
			deleteFileTree(multifileBinPath);
		
		testPartOfStringCompileAndRun(true, "throw_exception_not_specified", false, "", 
				"SEVERE: The thrown exception was not specified in the formal specification\n"
				+ "Exception in thread \"main\" java.lang.IllegalArgumentException\n"
				+ "	at Foo.bar(throw_exception_not_specified.java:7)\n"
				+ "	at Foo.bar$spec(throw_exception_not_specified.java)\n"
				+ "	at Main.main(throw_exception_not_specified.java:14)");
		testCompile("no_throw_exception_type", false, "",
				"----------\n" + 
				"1. ERROR in SOURCE_FILE_FULL_PATH (at line 4)\n" +
				"	* @throws | false\n" +
				"	  ^^^^^^^\n" +
				"Exception type expected after @throws tag\n" +
				"----------\n" +
				"1 problem (1 error)\n");
		testCompile("no_may_throw_exception_type", false, "",
				"----------\n" + 
				"1. ERROR in SOURCE_FILE_FULL_PATH (at line 4)\n" +
				"	* @may_throw | false\n" +
				"	  ^^^^^^^^^^\n" +
				"Exception type expected after @may_throw tag\n" +
				"----------\n" +
				"1 problem (1 error)\n");
		testCompileAndRun(true, "correct_throw_exception", false, "", 
				"Exception in thread \"main\" java.lang.IllegalArgumentException\n"
				+ "	at Foo.bar(correct_throw_exception.java:7)\n"
				+ "	at Foo.bar$spec(correct_throw_exception.java)\n"
				+ "	at Main.main(correct_throw_exception.java:14)\n");
		testPartOfStringCompileAndRun(true, "incorrect_throw_exception", false, "",
				"SEVERE: The thrown exception was not specified in the formal specification\n"
				+ "Exception in thread \"main\" java.lang.IllegalArgumentException\n"
				+ "	at Foo.bar(incorrect_throw_exception.java:7)\n"
				+ "	at Foo.bar$spec(incorrect_throw_exception.java)\n"
				+ "	at Main.main(incorrect_throw_exception.java:14)\n");
		testCompileAndRun(true, "incorrect_throw_exception_no_throw", true, "", "");
		testCompileAndRun(true, "correct_non_trivial_throw_exception", false, "", 
				"Exception in thread \"main\" java.lang.IllegalArgumentException\n"
				+ "	at Foo.bar(correct_non_trivial_throw_exception.java:8)\n"
				+ "	at Foo.bar$spec(correct_non_trivial_throw_exception.java)\n"
				+ "	at Main.main(correct_non_trivial_throw_exception.java:15)\n");
		testCompileAndRun(true, "correct_non_trivial_throw_exception_no_throw", true, "", "");
		testCompileAndRun(true, "correct_may_throw_exception", false, "", 
				"Exception in thread \"main\" java.lang.IllegalArgumentException\n"
				+ "	at Foo.bar(correct_may_throw_exception.java:7)\n"
				+ "	at Foo.bar$spec(correct_may_throw_exception.java)\n"
				+ "	at Main.main(correct_may_throw_exception.java:14)\n");
		testCompileAndRun(true, "correct_throw_may_throw_exception", false, "",
				"Exception in thread \"main\" java.lang.IllegalArgumentException\n"
				+ "	at Foo.bar(correct_throw_may_throw_exception.java:8)\n"
				+ "	at Foo.bar$spec(correct_throw_may_throw_exception.java)\n"
				+ "	at Main.main(correct_throw_may_throw_exception.java:15)\n");
		testCompileAndRun(true, "no_exception_throw_condition_satisfied", false, 
				"",
				"Exception in thread \"main\" java.lang.AssertionError: @throws condition holds but specified exception type not thrown\n"
				+ "	at Main.foo$post(no_exception_throw_condition_satisfied.java:6)\n"
				+ "	at Main.foo(no_exception_throw_condition_satisfied.java:7)\n"
				+ "	at Main.main(no_exception_throw_condition_satisfied.java:11)\n");
		testPartOfStringCompileAndRun(true, "wrong_exception_throw_condition_satisfied", false, "", 
				"SEVERE: @throws condition holds but specified exception type not thrown\n");
		testPartOfStringCompileAndRun(true, "wrong_exception_throw_condition_satisfied", false, "", 
				"SEVERE: The thrown exception was not specified in the formal specification\n"
				+ "Exception in thread \"main\" java.lang.ArithmeticException\n"
				+ "	at Main.foo(wrong_exception_throw_condition_satisfied.java:7)\n"
				+ "	at Main.main(wrong_exception_throw_condition_satisfied.java:11)\n");
		testCompileAndRun(true, "no_throw_may_throw_condition_satisfied", true, "", "");
		testCompileAndRun(true, "no_throw_may_throw_condition_not_satisfied", true, "", "");
		testCompileAndRun(true, "multiple_throw_conditions_satisfied_no_throw", false, "",
				"Exception in thread \"main\" java.lang.AssertionError: @throws condition holds but specified exception type not thrown\n"
				+ "	at Main.foo$post(multiple_throw_conditions_satisfied_no_throw.java:7)\n"
				+ "	at Main.foo(multiple_throw_conditions_satisfied_no_throw.java:8)\n"
				+ "	at Main.main(multiple_throw_conditions_satisfied_no_throw.java:12)\n");
		testPartOfStringCompileAndRun(true, "multiple_throw_conditions_satisfied_throw_second", false, "",
				"SEVERE: @throws condition holds but specified exception type not thrown\n"
				+ "Exception in thread \"main\" java.lang.ArithmeticException\n"
				+ "	at Main.foo(multiple_throw_conditions_satisfied_throw_second.java:8)\n"
				+ "	at Main.main(multiple_throw_conditions_satisfied_throw_second.java:12)\n");
		testCompileAndRun(true, "multiple_throw_conditions_satisfied_throw_first", false, "",
				"Exception in thread \"main\" java.lang.IllegalArgumentException\n"
				+ "	at Main.foo(multiple_throw_conditions_satisfied_throw_first.java:8)\n"
				+ "	at Main.main(multiple_throw_conditions_satisfied_throw_first.java:12)\n");
		testCompileAndRun(true, "multiple_throw_conditions_first_satisfied_thrown", false, "", 
				"Exception in thread \"main\" java.lang.IllegalArgumentException\n"
				+ "	at Main.foo(multiple_throw_conditions_first_satisfied_thrown.java:8)\n"
				+ "	at Main.main(multiple_throw_conditions_first_satisfied_thrown.java:12)\n");
		testCompileAndRun(true, "multiple_throw_conditions_second_satisfied_thrown", false, "", 
				"Exception in thread \"main\" java.lang.ArithmeticException\n"
				+ "	at Main.foo(multiple_throw_conditions_second_satisfied_thrown.java:8)\n"
				+ "	at Main.main(multiple_throw_conditions_second_satisfied_thrown.java:12)\n");
		testCompileAndRun(true, "multiple_throw_conditions_none_satisfied_none_thrown", true, "", "");
		testCompileAndRun(true, "multiple_throw_conditions_first_satisfied_none_thrown", false, "", 
				"Exception in thread \"main\" java.lang.AssertionError: @throws condition holds but specified exception type not thrown\n"
				+ "	at Main.foo$post(multiple_throw_conditions_first_satisfied_none_thrown.java:7)\n"
				+ "	at Main.foo(multiple_throw_conditions_first_satisfied_none_thrown.java:8)\n"
				+ "	at Main.main(multiple_throw_conditions_first_satisfied_none_thrown.java:12)\n");
		testCompileAndRun(true, "multiple_throw_conditions_second_satisfied_none_thrown", false, "", 
				"Exception in thread \"main\" java.lang.AssertionError: @throws condition holds but specified exception type not thrown\n"
				+ "	at Main.foo$post(multiple_throw_conditions_second_satisfied_none_thrown.java:7)\n"
				+ "	at Main.foo(multiple_throw_conditions_second_satisfied_none_thrown.java:8)\n"
				+ "	at Main.main(multiple_throw_conditions_second_satisfied_none_thrown.java:12)\n");
		testPartOfStringCompileAndRun(true, "multiple_throw_conditions_none_satisfied_first_thrown", false, "", 
				"SEVERE: The thrown exception was not specified in the formal specification\n"
				+ "Exception in thread \"main\" java.lang.IllegalArgumentException\n"
				+ "	at Main.foo(multiple_throw_conditions_none_satisfied_first_thrown.java:8)\n"
				+ "	at Main.main(multiple_throw_conditions_none_satisfied_first_thrown.java:12)\n");
		testPartOfStringCompileAndRun(true, "multiple_throw_conditions_none_satisfied_second_thrown", false, "",
				"SEVERE: The thrown exception was not specified in the formal specification\n"
				+ "Exception in thread \"main\" java.lang.ArithmeticException\n"
				+ "	at Main.foo(multiple_throw_conditions_none_satisfied_second_thrown.java:8)\n"
				+ "	at Main.main(multiple_throw_conditions_none_satisfied_second_thrown.java:12)\n");
		testCompileAndRun(true, "multiple_may_throw_conditions_none_satisfied_no_throw", true, "", "");
		testCompileAndRun(true, "multiple_may_throw_conditions_first_satisfied_no_throw", true, "", "");
		testCompileAndRun(true, "multiple_may_throw_conditions_second_satisfied_no_throw", true, "", "");
		testCompileAndRun(true, "multiple_may_throw_conditions_satisfied_no_throw", true, "", "");
		testPartOfStringCompileAndRun(true, "multiple_may_throw_conditions_none_satisfied_first_thrown", false, "", 
				"SEVERE: The thrown exception was not specified in the formal specification\n"
				+ "Exception in thread \"main\" java.lang.IllegalArgumentException\n"
				+ "	at Main.foo(multiple_may_throw_conditions_none_satisfied_first_thrown.java:8)\n"
				+ "	at Main.main(multiple_may_throw_conditions_none_satisfied_first_thrown.java:12)\n");
		testPartOfStringCompileAndRun(true, "multiple_may_throw_conditions_none_satisfied_second_thrown", false, "",
				"SEVERE: The thrown exception was not specified in the formal specification\n"
				+ "Exception in thread \"main\" java.lang.ArithmeticException\n"
				+ "	at Main.foo(multiple_may_throw_conditions_none_satisfied_second_thrown.java:8)\n"
				+ "	at Main.main(multiple_may_throw_conditions_none_satisfied_second_thrown.java:12)\n");
		testCompileAndRun(true, "multiple_may_throw_conditions_first_satisfied_first_thrown", false, "", 
				"Exception in thread \"main\" java.lang.IllegalArgumentException\n"
				+ "	at Main.foo(multiple_may_throw_conditions_first_satisfied_first_thrown.java:8)\n"
				+ "	at Main.main(multiple_may_throw_conditions_first_satisfied_first_thrown.java:12)\n");
		testPartOfStringCompileAndRun(true, "multiple_may_throw_conditions_first_satisfied_second_thrown", false, "", 
				"SEVERE: The thrown exception was not specified in the formal specification\n"
				+ "Exception in thread \"main\" java.lang.ArithmeticException\n"
				+ "	at Main.foo(multiple_may_throw_conditions_first_satisfied_second_thrown.java:8)\n"
				+ "	at Main.main(multiple_may_throw_conditions_first_satisfied_second_thrown.java:12)\n");
		testPartOfStringCompileAndRun(true, "multiple_may_throw_conditions_second_satisfied_first_thrown", false, "", 
				"SEVERE: The thrown exception was not specified in the formal specification\n"
				+ "Exception in thread \"main\" java.lang.IllegalArgumentException\n"
				+ "	at Main.foo(multiple_may_throw_conditions_second_satisfied_first_thrown.java:8)\n"
				+ "	at Main.main(multiple_may_throw_conditions_second_satisfied_first_thrown.java:12)");
		testCompileAndRun(true, "multiple_may_throw_conditions_second_satisfied_second_thrown", false, "", 
				"Exception in thread \"main\" java.lang.ArithmeticException\n"
				+ "	at Main.foo(multiple_may_throw_conditions_second_satisfied_second_thrown.java:8)\n"
				+ "	at Main.main(multiple_may_throw_conditions_second_satisfied_second_thrown.java:12)\n");
		testCompileAndRun(true, "multiple_may_throw_conditions_satisfied_first_thrown", false, "", 
				"Exception in thread \"main\" java.lang.IllegalArgumentException\n"
				+ "	at Main.foo(multiple_may_throw_conditions_satisfied_first_thrown.java:8)\n"
				+ "	at Main.main(multiple_may_throw_conditions_satisfied_first_thrown.java:12)\n");
		testCompileAndRun(true, "multiple_may_throw_conditions_satisfied_second_thrown", false, "", 
				"Exception in thread \"main\" java.lang.ArithmeticException\n"
				+ "	at Main.foo(multiple_may_throw_conditions_satisfied_second_thrown.java:8)\n"
				+ "	at Main.main(multiple_may_throw_conditions_satisfied_second_thrown.java:12)\n");
		testCompileAndRun(true, "multiple_non_trivial_throw_conditions_first_satisfied_no_throw", false, "", 
				"Exception in thread \"main\" java.lang.AssertionError: @throws condition holds but specified exception type not thrown\n"
				+ "	at Main.foo$post(multiple_non_trivial_throw_conditions_first_satisfied_no_throw.java:7)\n"
				+ "	at Main.foo(multiple_non_trivial_throw_conditions_first_satisfied_no_throw.java:8)\n"
				+ "	at Main.main(multiple_non_trivial_throw_conditions_first_satisfied_no_throw.java:12)\n");
		testCompileAndRun(true, "multiple_non_trivial_throws_first_thrown", false, "", 
				"Exception in thread \"main\" java.lang.IllegalArgumentException\n"
				+ "	at Main.foo(multiple_non_trivial_throws_first_thrown.java:8)\n"
				+ "	at Main.main(multiple_non_trivial_throws_first_thrown.java:12)\n");
		testPartOfStringCompileAndRun(true, "multiple_non_trivial_incorrect_throws_first_thrown", false, "", 
				"SEVERE: The thrown exception was not specified in the formal specification\n"
				+ "Exception in thread \"main\" java.lang.IllegalArgumentException\n"
				+ "	at Main.foo(multiple_non_trivial_incorrect_throws_first_thrown.java:8)\n"
				+ "	at Main.main(multiple_non_trivial_incorrect_throws_first_thrown.java:12)\n");
		testCompileAndRun(true, "incorrect_non_trivial_throws_no_throw", true, "", "");
		testPartOfStringCompileAndRun(true, "incorrect_non_trivial_throws_second_thrown", false, "",
				"SEVERE: The thrown exception was not specified in the formal specification\n"
				+ "Exception in thread \"main\" java.lang.ArithmeticException\n"
				+ "	at Main.foo(incorrect_non_trivial_throws_second_thrown.java:8)\n"
				+ "	at Main.main(incorrect_non_trivial_throws_second_thrown.java:12)\n");
		testCompileAndRun(true, "non_trivial_throws_no_throw", false, "", 
				"Exception in thread \"main\" java.lang.AssertionError: @throws condition holds but specified exception type not thrown\n"
				+ "	at Main.foo$post(non_trivial_throws_no_throw.java:7)\n"
				+ "	at Main.foo(non_trivial_throws_no_throw.java:8)\n"
				+ "	at Main.main(non_trivial_throws_no_throw.java:12)\n");
		testPartOfStringCompileAndRun(true, "non_trivial_throws_first_thrown", false, "", 
				"Exception in thread \"main\" java.lang.IllegalArgumentException\n"
				+ "	at Main.foo(non_trivial_throws_first_thrown.java:8)\n"
				+ "	at Main.main(non_trivial_throws_first_thrown.java:12)\n");
		testPartOfStringCompileAndRun(true, "non_trivial_throws_second_thrown", false, "", 
				"SEVERE: @throws condition holds but specified exception type not thrown\n"
				+ "Exception in thread \"main\" java.lang.ArithmeticException\n"
				+ "	at Main.foo(non_trivial_throws_second_thrown.java:9)\n"
				+ "	at Main.main(non_trivial_throws_second_thrown.java:13)\n");
		testCompileAndRun(true, "non_trivial_throws_second_correct_no_throw", false, "", 
				"Exception in thread \"main\" java.lang.AssertionError: @throws condition holds but specified exception type not thrown\n"
				+ "	at Main.foo$post(non_trivial_throws_second_correct_no_throw.java:7)\n"
				+ "	at Main.foo(non_trivial_throws_second_correct_no_throw.java:8)\n"
				+ "	at Main.main(non_trivial_throws_second_correct_no_throw.java:12)\n");
		testCompileAndRun(true, "non_trivial_throws_second_correct_second_thrown", false, "", 
				"Exception in thread \"main\" java.lang.ArithmeticException\n"
				+ "	at Main.foo(non_trivial_throws_second_correct_second_thrown.java:8)\n"
				+ "	at Main.main(non_trivial_throws_second_correct_second_thrown.java:12)\n");

		
		testCompile("Minimal", true, "", "");
		
		testCompileAndRun(false, "GameCharacter_pre", true, "",
				"No exception was thrown! :-(\n" +
				"No exception was thrown! :-(\n");
		testCompileAndRun(true, "GameCharacter_pre", true, "",
				  "java.lang.AssertionError: Precondition does not hold\n"
				  + "	at GameCharacter.takeDamage$pre(GameCharacter_pre.java:20)\n"
				  + "	at GameCharacter.takeDamage$spec(GameCharacter_pre.java)\n"
				  + "	at Main.main(GameCharacter_pre.java:36)\n"
				  + "java.lang.AssertionError: Precondition does not hold\n"
				  + "	at GameCharacter.<init>(GameCharacter_pre.java:9)\n"
				  + "	at Main.main(GameCharacter_pre.java:44)\n");
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
				"	at GameCharacter.takeDamage$spec(GameCharacter_pre_post.java)\n" +
				"	at Main.main(GameCharacter_pre_post.java:98)\n" +
				"java.lang.AssertionError: Postcondition does not hold\n" +
				"	at GameCharacter.setHealth$post(GameCharacter_pre_post.java:13)\n" +
				"	at GameCharacter.setHealth(GameCharacter_pre_post.java:17)\n" +
				"	at GameCharacter.setHealth$spec(GameCharacter_pre_post.java)\n" +
				"	at Main.main(GameCharacter_pre_post.java:105)\n" +
				"java.lang.AssertionError: Postcondition does not hold\n" +
				"	at GameCharacter.simpleReturnTest$post(GameCharacter_pre_post.java:54)\n" +
				"	at GameCharacter.simpleReturnTest(GameCharacter_pre_post.java:58)\n" +
				"	at GameCharacter.simpleReturnTest$spec(GameCharacter_pre_post.java)\n" +
				"	at Main.main(GameCharacter_pre_post.java:112)\n" +
				"java.lang.AssertionError: Postcondition does not hold\n" +
				"	at GameCharacter.returnInsideIfTest$post(GameCharacter_pre_post.java:62)\n" +
				"	at GameCharacter.returnInsideIfTest(GameCharacter_pre_post.java:67)\n" +
				"	at GameCharacter.returnInsideIfTest$spec(GameCharacter_pre_post.java)\n" +
				"	at Main.main(GameCharacter_pre_post.java:119)\n" +
				"java.lang.AssertionError: Postcondition does not hold\n" +
				"	at GameCharacter.returnInsideIfTest$post(GameCharacter_pre_post.java:62)\n" +
				"	at GameCharacter.returnInsideIfTest(GameCharacter_pre_post.java:70)\n" +
				"	at GameCharacter.returnInsideIfTest$spec(GameCharacter_pre_post.java)\n" +
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
	    		"1. ERROR in SOURCE_FILE_FULL_PATH (at line 9)\n" +
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
	    		"----------\n"
	    		+ "1. ERROR in SOURCE_FILE_FULL_PATH (at line 8)\n"
	    		+ "	* @throws IllegalArgumentException | 10000 <= y\n"
	    		+ "	                                     ^^^^^^^^^^\n"
	    		+ "Type mismatch: cannot convert from Object to boolean\n"
	    		+ "----------\n"
	    		+ "2. ERROR in SOURCE_FILE_FULL_PATH (at line 8)\n"
	    		+ "	* @throws IllegalArgumentException | 10000 <= y\n"
	    		+ "	                                              ^\n"
	    		+ "y cannot be resolved to a variable\n"
	    		+ "----------\n"
	    		+ "3. ERROR in SOURCE_FILE_FULL_PATH (at line 11)\n"
	    		+ "	* @throws IlegalArgumentExeption | true\n"
	    		+ "	          ^^^^^^^^^^^^^^^^^^^^^^\n"
	    		+ "IlegalArgumentExeption cannot be resolved to a type\n"
	    		+ "----------\n"
	    		+ "4. ERROR in SOURCE_FILE_FULL_PATH (at line 15)\n"
	    		+ "	*    | 5000 <= y\n"
	    		+ "	       ^^^^^^^^^\n"
	    		+ "Type mismatch: cannot convert from Object to boolean\n"
	    		+ "----------\n"
	    		+ "5. ERROR in SOURCE_FILE_FULL_PATH (at line 15)\n"
	    		+ "	*    | 5000 <= y\n"
	    		+ "	               ^\n"
	    		+ "y cannot be resolved to a variable\n"
	    		+ "----------\n"
	    		+ "5 problems (5 errors)\n"
	    		+ "");
	    testPartOfStringCompileAndRun(true, "throws_may_throw_success", true,
	    		"Caught the IAE\n" +
	    		"Caught the IAE\n",
	    		"SEVERE: The thrown exception was not specified in the formal specification\n"
	    		+ "java.lang.AssertionError: @throws condition holds but specified exception type not thrown\n"
	    		+ "	at Main.foo$post(throws_may_throw_success.java:8)\n"
	    		+ "	at Main.foo(throws_may_throw_success.java:13)\n"
	    		+ "	at Main.main(throws_may_throw_success.java:33)\n"
	    		+ "java.lang.AssertionError: @throws condition holds but specified exception type not thrown\n"
	    		+ "	at Main.foo$post(throws_may_throw_success.java:8)\n"
	    		+ "	at Main.foo(throws_may_throw_success.java:13)\n"
	    		+ "	at Main.main(throws_may_throw_success.java:42)\n");
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
	    		"Syntax error on token \",\", Expression expected after this token\n" +
	    		"----------\n" +
	    		"2. ERROR in SOURCE_FILE_FULL_PATH (at line 11)\n" +
	    		"	* @mutates | quux, bar(\n" +
	    		"	                      ^\n" +
	    		"Syntax error, insert \")\" to complete Expression\n" +
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
	    		"1. ERROR in SOURCE_FILE_FULL_PATH (at line 10)\n" +
	    		"	* @inspects | this, ...stuff, other, zazz, x\n" +
	    		"	                                     ^^^^\n" +
	    		"zazz cannot be resolved to a variable\n" +
	    		"----------\n" +
	    		"2. ERROR in SOURCE_FILE_FULL_PATH (at line 10)\n" +
	    		"	* @inspects | this, ...stuff, other, zazz, x\n" +
	    		"	                                           ^\n" +
	    		"The field Foo.x is not visible\n" +
	    		"----------\n" +
	    		"3. ERROR in SOURCE_FILE_FULL_PATH (at line 11)\n" +
	    		"	* @mutates | quux, bar(3), ...x, x\n" +
	    		"	                   ^^^\n" +
	    		"The method bar() in the type Foo is not applicable for the arguments (int)\n" +
	    		"----------\n" +
	    		"4. ERROR in SOURCE_FILE_FULL_PATH (at line 11)\n" +
	    		"	* @mutates | quux, bar(3), ...x, x\n" +
	    		"	                              ^\n" +
	    		"The field Foo.x is not visible\n" +
	    		"----------\n" +
	    		"5. ERROR in SOURCE_FILE_FULL_PATH (at line 11)\n" +
	    		"	* @mutates | quux, bar(3), ...x, x\n" +
	    		"	                                 ^\n" +
	    		"The field Foo.x is not visible\n" +
	    		"----------\n" +
	    		"6. ERROR in SOURCE_FILE_FULL_PATH (at line 12)\n" +
	    		"	* @mutates_properties | bar(), baz(3), other, (...x).bar(), x, (...stuff).quux()\n" +
	    		"	                               ^^^^^^\n" +
	    		"Method calls with arguments are not supported here\n" +
	    		"----------\n" +
	    		"7. ERROR in SOURCE_FILE_FULL_PATH (at line 12)\n" +
	    		"	* @mutates_properties | bar(), baz(3), other, (...x).bar(), x, (...stuff).quux()\n" +
	    		"	                                       ^^^^^\n" +
	    		"Method call expected\n" +
	    		"----------\n" +
	    		"8. ERROR in SOURCE_FILE_FULL_PATH (at line 12)\n" +
	    		"	* @mutates_properties | bar(), baz(3), other, (...x).bar(), x, (...stuff).quux()\n" +
	    		"	                                                  ^\n" +
	    		"Can only iterate over an array or an instance of java.lang.Iterable\n" +
	    		"----------\n" +
	    		"9. ERROR in SOURCE_FILE_FULL_PATH (at line 12)\n" +
	    		"	* @mutates_properties | bar(), baz(3), other, (...x).bar(), x, (...stuff).quux()\n" +
	    		"	                                                  ^\n" +
	    		"The field Foo.x is not visible\n" +
	    		"----------\n" +
	    		"10. ERROR in SOURCE_FILE_FULL_PATH (at line 12)\n" +
	    		"	* @mutates_properties | bar(), baz(3), other, (...x).bar(), x, (...stuff).quux()\n" +
	    		"	                                                     ^^^\n" +
	    		"The method bar() is undefined for the type Object\n" +
	    		"----------\n" +
	    		"11. ERROR in SOURCE_FILE_FULL_PATH (at line 12)\n" +
	    		"	* @mutates_properties | bar(), baz(3), other, (...x).bar(), x, (...stuff).quux()\n" +
	    		"	                                                            ^\n" +
	    		"Method call expected\n" +
	    		"----------\n" +
	    		"12. ERROR in SOURCE_FILE_FULL_PATH (at line 12)\n" +
	    		"	* @mutates_properties | bar(), baz(3), other, (...x).bar(), x, (...stuff).quux()\n" +
	    		"	                                                                          ^^^^\n" +
	    		"The method quux() is undefined for the type Foo\n" +
	    		"----------\n" +
	    		"12 problems (12 errors)\n");
	    testCompileAndRun(true, "effect_clauses_success", true, "", "");
	    testCompileAndRun(true, "abstract_methods", true, "Success!\n",
	    		  "java.lang.AssertionError: Precondition does not hold\n"
	    		+ "	at Foo.foo$pre(abstract_methods.java:6)\n"
	    		+ "	at Foo.foo$spec(abstract_methods.java)\n"
	    		+ "	at Main.main(abstract_methods.java:62)\n"
	    		+ "java.lang.AssertionError: Postcondition does not hold\n"
	    		+ "	at Foo.foo$post(abstract_methods.java:7)\n"
	    		+ "	at Foo.foo$spec(abstract_methods.java)\n"
	    		+ "	at Main.main(abstract_methods.java:69)\n"
	    		+ "java.lang.AssertionError: Postcondition does not hold\n"
	    		+ "	at Foo.foo$post(abstract_methods.java:8)\n"
	    		+ "	at Foo.foo$spec(abstract_methods.java)\n"
	    		+ "	at Main.main(abstract_methods.java:76)\n");
	    testCompileAndRun(true, "old_exception", true, "Success\nSuccess\n", "");
	    testCompile("issue16", false, "", "----------\n" +
	    		"1. ERROR in SOURCE_FILE_FULL_PATH (at line 2)\n" +
	    		"	* @invar | ( */\n" +
	    		"	           ^\n" +
	    		"Syntax error on token \"(\", delete this token\n" +
	    		"----------\n" +
	    		"1 problem (1 error)\n");
	    testCompileMultifile("logicalcollections", true, "", "");
	    testCompileAndRunMultifile("abstract_methods", true,
	    		  ".\n"
	    		+ "+-- JUnit Jupiter [OK]\n"
	    		+ "| '-- AbstractMethodsTest [OK]\n"
	    		+ "|   '-- test() [OK]\n"
	    		+ "'-- JUnit Vintage [OK]\n"
	    		+ "\n"
	    		+ "Test run finished after XX ms\n"
	    		+ "[         3 containers found      ]\n"
	    		+ "[         0 containers skipped    ]\n"
	    		+ "[         3 containers started    ]\n"
	    		+ "[         0 containers aborted    ]\n"
	    		+ "[         3 containers successful ]\n"
	    		+ "[         0 containers failed     ]\n"
	    		+ "[         1 tests found           ]\n"
	    		+ "[         0 tests skipped         ]\n"
	    		+ "[         1 tests started         ]\n"
	    		+ "[         0 tests aborted         ]\n"
	    		+ "[         1 tests successful      ]\n"
	    		+ "[         0 tests failed          ]", "");
	    testCompileAndRunMultifile("fractions", true,
	    		".\n" +
	    		"+-- JUnit Jupiter [OK]\n" +
	    		"| +-- FractionContainerTest [OK]\n" +
	    		"| | +-- testAdd() [OK]\n" +
	    		"| | +-- testEquals() [OK]\n" +
	    		"| | '-- testFinancial() [OK]\n" +
	    		"| '-- FractionTest [OK]\n" +
	    		"|   '-- test() [OK]\n" +
	    		"'-- JUnit Vintage [OK]\n" +
	    		"\n" +
	    		"Test run finished after XX ms\n" +
	    		"[         4 containers found      ]\n" +
	    		"[         0 containers skipped    ]\n" +
	    		"[         4 containers started    ]\n" +
	    		"[         0 containers aborted    ]\n" +
	    		"[         4 containers successful ]\n" +
	    		"[         0 containers failed     ]\n" +
	    		"[         4 tests found           ]\n" +
	    		"[         0 tests skipped         ]\n" +
	    		"[         4 tests started         ]\n" +
	    		"[         0 tests aborted         ]\n" +
	    		"[         4 tests successful      ]\n" +
	    		"[         0 tests failed          ]", "");
	    testCompileAndRunMultifile("teams", true,
	    		".\n" +
	    		"+-- JUnit Jupiter [OK]\n" +
	    		"| '-- TeamsTest [OK]\n" +
	    		"|   '-- test() [OK]\n" +
	    		"'-- JUnit Vintage [OK]\n" +
	    		"\n" +
	    		"Test run finished after XX ms\n" +
	    		"[         3 containers found      ]\n" +
	    		"[         0 containers skipped    ]\n" +
	    		"[         3 containers started    ]\n" +
	    		"[         0 containers aborted    ]\n" +
	    		"[         3 containers successful ]\n" +
	    		"[         0 containers failed     ]\n" +
	    		"[         1 tests found           ]\n" +
	    		"[         0 tests skipped         ]\n" +
	    		"[         1 tests started         ]\n" +
	    		"[         0 tests aborted         ]\n" +
	    		"[         1 tests successful      ]\n" +
	    		"[         0 tests failed          ]", "");
	    testCompileAndRunMultifile("bigteams", true,
	    		".\n" +
	    		"+-- JUnit Jupiter [OK]\n" +
	    		"| '-- BigTeamsTest [OK]\n" +
	    		"|   '-- test() [OK]\n" +
	    		"'-- JUnit Vintage [OK]\n" +
	    		"\n" +
	    		"Test run finished after XX ms\n" +
	    		"[         3 containers found      ]\n" +
	    		"[         0 containers skipped    ]\n" +
	    		"[         3 containers started    ]\n" +
	    		"[         0 containers aborted    ]\n" +
	    		"[         3 containers successful ]\n" +
	    		"[         0 containers failed     ]\n" +
	    		"[         1 tests found           ]\n" +
	    		"[         0 tests skipped         ]\n" +
	    		"[         1 tests started         ]\n" +
	    		"[         0 tests aborted         ]\n" +
	    		"[         1 tests successful      ]\n" +
	    		"[         0 tests failed          ]", "");
	    testCompileAndRunMultifile("bigteams_nested_abs", true,
	    		".\n" +
	    		"+-- JUnit Jupiter [OK]\n" +
	    		"| '-- BigTeamsTest [OK]\n" +
	    		"|   '-- test() [OK]\n" +
	    		"'-- JUnit Vintage [OK]\n" +
	    		"\n" +
	    		"Test run finished after XX ms\n" +
	    		"[         3 containers found      ]\n" +
	    		"[         0 containers skipped    ]\n" +
	    		"[         3 containers started    ]\n" +
	    		"[         0 containers aborted    ]\n" +
	    		"[         3 containers successful ]\n" +
	    		"[         0 containers failed     ]\n" +
	    		"[         1 tests found           ]\n" +
	    		"[         0 tests skipped         ]\n" +
	    		"[         1 tests started         ]\n" +
	    		"[         0 tests aborted         ]\n" +
	    		"[         1 tests successful      ]\n" +
	    		"[         0 tests failed          ]", "");
	    testCompileAndRunMultifile("html", true,
	    		".\n" +
	    		"+-- JUnit Jupiter [OK]\n" +
	    		"| '-- HtmlTest [OK]\n" +
	    		"|   '-- test() [OK]\n" +
	    		"'-- JUnit Vintage [OK]\n" +
	    		"\n" +
	    		"Test run finished after XX ms\n" +
	    		"[         3 containers found      ]\n" +
	    		"[         0 containers skipped    ]\n" +
	    		"[         3 containers started    ]\n" +
	    		"[         0 containers aborted    ]\n" +
	    		"[         3 containers successful ]\n" +
	    		"[         0 containers failed     ]\n" +
	    		"[         1 tests found           ]\n" +
	    		"[         0 tests skipped         ]\n" +
	    		"[         1 tests started         ]\n" +
	    		"[         0 tests aborted         ]\n" +
	    		"[         1 tests successful      ]\n" +
	    		"[         0 tests failed          ]", "");
	    testCompileAndRunMultifile("networks", true,
	    		".\n" +
	    		"+-- JUnit Jupiter [OK]\n" +
	    		"| +-- NodeAppearancesTest [OK]\n" +
	    		"| | '-- test() [OK]\n" +
	    		"| '-- NodesTest [OK]\n" +
	    		"|   '-- test() [OK]\n" +
	    		"'-- JUnit Vintage [OK]\n" +
	    		"\n" +
	    		"Test run finished after XX ms\n" +
	    		"[         4 containers found      ]\n" +
	    		"[         0 containers skipped    ]\n" +
	    		"[         4 containers started    ]\n" +
	    		"[         0 containers aborted    ]\n" +
	    		"[         4 containers successful ]\n" +
	    		"[         0 containers failed     ]\n" +
	    		"[         2 tests found           ]\n" +
	    		"[         0 tests skipped         ]\n" +
	    		"[         2 tests started         ]\n" +
	    		"[         0 tests aborted         ]\n" +
	    		"[         2 tests successful      ]\n" +
	    		"[         0 tests failed          ]", "");
	    testCompileAndRunMultifile("exams_rooms", true,
	    		".\n" +
	    		"+-- JUnit Jupiter [OK]\n" +
	    		"| '-- ExamsRoomsTest [OK]\n" +
	    		"|   '-- test() [OK]\n" +
	    		"'-- JUnit Vintage [OK]\n" +
	    		"\n" +
	    		"Test run finished after XX ms\n" +
	    		"[         3 containers found      ]\n" +
	    		"[         0 containers skipped    ]\n" +
	    		"[         3 containers started    ]\n" +
	    		"[         0 containers aborted    ]\n" +
	    		"[         3 containers successful ]\n" +
	    		"[         0 containers failed     ]\n" +
	    		"[         1 tests found           ]\n" +
	    		"[         0 tests skipped         ]\n" +
	    		"[         1 tests started         ]\n" +
	    		"[         0 tests aborted         ]\n" +
	    		"[         1 tests successful      ]\n" +
	    		"[         0 tests failed          ]", "");
	    testCompileAndRunMultifile("drawit", true,
	    		".\n"
	    		+ "+-- JUnit Jupiter [OK]\n"
	    		+ "| +-- DoublePointTest [OK]\n"
	    		+ "| | +-- testConstructorAndGetters() [OK]\n"
	    		+ "| | +-- testMinus() [OK]\n"
	    		+ "| | +-- testPlus() [OK]\n"
	    		+ "| | '-- testRound() [OK]\n"
	    		+ "| +-- DoubleVectorTest [OK]\n"
	    		+ "| | +-- testAsAngle() [OK]\n"
	    		+ "| | +-- testConstructorAndGetters() [OK]\n"
	    		+ "| | +-- testCrossProduct() [OK]\n"
	    		+ "| | +-- testDotProduct() [OK]\n"
	    		+ "| | +-- testGetSize() [OK]\n"
	    		+ "| | +-- testPlus() [OK]\n"
	    		+ "| | '-- testScale() [OK]\n"
	    		+ "| +-- ExtentOfLeftTopRightBottomTest [OK]\n"
	    		+ "| | +-- testContains() [OK]\n"
	    		+ "| | +-- testEqualsObject() [OK]\n"
	    		+ "| | +-- testGetBottom() [OK]\n"
	    		+ "| | +-- testGetBottomRight() [OK]\n"
	    		+ "| | +-- testGetHeight() [OK]\n"
	    		+ "| | +-- testGetLeft() [OK]\n"
	    		+ "| | +-- testGetRight() [OK]\n"
	    		+ "| | +-- testGetTop() [OK]\n"
	    		+ "| | +-- testGetTopLeft() [OK]\n"
	    		+ "| | +-- testGetWidth() [OK]\n"
	    		+ "| | +-- testHashCode() [OK]\n"
	    		+ "| | +-- testToString() [OK]\n"
	    		+ "| | +-- testWithBottom() [OK]\n"
	    		+ "| | +-- testWithHeight() [OK]\n"
	    		+ "| | +-- testWithLeft() [OK]\n"
	    		+ "| | +-- testWithRight() [OK]\n"
	    		+ "| | +-- testWithTop() [OK]\n"
	    		+ "| | '-- testWithWidth() [OK]\n"
	    		+ "| +-- ExtentOfLeftTopRightBottomTest [OK]\n"
	    		+ "| | +-- testContains() [OK]\n"
	    		+ "| | +-- testEqualsObject() [OK]\n"
	    		+ "| | +-- testGetBottom() [OK]\n"
	    		+ "| | +-- testGetBottomRight() [OK]\n"
	    		+ "| | +-- testGetHeight() [OK]\n"
	    		+ "| | +-- testGetLeft() [OK]\n"
	    		+ "| | +-- testGetRight() [OK]\n"
	    		+ "| | +-- testGetTop() [OK]\n"
	    		+ "| | +-- testGetTopLeft() [OK]\n"
	    		+ "| | +-- testGetWidth() [OK]\n"
	    		+ "| | +-- testHashCode() [OK]\n"
	    		+ "| | +-- testToString() [OK]\n"
	    		+ "| | +-- testWithBottom() [OK]\n"
	    		+ "| | +-- testWithHeight() [OK]\n"
	    		+ "| | +-- testWithLeft() [OK]\n"
	    		+ "| | +-- testWithRight() [OK]\n"
	    		+ "| | +-- testWithTop() [OK]\n"
	    		+ "| | '-- testWithWidth() [OK]\n"
	    		+ "| +-- ExtentOfLeftTopWidthHeightTest [OK]\n"
	    		+ "| | +-- testContains() [OK]\n"
	    		+ "| | +-- testGetBottom() [OK]\n"
	    		+ "| | +-- testGetBottomRight() [OK]\n"
	    		+ "| | +-- testGetHeight() [OK]\n"
	    		+ "| | +-- testGetLeft() [OK]\n"
	    		+ "| | +-- testGetRight() [OK]\n"
	    		+ "| | +-- testGetTop() [OK]\n"
	    		+ "| | +-- testGetTopLeft() [OK]\n"
	    		+ "| | +-- testGetWidth() [OK]\n"
	    		+ "| | +-- testWithBottom() [OK]\n"
	    		+ "| | +-- testWithHeight() [OK]\n"
	    		+ "| | +-- testWithLeft() [OK]\n"
	    		+ "| | +-- testWithRight() [OK]\n"
	    		+ "| | +-- testWithTop() [OK]\n"
	    		+ "| | '-- testWithWidth() [OK]\n"
	    		+ "| +-- ExtentOfLeftTopWidthHeightTest [OK]\n"
	    		+ "| | +-- testContains() [OK]\n"
	    		+ "| | +-- testGetBottom() [OK]\n"
	    		+ "| | +-- testGetBottomRight() [OK]\n"
	    		+ "| | +-- testGetHeight() [OK]\n"
	    		+ "| | +-- testGetLeft() [OK]\n"
	    		+ "| | +-- testGetRight() [OK]\n"
	    		+ "| | +-- testGetTop() [OK]\n"
	    		+ "| | +-- testGetTopLeft() [OK]\n"
	    		+ "| | +-- testGetWidth() [OK]\n"
	    		+ "| | +-- testWithBottom() [OK]\n"
	    		+ "| | +-- testWithHeight() [OK]\n"
	    		+ "| | +-- testWithLeft() [OK]\n"
	    		+ "| | +-- testWithRight() [OK]\n"
	    		+ "| | +-- testWithTop() [OK]\n"
	    		+ "| | '-- testWithWidth() [OK]\n"
	    		+ "| +-- IntPointTest [OK]\n"
	    		+ "| | +-- testAsDoublePoint() [OK]\n"
	    		+ "| | +-- testConstructorAndGetters() [OK]\n"
	    		+ "| | +-- testEquals() [OK]\n"
	    		+ "| | +-- testIsOnLineSegment() [OK]\n"
	    		+ "| | +-- testLineSegmentsIntersect() [OK]\n"
	    		+ "| | +-- testMinus() [OK]\n"
	    		+ "| | '-- testPlus() [OK]\n"
	    		+ "| +-- IntVectorTest [OK]\n"
	    		+ "| | +-- testAsDoubleVector() [OK]\n"
	    		+ "| | +-- testConstructorAndGetters() [OK]\n"
	    		+ "| | +-- testCrossProduct() [OK]\n"
	    		+ "| | +-- testDotProduct() [OK]\n"
	    		+ "| | '-- testIsCollinearWith() [OK]\n"
	    		+ "| +-- PointArraysTest [OK]\n"
	    		+ "| | +-- testCheckDefinesProperPolygon_coincidingVertices() [OK]\n"
	    		+ "| | +-- testCheckDefinesProperPolygon_intersectingEdges() [OK]\n"
	    		+ "| | +-- testCheckDefinesProperPolygon_proper() [OK]\n"
	    		+ "| | +-- testCheckDefinesProperPolygon_vertexOnEdge() [OK]\n"
	    		+ "| | +-- testCopy() [OK]\n"
	    		+ "| | +-- testInsert() [OK]\n"
	    		+ "| | +-- testRemove() [OK]\n"
	    		+ "| | '-- testUpdate() [OK]\n"
	    		+ "| +-- RoundedPolygonTest [OK]\n"
	    		+ "| | +-- testContains_false() [OK]\n"
	    		+ "| | +-- testContains_true_interior() [OK]\n"
	    		+ "| | +-- testContains_true_on_edge() [OK]\n"
	    		+ "| | +-- testContains_true_vertex() [OK]\n"
	    		+ "| | +-- testFastRoundedPolygonContainsTestStrategy() [OK]\n"
	    		+ "| | +-- testGetters() [OK]\n"
	    		+ "| | +-- testInsert_proper() [OK]\n"
	    		+ "| | +-- testPreciseRoundedPolygonContainsTestStrategy() [OK]\n"
	    		+ "| | +-- testRemove_improper() [OK]\n"
	    		+ "| | +-- testRemove_proper() [OK]\n"
	    		+ "| | +-- testSetRadius() [OK]\n"
	    		+ "| | +-- testSetVertices_improper() [OK]\n"
	    		+ "| | +-- testSetVertices_proper() [OK]\n"
	    		+ "| | +-- testUpdate_improper() [OK]\n"
	    		+ "| | '-- testUpdate_proper() [OK]\n"
	    		+ "| +-- ShapeGroupTest_LeavesOnly_NoSetExtent [OK]\n"
	    		+ "| | +-- testGetExtent() [OK]\n"
	    		+ "| | +-- testGetOriginalExtent() [OK]\n"
	    		+ "| | +-- testGetParentGroup() [OK]\n"
	    		+ "| | +-- testGetShape() [OK]\n"
	    		+ "| | +-- testToGlobalCoordinates() [OK]\n"
	    		+ "| | +-- testToInnerCoordinates_IntPoint() [OK]\n"
	    		+ "| | '-- testToInnerCoordinates_IntVector() [OK]\n"
	    		+ "| +-- ShapeGroupTest_LeavesOnly_NoSetExtent [OK]\n"
	    		+ "| | +-- testGetExtent() [OK]\n"
	    		+ "| | +-- testGetOriginalExtent() [OK]\n"
	    		+ "| | +-- testGetParentGroup() [OK]\n"
	    		+ "| | +-- testGetShape() [OK]\n"
	    		+ "| | +-- testToGlobalCoordinates() [OK]\n"
	    		+ "| | +-- testToInnerCoordinates_IntPoint() [OK]\n"
	    		+ "| | '-- testToInnerCoordinates_IntVector() [OK]\n"
	    		+ "| +-- ShapeGroupTest_LeavesOnly_SetExtent [OK]\n"
	    		+ "| | +-- testGetExtent() [OK]\n"
	    		+ "| | +-- testGetOriginalExtent() [OK]\n"
	    		+ "| | +-- testGetParentGroup() [OK]\n"
	    		+ "| | +-- testGetShape() [OK]\n"
	    		+ "| | +-- testToGlobalCoordinates() [OK]\n"
	    		+ "| | +-- testToInnerCoordinates_IntPoint() [OK]\n"
	    		+ "| | '-- testToInnerCoordinates_IntVector() [OK]\n"
	    		+ "| +-- ShapeGroupTest_LeavesOnly_SetExtent [OK]\n"
	    		+ "| | +-- testGetExtent() [OK]\n"
	    		+ "| | +-- testGetOriginalExtent() [OK]\n"
	    		+ "| | +-- testGetParentGroup() [OK]\n"
	    		+ "| | +-- testGetShape() [OK]\n"
	    		+ "| | +-- testToGlobalCoordinates() [OK]\n"
	    		+ "| | +-- testToInnerCoordinates_IntPoint() [OK]\n"
	    		+ "| | '-- testToInnerCoordinates_IntVector() [OK]\n"
	    		+ "| +-- ShapeGroupTest_Nonleaves_1Level [OK]\n"
	    		+ "| | +-- testBringToFront1() [OK]\n"
	    		+ "| | +-- testBringToFront2() [OK]\n"
	    		+ "| | +-- testGetExtent() [OK]\n"
	    		+ "| | +-- testGetOriginalExtent() [OK]\n"
	    		+ "| | +-- testGetParentGroup() [OK]\n"
	    		+ "| | +-- testGetShape() [OK]\n"
	    		+ "| | +-- testGetSubgroup() [OK]\n"
	    		+ "| | +-- testGetSubgroupAt() [OK]\n"
	    		+ "| | +-- testGetSubgroupCount() [OK]\n"
	    		+ "| | +-- testGetSubgroups() [OK]\n"
	    		+ "| | +-- testSendToBack1() [OK]\n"
	    		+ "| | +-- testSendToBack2() [OK]\n"
	    		+ "| | +-- testSendToBack_bringToFront() [OK]\n"
	    		+ "| | +-- testToGlobalCoordinates() [OK]\n"
	    		+ "| | +-- testToInnerCoordinates_IntPoint() [OK]\n"
	    		+ "| | '-- testToInnerCoordinates_IntVector() [OK]\n"
	    		+ "| +-- ShapeGroupTest_Nonleaves_1Level [OK]\n"
	    		+ "| | +-- testBringToFront1() [OK]\n"
	    		+ "| | +-- testBringToFront2() [OK]\n"
	    		+ "| | +-- testGetExtent() [OK]\n"
	    		+ "| | +-- testGetOriginalExtent() [OK]\n"
	    		+ "| | +-- testGetParentGroup() [OK]\n"
	    		+ "| | +-- testGetShape() [OK]\n"
	    		+ "| | +-- testGetSubgroup() [OK]\n"
	    		+ "| | +-- testGetSubgroupAt() [OK]\n"
	    		+ "| | +-- testGetSubgroupCount() [OK]\n"
	    		+ "| | +-- testGetSubgroups() [OK]\n"
	    		+ "| | +-- testSendToBack1() [OK]\n"
	    		+ "| | +-- testSendToBack2() [OK]\n"
	    		+ "| | +-- testSendToBack_bringToFront() [OK]\n"
	    		+ "| | +-- testToGlobalCoordinates() [OK]\n"
	    		+ "| | +-- testToInnerCoordinates_IntPoint() [OK]\n"
	    		+ "| | '-- testToInnerCoordinates_IntVector() [OK]\n"
	    		+ "| +-- ShapeGroupTest_Nonleaves_1Level_setExtent [OK]\n"
	    		+ "| | +-- testBringToFront1() [OK]\n"
	    		+ "| | +-- testBringToFront2() [OK]\n"
	    		+ "| | +-- testGetExtent() [OK]\n"
	    		+ "| | +-- testGetOriginalExtent() [OK]\n"
	    		+ "| | +-- testGetParentGroup() [OK]\n"
	    		+ "| | +-- testGetShape() [OK]\n"
	    		+ "| | +-- testGetSubgroup() [OK]\n"
	    		+ "| | +-- testGetSubgroupAt() [OK]\n"
	    		+ "| | +-- testGetSubgroupCount() [OK]\n"
	    		+ "| | +-- testGetSubgroups() [OK]\n"
	    		+ "| | +-- testSendToBack1() [OK]\n"
	    		+ "| | +-- testSendToBack2() [OK]\n"
	    		+ "| | +-- testSendToBack_bringToFront() [OK]\n"
	    		+ "| | +-- testToGlobalCoordinates() [OK]\n"
	    		+ "| | +-- testToInnerCoordinates_IntPoint() [OK]\n"
	    		+ "| | '-- testToInnerCoordinates_IntVector() [OK]\n"
	    		+ "| +-- ShapeGroupTest_Nonleaves_1Level_setExtent [OK]\n"
	    		+ "| | +-- testBringToFront1() [OK]\n"
	    		+ "| | +-- testBringToFront2() [OK]\n"
	    		+ "| | +-- testGetExtent() [OK]\n"
	    		+ "| | +-- testGetOriginalExtent() [OK]\n"
	    		+ "| | +-- testGetParentGroup() [OK]\n"
	    		+ "| | +-- testGetShape() [OK]\n"
	    		+ "| | +-- testGetSubgroup() [OK]\n"
	    		+ "| | +-- testGetSubgroupAt() [OK]\n"
	    		+ "| | +-- testGetSubgroupCount() [OK]\n"
	    		+ "| | +-- testGetSubgroups() [OK]\n"
	    		+ "| | +-- testSendToBack1() [OK]\n"
	    		+ "| | +-- testSendToBack2() [OK]\n"
	    		+ "| | +-- testSendToBack_bringToFront() [OK]\n"
	    		+ "| | +-- testToGlobalCoordinates() [OK]\n"
	    		+ "| | +-- testToInnerCoordinates_IntPoint() [OK]\n"
	    		+ "| | '-- testToInnerCoordinates_IntVector() [OK]\n"
	    		+ "| +-- ShapeGroupTest_Nonleaves_2Levels [OK]\n"
	    		+ "| | +-- testBringToFront1() [OK]\n"
	    		+ "| | +-- testBringToFront2() [OK]\n"
	    		+ "| | +-- testExporter() [OK]\n"
	    		+ "| | +-- testGetDrawingCommands() [OK]\n"
	    		+ "| | +-- testGetExtent() [OK]\n"
	    		+ "| | +-- testGetOriginalExtent() [OK]\n"
	    		+ "| | +-- testGetParentGroup() [OK]\n"
	    		+ "| | +-- testGetShape() [OK]\n"
	    		+ "| | +-- testGetSubgroup() [OK]\n"
	    		+ "| | +-- testGetSubgroupAt() [OK]\n"
	    		+ "| | +-- testGetSubgroupCount() [OK]\n"
	    		+ "| | +-- testGetSubgroups() [OK]\n"
	    		+ "| | +-- testRoundedPolygonShape_contains() [OK]\n"
	    		+ "| | +-- testRoundedPolygonShape_createControlPoints_getLocation() [OK]\n"
	    		+ "| | +-- testRoundedPolygonShape_createControlPoints_move() [OK]\n"
	    		+ "| | +-- testRoundedPolygonShape_createControlPoints_remove() [OK]\n"
	    		+ "| | +-- testRoundedPolygonShape_getters() [OK]\n"
	    		+ "| | +-- testRoundedPolygonShape_toGlobalCoordinates() [OK]\n"
	    		+ "| | +-- testRoundedPolygonShape_toShapeCoordinates() [OK]\n"
	    		+ "| | +-- testSendToBack1() [OK]\n"
	    		+ "| | +-- testSendToBack2() [OK]\n"
	    		+ "| | +-- testSendToBack_bringToFront() [OK]\n"
	    		+ "| | +-- testShapeGroupShape_contains() [OK]\n"
	    		+ "| | +-- testShapeGroupShape_createControlPoints_getLocation() [OK]\n"
	    		+ "| | +-- testShapeGroupShape_createControlPoints_move_bottomRight() [OK]\n"
	    		+ "| | +-- testShapeGroupShape_createControlPoints_move_upperLeft() [OK]\n"
	    		+ "| | +-- testShapeGroupShape_getters() [OK]\n"
	    		+ "| | +-- testShapeGroupShape_toGlobalCoordinates() [OK]\n"
	    		+ "| | +-- testShapeGroupShape_toShapeCoordinates() [OK]\n"
	    		+ "| | +-- testToGlobalCoordinates() [OK]\n"
	    		+ "| | +-- testToInnerCoordinates_IntPoint() [OK]\n"
	    		+ "| | '-- testToInnerCoordinates_IntVector() [OK]\n"
	    		+ "| '-- ShapeGroupTest_Nonleaves_2Levels [OK]\n"
	    		+ "|   +-- testBringToFront1() [OK]\n"
	    		+ "|   +-- testBringToFront2() [OK]\n"
	    		+ "|   +-- testGetDrawingCommands() [OK]\n"
	    		+ "|   +-- testGetExtent() [OK]\n"
	    		+ "|   +-- testGetOriginalExtent() [OK]\n"
	    		+ "|   +-- testGetParentGroup() [OK]\n"
	    		+ "|   +-- testGetShape() [OK]\n"
	    		+ "|   +-- testGetSubgroup() [OK]\n"
	    		+ "|   +-- testGetSubgroupAt() [OK]\n"
	    		+ "|   +-- testGetSubgroupCount() [OK]\n"
	    		+ "|   +-- testGetSubgroups() [OK]\n"
	    		+ "|   +-- testRoundedPolygonShape_contains() [OK]\n"
	    		+ "|   +-- testRoundedPolygonShape_createControlPoints_getLocation() [OK]\n"
	    		+ "|   +-- testRoundedPolygonShape_createControlPoints_move() [OK]\n"
	    		+ "|   +-- testRoundedPolygonShape_createControlPoints_remove() [OK]\n"
	    		+ "|   +-- testRoundedPolygonShape_getters() [OK]\n"
	    		+ "|   +-- testRoundedPolygonShape_toGlobalCoordinates() [OK]\n"
	    		+ "|   +-- testRoundedPolygonShape_toShapeCoordinates() [OK]\n"
	    		+ "|   +-- testSendToBack1() [OK]\n"
	    		+ "|   +-- testSendToBack2() [OK]\n"
	    		+ "|   +-- testSendToBack_bringToFront() [OK]\n"
	    		+ "|   +-- testShapeGroupShape_contains() [OK]\n"
	    		+ "|   +-- testShapeGroupShape_createControlPoints_getLocation() [OK]\n"
	    		+ "|   +-- testShapeGroupShape_createControlPoints_move_bottomRight() [OK]\n"
	    		+ "|   +-- testShapeGroupShape_createControlPoints_move_upperLeft() [OK]\n"
	    		+ "|   +-- testShapeGroupShape_getters() [OK]\n"
	    		+ "|   +-- testShapeGroupShape_toGlobalCoordinates() [OK]\n"
	    		+ "|   +-- testShapeGroupShape_toShapeCoordinates() [OK]\n"
	    		+ "|   +-- testToGlobalCoordinates() [OK]\n"
	    		+ "|   +-- testToInnerCoordinates_IntPoint() [OK]\n"
	    		+ "|   '-- testToInnerCoordinates_IntVector() [OK]\n"
	    		+ "'-- JUnit Vintage [OK]\n"
	    		+ "\n"
	    		+ "Test run finished after XX ms\n"
	    		+ "[        22 containers found      ]\n"
	    		+ "[         0 containers skipped    ]\n"
	    		+ "[        22 containers started    ]\n"
	    		+ "[         0 containers aborted    ]\n"
	    		+ "[        22 containers successful ]\n"
	    		+ "[         0 containers failed     ]\n"
	    		+ "[       267 tests found           ]\n"
	    		+ "[         0 tests skipped         ]\n"
	    		+ "[       267 tests started         ]\n"
	    		+ "[         0 tests aborted         ]\n"
	    		+ "[       267 tests successful      ]\n"
	    		+ "[         0 tests failed          ]", "");
	    testCompileAndRun(true, "invariants_fail", false, "",
	    		"Exception in thread \"main\" java.lang.AssertionError: Representation invariant does not hold\n" + 
	    		"	at invariants_fail.$classInvariants(invariants_fail.java:5)\n" + 
	    		"	at invariants_fail.<init>(invariants_fail.java:1)\n" + 
	    		"	at Main.main(invariants_fail.java:14)\n");
	    testCompileAndRun(true, "invariants_fail2", false, "",
	    		"Exception in thread \"main\" java.lang.AssertionError: Representation invariant does not hold\n" + 
	    		"	at invariants_fail2.$classInvariants(invariants_fail2.java:5)\n" + 
	    		"	at invariants_fail2.getDifference(invariants_fail2.java:10)\n" + 
	    		"	at invariants_fail2.<init>(invariants_fail2.java:16)\n" + 
	    		"	at Main.main(invariants_fail2.java:24)\n");
	    testCompileAndRun(true, "invariants_fail3", true, "",
	    		"java.lang.AssertionError: Representation invariant does not hold\n" + 
	    		"	at invariants_fail3.$classInvariants(invariants_fail3.java:5)\n" + 
	    		"	at invariants_fail3.foo(invariants_fail3.java:12)\n" + 
	    		"	at invariants_fail3.bar1(invariants_fail3.java:19)\n" + 
	    		"	at Main.main(invariants_fail3.java:45)\n" + 
	    		"java.lang.AssertionError: Representation invariant does not hold\n" + 
	    		"	at invariants_fail3.$classInvariants(invariants_fail3.java:5)\n" + 
	    		"	at invariants_fail3.bar1(invariants_fail3.java:16)\n" + 
	    		"	at invariants_fail3.bar2(invariants_fail3.java:25)\n" + 
	    		"	at Main.main(invariants_fail3.java:52)\n" + 
	    		"java.lang.AssertionError: Representation invariant does not hold\n" + 
	    		"	at invariants_fail3.$classInvariants(invariants_fail3.java:5)\n" + 
	    		"	at invariants_fail3.bar2(invariants_fail3.java:22)\n" + 
	    		"	at invariants_fail3.bar3(invariants_fail3.java:31)\n" + 
	    		"	at Main.main(invariants_fail3.java:59)\n" + 
	    		"java.lang.AssertionError: Representation invariant does not hold\n" + 
	    		"	at invariants_fail3.$classInvariants(invariants_fail3.java:5)\n" + 
	    		"	at invariants_fail3.bar3(invariants_fail3.java:28)\n" + 
	    		"	at invariants_fail3.bar4(invariants_fail3.java:37)\n" + 
	    		"	at Main.main(invariants_fail3.java:66)\n");
	    testCompileAndRun(true, "invariants_fail4", true, "",
	    		"java.lang.AssertionError: Representation invariant does not hold\n" + 
	    		"	at invariants_fail4.$packageInvariants(invariants_fail4.java:5)\n" + 
	    		"	at invariants_fail4.getDifference(invariants_fail4.java:12)\n" + 
	    		"	at invariants_fail4.foo(invariants_fail4.java:21)\n" + 
	    		"	at Main.main(invariants_fail4.java:54)\n" + 
	    		"java.lang.AssertionError: Representation invariant does not hold\n" + 
	    		"	at invariants_fail4.$packageInvariants(invariants_fail4.java:5)\n" + 
	    		"	at invariants_fail4.foo(invariants_fail4.java:17)\n" + 
	    		"	at invariants_fail4.bar1(invariants_fail4.java:28)\n" + 
	    		"	at Main.main(invariants_fail4.java:61)\n" + 
	    		"java.lang.AssertionError: Representation invariant does not hold\n" + 
	    		"	at invariants_fail4.$packageInvariants(invariants_fail4.java:5)\n" + 
	    		"	at invariants_fail4.bar1(invariants_fail4.java:24)\n" + 
	    		"	at invariants_fail4.bar2(invariants_fail4.java:34)\n" + 
	    		"	at Main.main(invariants_fail4.java:68)\n" + 
	    		"java.lang.AssertionError: Representation invariant does not hold\n" + 
	    		"	at invariants_fail4.$packageInvariants(invariants_fail4.java:5)\n" + 
	    		"	at invariants_fail4.bar2(invariants_fail4.java:31)\n" + 
	    		"	at invariants_fail4.bar3(invariants_fail4.java:40)\n" + 
	    		"	at Main.main(invariants_fail4.java:75)\n" + 
	    		"java.lang.AssertionError: Representation invariant does not hold\n" + 
	    		"	at invariants_fail4.$packageInvariants(invariants_fail4.java:5)\n" + 
	    		"	at invariants_fail4.bar3(invariants_fail4.java:37)\n" + 
	    		"	at invariants_fail4.bar4(invariants_fail4.java:46)\n" + 
	    		"	at Main.main(invariants_fail4.java:82)\n");
	    testCompileAndRun(true, "invariants_fail5", false, "",
	    		"Exception in thread \"main\" java.lang.AssertionError: Representation invariant does not hold\n" + 
	    		"	at invariants_fail5.$packageInvariants(invariants_fail5.java:5)\n" + 
	    		"	at invariants_fail5.<init>(invariants_fail5.java:1)\n" + 
	    		"	at Main.main(invariants_fail5.java:14)\n");
	    testCompileAndRun(true, "invariants_fail6", true, "",
	    		"java.lang.AssertionError: Representation invariant does not hold\n" + 
	    		"	at invariants_fail6.$packageInvariants(invariants_fail6.java:7)\n" + 
	    		"	at invariants_fail6.getDifference(invariants_fail6.java:13)\n" + 
	    		"	at invariants_fail6.foo(invariants_fail6.java:24)\n" + 
	    		"	at Main.main(invariants_fail6.java:57)\n" + 
	    		"java.lang.AssertionError: Representation invariant does not hold\n" + 
	    		"	at invariants_fail6.$packageInvariants(invariants_fail6.java:7)\n" + 
	    		"	at invariants_fail6.foo(invariants_fail6.java:20)\n" + 
	    		"	at invariants_fail6.bar1(invariants_fail6.java:31)\n" + 
	    		"	at Main.main(invariants_fail6.java:64)\n" + 
	    		"java.lang.AssertionError: Representation invariant does not hold\n" + 
	    		"	at invariants_fail6.$packageInvariants(invariants_fail6.java:7)\n" + 
	    		"	at invariants_fail6.bar1(invariants_fail6.java:27)\n" + 
	    		"	at invariants_fail6.bar2(invariants_fail6.java:37)\n" + 
	    		"	at Main.main(invariants_fail6.java:71)\n" + 
	    		"java.lang.AssertionError: Representation invariant does not hold\n" + 
	    		"	at invariants_fail6.$packageInvariants(invariants_fail6.java:7)\n" + 
	    		"	at invariants_fail6.bar2(invariants_fail6.java:34)\n" + 
	    		"	at invariants_fail6.bar3(invariants_fail6.java:43)\n" + 
	    		"	at Main.main(invariants_fail6.java:78)\n" + 
	    		"java.lang.AssertionError: Representation invariant does not hold\n" + 
	    		"	at invariants_fail6.$packageInvariants(invariants_fail6.java:7)\n" + 
	    		"	at invariants_fail6.bar3(invariants_fail6.java:40)\n" + 
	    		"	at invariants_fail6.bar4(invariants_fail6.java:49)\n" + 
	    		"	at Main.main(invariants_fail6.java:85)\n");
	    testCompileAndRun(true, "invariants_fail7", false, "",
	    		"Exception in thread \"main\" java.lang.AssertionError: A class representation invariant of an object must not directly or indirectly call a nonprivate method that inspects or mutates the object.\n" + 
	    		"	at invariants_fail7.$classInvariants(invariants_fail7.java:1)\n" + 
	    		"	at invariants_fail7.getDifference(invariants_fail7.java:10)\n" + 
	    		"	at invariants_fail7.$classInvariants(invariants_fail7.java:5)\n" + 
	    		"	at invariants_fail7.<init>(invariants_fail7.java:1)\n" + 
	    		"	at Main.main(invariants_fail7.java:18)\n");
	    testCompileAndRun(true, "invariants_fail8", false, "",
	    		"Exception in thread \"main\" java.lang.AssertionError: A package representation invariant of an object must not directly or indirectly call a public or protected method that inspects or mutates the object.\n" + 
	    		"	at invariants_fail8.$packageInvariants(invariants_fail8.java:1)\n" + 
	    		"	at invariants_fail8.getDifference(invariants_fail8.java:10)\n" + 
	    		"	at invariants_fail8.$packageInvariants(invariants_fail8.java:5)\n" + 
	    		"	at invariants_fail8.<init>(invariants_fail8.java:1)\n" + 
	    		"	at Main.main(invariants_fail8.java:18)\n");
	    testCompileAndRun(true, "invariants_fail9", false, "",
	    		"Exception in thread \"main\" java.lang.AssertionError: Abstract state invariant does not hold\n" + 
	    		"	at invariants_fail9.$packageInvariants(invariants_fail9.java:2)\n" + 
	    		"	at invariants_fail9.<init>(invariants_fail9.java:4)\n" + 
	    		"	at Main.main(invariants_fail9.java:18)\n");
	    testCompileAndRun(true, "invariants_fail10", false, "",
	    		"Exception in thread \"main\" java.lang.AssertionError: Abstract state invariant does not hold\n" + 
	    		"	at invariants_fail10.$classInvariants(invariants_fail10.java:2)\n" + 
	    		"	at invariants_fail10.<init>(invariants_fail10.java:4)\n" + 
	    		"	at Main.main(invariants_fail10.java:18)\n");
	    testCompileAndRun(true, "invariants_fail11", false, "",
	    		"Exception in thread \"main\" java.lang.AssertionError: Representation invariant does not hold\n" + 
	    		"	at invariants_fail11.$classInvariants(invariants_fail11.java:5)\n" + 
	    		"	at invariants_fail11.foo(invariants_fail11.java:13)\n" + 
	    		"	at Main.main(invariants_fail11.java:19)\n");
	    testCompileAndRun(true, "invariants_fail12", true, "",
	    		"java.lang.AssertionError: Representation invariant does not hold\n" + 
	    		"	at invariants_fail12.$classInvariants(invariants_fail12.java:5)\n" + 
	    		"	at invariants_fail12.<init>(invariants_fail12.java:13)\n" + 
	    		"	at Main.main(invariants_fail12.java:31)\n" + 
	    		"java.lang.AssertionError: Representation invariant does not hold\n" + 
	    		"	at invariants_fail12.$classInvariants(invariants_fail12.java:5)\n" + 
	    		"	at invariants_fail12.foo(invariants_fail12.java:21)\n" + 
	    		"	at Main.main(invariants_fail12.java:38)\n");
	    testCompileAndRun(true, "invariants_fail13", false, "",
	    		"Exception in thread \"main\" java.lang.AssertionError: Representation invariant does not hold\n" + 
	    		"	at invariants_fail13.$classInvariants(invariants_fail13.java:5)\n" + 
	    		"	at invariants_fail13.foo(invariants_fail13.java:12)\n" + 
	    		"	at Main.main(invariants_fail13.java:18)\n");
		testCompile("issue18", false, "",
				"----------\n" + 
				"1. ERROR in SOURCE_FILE_FULL_PATH (at line 8)\n" + 
				"	private final int y;\n" + 
				"	                  ^\n" + 
				"The blank final field y may not have been initialized\n" + 
				"----------\n" + 
				"1 problem (1 error)\n");
		testCompileAndRun(true, "issue23", false, "",
				  "Exception in thread \"main\" java.lang.AssertionError: Precondition does not hold\n"
				  + "	at Foo.foo$pre(issue23.java:2)\n"
				  + "	at Foo.foo$spec(issue23.java)\n"
				  + "	at Main.main(issue23.java:8)\n");
		testCompileAndRun(true, "issue23bis", false, "",
				  "Exception in thread \"main\" java.lang.AssertionError: Precondition does not hold\n"
				+ "	at Foo.foo$pre(issue23bis.java:2)\n"
				+ "	at Foo.foo$spec(issue23bis.java)\n"
				+ "	at Main.main(issue23bis.java:8)\n");
		testCompileAndRun(true, "effectchecker", true,
				"public static void fsc4j.EffectChecker.assertCanCreate(java.lang.Object)\n", "");
		testCompile("issue32", false, "",
				"----------\n"
				+ "1. ERROR in SOURCE_FILE_FULL_PATH (at line 3)\n"
				+ "	[5,]\n"
				+ "	^^^^\n"
				+ "Syntax error on tokens, delete these tokens\n"
				+ "----------\n"
				+ "1 problem (1 error)\n");
		testCompileAndRunMultifile("abstract_class_ctor_inv_checks", true,
				".\n"
				+ "+-- JUnit Jupiter [OK]\n"
				+ "| '-- ListTest [OK]\n"
				+ "|   '-- test() [OK]\n"
				+ "'-- JUnit Vintage [OK]\n"
				+ "\n"
				+ "Test run finished after XX ms\n"
				+ "[         3 containers found      ]\n"
				+ "[         0 containers skipped    ]\n"
				+ "[         3 containers started    ]\n"
				+ "[         0 containers aborted    ]\n"
				+ "[         3 containers successful ]\n"
				+ "[         0 containers failed     ]\n"
				+ "[         1 tests found           ]\n"
				+ "[         0 tests skipped         ]\n"
				+ "[         1 tests started         ]\n"
				+ "[         0 tests aborted         ]\n"
				+ "[         1 tests successful      ]\n"
				+ "[         0 tests failed          ]", "");
	    testCompileAndRun(true, "issue34", false, "",
	    		"Exception in thread \"main\" java.lang.AssertionError: Representation invariant does not hold\n"
	    		+ "	at Main.$classInvariants(issue34.java:4)\n"
	    		+ "	at Main.getX(issue34.java:8)\n"
	    		+ "	at Main.foo$post(issue34.java:16)\n"
	    		+ "	at Main.foo(issue34.java:23)\n"
	    		+ "	at Main.<init>(issue34.java:11)\n"
	    		+ "	at Main.main(issue34.java:26)\n");
	    testCompileAndRun(true, "issue34bis", false, "",
	    		"Exception in thread \"main\" java.lang.AssertionError: Representation invariant does not hold\n"
	    		+ "	at Main.$classInvariants(issue34bis.java:4)\n"
	    		+ "	at Main.getX(issue34bis.java:8)\n"
	    		+ "	at Main.foo$pre(issue34bis.java:16)\n"
	    		+ "	at Main.foo(issue34bis.java:22)\n"
	    		+ "	at Main.<init>(issue34bis.java:11)\n"
	    		+ "	at Main.main(issue34bis.java:26)\n");
		testCompileAndRun(true, "issue35", true, "", "");
			    
		System.out.println("s4jie2TestSuite: All tests passed.");
	}

}
