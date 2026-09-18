package com.roommind.dto;

import com.roommind.enums.MemberRole;

import lombok.Builder;
import lombok.Getter;

/**
 * One person in a group, as the member list shows them.
 *
 * The id is the person's, not the membership row's: everything the screen does
 * next — open their chat, remove them — is addressed by user id, and the
 * membership row's own id is never needed outside the database.
 *
 * This is PersonResponse plus the role rather than a field added to it, because
 * PersonResponse is also the people page, where roles mean nothing.
 */
@Getter
@Builder
public class GroupMemberResponse {

	private final Long id;

	private final String username;

	private final MemberRole role;
}
