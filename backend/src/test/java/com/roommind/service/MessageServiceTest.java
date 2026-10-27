package com.roommind.service;

import java.util.Optional;

import com.roommind.dto.MessageResponse;
import com.roommind.dto.SendMessageRequest;
import com.roommind.entity.Conversation;
import com.roommind.entity.Message;
import com.roommind.entity.User;
import com.roommind.enums.ConversationType;
import com.roommind.mapper.MessageMapper;
import com.roommind.repository.ConversationRepository;
import com.roommind.repository.MessageRepository;
import com.roommind.repository.UserRepository;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.messaging.simp.SimpMessagingTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * Spotting a question for the assistant. Firing on the wrong message would
 * mean a paid model call nobody asked for; missing one leaves a question
 * unanswered. So both directions are tested.
 */
@ExtendWith(MockitoExtension.class)
class MessageServiceTest {

	@Mock
	private MessageRepository messageRepository;

	@Mock
	private ConversationRepository conversationRepository;

	@Mock
	private UserRepository userRepository;

	@Mock
	private ConversationService conversationService;

	@Mock
	private MessageMapper messageMapper;

	@Mock
	private SimpMessagingTemplate messagingTemplate;

	@InjectMocks
	private MessageService messageService;

	// What counts as a mention.

	@Test
	void anyMentionOfAiCounts() {
		assertThat(MessageService.mentionsAi("@ai what did we decide?")).isTrue();
		assertThat(MessageService.mentionsAi("hey @AI, when is the meeting")).isTrue();
		assertThat(MessageService.mentionsAi("so... @Ai?")).isTrue();
		assertThat(MessageService.mentionsAi("can you sum up\n@ai")).isTrue();
		assertThat(MessageService.mentionsAi("(@ai) thoughts")).isTrue();
	}

	@Test
	void lookalikesDoNotCount() {
		// An email address, a longer name, a handle that only starts the same.
		assertThat(MessageService.mentionsAi("write to bob@ai.com")).isFalse();
		assertThat(MessageService.mentionsAi("@aisha are you coming?")).isFalse();
		assertThat(MessageService.mentionsAi("ping @ai_bot")).isFalse();
		assertThat(MessageService.mentionsAi("@@ai")).isFalse();
		// Talking about AI is not talking to it.
		assertThat(MessageService.mentionsAi("ai is everywhere these days")).isFalse();
	}

	// When the group check runs.

	@Test
	void anOrdinaryMessageCostsNoExtraQuery() {
		givenSendable();

		messageService.send("7", 5L, request("see you friday"));

		verify(conversationRepository, never()).existsByIdAndType(anyLong(), any());
	}

	@Test
	void aMentionChecksThatTheConversationIsAGroup() {
		givenSendable();

		messageService.send("7", 5L, request("@ai what did we decide?"));

		verify(conversationRepository).existsByIdAndType(5L, ConversationType.GROUP);
	}

	private void givenSendable() {
		given(userRepository.findById(7L)).willReturn(Optional.of(User.builder().id(7L).username("alice").build()));
		given(conversationRepository.getReferenceById(5L)).willReturn(Conversation.builder().id(5L).build());
		given(messageRepository.save(any(Message.class))).willAnswer(call -> call.getArgument(0));
		given(messageMapper.toResponse(any(Message.class))).willReturn(MessageResponse.builder().id(30L).build());
	}

	private static SendMessageRequest request(String body) {
		SendMessageRequest request = new SendMessageRequest();
		request.setBody(body);
		return request;
	}
}
