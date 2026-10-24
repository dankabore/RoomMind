package com.roommind.entity;

import java.time.Instant;

import com.roommind.enums.Language;

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
 * A message's saved translation into one language. Written once, the first
 * time anyone reading that language asks, and handed to everyone after.
 *
 * The message is a plain id rather than a link to the Message entity, as in
 * GroupSummary: nothing here needs the message itself. The database still
 * checks that it exists, and removes this row when the message goes.
 *
 * Its own generated id, rather than message and language together as the key:
 * a two-column key in JPA needs a separate key class, and the unique
 * constraint in the migration already keeps each pair to one row.
 */
@Entity
@Table(name = "message_translations")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MessageTranslation {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "message_id", nullable = false)
	private Long messageId;

	@Enumerated(EnumType.STRING)
	@Column(name = "language", nullable = false, length = 5)
	private Language language;

	@Column(nullable = false, columnDefinition = "text")
	private String translation;

	@Column(name = "created_at", nullable = false)
	private Instant createdAt;
}
