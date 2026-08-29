package com.roommind.entity;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
 * A registered account. The email is what you log in with, the username is the
 * public handle other people see. Both are unique.
 *
 * No @Data here on purpose: it would generate equals/hashCode over every field,
 * which misbehaves on an entity whose id is assigned by the database.
 */
@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false, unique = true, length = 255)
	private String email;

	@Column(nullable = false, unique = true, length = 50)
	private String username;

	// Never leaves the server: no response DTO exposes it.
	@Column(name = "password_hash", nullable = false, length = 72)
	private String passwordHash;

	@Column(name = "created_at", nullable = false)
	private Instant createdAt;
}
