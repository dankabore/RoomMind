package com.roommind.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

/**
 * Who is to become the group's admin. The group is in the path and the admin
 * handing the role over comes from the token, so the new admin is the only
 * thing left to say.
 */
@Getter
@Setter
public class TransferAdminRequest {

	@NotNull(message = "Choose who takes over as admin.")
	private Long userId;
}
