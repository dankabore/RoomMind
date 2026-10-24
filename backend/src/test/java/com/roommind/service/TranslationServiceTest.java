package com.roommind.service;

import java.util.Optional;

import com.roommind.dto.TranslationResponse;
import com.roommind.entity.Message;
import com.roommind.entity.MessageTranslation;
import com.roommind.entity.User;
import com.roommind.enums.Language;
import com.roommind.repository.MessageRepository;
import com.roommind.repository.MessageTranslationRepository;
import com.roommind.repository.UserRepository;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

/**
 * Who may have a message translated, into what, and when the model is asked
 * at all. Every refusal must happen before the model is called, and a saved
 * translation must stop it being called again, since each call is paid for.
 */
@ExtendWith(MockitoExtension.class)
class TranslationServiceTest {

	@Mock
	private MessageRepository messageRepository;

	@Mock
	private UserRepository userRepository;

	@Mock
	private MessageTranslationRepository messageTranslationRepository;

	@Mock
	private ConversationService conversationService;

	@Mock
	private AiService aiService;

	@InjectMocks
	private TranslationService translationService;

	@Test
	void translatesIntoTheCallersProfileLanguage() {
		givenFrenchReaderAndMessage();
		given(aiService.translate("are we still on?", "French")).willReturn("On est toujours partants ?");

		TranslationResponse response = translationService.translate("7", 5L, 30L);

		assertThat(response.getMessageId()).isEqualTo(30L);
		assertThat(response.getLanguage()).isEqualTo(Language.FR);
		assertThat(response.getTranslation()).isEqualTo("On est toujours partants ?");
	}

	@Test
	void aStrangerIsTurnedAwayBeforeAnythingIsRead() {
		willThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "That conversation does not exist."))
			.given(conversationService).requireMember(5L, 7L);

		assertThatThrownBy(() -> translationService.translate("7", 5L, 30L))
			.isInstanceOf(ResponseStatusException.class);
		verifyNoInteractions(messageRepository, aiService);
	}

	@Test
	void aMessageFromAnotherConversationIs404() {
		// The caller is a member of 5, but message 30 is somewhere else.
		given(messageRepository.findByIdAndConversationId(30L, 5L)).willReturn(Optional.empty());

		assertThatThrownBy(() -> translationService.translate("7", 5L, 30L))
			.isInstanceOfSatisfying(ResponseStatusException.class,
				e -> assertThat(e.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND));
		verifyNoInteractions(aiService);
	}

	// The cache.

	@Test
	void aSavedTranslationIsReturnedWithoutAskingTheModel() {
		givenFrenchReaderAndMessage();
		given(messageTranslationRepository.findByMessageIdAndLanguage(30L, Language.FR))
			.willReturn(Optional.of(MessageTranslation.builder().translation("Déjà traduit.").build()));

		assertThat(translationService.translate("7", 5L, 30L).getTranslation()).isEqualTo("Déjà traduit.");
		verifyNoInteractions(aiService);
		verify(messageTranslationRepository, never()).save(any());
	}

	@Test
	void aNewTranslationIsSavedForThatMessageAndLanguage() {
		givenFrenchReaderAndMessage();
		given(aiService.translate("are we still on?", "French")).willReturn("On est toujours partants ?");

		translationService.translate("7", 5L, 30L);

		ArgumentCaptor<MessageTranslation> saved = ArgumentCaptor.forClass(MessageTranslation.class);
		verify(messageTranslationRepository).save(saved.capture());
		assertThat(saved.getValue().getMessageId()).isEqualTo(30L);
		assertThat(saved.getValue().getLanguage()).isEqualTo(Language.FR);
		assertThat(saved.getValue().getTranslation()).isEqualTo("On est toujours partants ?");
		assertThat(saved.getValue().getCreatedAt()).isNotNull();
	}

	@Test
	void aFailedCallSavesNothing() {
		givenFrenchReaderAndMessage();
		given(aiService.translate(anyString(), anyString()))
			.willThrow(new ResponseStatusException(HttpStatus.GATEWAY_TIMEOUT, "slow"));

		assertThatThrownBy(() -> translationService.translate("7", 5L, 30L))
			.isInstanceOf(ResponseStatusException.class);
		verify(messageTranslationRepository, never()).save(any());
	}

	@Test
	void losingARaceToSaveStillAnswers() {
		// Another reader's request saved the same translation a moment earlier.
		givenFrenchReaderAndMessage();
		given(aiService.translate("are we still on?", "French")).willReturn("On est toujours partants ?");
		given(messageTranslationRepository.save(any()))
			.willThrow(new DataIntegrityViolationException("uq_message_translations"));

		assertThat(translationService.translate("7", 5L, 30L).getTranslation())
			.isEqualTo("On est toujours partants ?");
	}

	private void givenFrenchReaderAndMessage() {
		given(messageRepository.findByIdAndConversationId(30L, 5L))
			.willReturn(Optional.of(Message.builder().id(30L).body("are we still on?").build()));
		given(userRepository.findById(7L))
			.willReturn(Optional.of(User.builder().id(7L).language(Language.FR).build()));
	}
}
