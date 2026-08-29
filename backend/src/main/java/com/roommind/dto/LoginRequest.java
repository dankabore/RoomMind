package com.roommind.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * The body of POST /api/auth/login. No @Email or @Size here: a badly formed
 * login should fail as "wrong credentials", not as a validation error that
 * tells the caller how the stored passwords are shaped.
 */
@Getter
@Setter
@NoArgsConstructor
public class LoginRequest {

	@NotBlank
	private String email;

	@NotBlank
	private String password;
}
