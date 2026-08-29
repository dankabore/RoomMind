package com.roommind.dto;

import lombok.Builder;
import lombok.Getter;

/**
 * The body of a successful login. expiresIn is in seconds, so the frontend can
 * tell how long the token is good for without decoding it.
 */
@Getter
@Builder
public class TokenResponse {

	private final String token;

	private final long expiresIn;
}
