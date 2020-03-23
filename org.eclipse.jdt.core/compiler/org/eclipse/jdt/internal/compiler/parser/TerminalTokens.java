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
							TokenNameabstract = 54,
							TokenNameassert = 85,
							TokenNameboolean = 110,
							TokenNamebreak = 86,
							TokenNamebyte = 111,
							TokenNamecase = 95,
							TokenNamecatch = 112,
							TokenNamechar = 113,
							TokenNameclass = 72,
							TokenNamecontinue = 87,
							TokenNameconst = 137,
							TokenNamedefault = 80,
							TokenNamedo = 88,
							TokenNamedouble = 114,
							TokenNameelse = 123,
							TokenNameenum = 78,
							TokenNameextends = 96,
							TokenNamefalse = 40,
							TokenNamefinal = 55,
							TokenNamefinally = 121,
							TokenNamefloat = 115,
							TokenNamefor = 89,
							TokenNamegoto = 138,
							TokenNameif = 90,
							TokenNameimplements = 134,
							TokenNameimport = 116,
							TokenNameinstanceof = 17,
							TokenNameint = 117,
							TokenNameinterface = 77,
							TokenNamelong = 118,
							TokenNamenative = 56,
							TokenNamenew = 36,
							TokenNamenon_sealed = 57,
							TokenNamenull = 41,
							TokenNamepackage = 94,
							TokenNameprivate = 58,
							TokenNameprotected = 59,
							TokenNamepublic = 60,
							TokenNamereturn = 91,
							TokenNameshort = 119,
							TokenNamestatic = 39,
							TokenNamestrictfp = 61,
							TokenNamesuper = 34,
							TokenNameswitch = 65,
							TokenNamesynchronized = 42,
							TokenNamethis = 35,
							TokenNamethrow = 82,
							TokenNamethrows = 122,
							TokenNametransient = 62,
							TokenNametrue = 43,
							TokenNametry = 92,
							TokenNamevoid = 120,
							TokenNamevolatile = 63,
							TokenNamewhile = 83,
							TokenNamemodule = 124,
							TokenNameopen = 125,
							TokenNamerequires = 126,
							TokenNametransitive = 132,
							TokenNameexports = 127,
							TokenNameopens = 128,
							TokenNameto = 135,
							TokenNameuses = 129,
							TokenNameprovides = 130,
							TokenNamewith = 136,
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
							TokenNameAND_AND = 30,
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
							TokenNameRPAREN = 25,
							TokenNameLBRACE = 38,
							TokenNameRBRACE = 33,
							TokenNameLBRACKET = 6,
							TokenNameRBRACKET = 71,
							TokenNameSEMICOLON = 26,
							TokenNameQUESTION = 29,
							TokenNameCOLON = 67,
							TokenNameCOMMA = 32,
							TokenNameDOT = 1,
							TokenNameEQUAL = 81,
							TokenNameAT = 37,
							TokenNameELLIPSIS = 108,
							TokenNameARROW = 109,
							TokenNameCOLON_COLON = 7,
							TokenNameBeginLambda = 51,
							TokenNameBeginIntersectionCast = 70,
							TokenNameBeginTypeArguments = 93,
							TokenNameElidedSemicolonAndRightBrace = 75,
							TokenNameAT308 = 27,
							TokenNameAT308DOTDOTDOT = 133,
							TokenNameJAVADOC_FORMAL_PART_START = 64,
							TokenNameJAVADOC_FORMAL_PART_SEPARATOR = 73,
							TokenNameJAVADOC_FORMAL_PART_END = 74,
							TokenNameold = 52,
							TokenNameBeginCaseExpr = 76,
							TokenNameRestrictedIdentifierYield = 84,
							TokenNameRestrictedIdentifierrecord = 79,
							TokenNameRestrictedIdentifiersealed = 53,
							TokenNameRestrictedIdentifierpermits = 131,
							TokenNameEOF = 66,
							TokenNameERROR = 139;
}
