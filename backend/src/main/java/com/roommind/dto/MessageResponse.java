package com.roommind.dto;

import java.time.Instant;

import lombok.Builder;
import lombok.Getter;

/**
 * One message as the chat screen needs it. The sender's username travels with
 * it so drawing the list never has to go looking up who wrote what.
 *
 * `senderId` is separate from the name because the screen compares it against
 * the reader's own id to decide which side of the chat a message sits on.
 */
@Getter
@Builder
public class MessageResponse {

	private final Long id;

	private final Long conversationId;

	private final Long senderId;

	private final String senderUsername;

	private final String body;

	private final Instant createdAt;
}
