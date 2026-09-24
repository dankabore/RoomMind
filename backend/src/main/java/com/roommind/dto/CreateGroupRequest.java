package com.roommind.dto;

import java.util.List;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

/**
 * A new group: what it is called and who is in it at the start.
 *
 * The creator is not listed here — they are already known from the token, and
 * they become the group's admin. The member list may be left out entirely; a
 * group of one is a valid thing to create and people can be added afterwards.
 */
@Getter
@Setter
public class CreateGroupRequest {

	@NotBlank(message = "Give the group a name.")
	@Size(max = 100, message = "That name is too long.")
	private String name;

	private List<Long> memberIds;
}
