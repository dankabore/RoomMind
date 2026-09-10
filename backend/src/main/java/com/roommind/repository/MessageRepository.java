package com.roommind.repository;

import java.util.List;

import com.roommind.entity.Message;

import org.springframework.data.domain.Limit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MessageRepository extends JpaRepository<Message, Long> {

	/**
	 * The newest messages in a conversation.
	 *
	 * `join fetch m.sender` loads each message's author in the same query. The
	 * sender link is lazy, so without it, reading fifty senders' names would set
	 * off fifty extra queries — the problem usually called N+1.
	 */
	@Query("""
		select m from Message m
		join fetch m.sender
		where m.conversation.id = :conversationId
		order by m.id desc
		""")
	List<Message> findNewest(@Param("conversationId") Long conversationId, Limit limit);

	/**
	 * The same, but only messages older than one already on screen. Scrolling up
	 * walks backwards through the conversation by passing the oldest id it holds.
	 *
	 * Paging on the id rather than counting rows to skip means new arrivals at
	 * the bottom cannot shift the window and make a message appear twice or get
	 * missed. Ids only ever increase, so "older than this one" stays true.
	 */
	@Query("""
		select m from Message m
		join fetch m.sender
		where m.conversation.id = :conversationId and m.id < :before
		order by m.id desc
		""")
	List<Message> findOlderThan(@Param("conversationId") Long conversationId, @Param("before") Long before,
			Limit limit);
}
