package com.jantvrdik.intellij.latte.parser;

import com.intellij.lang.PsiBuilder;
import com.intellij.lang.parser.GeneratedParserUtilBase;
import com.jantvrdik.intellij.latte.config.LatteConfiguration;
import com.jantvrdik.intellij.latte.config.LatteMacro;

import static com.jantvrdik.intellij.latte.psi.LatteTypes.*;

/**
 * External rules for LatteParser.
 */
public class LatteParserUtil extends GeneratedParserUtilBase {

	/**
	 * Looks for a classic macro a returns true if it finds the macro a and it is pair or unpaired (based on pair parameter).
	 */
	public static boolean checkPairMacro(PsiBuilder builder, int level, boolean pair) {
		if (builder.getTokenType() != T_MACRO_OPEN_TAG_OPEN) return false;

		PsiBuilder.Marker marker = builder.mark();
		String macroName;

		consumeTokenFast(builder, T_MACRO_OPEN_TAG_OPEN);
		consumeTokenFast(builder, T_MACRO_NOESCAPE);
		if (nextTokenIsFast(builder, T_MACRO_NAME, T_MACRO_SHORTNAME)) {
			macroName = builder.getTokenText();
			assert macroName != null;

		} else {
			macroName = "=";
		}

		boolean result;

		// hard coded rule for macro _ because of dg's poor design decision
		// macro _ is pair only if it has empty arguments, otherwise it is unpaired
		// see https://github.com/nette/nette/blob/v2.1.2/Nette/Latte/Macros/CoreMacros.php#L193
		if (macroName.equals("_")) {
			boolean emptyArgs = true;
			builder.advanceLexer();
			while (emptyArgs && nextTokenIsFast(builder, T_MACRO_ARGS, T_MACRO_ARGS_NUMBER, T_MACRO_ARGS_STRING, T_MACRO_ARGS_VAR)) {
				emptyArgs = (builder.getTokenText().trim().length() == 0);
				builder.advanceLexer();
			}
			result = (emptyArgs == pair);

		// all other macros which respect rules
		} else {
			LatteMacro macro = LatteConfiguration.INSTANCE.getMacro(builder.getProject(), macroName);
			result = (macro != null ? (macro.type == (pair ? LatteMacro.Type.PAIR : LatteMacro.Type.UNPAIRED)) : !pair);
		}

		marker.rollbackTo();
		return result;
	}

	/**
	 * Looks for token T_HTML_TAG_NAME and returns true if the HTML tag is void (e.g. <img>).
	 */
	public static boolean checkVoidTag(PsiBuilder builder, int level) {
		if (builder.getTokenType() != T_HTML_TAG_NAME) return false;
		String tagName = builder.getTokenText();
		return LatteConfiguration.INSTANCE.isHtmlTagVoid(tagName);
	}
}