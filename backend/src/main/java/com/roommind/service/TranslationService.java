package com.roommind.service;

import java.time.Instant;

import com.roommind.dto.TranslationResponse;
import com.roommind.entity.Message;
import com.roommind.entity.MessageTranslation;
import com.roommind.enums.Language;
import com.roommind.repository.MessageRepository;
import com.roommind.repository.MessageTranslationRepository;
import com.roommind.repository.UserRepository;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import lombok.RequiredArgsConstructor;

/**
 * Translating one message for the person reading it.
 *
 * Works in every conversation, direct ones included: it is one message at a
 * time, so unlike summaries there is nothing about a busy group that makes it
 * more useful there.
 *
 * The original is never touched. Translations are saved beside the message,
 * one per language, and shared by everyone who reads that language.
 */
@Service
@RequiredArgsConstructor
public class TranslationService {

	private final MessageRepository messageRepository;

	private final UserRepository userRepository;

	private final MessageTranslationRepository messageTranslationRepository;

	private final ConversationService conversationService;

	private final AiService aiService;

	/**
	 * The message in the caller's own language, as set on their profile. The
	 * caller cannot choose another one here, so the translation always matches
	 * the profile.
	 *
	 * Membership first, so a stranger gets the same 404 as for a conversation
	 * that does not exist and the model is never called for them. Then the
	 * message must be in that conversation, for the reason given on
	 * findByIdAndConversationId.
	 *
	 * Not @Transactional, like summaries: no connection is held while the model
	 * answers.
	 */
	public TranslationResponse translate(String subject, Long conversationId, Long messageId) {
		Long callerId = Long.valueOf(subject);
		conversationService.requireMember(conversationId, callerId);

		Message message = messageRepository.findByIdAndConversationId(messageId, conversationId)
			.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "That message does not exist."));

		Language language = userRepository.findById(callerId)
			.orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "This account no longer exists."))
			.getLanguage();

		// Somebody reading this language has asked before: hand back what was
		// saved, with no model call.
		String translation = messageTranslationRepository.findByMessageIdAndLanguage(messageId, language)
			.map(MessageTranslation::getTranslation)
			.orElseGet(() -> translateAndSave(message, language));

		return TranslationResponse.builder()
			.messageId(message.getId())
			.language(language)
			.translation(translation)
			.build();
	}

	/**
	 * The first request for this message in this language. A failed call
	 * throws before the save, so nothing is stored and the next press tries
	 * again.
	 *
	 * Two readers can press at the same moment, both find nothing saved, and
	 * both ask the model. The unique constraint lets only the first save in;
	 * the second is refused, and its translation is still a perfectly good
	 * answer for the person waiting on it, so it is returned anyway. The cost
	 * is one extra call, the same trade summaries make.
	 */
	private String translateAndSave(Message message, Language language) {
		String translation = aiService.translate(message.getBody(), language.getEnglishName());

		try {
			messageTranslationRepository.save(MessageTranslation.builder()
				.messageId(message.getId())
				.language(language)
				.translation(translation)
				.createdAt(Instant.now())
				.build());
		} catch (DataIntegrityViolationException alreadySaved) {
			// The other request's row is already there; nothing more to do.
		}

		return translation;
	}
}
