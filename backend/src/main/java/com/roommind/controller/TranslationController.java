package com.roommind.controller;

import com.roommind.dto.TranslationResponse;
import com.roommind.service.TranslationService;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/conversations/{conversationId}/messages/{messageId}/translation")
@RequiredArgsConstructor
public class TranslationController {

	private final TranslationService translationService;

	/**
	 * The message in the caller's language. No body: the language comes from
	 * the caller's profile, not the request.
	 *
	 * POST for the same reason as summaries: it can be a paid call, and a GET is
	 * something browsers and tools repeat on their own. 200, because nothing the
	 * caller would act on is created.
	 */
	@PostMapping
	public ResponseEntity<TranslationResponse> translate(
			@AuthenticationPrincipal Jwt jwt,
			@PathVariable Long conversationId,
			@PathVariable Long messageId) {
		return ResponseEntity.ok(translationService.translate(jwt.getSubject(), conversationId, messageId));
	}
}
