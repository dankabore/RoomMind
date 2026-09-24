package com.roommind.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

/**
 * Who to add to a group. The group itself is in the path, and the admin doing
 * the adding comes from the token.
 */
@Getter
@Setter
public class AddMemberRequest {

	@NotNull(message = "Choose someone to add.")
	private Long userId;
}
