package com.roommind.dto;

import lombok.Builder;
import lombok.Getter;

/**
 * One row of the dashboard: a conversation, who it is with, and the last thing
 * said in it.
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

	private final PersonResponse otherUser;

	private final MessageResponse lastMessage;
}
