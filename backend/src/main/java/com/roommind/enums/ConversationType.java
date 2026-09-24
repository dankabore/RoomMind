package com.roommind.enums;

/**
 * What kind of conversation this is.
 *
 * A DIRECT conversation is found by who is in it — there is exactly one between
 * any two people — while a GROUP is opened by its id and keeps its identity as
 * people are added and removed. That difference is why the kind has to be
 * stored: once groups exist, two people sharing a two-person conversation no
 * longer means it is their private one.
 *
 * Stored as text rather than a number, so a row still says GROUP when read
 * straight from the database and reordering the constants cannot silently
 * change what existing rows mean.
 */
public enum ConversationType {

	DIRECT,

	GROUP
}
