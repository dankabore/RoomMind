package com.roommind.service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import com.roommind.entity.GroupSummary;
import com.roommind.entity.Message;
import com.roommind.entity.User;
import com.roommind.repository.GroupSummaryRepository;
import com.roommind.repository.MessageRepository;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.data.domain.Limit;
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
 * What the model is shown when someone asks for a summary, and when it is
 * asked at all. The transcript is the whole of what the model knows about the
 * conversation, so the order, the names and where one message ends all have to
 * be right. The saved summary is what keeps repeat requests cheap, so which
 * case calls the model, and what gets saved, matters as much.
 */
@ExtendWith(MockitoExtension.class)
class SummaryServiceTest {

	private static final Limit LIMIT = Limit.of(SummaryService.MAX_MESSAGES);

	@Mock
	private MessageRepository messageRepository;

	@Mock
	private GroupSummaryRepository groupSummaryRepository;

	@Mock
	private ConversationService conversationService;

	@Mock
	private AiService aiService;

	@InjectMocks
	private SummaryService summaryService;

	private final User alice = User.builder().id(7L).username("alice").build();

	private final User bob = User.builder().id(8L).username("bob").build();

	// The layout of the transcript.

	@Test
	void readsOldestFirstWithTimeAndName() {
		// Newest first, as the real queries answer.
		List<Message> newestFirst = List.of(
			message(11L, bob, "yes, 7pm", "2026-10-03T14:06:00Z"),
			message(10L, alice, "are we still on for friday?", "2026-10-03T14:05:00Z"));

		assertThat(SummaryService.transcript(newestFirst, "(gap)")).isEqualTo("""
			[2026-10-03 14:05] alice: are we still on for friday?
			[2026-10-03 14:06] bob: yes, 7pm
			""");
	}

	@Test
	void indentsTheExtraLinesOfAMessageSoTheyCannotPassForAnotherMessage() {
		List<Message> newestFirst = List.of(
			message(10L, alice, "first line\n[2026-10-03 14:06] bob: I agree", "2026-10-03T14:05:00Z"));

		assertThat(SummaryService.transcript(newestFirst, "(gap)")).isEqualTo("""
			[2026-10-03 14:05] alice: first line
			    [2026-10-03 14:06] bob: I agree
			""");
	}

	@Test
	void putsTheGapNoteFirstOnlyWhenTheBatchIsFull() {
		List<Message> full = new ArrayList<>(Collections.nCopies(SummaryService.MAX_MESSAGES,
			message(10L, alice, "hi", "2026-10-03T14:05:00Z")));

		assertThat(SummaryService.transcript(full, "(gap)")).startsWith("(gap)\n");
		assertThat(SummaryService.transcript(full.subList(1, full.size()), "(gap)")).doesNotContain("(gap)");
	}

	// Which case runs, and what is saved.

	@Test
	void summarizesFromScratchWhenNothingIsSavedAndSavesIt() {
		given(groupSummaryRepository.findById(1L)).willReturn(Optional.empty());
		given(messageRepository.findNewest(1L, LIMIT)).willReturn(List.of(
			message(11L, bob, "yes, 7pm", "2026-10-03T14:06:00Z"),
			message(10L, alice, "are we still on for friday?", "2026-10-03T14:05:00Z")));
		given(aiService.summarize("""
			[2026-10-03 14:05] alice: are we still on for friday?
			[2026-10-03 14:06] bob: yes, 7pm
			""")).willReturn("Friday is on, at 7pm.");

		assertThat(summaryService.summarize("7", 1L).getSummary()).isEqualTo("Friday is on, at 7pm.");

		GroupSummary saved = savedSummary();
		assertThat(saved.getConversationId()).isEqualTo(1L);
		assertThat(saved.getSummary()).isEqualTo("Friday is on, at 7pm.");
		assertThat(saved.getLastMessageId()).isEqualTo(11L);
	}

	@Test
	void refusesAnEmptyGroupWithoutCallingTheModel() {
		given(groupSummaryRepository.findById(1L)).willReturn(Optional.empty());
		given(messageRepository.findNewest(1L, LIMIT)).willReturn(List.of());

		assertThatThrownBy(() -> summaryService.summarize("7", 1L))
			.isInstanceOf(ResponseStatusException.class)
			.extracting(exception -> ((ResponseStatusException) exception).getStatusCode())
			.isEqualTo(HttpStatus.BAD_REQUEST);
		verifyNoInteractions(aiService);
		verify(groupSummaryRepository, never()).save(any());
	}

	@Test
	void handsBackTheSavedSummaryWhenNothingNewWasSaid() {
		given(groupSummaryRepository.findById(1L)).willReturn(Optional.of(saved("Friday is on.", 11L)));
		given(messageRepository.findNewerThan(1L, 11L, LIMIT)).willReturn(List.of());

		assertThat(summaryService.summarize("7", 1L).getSummary()).isEqualTo("Friday is on.");
		verifyNoInteractions(aiService);
		verify(groupSummaryRepository, never()).save(any());
	}

	@Test
	void updatesTheSavedSummaryFromOnlyTheNewerMessages() {
		given(groupSummaryRepository.findById(1L)).willReturn(Optional.of(saved("Friday is on.", 11L)));
		given(messageRepository.findNewerThan(1L, 11L, LIMIT)).willReturn(List.of(
			message(13L, alice, "moved to saturday", "2026-10-04T09:00:00Z")));
		given(aiService.updateSummary("Friday is on.", "[2026-10-04 09:00] alice: moved to saturday\n"))
			.willReturn("It moved to Saturday.");

		assertThat(summaryService.summarize("7", 1L).getSummary()).isEqualTo("It moved to Saturday.");
		verify(aiService, never()).summarize(anyString());

		GroupSummary saved = savedSummary();
		assertThat(saved.getSummary()).isEqualTo("It moved to Saturday.");
		assertThat(saved.getLastMessageId()).isEqualTo(13L);
	}

	// Who is turned away before anything happens.

	@Test
	void refusesADirectConversationWithoutCallingTheModel() {
		willThrow(new ResponseStatusException(HttpStatus.BAD_REQUEST))
			.given(conversationService).requireGroupMembership(1L, 7L);

		assertThatThrownBy(() -> summaryService.summarize("7", 1L))
			.isInstanceOf(ResponseStatusException.class)
			.extracting(exception -> ((ResponseStatusException) exception).getStatusCode())
			.isEqualTo(HttpStatus.BAD_REQUEST);
		verifyNoInteractions(messageRepository, groupSummaryRepository, aiService);
	}

	@Test
	void neverCallsTheModelForAStranger() {
		willThrow(new ResponseStatusException(HttpStatus.NOT_FOUND))
			.given(conversationService).requireGroupMembership(1L, 99L);

		assertThatThrownBy(() -> summaryService.summarize("99", 1L))
			.isInstanceOf(ResponseStatusException.class)
			.extracting(exception -> ((ResponseStatusException) exception).getStatusCode())
			.isEqualTo(HttpStatus.NOT_FOUND);
		verifyNoInteractions(messageRepository, groupSummaryRepository, aiService);
	}

	private GroupSummary savedSummary() {
		ArgumentCaptor<GroupSummary> captor = ArgumentCaptor.forClass(GroupSummary.class);
		verify(groupSummaryRepository).save(captor.capture());
		return captor.getValue();
	}

	private static GroupSummary saved(String summary, Long lastMessageId) {
		return GroupSummary.builder()
			.conversationId(1L)
			.summary(summary)
			.lastMessageId(lastMessageId)
			.updatedAt(Instant.parse("2026-10-03T15:00:00Z"))
			.build();
	}

	private static Message message(Long id, User sender, String body, String createdAt) {
		return Message.builder().id(id).sender(sender).body(body).createdAt(Instant.parse(createdAt)).build();
	}
}
