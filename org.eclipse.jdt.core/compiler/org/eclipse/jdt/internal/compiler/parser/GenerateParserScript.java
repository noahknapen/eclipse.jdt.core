package org.eclipse.jdt.internal.compiler.parser;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

public class GenerateParserScript {

	private static void assertTrue(boolean b) { if (!b) throw new AssertionError(); }

	public static void main(String[] args) throws IOException, InterruptedException {
		File grammarDir = new File("grammar"); //$NON-NLS-1$
		File parserDir = new File("compiler/org/eclipse/jdt/internal/compiler/parser"); //$NON-NLS-1$
		String jikespg = System.getenv("JIKESPG"); //$NON-NLS-1$
		assertTrue(jikespg != null);

		// Run JikesPG
		Process process = Runtime.getRuntime().exec(new String[] {jikespg, "java.g"}, null, grammarDir); //$NON-NLS-1$
		try (BufferedReader jikespgOutput = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
			for (;;) {
				String line = jikespgOutput.readLine();
				if (line == null)
					break;
				System.out.println(line);
			}
		}
		int exitCode = process.waitFor();
		assertTrue(exitCode == 0);

		// Update parserNN.rsc and readableNames.props
		File javadclFile = new File(grammarDir, "javadcl.java"); //$NON-NLS-1$
		File javahdrFile = new File(grammarDir, "javahdr.java"); //$NON-NLS-1$
		Parser.buildFilesFromLPG(javadclFile.toString(), javahdrFile.toString());
		for (int i = 1; i <= 24; i++) {
			String filename = "parser"+i+".rsc"; //$NON-NLS-1$ //$NON-NLS-2$
			Files.move(new File(filename).toPath(), new File(parserDir, filename).toPath(), StandardCopyOption.REPLACE_EXISTING);
		}
		{
			String filename = "readableNames.props"; //$NON-NLS-1$
			Files.move(new File(filename).toPath(), new File(parserDir, filename).toPath(), StandardCopyOption.REPLACE_EXISTING);
		}

		// Update TerminalTokens.java
		File javasymFile = new File(grammarDir, "javasym.java"); //$NON-NLS-1$
		File terminalTokensFile = new File(parserDir, "TerminalTokens.java"); //$NON-NLS-1$
		String javasymText = new String(Files.readAllBytes(javasymFile.toPath())).replace("\r\n",  "\n"); //$NON-NLS-1$ //$NON-NLS-2$
		String terminalTokensText = new String(Files.readAllBytes(terminalTokensFile.toPath()));
		{
			String startTag = "// BEGIN_AUTOGENERATED_REGION\n"; //$NON-NLS-1$
			int start = terminalTokensText.indexOf(startTag);
			assertTrue(start >= 0);
			start += startTag.length();
			String terminalTokensProlog = terminalTokensText.substring(0, start);
			
			String javasymProlog = 
					"interface javasym\n" +  //$NON-NLS-1$
					"{\n" +  //$NON-NLS-1$
					"    public final static int\n" + //$NON-NLS-1$
					"      "; //$NON-NLS-1$
			assertTrue(javasymText.startsWith(javasymProlog));
			javasymText = javasymText.substring(javasymProlog.length());
			javasymText = javasymText.replace(",\n      ", ",\n\t\t\t\t\t\t\t"); //$NON-NLS-1$ //$NON-NLS-2$
			javasymText = javasymText.replace("TokenName$eof", "TokenNameEOF"); //$NON-NLS-1$ //$NON-NLS-2$
			javasymText = javasymText.replace("TokenName$error", "TokenNameERROR"); //$NON-NLS-1$ //$NON-NLS-2$
			Files.write(terminalTokensFile.toPath(), (terminalTokensProlog + "\tint " + javasymText).getBytes()); //$NON-NLS-1$
		}

		// Update ParserBasicInformation.java
		File javadefFile = new File(grammarDir, "javadef.java"); //$NON-NLS-1$
		File parserBasicInformationFile = new File(parserDir, "ParserBasicInformation.java"); //$NON-NLS-1$
		String javadefText = new String(Files.readAllBytes(javadefFile.toPath())).replace("\r\n", "\n"); //$NON-NLS-1$ //$NON-NLS-2$
		String parserBasicInformationText = new String(Files.readAllBytes(parserBasicInformationFile.toPath()));
		{
			String startTag = "// BEGIN_AUTOGENERATED_REGION"; //$NON-NLS-1$
			int start = parserBasicInformationText.indexOf(startTag);
			assertTrue(start >= 0);
			start += startTag.length();
			String parserBasicInformationProlog = parserBasicInformationText.substring(0, start);
			
			String javadefProlog =
					"interface javadef\n" +  //$NON-NLS-1$
					"{\n" +  //$NON-NLS-1$
					"    public final static int"; //$NON-NLS-1$
			assertTrue(javadefText.startsWith(javadefProlog));
			javadefText = javadefText.substring(javadefProlog.length());
			javadefText = javadefText.replace("\n      ", "\n\t\t\t\t\t"); //$NON-NLS-1$ //$NON-NLS-2$
			javadefText = javadefText.replaceAll(" +", " "); //$NON-NLS-1$ //$NON-NLS-2$
			javadefText = javadefText.replace("};\n\n", "}\n"); //$NON-NLS-1$ //$NON-NLS-2$
			
			Files.write(parserBasicInformationFile.toPath(), (parserBasicInformationProlog + javadefText).getBytes());
		}

		// Update method consumeRule in Parser.java
		File parserFile = new File(parserDir, "Parser.java"); //$NON-NLS-1$
		String parserText = new String(Files.readAllBytes(parserFile.toPath()));
		File javaActionFile = new File(grammarDir, "JavaAction.java"); //$NON-NLS-1$
		String javaActionText = new String(Files.readAllBytes(javaActionFile.toPath())).replace("\r\n",  "\n"); //$NON-NLS-1$ //$NON-NLS-2$
		{
			String startTag = "// BEGIN_AUTOGENERATED_REGION_CONSUME_RULE\n"; //$NON-NLS-1$
			String endTag = "// END_AUTOGENERATED_REGION_CONSUME_RULE\n"; //$NON-NLS-1$
			int start = parserText.indexOf(startTag);
			assertTrue(start >= 0);
			start += startTag.length();
			int end = parserText.indexOf(endTag, start);
			assertTrue(end >= 0);
			
			String newParserText = parserText.substring(0, start) + javaActionText + parserText.substring(end);
			
			Files.write(parserFile.toPath(), newParserText.getBytes());
		}

		// Clean up JikesPG output files
		Files.delete(javadclFile.toPath());
		Files.delete(javahdrFile.toPath());
		Files.delete(javaActionFile.toPath());
		Files.delete(javasymFile.toPath());
		Files.delete(javadefFile.toPath());
		Files.delete(new File(grammarDir, "javaprs.java").toPath()); //$NON-NLS-1$
		Files.delete(new File(grammarDir, "java.l").toPath()); //$NON-NLS-1$
	}

}