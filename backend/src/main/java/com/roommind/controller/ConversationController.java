package com.roommind.controller;

import com.roommind.dto.ConversationResponse;
import com.roommind.dto.OpenDirectRequest;
import com.roommind.service.ConversationService;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/conversations")
@RequiredArgsConstructor
public class ConversationController {

	private final ConversationService conversationService;

	/**
	 * Opens the conversation with one other person, which is what clicking their
	 * name on the people page does.
	 *
	 * 200 rather than the usual 201-on-create, because from the caller's side
	 * this is always the same request — "give me my conversation with this
	 * person" — and the answer is the same either way. Whether a row had to be
	 * written to satisfy it is not something the caller does anything with.
	 */
	@PostMapping("/direct")
	public ResponseEntity<ConversationResponse> openDirect(
			@AuthenticationPrincipal Jwt jwt,
			@Valid @RequestBody OpenDirectRequest request) {
		return ResponseEntity.ok(conversationService.openDirect(jwt.getSubject(), request));
	}
}
