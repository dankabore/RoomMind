package com.roommind.controller;

import java.util.List;

import com.roommind.dto.AddMemberRequest;
import com.roommind.dto.ConversationResponse;
import com.roommind.dto.ConversationSummaryResponse;
import com.roommind.dto.CreateGroupRequest;
import com.roommind.dto.GroupResponse;
import com.roommind.dto.OpenDirectRequest;
import com.roommind.dto.TransferAdminRequest;
import com.roommind.service.ConversationService;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
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
	 * A group and its members. Any member can read it; only the admin can change
	 * it with the two below.
	 */
	@GetMapping("/{conversationId}/members")
	public ResponseEntity<GroupResponse> readGroup(
			@AuthenticationPrincipal Jwt jwt,
			@PathVariable Long conversationId) {
		return ResponseEntity.ok(conversationService.readGroup(jwt.getSubject(), conversationId));
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
	 * Hands the admin role to another member. PUT, not POST: a group has exactly
	 * one admin, so this sets who it is, and sending the same request twice
	 * leaves the same person in the job.
	 */
	@PutMapping("/{conversationId}/admin")
	public ResponseEntity<GroupResponse> transferAdmin(
			@AuthenticationPrincipal Jwt jwt,
			@PathVariable Long conversationId,
			@Valid @RequestBody TransferAdminRequest request) {
		return ResponseEntity.ok(conversationService.transferAdmin(jwt.getSubject(), conversationId, request));
	}

	/**
	 * Leaves a group. 204: from the caller's side there is nothing left to
	 * describe — they can no longer read the group they just left.
	 */
	@PostMapping("/{conversationId}/leave")
	public ResponseEntity<Void> leave(
			@AuthenticationPrincipal Jwt jwt,
			@PathVariable Long conversationId) {
		conversationService.leave(jwt.getSubject(), conversationId);
		return ResponseEntity.noContent().build();
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
