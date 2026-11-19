package com.roommind.controller;

import java.util.List;

import com.roommind.dto.PersonResponse;
import com.roommind.dto.UpdateProfileRequest;
import com.roommind.dto.UserResponse;
import com.roommind.service.UserService;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

	private final UserService userService;

	/**
	 * The people page. Signing in is enough to see it — SecurityConfig already
	 * requires a token on everything it does not name as open, so there is no
	 * rule to add here.
	 *
	 * `search` is optional: without it the whole list comes back, with it the
	 * list is narrowed to usernames starting with what was typed.
	 */
	@GetMapping
	public ResponseEntity<List<PersonResponse>> list(
			@AuthenticationPrincipal Jwt jwt,
			@RequestParam(required = false) String search) {
		return ResponseEntity.ok(userService.listPeople(jwt.getSubject(), search));
	}

	/**
	 * Your profile. "me" rather than an id in the path, because the only
	 * profile you can read or change here is your own — the token says whose.
	 */
	@GetMapping("/me")
	public ResponseEntity<UserResponse> profile(@AuthenticationPrincipal Jwt jwt) {
		return ResponseEntity.ok(userService.profile(jwt.getSubject()));
	}

	// PUT: the whole profile is sent and replaces what was there, so sending
	// the same body twice leaves it the same.
	@PutMapping("/me")
	public ResponseEntity<UserResponse> updateProfile(
			@AuthenticationPrincipal Jwt jwt,
			@Valid @RequestBody UpdateProfileRequest request) {
		return ResponseEntity.ok(userService.updateProfile(jwt.getSubject(), request));
	}
}
