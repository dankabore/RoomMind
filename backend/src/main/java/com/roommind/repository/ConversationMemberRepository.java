package com.roommind.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import com.roommind.entity.ConversationMember;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ConversationMemberRepository extends JpaRepository<ConversationMember, Long> {

	/**
	 * The permission check for the whole feature: is this person in this
	 * conversation. Asking for the boolean rather than loading the row keeps it
	 * to a single count query.
	 */
	boolean existsByConversationIdAndUserId(Long conversationId, Long userId);

	/**
	 * How many people are in a conversation. Leaving reads this to tell the two
	 * cases apart: an admin with others still in the group has to hand the role
	 * on first, while an admin who is the last one left takes the group with
	 * them.
	 */
	long countByConversationId(Long conversationId);

	/**
	 * One person's membership row with the conversation attached, which is what
	 * every group permission check needs: whether they are in it at all, whether
	 * it is a group, and whether they are its admin. The fetch join answers all
	 * three from one query rather than loading the conversation on first touch.
	 */
	@Query("""
		select cm from ConversationMember cm
		join fetch cm.conversation
		where cm.conversation.id = :conversationId and cm.user.id = :userId
		""")
	Optional<ConversationMember> findMembership(@Param("conversationId") Long conversationId,
			@Param("userId") Long userId);

	/**
	 * Everyone in one conversation, by name, with their accounts joined in. This
	 * is a group's member list; without the fetch join, reading each username
	 * would cost a query of its own.
	 */
	@Query("""
		select cm from ConversationMember cm
		join fetch cm.user
		where cm.conversation.id = :conversationId
		order by cm.user.username
		""")
	List<ConversationMember> findMembersOf(@Param("conversationId") Long conversationId);

	/**
	 * Everyone in these conversations except the caller — for a direct
	 * conversation, the one person on the other side. Fetched for all the
	 * conversations at once with their accounts joined in, so a dashboard of
	 * twenty conversations costs one query here rather than twenty.
	 */
	@Query("""
		select cm from ConversationMember cm
		join fetch cm.user
		where cm.conversation.id in :conversationIds and cm.user.id <> :userId
		""")
	List<ConversationMember> findOtherMembers(@Param("conversationIds") Collection<Long> conversationIds,
			@Param("userId") Long userId);
}
