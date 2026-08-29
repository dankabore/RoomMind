package com.roommind.mapper;

import com.roommind.dto.UserResponse;
import com.roommind.entity.User;

import org.mapstruct.Mapper;

/**
 * Turns a User into the shape the API returns. Every field is a straight copy,
 * which is exactly the case MapStruct is for — it writes the implementation at
 * compile time, so a field added to one side and forgotten on the other becomes
 * a build warning instead of a silently missing value in the JSON.
 *
 * The other direction is not here on purpose. Building a User from a
 * RegisterRequest is not a copy: the email is lowercased, the password is
 * hashed, and createdAt is generated. AuthService does that by hand.
 */
@Mapper(componentModel = "spring")
public interface UserMapper {

	UserResponse toResponse(User user);
}
