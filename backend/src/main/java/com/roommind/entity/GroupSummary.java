package com.roommind.entity;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * A group's saved summary and how far through the conversation it reaches.
 *
 * The id is the conversation's id, set by the code rather than generated, so
 * there is at most one summary per group. It is a plain id and not a link to
 * the Conversation entity because nothing here needs the conversation itself,
 * only to know which one this belongs to.
 */
@Entity
@Table(name = "group_summaries")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GroupSummary {

	@Id
	@Column(name = "conversation_id")
	private Long conversationId;

	@Column(nullable = false, columnDefinition = "text")
	private String summary;

	@Column(name = "last_message_id", nullable = false)
	private Long lastMessageId;

	@Column(name = "updated_at", nullable = false)
	private Instant updatedAt;
}
