package com.roommind.repository;

import java.util.Optional;

import com.roommind.entity.MessageTranslation;
import com.roommind.enums.Language;

import org.springframework.data.jpa.repository.JpaRepository;

public interface MessageTranslationRepository extends JpaRepository<MessageTranslation, Long> {

	/**
	 * The saved translation of one message into one language, if anyone has
	 * asked for it before. Spring Data writes the query from the method name.
	 */
	Optional<MessageTranslation> findByMessageIdAndLanguage(Long messageId, Language language);
}
