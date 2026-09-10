package com.roommind.dto;

import lombok.Builder;
import lombok.Getter;

/**
 * A direct conversation as it looks to one of the two people in it. `otherUser`
 * is the person on the far side — whichever of the pair is not the caller —
 * because that is the name a chat screen puts at the top.
 *
 * Group chats will need a different shape, since there is no single other
 * person. That is the group phase's problem; nothing here anticipates it.
 */
@Getter
@Builder
public class ConversationResponse {

	private final Long id;

	private final PersonResponse otherUser;
}
