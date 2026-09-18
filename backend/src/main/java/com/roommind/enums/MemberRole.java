package com.roommind.enums;

/**
 * What a member is allowed to do in a group.
 *
 * There is one ADMIN per group — whoever created it, until they hand the role
 * on — and they are the only one who can add or remove people. Everyone else is
 * a plain MEMBER. Both people in a direct conversation are MEMBER; the role is
 * not read there.
 *
 * Stored as text, for the same reason as ConversationType.
 */
public enum MemberRole {

	ADMIN,

	MEMBER
}
