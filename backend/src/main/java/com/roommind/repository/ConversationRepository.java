package com.roommind.repository;

import java.util.Optional;

import com.roommind.entity.Conversation;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ConversationRepository extends JpaRepository<Conversation, Long> {

	/**
	 * The one-to-one conversation these two people share, if they already have
	 * one. This is what stops a second conversation appearing every time someone
	 * opens the same chat.
	 *
	 * The member count of exactly two is doing real work. Without it, a group
	 * that happened to contain both people would match here, and a private
	 * message would be posted into that group instead.
	 */
	@Query("""
		select c from Conversation c
		where exists (select 1 from ConversationMember m where m.conversation = c and m.user.id = :userId)
		  and exists (select 1 from ConversationMember m where m.conversation = c and m.user.id = :otherUserId)
		  and (select count(m) from ConversationMember m where m.conversation = c) = 2
		""")
	Optional<Conversation> findDirectBetween(@Param("userId") Long userId, @Param("otherUserId") Long otherUserId);
}
