package com.roommind.service;

import java.time.Instant;

import com.roommind.dto.ConversationResponse;
import com.roommind.dto.OpenDirectRequest;
import com.roommind.entity.Conversation;
import com.roommind.entity.ConversationMember;
import com.roommind.entity.User;
import com.roommind.mapper.UserMapper;
import com.roommind.repository.ConversationMemberRepository;
import com.roommind.repository.ConversationRepository;
import com.roommind.repository.UserRepository;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import lombok.RequiredArgsConstructor;

/**
 * Opening conversations, and deciding who is allowed into one.
 */
@Service
@RequiredArgsConstructor
public class ConversationService {

	private final ConversationRepository conversationRepository;

	private final ConversationMemberRepository conversationMemberRepository;

	private final UserRepository userRepository;

	private final UserMapper userMapper;

	/**
	 * Hands back the conversation between the caller and one other person,
	 * creating it if this is the first time they have opened it.
	 *
	 * Find-or-create rather than plain create, because opening the same chat
	 * twice must land in the same place. If this only ever created, every visit
	 * would start a fresh thread and the history would scatter across them.
	 *
	 * @Transactional because a conversation with only one of its two members
	 * written is a conversation nobody can use. Either all three rows land or
	 * none do.
	 */
	@Transactional
	public ConversationResponse openDirect(String subject, OpenDirectRequest request) {
		Long callerId = Long.valueOf(subject);
		Long otherUserId = request.getUserId();

		if (callerId.equals(otherUserId)) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "You cannot start a conversation with yourself.");
		}

		User otherUser = userRepository.findById(otherUserId)
			.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "That account does not exist."));

		Conversation conversation = conversationRepository.findDirectBetween(callerId, otherUserId)
			.orElseGet(() -> createDirect(userRepository.getReferenceById(callerId), otherUser));

		return ConversationResponse.builder()
			.id(conversation.getId())
			.otherUser(userMapper.toPersonResponse(otherUser))
			.build();
	}

	/**
	 * Stops here unless the caller is in the conversation. Every endpoint that
	 * touches a conversation's contents starts with this.
	 *
	 * The answer is 404 and not 403 on purpose. "Forbidden" would confirm the
	 * conversation exists, which is enough to work out who is talking to whom by
	 * trying ids one after another. "Not found" tells a stranger nothing, and
	 * reads the same as an id that was never real.
	 */
	public void requireMember(Long conversationId, Long userId) {
		if (!conversationMemberRepository.existsByConversationIdAndUserId(conversationId, userId)) {
			throw new ResponseStatusException(HttpStatus.NOT_FOUND, "That conversation does not exist.");
		}
	}

	private Conversation createDirect(User caller, User otherUser) {
		Instant now = Instant.now();
		Conversation conversation = conversationRepository.save(Conversation.builder().createdAt(now).build());

		conversationMemberRepository.save(member(conversation, caller, now));
		conversationMemberRepository.save(member(conversation, otherUser, now));

		return conversation;
	}

	private ConversationMember member(Conversation conversation, User user, Instant joinedAt) {
		return ConversationMember.builder()
			.conversation(conversation)
			.user(user)
			.joinedAt(joinedAt)
			.build();
	}
}
