package com.roommind.service;

import java.time.Instant;
import java.util.List;

import com.roommind.config.WebSocketConfig;
import com.roommind.entity.Message;
import com.roommind.entity.User;
import com.roommind.mapper.MessageMapper;
import com.roommind.repository.ConversationRepository;
import com.roommind.repository.MessageRepository;
import com.roommind.repository.UserRepository;

import org.springframework.data.domain.Limit;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * The @ai assistant: reads the recent conversation, asks the model the
 * question, and posts the answer into the group as a message of its own.
 *
 * The answer is saved and pushed here rather than through MessageService.send.
 * That method is for people (it checks membership, and looks for @ai), and
 * going around it is also what stops an answer that happens to contain "@ai"
 * from setting off another answer.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AssistantService {

	// The messages table's limit. An answer is asked to be short, but the model
	// decides, and one over the limit would otherwise fail to save.
	private static final int MAX_BODY = 4000;

	private final MessageRepository messageRepository;

	private final ConversationRepository conversationRepository;

	private final UserRepository userRepository;

	private final AiService aiService;

	private final MessageMapper messageMapper;

	private final SimpMessagingTemplate messagingTemplate;

	/**
	 * Answers one question. @Async: the caller gets control back immediately
	 * and this runs on a background thread, so sending the question is as fast
	 * as sending any other message.
	 *
	 * The same number of recent messages as a summary reads, in the same
	 * layout. The question is already saved by the time this runs, so it is the
	 * last line of what the model sees.
	 *
	 * Nobody is waiting on an HTTP response here, so a failure cannot be
	 * returned to anyone. It is posted into the chat instead, in the assistant's
	 * name: silence would leave the person who asked wondering whether @ai did
	 * anything at all.
	 */
	@Async
	public void answer(Long conversationId, String askerUsername, String question) {
		String body;
		try {
			List<Message> newestFirst = messageRepository.findNewest(conversationId,
					Limit.of(SummaryService.MAX_MESSAGES));
			body = aiService.answer(askerUsername, question,
					SummaryService.transcript(newestFirst, "(Earlier messages are not included.)"));
		} catch (ResponseStatusException e) {
			// AiService has already logged the cause and worded the reason for a
			// reader ("The AI service is busy right now. Try again in a minute.").
			body = "Sorry, I could not answer that. " + e.getReason();
		} catch (RuntimeException e) {
			log.error("The assistant failed in conversation {}", conversationId, e);
			body = "Sorry, I could not answer that. Something went wrong.";
		}

		try {
			post(conversationId, body);
		} catch (RuntimeException e) {
			// Most likely the group ended while the model was thinking, so there
			// is no conversation left to post into.
			log.warn("Could not post the assistant's answer in conversation {}: {}", conversationId, e.getMessage());
		}
	}

	/**
	 * Saves the answer as a message from the assistant's account and pushes it
	 * to everyone watching the group, exactly as a person's message is.
	 */
	private void post(Long conversationId, String body) {
		User assistant = userRepository.findByAssistantTrue()
			.orElseThrow(() -> new IllegalStateException("The assistant account is missing."));

		Message message = Message.builder()
			.conversation(conversationRepository.getReferenceById(conversationId))
			.sender(assistant)
			.body(body.length() > MAX_BODY ? body.substring(0, MAX_BODY) : body)
			.createdAt(Instant.now())
			.build();

		messagingTemplate.convertAndSend(WebSocketConfig.CONVERSATION_TOPIC + conversationId,
				messageMapper.toResponse(messageRepository.save(message)));
	}
}
