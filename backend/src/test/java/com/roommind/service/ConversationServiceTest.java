package com.roommind.service;

import com.roommind.repository.ConversationMemberRepository;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

/**
 * requireMember is the guard every endpoint that touches a conversation's
 * contents starts with, so what it answers to a stranger matters more than
 * most things here: 404 and not 403, or the answer itself tells them the
 * conversation exists.
 *
 * No database and no Spring: the repository is a stand-in that returns
 * whatever each test needs, which is the whole of what a unit test is.
 */
@ExtendWith(MockitoExtension.class)
class ConversationServiceTest {

	@Mock
	private ConversationMemberRepository conversationMemberRepository;

	@InjectMocks
	private ConversationService conversationService;

	@Test
	void letsAMemberThrough() {
		given(conversationMemberRepository.existsByConversationIdAndUserId(1L, 7L)).willReturn(true);

		assertThatCode(() -> conversationService.requireMember(1L, 7L)).doesNotThrowAnyException();
	}

	@Test
	void answersNotFoundToAStranger() {
		given(conversationMemberRepository.existsByConversationIdAndUserId(1L, 99L)).willReturn(false);

		assertThatThrownBy(() -> conversationService.requireMember(1L, 99L))
			.isInstanceOf(ResponseStatusException.class)
			.extracting(exception -> ((ResponseStatusException) exception).getStatusCode())
			.isEqualTo(HttpStatus.NOT_FOUND);
	}
}
