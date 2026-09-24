package com.roommind.entity;

import java.time.Instant;

import com.roommind.enums.ConversationType;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * A thread of messages between people, either a direct one between two or a
 * named group.
 *
 * It holds no list of members or messages. Those point here instead, which is
 * what lets a conversation be read a page at a time rather than loaded whole.
 */
@Entity
@Table(name = "conversations")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Conversation {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	/**
	 * EnumType.STRING, not the default ORDINAL: the default stores the
	 * constant's position, so inserting a value into the enum later would
	 * quietly change what every existing row means.
	 */
	@Enumerated(EnumType.STRING)
	@Column(name = "type", nullable = false, length = 20)
	private ConversationType type;

	/**
	 * Null for a direct conversation, which the database enforces. Its name is
	 * whoever is on the other side, and that differs depending on which of the
	 * two is reading, so there is nothing to store.
	 */
	@Column(name = "name", length = 100)
	private String name;

	@Column(name = "created_at", nullable = false)
	private Instant createdAt;
}
