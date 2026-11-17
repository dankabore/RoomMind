package com.roommind.service;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import com.roommind.config.WebSocketConfig;
import com.roommind.dto.MessageResponse;
import com.roommind.entity.Conversation;
import com.roommind.entity.Message;
import com.roommind.entity.User;
import com.roommind.mapper.MessageMapper;
import com.roommind.repository.ConversationRepository;
import com.roommind.repository.MessageRepository;
import com.roommind.repository.UserRepository;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.data.domain.Limit;
import org.springframework.http.HttpStatus;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.server.ResponseStatusException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * What the assistant posts, and as whom. Called directly here, so it runs on
 * the test's own thread; running in the background is Spring's doing and only
 * happens inside the running app.
 */
@ExtendWith(MockitoExtension.class)
class AssistantServiceTest {

	private static final Limit LIMIT = Limit.of(SummaryService.MAX_MESSAGES);

	@Mock
	private MessageRepository messageRepository;

	@Mock
	private ConversationRepository conversationRepository;

	@Mock
	private UserRepository userRepository;

	@Mock
	private AiService aiService;

	@Mock
	private MessageMapper messageMapper;

	@Mock
	private SimpMessagingTemplate messagingTemplate;

	@InjectMocks
	private AssistantService assistantService;

	private final User alice = User.builder().id(7L).username("alice").build();

	private final User assistant = User.builder().id(1L).username("ai").assistant(true).build();

	@Test
	void asksTheModelWithTheQuestionAndTheConversation() {
		givenHistory();
		givenPostable();
		given(aiService.answer(anyString(), anyString(), anyString())).willReturn("Friday at 7pm.");

		assistantService.answer(5L, "alice", "@ai when are we meeting?");

		verify(aiService).answer("alice", "@ai when are we meeting?", """
			[2026-10-03 14:05] alice: friday at 7pm then
			[2026-10-03 14:06] alice: @ai when are we meeting?
			""");
	}

	@Test
	void theAnswerIsSavedFromTheAssistantAndPushedToTheGroup() {
		givenHistory();
		givenPostable();
		given(aiService.answer(anyString(), anyString(), anyString())).willReturn("Friday at 7pm.");
		// The real save hands back the stored message; the mock has to be told to.
		given(messageRepository.save(any(Message.class))).willAnswer(call -> call.getArgument(0));
		MessageResponse pushed = MessageResponse.builder().id(31L).build();
		given(messageMapper.toResponse(any(Message.class))).willReturn(pushed);

		assistantService.answer(5L, "alice", "@ai when are we meeting?");

		Message saved = savedMessage();
		assertThat(saved.getSender()).isSameAs(assistant);
		assertThat(saved.getConversation().getId()).isEqualTo(5L);
		assertThat(saved.getBody()).isEqualTo("Friday at 7pm.");
		assertThat(saved.getCreatedAt()).isNotNull();
		verify(messagingTemplate).convertAndSend(WebSocketConfig.CONVERSATION_TOPIC + 5L, pushed);
	}

	@Test
	void aFailureIsPostedIntoTheChatRatherThanLost() {
		givenHistory();
		givenPostable();
		given(aiService.answer(anyString(), anyString(), anyString()))
			.willThrow(new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
					"The AI service is busy right now. Try again in a minute."));

		assistantService.answer(5L, "alice", "@ai when are we meeting?");

		assertThat(savedMessage().getBody())
			.isEqualTo("Sorry, I could not answer that. The AI service is busy right now. Try again in a minute.");
	}

	@Test
	void anAnswerLongerThanAMessageIsCutToFit() {
		givenHistory();
		givenPostable();
		given(aiService.answer(anyString(), anyString(), anyString())).willReturn("x".repeat(5000));

		assistantService.answer(5L, "alice", "@ai tell me everything");

		assertThat(savedMessage().getBody()).hasSize(4000);
	}

	@Test
	void aGroupThatEndedMeanwhileIsNotAnError() {
		// The save is refused because the conversation is gone. Nothing may
		// escape: on a background thread there is nobody to catch it.
		givenHistory();
		givenPostable();
		given(aiService.answer(anyString(), anyString(), anyString())).willReturn("Friday at 7pm.");
		given(messageRepository.save(any(Message.class))).willThrow(new IllegalStateException("gone"));

		assertThatCode(() -> assistantService.answer(5L, "alice", "@ai when?")).doesNotThrowAnyException();
		verify(messagingTemplate, never()).convertAndSend(anyString(), any(Object.class));
	}

	private void givenHistory() {
		// Newest first, as the real query answers.
		given(messageRepository.findNewest(5L, LIMIT)).willReturn(List.of(
			message(30L, "@ai when are we meeting?", "2026-10-03T14:06:00Z"),
			message(29L, "friday at 7pm then", "2026-10-03T14:05:00Z")));
	}

	private void givenPostable() {
		given(userRepository.findByAssistantTrue()).willReturn(Optional.of(assistant));
		given(conversationRepository.getReferenceById(5L)).willReturn(Conversation.builder().id(5L).build());
	}

	private Message savedMessage() {
		ArgumentCaptor<Message> saved = ArgumentCaptor.forClass(Message.class);
		verify(messageRepository).save(saved.capture());
		return saved.getValue();
	}

	private Message message(Long id, String body, String at) {
		return Message.builder().id(id).sender(alice).body(body).createdAt(Instant.parse(at)).build();
	}
}
