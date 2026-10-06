package com.roommind.controller;

import com.roommind.dto.SummaryResponse;
import com.roommind.service.SummaryService;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/conversations/{conversationId}/summary")
@RequiredArgsConstructor
public class SummaryController {

	private final SummaryService summaryService;

	/**
	 * Summarizes the conversation's recent messages.
	 *
	 * POST rather than GET because each call asks Claude afresh and is paid
	 * for; a GET is the kind of request browsers and tools feel free to repeat
	 * on their own. 200 rather than 201, because nothing is stored.
	 */
	@PostMapping
	public ResponseEntity<SummaryResponse> summarize(
			@AuthenticationPrincipal Jwt jwt,
			@PathVariable Long conversationId) {
		return ResponseEntity.ok(summaryService.summarize(jwt.getSubject(), conversationId));
	}
}
