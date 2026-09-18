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
	 * Both conditions are doing real work. Without the type, a two-person group
	 * would answer as these people's private conversation; without the count of
	 * exactly two, a larger group containing both of them would. Either way a
	 * private message would end up posted to a group.
	 */
	@Query("""
		select c from Conversation c
		where c.type = com.roommind.enums.ConversationType.DIRECT
		  and exists (select 1 from ConversationMember m where m.conversation = c and m.user.id = :userId)
		  and exists (select 1 from ConversationMember m where m.conversation = c and m.user.id = :otherUserId)
		  and (select count(m) from ConversationMember m where m.conversation = c) = 2
		""")
	Optional<Conversation> findDirectBetween(@Param("userId") Long userId, @Param("otherUserId") Long otherUserId);
}
