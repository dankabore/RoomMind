package com.roommind.repository;

import java.util.Collection;
import java.util.List;

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
