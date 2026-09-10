package com.roommind.controller;

import java.util.List;

import com.roommind.dto.PersonResponse;
import com.roommind.service.UserService;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

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
}
