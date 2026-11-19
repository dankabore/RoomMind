package com.roommind.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * The languages a person can choose to read in. Translation reads it from the
 * reader's profile to know what to translate into.
 *
 * A fixed list rather than free text, so the translator is never handed
 * "francais", "French" and "FR" as three different things. Each constant is the
 * two-letter ISO 639-1 code, the standard short name for a language.
 *
 * Stored as text, for the same reason as ConversationType. Adding a language
 * needs a new migration as well, since the users table checks the value.
 */
@Getter
@RequiredArgsConstructor
public enum Language {

	EN("English"),

	FR("French"),

	ES("Spanish"),

	DE("German"),

	IT("Italian"),

	PT("Portuguese"),

	AR("Arabic"),

	ZH("Chinese");

	/**
	 * What the model is told to translate into. A name rather than the code,
	 * because "translate into French" leaves the model nothing to guess.
	 * The JSON and the database still use the code.
	 */
	private final String englishName;
}
