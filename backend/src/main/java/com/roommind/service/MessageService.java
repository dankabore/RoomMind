package com.roommind.service;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;

import com.roommind.config.WebSocketConfig;
import com.roommind.dto.MessageResponse;
import com.roommind.dto.SendMessageRequest;
import com.roommind.entity.Message;
import com.roommind.entity.User;
import com.roommind.enums.ConversationType;
import com.roommind.mapper.MessageMapper;
import com.roommind.repository.ConversationRepository;
import com.roommind.repository.MessageRepository;
import com.roommind.repository.UserRepository;

import org.springframework.core.task.TaskRejectedException;
import org.springframework.data.domain.Limit;
import org.springframework.http.HttpStatus;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Writing messages into a conversation and reading them back out.
 *
 * Both operations begin by asking ConversationService whether the caller
 * belongs in the conversation at all, so a signed-in stranger who guesses an id
 * gets nowhere.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MessageService {

	/**
	 * How many messages one read returns. Fifty is roughly a screenful and a
	 * half, so opening a chat shows recent history without waiting for a
	 * conversation that may hold thousands.
	 */
	private static final int PAGE_SIZE = 50;

	// (?<![\w@.]) : not straight after a letter, digit, _, @ or dot.
	// @ai\b       : "@ai", then the end of the word.
	// Compiled once, since every sent message is checked against it.
	private static final Pattern AI_MENTION = Pattern.compile("(?<![\\w@.])@ai\\b", Pattern.CASE_INSENSITIVE);

	private final MessageRepository messageRepository;

	private final ConversationRepository conversationRepository;

	private final UserRepository userRepository;

	private final ConversationService conversationService;

	private final MessageMapper messageMapper;

	private final AssistantService assistantService;

	// Publishes to a websocket address. Everyone subscribed to that address gets
	// a copy; nobody else does.
	private final SimpMessagingTemplate messagingTemplate;

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

		MessageResponse response = messageMapper.toResponse(messageRepository.save(message));

		// Pushed to everyone watching the conversation, the sender's own open
		// connection included. It goes out after save has returned, so nobody is
		// told about a message the database does not yet hold. If this method is
		// ever made @Transactional, that stops being true: the push would leave
		// before the commit and would need to wait for it.
		messagingTemplate.convertAndSend(WebSocketConfig.CONVERSATION_TOPIC + conversationId, response);

		// After the push, so the question is on everyone's screen before any
		// answer could be. The call returns at once: the assistant works in the
		// background and its answer arrives later, as a message of its own.
		if (asksAssistant(conversationId, message.getBody())) {
			try {
				assistantService.answer(conversationId, sender.getUsername(), message.getBody());
			} catch (TaskRejectedException e) {
				// Every assistant thread is busy and the queue is full. The message
				// itself is saved and pushed, so the send still succeeds; only the
				// answer is skipped.
				log.warn("Assistant queue full; question in conversation {} not answered", conversationId);
			}
		}

		return response;
	}

	/**
	 * Whether this message is a question for the assistant: it mentions @ai and
	 * was sent in a group. A direct conversation is left alone, @ai or not, as
	 * the project scope says.
	 *
	 * The text is checked first, because it costs nothing; the group check is a
	 * query, so almost every message never makes it.
	 */
	private boolean asksAssistant(Long conversationId, String body) {
		return mentionsAi(body) && conversationRepository.existsByIdAndType(conversationId, ConversationType.GROUP);
	}

	/**
	 * Whether `@ai` appears as a mention of its own, in any case: "@ai", "@AI,"
	 * and "hey @Ai what did we decide?" all count.
	 *
	 * The two edges are what keep it from firing by accident. Nothing that could
	 * be part of a word or an address may come before the @, so "bob@ai.com" is
	 * not a mention; and the word must end after "ai", so "@aisha" is not one
	 * either.
	 */
	static boolean mentionsAi(String body) {
		return AI_MENTION.matcher(body).find();
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
