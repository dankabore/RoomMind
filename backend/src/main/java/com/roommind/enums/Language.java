package com.roommind.enums;

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
public enum Language {

	EN,

	FR,

	ES,

	DE,

	IT,

	PT,

	AR,

	ZH
}
