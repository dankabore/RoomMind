package com.roommind.service;

import java.time.Instant;
import java.util.Collections;
import java.util.List;

import com.roommind.dto.MessageResponse;
import com.roommind.dto.SendMessageRequest;
import com.roommind.entity.Message;
import com.roommind.entity.User;
import com.roommind.mapper.MessageMapper;
import com.roommind.repository.ConversationRepository;
import com.roommind.repository.MessageRepository;
import com.roommind.repository.UserRepository;

import org.springframework.data.domain.Limit;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import lombok.RequiredArgsConstructor;

/**
 * Writing messages into a conversation and reading them back out.
 *
 * Both operations begin by asking ConversationService whether the caller
 * belongs in the conversation at all, so a signed-in stranger who guesses an id
 * gets nowhere.
 */
@Service
@RequiredArgsConstructor
public class MessageService {

	/**
	 * How many messages one read returns. Fifty is roughly a screenful and a
	 * half, so opening a chat shows recent history without waiting for a
	 * conversation that may hold thousands.
	 */
	private static final int PAGE_SIZE = 50;

	private final MessageRepository messageRepository;

	private final ConversationRepository conversationRepository;

	private final UserRepository userRepository;

	private final ConversationService conversationService;

	private final MessageMapper messageMapper;

	public MessageResponse send(String subject, Long conversationId, SendMessageRequest request) {
		Long senderId = Long.valueOf(subject);
		conversationService.requireMember(conversationId, senderId);

		// The sender is loaded properly rather than referenced, because the
		// response echoes their username back and a bare reference has no name in
		// it yet.
		User sender = userRepository.findById(senderId)
			.orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "This account no longer exists."));

		Message message = Message.builder()
			// getReferenceById gives a stand-in carrying just the id, which is all
			// that gets written to the conversation_id column. Membership was
			// already confirmed above, so there is nothing to check by loading it.
			.conversation(conversationRepository.getReferenceById(conversationId))
			.sender(sender)
			// Trimmed so trailing newlines from the input box do not become blank
			// space in the chat. @NotBlank has already ruled out an empty result.
			.body(request.getBody().trim())
			.createdAt(Instant.now())
			.build();

		return messageMapper.toResponse(messageRepository.save(message));
	}

	/**
	 * A page of a conversation, oldest first.
	 *
	 * With no `before`, this is the newest fifty — what opening a chat shows.
	 * With one, it is the fifty immediately older than that message, which is
	 * what scrolling up asks for.
	 *
	 * The database is asked for them newest-first, since "the fifty most recent"
	 * is only answerable from that end. They are flipped before returning so the
	 * list reads top to bottom the way a chat is drawn.
	 */
	public List<MessageResponse> read(String subject, Long conversationId, Long before) {
		conversationService.requireMember(conversationId, Long.valueOf(subject));

		Limit limit = Limit.of(PAGE_SIZE);
		List<Message> newestFirst = before == null
			? messageRepository.findNewest(conversationId, limit)
			: messageRepository.findOlderThan(conversationId, before, limit);

		List<MessageResponse> page = messageMapper.toResponses(newestFirst);
		Collections.reverse(page);
		return page;
	}
}
