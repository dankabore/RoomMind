package com.roommind.controller;

import java.util.List;

import com.roommind.dto.MessageResponse;
import com.roommind.dto.SendMessageRequest;
import com.roommind.service.MessageService;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/conversations/{conversationId}/messages")
@RequiredArgsConstructor
public class MessageController {

	private final MessageService messageService;

	// 201: this one really does create something every time it is called.
	@PostMapping
	public ResponseEntity<MessageResponse> send(
			@AuthenticationPrincipal Jwt jwt,
			@PathVariable Long conversationId,
			@Valid @RequestBody SendMessageRequest request) {
		return ResponseEntity.status(HttpStatus.CREATED)
			.body(messageService.send(jwt.getSubject(), conversationId, request));
	}

	/**
	 * A page of the conversation, oldest first.
	 *
	 * `before` is the id of the oldest message already on screen; leaving it off
	 * asks for the most recent page. The client never counts pages — it just
	 * keeps handing back the oldest id it holds.
	 */
	@GetMapping
	public ResponseEntity<List<MessageResponse>> read(
			@AuthenticationPrincipal Jwt jwt,
			@PathVariable Long conversationId,
			@RequestParam(required = false) Long before) {
		return ResponseEntity.ok(messageService.read(jwt.getSubject(), conversationId, before));
	}
}
