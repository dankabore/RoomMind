package com.roommind.dto;

import lombok.Builder;
import lombok.Getter;

/**
 * How another account appears on the people page: the handle you know them by
 * and the id needed to start a conversation with them.
 *
 * Separate from UserResponse on purpose. That one describes you to yourself and
 * includes your email; this one describes everybody else to everybody else, and
 * listing every address in the database on a page any signed-in person can open
 * would hand out contact details nobody agreed to share.
 */
@Getter
@Builder
public class PersonResponse {

	private final Long id;

	private final String username;
}
