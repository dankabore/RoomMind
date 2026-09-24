package com.roommind.dto;

import java.time.Instant;

import com.roommind.enums.ConversationType;

import lombok.Builder;
import lombok.Getter;

/**
 * One row of the dashboard: a conversation, what it is, and the last thing said
 * in it.
 *
 * A row is either a direct conversation or a group, and which one decides what
 * names it: `otherUser` is filled for a direct conversation and null for a
 * group, `name` the other way round. `type` says which without the screen
 * having to work it out from what is missing.
 *
 * The last message comes back whole, in the same shape the chat screen uses,
 * rather than cut down to a preview. How much of it fits on one line depends on
 * how wide the row is, which only the screen knows — and CSS shortens it with
 * one class.
 */
@Getter
@Builder
public class ConversationSummaryResponse {

	private final Long id;

	private final ConversationType type;

	/** The group's name. Null for a direct conversation, which has none. */
	private final String name;

	/** The person on the other side. Null for a group, which has no single one. */
	private final PersonResponse otherUser;

	/**
	 * The last thing said. Null for a group nobody has written in yet, which is
	 * still on the list because it was made on purpose.
	 */
	private final MessageResponse lastMessage;

	/**
	 * When the conversation itself began. The row's time comes from the last
	 * message when there is one; this is what an empty group shows instead, and
	 * what puts it in order among the rest.
	 */
	private final Instant createdAt;
}
