package com.roommind.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

/**
 * Who you want to talk to. The caller is already known from the token, so the
 * only thing to say is the other person.
 */
@Getter
@Setter
public class OpenDirectRequest {

	@NotNull(message = "Choose someone to message.")
	private Long userId;
}
