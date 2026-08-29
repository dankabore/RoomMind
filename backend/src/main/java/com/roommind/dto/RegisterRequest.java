package com.roommind.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * The body of POST /api/auth/register. Jackson builds this by calling the
 * no-argument constructor and then the setters, which is why both are here.
 */
@Getter
@Setter
@NoArgsConstructor
public class RegisterRequest {

	@NotBlank
	@Email
	@Size(max = 255)
	private String email;

	@NotBlank
	@Size(min = 3, max = 50)
	private String username;

	// 72 is BCrypt's own limit: it silently ignores anything past that byte,
	// so we reject it up front rather than accept a password we do not fully use.
	@NotBlank
	@Size(min = 8, max = 72)
	private String password;
}
