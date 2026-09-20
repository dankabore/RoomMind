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

	/**
	 * The newest message in every conversation this person belongs to, most
	 * recent first. This is the dashboard: one row per conversation,
	 * ordered by how recently anything was said in it.
	 *
	 * max(id) finds each conversation's latest message because ids only ever
	 * increase. A conversation with no messages has no max, so it drops out on
	 * its own — which keeps chats that were opened but never used off the list.
	 *
	 * The conversation is fetched alongside each message, because the row drawn
	 * from it needs the conversation's kind and, for a group, its name.
	 */
	@Query("""
		select m from Message m
		join fetch m.sender
		join fetch m.conversation
		where m.id in (
			select max(m2.id) from Message m2
			where m2.conversation.id in (
				select cm.conversation.id from ConversationMember cm where cm.user.id = :userId
			)
			group by m2.conversation.id
		)
		order by m.id desc
		""")
	List<Message> findLatestInEachConversationOf(@Param("userId") Long userId);
}
