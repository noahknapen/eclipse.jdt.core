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
							TokenNameabstract = 42,
							TokenNameassert = 85,
							TokenNameboolean = 110,
							TokenNamebreak = 86,
							TokenNamebyte = 111,
							TokenNamecase = 95,
							TokenNamecatch = 112,
							TokenNamechar = 113,
							TokenNameclass = 72,
							TokenNamecontinue = 87,
							TokenNameconst = 138,
							TokenNamedefault = 80,
							TokenNamedo = 88,
							TokenNamedouble = 114,
							TokenNameelse = 125,
							TokenNameenum = 78,
							TokenNameextends = 96,
							TokenNamefalse = 43,
							TokenNamefinal = 44,
							TokenNamefinally = 121,
							TokenNamefloat = 115,
							TokenNamefor = 89,
							TokenNamegoto = 139,
							TokenNameif = 90,
							TokenNameimplements = 135,
							TokenNameimport = 116,
							TokenNameinstanceof = 17,
							TokenNameint = 117,
							TokenNameinterface = 77,
							TokenNamelong = 118,
							TokenNamenative = 45,
							TokenNamenew = 36,
							TokenNamenon_sealed = 46,
							TokenNamenull = 47,
							TokenNamepackage = 94,
							TokenNameprivate = 48,
							TokenNameprotected = 49,
							TokenNamepublic = 50,
							TokenNamereturn = 91,
							TokenNameshort = 119,
							TokenNamestatic = 38,
							TokenNamestrictfp = 51,
							TokenNamesuper = 34,
							TokenNameswitch = 65,
							TokenNamesynchronized = 40,
							TokenNamethis = 35,
							TokenNamethrow = 82,
							TokenNamethrows = 122,
							TokenNametransient = 52,
							TokenNametrue = 53,
							TokenNametry = 92,
							TokenNamevoid = 120,
							TokenNamevolatile = 54,
							TokenNamewhile = 83,
							TokenNamemodule = 123,
							TokenNameopen = 124,
							TokenNamerequires = 126,
							TokenNametransitive = 132,
							TokenNameexports = 127,
							TokenNameopens = 128,
							TokenNameto = 136,
							TokenNameuses = 129,
							TokenNameprovides = 130,
							TokenNamewith = 137,
							TokenNameIntegerLiteral = 55,
							TokenNameLongLiteral = 56,
							TokenNameFloatingPointLiteral = 57,
							TokenNameDoubleLiteral = 58,
							TokenNameCharacterLiteral = 59,
							TokenNameStringLiteral = 60,
							TokenNameTextBlock = 61,
							TokenNamePLUS_PLUS = 2,
							TokenNameMINUS_MINUS = 3,
							TokenNameEQUAL_EQUAL = 20,
							TokenNameLESS_EQUAL = 12,
							TokenNameGREATER_EQUAL = 13,
							TokenNameNOT_EQUAL = 21,
							TokenNameLEFT_SHIFT = 18,
							TokenNameRIGHT_SHIFT = 14,
							TokenNameUNSIGNED_RIGHT_SHIFT = 16,
							TokenNamePLUS_EQUAL = 97,
							TokenNameMINUS_EQUAL = 98,
							TokenNameMULTIPLY_EQUAL = 99,
							TokenNameDIVIDE_EQUAL = 100,
							TokenNameAND_EQUAL = 101,
							TokenNameOR_EQUAL = 102,
							TokenNameXOR_EQUAL = 103,
							TokenNameREMAINDER_EQUAL = 104,
							TokenNameLEFT_SHIFT_EQUAL = 105,
							TokenNameRIGHT_SHIFT_EQUAL = 106,
							TokenNameUNSIGNED_RIGHT_SHIFT_EQUAL = 107,
							TokenNameOR_OR = 31,
							TokenNameAND_AND = 29,
							TokenNamePLUS = 4,
							TokenNameMINUS = 5,
							TokenNameNOT = 68,
							TokenNameREMAINDER = 9,
							TokenNameXOR = 24,
							TokenNameAND = 22,
							TokenNameMULTIPLY = 8,
							TokenNameOR = 28,
							TokenNameTWIDDLE = 69,
							TokenNameDIVIDE = 10,
							TokenNameGREATER = 15,
							TokenNameLESS = 11,
							TokenNameLPAREN = 23,
							TokenNameRPAREN = 26,
							TokenNameLBRACE = 39,
							TokenNameRBRACE = 33,
							TokenNameLBRACKET = 6,
							TokenNameRBRACKET = 71,
							TokenNameSEMICOLON = 25,
							TokenNameQUESTION = 30,
							TokenNameCOLON = 67,
							TokenNameCOMMA = 32,
							TokenNameDOT = 1,
							TokenNameEQUAL = 81,
							TokenNameAT = 37,
							TokenNameELLIPSIS = 108,
							TokenNameARROW = 109,
							TokenNameCOLON_COLON = 7,
							TokenNameBeginLambda = 62,
							TokenNameBeginIntersectionCast = 70,
							TokenNameBeginTypeArguments = 93,
							TokenNameElidedSemicolonAndRightBrace = 75,
							TokenNameAT308 = 27,
							TokenNameAT308DOTDOTDOT = 133,
							TokenNameJAVADOC_FORMAL_PART_START = 63,
							TokenNameJAVADOC_FORMAL_PART_SEPARATOR = 73,
							TokenNameJAVADOC_FORMAL_PART_END = 74,
							TokenNameold = 64,
							TokenNameBeginCaseExpr = 76,
							TokenNameRestrictedIdentifierYield = 84,
							TokenNameRestrictedIdentifierrecord = 79,
							TokenNameRestrictedIdentifiersealed = 41,
							TokenNameRestrictedIdentifierpermits = 131,
							TokenNameBeginCaseElement = 134,
							TokenNameEOF = 66,
							TokenNameERROR = 140;
}
