package com.roommind.repository;

import com.roommind.entity.ConversationMember;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ConversationMemberRepository extends JpaRepository<ConversationMember, Long> {

	/**
	 * The permission check for the whole feature: is this person in this
	 * conversation. Asking for the boolean rather than loading the row keeps it
	 * to a single count query.
	 */
	boolean existsByConversationIdAndUserId(Long conversationId, Long userId);
}
