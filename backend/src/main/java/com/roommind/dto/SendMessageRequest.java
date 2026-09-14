package com.roommind.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

/**
 * The text being sent. Which conversation it goes to is in the URL, and who
 * sent it comes from the token — neither is accepted from the body, so nobody
 * can post as somebody else by editing the request.
 */
@Getter
@Setter
public class SendMessageRequest {

	// NotBlank rather than NotNull: a message of nothing but spaces is not a
	// message, and would show up in the chat as an empty bubble.
	@NotBlank(message = "A message cannot be empty.")
	@Size(max = 4000, message = "A message cannot be longer than 4000 characters.")
	private String body;
}
