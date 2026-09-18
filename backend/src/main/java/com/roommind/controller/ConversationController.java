package com.roommind.controller;

import java.util.List;

import com.roommind.dto.AddMemberRequest;
import com.roommind.dto.ConversationResponse;
import com.roommind.dto.ConversationSummaryResponse;
import com.roommind.dto.CreateGroupRequest;
import com.roommind.dto.GroupResponse;
import com.roommind.dto.OpenDirectRequest;
import com.roommind.service.ConversationService;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
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
	 * Your conversations, most recently active first, each with the last thing
	 * said in it. This is what the dashboard lists.
	 */
	@GetMapping
	public ResponseEntity<List<ConversationSummaryResponse>> list(@AuthenticationPrincipal Jwt jwt) {
		return ResponseEntity.ok(conversationService.listFor(jwt.getSubject()));
	}

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

	/**
	 * Creates a group. 201, unlike the direct endpoint above: this one always
	 * writes a new conversation, and asking twice with the same name makes two
	 * groups — which is right, since two groups may share a name.
	 */
	@PostMapping("/groups")
	public ResponseEntity<GroupResponse> createGroup(
			@AuthenticationPrincipal Jwt jwt,
			@Valid @RequestBody CreateGroupRequest request) {
		return ResponseEntity.status(HttpStatus.CREATED)
			.body(conversationService.createGroup(jwt.getSubject(), request));
	}

	/**
	 * Adds someone to a group. 201, because a membership is created.
	 *
	 * It answers with the whole group rather than just the person added, so the
	 * screen can redraw its member list from the reply instead of asking again.
	 */
	@PostMapping("/{conversationId}/members")
	public ResponseEntity<GroupResponse> addMember(
			@AuthenticationPrincipal Jwt jwt,
			@PathVariable Long conversationId,
			@Valid @RequestBody AddMemberRequest request) {
		return ResponseEntity.status(HttpStatus.CREATED)
			.body(conversationService.addMember(jwt.getSubject(), conversationId, request));
	}

	/**
	 * Removes someone from a group. 204: there is nothing left to describe, and
	 * the person removed is already named in the path.
	 */
	@DeleteMapping("/{conversationId}/members/{userId}")
	public ResponseEntity<Void> removeMember(
			@AuthenticationPrincipal Jwt jwt,
			@PathVariable Long conversationId,
			@PathVariable Long userId) {
		conversationService.removeMember(jwt.getSubject(), conversationId, userId);
		return ResponseEntity.noContent().build();
	}
}
