/*******************************************************************************
 * Copyright (c) 2000, 2019 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.compiler.parser;

/**
 * IMPORTANT NOTE: These constants are dedicated to the internal Scanner implementation.
 * It is mirrored in org.eclipse.jdt.core.compiler public package where it is API.
 * The mirror implementation is using the backward compatible ITerminalSymbols constant
 * definitions (stable with 2.0), whereas the internal implementation uses TerminalTokens
 * which constant values reflect the latest parser generation state.
 */
/**
 * Maps each terminal symbol in the java-grammar into a unique integer.
 * This integer is used to represent the terminal when computing a parsing action.
 *
 * Disclaimer : These constant values are generated automatically using a Java
 * grammar, therefore their actual values are subject to change if new keywords
 * were added to the language (for instance, 'assert' is a keyword in 1.4).
 */
public interface TerminalTokens {

	// special tokens not part of grammar - not autogenerated
	int TokenNameNotAToken = 0,
							TokenNameWHITESPACE = 1000,
							TokenNameCOMMENT_LINE = 1001,
							TokenNameCOMMENT_BLOCK = 1002,
							TokenNameCOMMENT_JAVADOC = 1003;

	// BEGIN_AUTOGENERATED_REGION
	int TokenNameIdentifier = 19,
							TokenNameabstract = 53,
							TokenNameassert = 84,
							TokenNameboolean = 108,
							TokenNamebreak = 85,
							TokenNamebyte = 109,
							TokenNamecase = 94,
							TokenNamecatch = 110,
							TokenNamechar = 111,
							TokenNameclass = 70,
							TokenNamecontinue = 86,
							TokenNameconst = 134,
							TokenNamedefault = 79,
							TokenNamedo = 87,
							TokenNamedouble = 112,
							TokenNameelse = 121,
							TokenNameenum = 76,
							TokenNameextends = 95,
							TokenNamefalse = 40,
							TokenNamefinal = 54,
							TokenNamefinally = 119,
							TokenNamefloat = 113,
							TokenNamefor = 88,
							TokenNamegoto = 135,
							TokenNameif = 89,
							TokenNameimplements = 131,
							TokenNameimport = 114,
							TokenNameinstanceof = 17,
							TokenNameint = 115,
							TokenNameinterface = 75,
							TokenNamelong = 116,
							TokenNamenative = 55,
							TokenNamenew = 36,
							TokenNamenull = 41,
							TokenNamepackage = 93,
							TokenNameprivate = 56,
							TokenNameprotected = 57,
							TokenNamepublic = 58,
							TokenNamereturn = 90,
							TokenNameshort = 117,
							TokenNamestatic = 39,
							TokenNamestrictfp = 59,
							TokenNamesuper = 34,
							TokenNameswitch = 63,
							TokenNamesynchronized = 42,
							TokenNamethis = 35,
							TokenNamethrow = 81,
							TokenNamethrows = 120,
							TokenNametransient = 60,
							TokenNametrue = 43,
							TokenNametry = 91,
							TokenNamevoid = 118,
							TokenNamevolatile = 61,
							TokenNamewhile = 82,
							TokenNamemodule = 122,
							TokenNameopen = 123,
							TokenNamerequires = 124,
							TokenNametransitive = 129,
							TokenNameexports = 125,
							TokenNameopens = 126,
							TokenNameto = 132,
							TokenNameuses = 127,
							TokenNameprovides = 128,
							TokenNamewith = 133,
							TokenNameIntegerLiteral = 44,
							TokenNameLongLiteral = 45,
							TokenNameFloatingPointLiteral = 46,
							TokenNameDoubleLiteral = 47,
							TokenNameCharacterLiteral = 48,
							TokenNameStringLiteral = 49,
							TokenNameTextBlock = 50,
							TokenNamePLUS_PLUS = 2,
							TokenNameMINUS_MINUS = 3,
							TokenNameEQUAL_EQUAL = 20,
							TokenNameLESS_EQUAL = 12,
							TokenNameGREATER_EQUAL = 13,
							TokenNameNOT_EQUAL = 21,
							TokenNameLEFT_SHIFT = 18,
							TokenNameRIGHT_SHIFT = 14,
							TokenNameUNSIGNED_RIGHT_SHIFT = 16,
							TokenNamePLUS_EQUAL = 96,
							TokenNameMINUS_EQUAL = 97,
							TokenNameMULTIPLY_EQUAL = 98,
							TokenNameDIVIDE_EQUAL = 99,
							TokenNameAND_EQUAL = 100,
							TokenNameOR_EQUAL = 101,
							TokenNameXOR_EQUAL = 102,
							TokenNameREMAINDER_EQUAL = 103,
							TokenNameLEFT_SHIFT_EQUAL = 104,
							TokenNameRIGHT_SHIFT_EQUAL = 105,
							TokenNameUNSIGNED_RIGHT_SHIFT_EQUAL = 106,
							TokenNameOR_OR = 31,
							TokenNameAND_AND = 30,
							TokenNamePLUS = 4,
							TokenNameMINUS = 5,
							TokenNameNOT = 66,
							TokenNameREMAINDER = 9,
							TokenNameXOR = 24,
							TokenNameAND = 22,
							TokenNameMULTIPLY = 8,
							TokenNameOR = 28,
							TokenNameTWIDDLE = 67,
							TokenNameDIVIDE = 10,
							TokenNameGREATER = 15,
							TokenNameLESS = 11,
							TokenNameLPAREN = 23,
							TokenNameRPAREN = 25,
							TokenNameLBRACE = 37,
							TokenNameRBRACE = 33,
							TokenNameLBRACKET = 6,
							TokenNameRBRACKET = 69,
							TokenNameSEMICOLON = 26,
							TokenNameQUESTION = 29,
							TokenNameCOLON = 65,
							TokenNameCOMMA = 32,
							TokenNameDOT = 1,
							TokenNameEQUAL = 80,
							TokenNameAT = 38,
							TokenNameELLIPSIS = 78,
							TokenNameARROW = 107,
							TokenNameCOLON_COLON = 7,
							TokenNameBeginLambda = 51,
							TokenNameBeginIntersectionCast = 68,
							TokenNameBeginTypeArguments = 92,
							TokenNameElidedSemicolonAndRightBrace = 73,
							TokenNameAT308 = 27,
							TokenNameAT308DOTDOTDOT = 130,
							TokenNameBeginCaseExpr = 74,
							TokenNameRestrictedIdentifierYield = 83,
							TokenNameRestrictedIdentifierrecord = 77,
							TokenNameJAVADOC_FORMAL_PART_START = 62,
							TokenNameJAVADOC_FORMAL_PART_SEPARATOR = 71,
							TokenNameJAVADOC_FORMAL_PART_END = 72,
							TokenNameold = 52,
							TokenNameEOF = 64,
							TokenNameERROR = 136;
}
