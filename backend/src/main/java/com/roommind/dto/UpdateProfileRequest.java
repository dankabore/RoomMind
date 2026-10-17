package com.roommind.dto;

import com.roommind.enums.Language;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

/**
 * The body of PUT /api/users/me. Both fields are sent every time — PUT replaces
 * the profile as a whole, so leaving displayName out clears it rather than
 * keeping the old one.
 *
 * A language the enum does not know ("klingon") never reaches the validation
 * below: Jackson cannot turn it into a Language and the request is a 400
 * before the controller runs.
 */
@Getter
@Setter
public class UpdateProfileRequest {

	// Optional. Empty or only spaces is stored as "not set".
	@Size(max = 50, message = "That display name is too long.")
	private String displayName;

	@NotNull(message = "Choose a language.")
	private Language language;
}
