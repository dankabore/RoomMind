package com.roommind.dto;

import java.time.Instant;

import lombok.Builder;
import lombok.Getter;

/**
 * The public view of an account. It has no passwordHash field, which is the
 * whole reason the controller returns this instead of the User entity.
 *
 * UserMapper fills it in; the builder is what MapStruct writes against.
 */
@Getter
@Builder
public class UserResponse {

	private final Long id;

	private final String email;

	private final String username;

	private final Instant createdAt;
}
