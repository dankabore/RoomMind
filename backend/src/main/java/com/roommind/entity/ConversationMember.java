package com.roommind.entity;

import java.time.Instant;

import com.roommind.enums.MemberRole;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * One person's place in one conversation. This is the row every permission
 * check in the feature comes down to: if it is missing, you cannot read the
 * conversation or post to it.
 *
 * It is a real entity rather than a many-to-many mapping between Conversation
 * and User, because a plain many-to-many can only record that a link exists.
 * This table also has to say when someone joined and whether they run the
 * group.
 *
 * Both links are LAZY: loading a membership row to answer "is this person
 * allowed in" should not drag the conversation and the account along with it.
 */
@Entity
@Table(name = "conversation_members")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ConversationMember {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "conversation_id", nullable = false)
	private Conversation conversation;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "user_id", nullable = false)
	private User user;

	@Column(name = "joined_at", nullable = false)
	private Instant joinedAt;

	/**
	 * Whether this person runs the group. It lives here rather than as an owner
	 * column on Conversation so that the admin is, by construction, someone who
	 * is actually a member, and so handing the role on is an update of two of
	 * these rows instead of a column that can end up pointing at someone who
	 * has left.
	 *
	 * Both members of a direct conversation are MEMBER and nothing reads it.
	 */
	@Enumerated(EnumType.STRING)
	@Column(name = "role", nullable = false, length = 20)
	private MemberRole role;
}
