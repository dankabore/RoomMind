package com.roommind.dto;

import java.util.List;

import lombok.Builder;
import lombok.Getter;

/**
 * A group and who is in it. Sent back whenever the membership changes, so the
 * screen can redraw the list from the answer instead of asking again.
 *
 * Separate from ConversationResponse, which has a single `otherUser` and only
 * makes sense for a conversation between two people.
 */
@Getter
@Builder
public class GroupResponse {

	private final Long id;

	private final String name;

	private final List<GroupMemberResponse> members;
}
