package com.roommind.service;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;

import com.roommind.dto.SummaryResponse;
import com.roommind.entity.GroupSummary;
import com.roommind.entity.Message;
import com.roommind.repository.GroupSummaryRepository;
import com.roommind.repository.MessageRepository;

import org.springframework.data.domain.Limit;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import lombok.RequiredArgsConstructor;

/**
 * Turning a conversation into something the AI model can summarize.
 *
 * This class gathers and lays out the text, decides when a summary is needed
 * and keeps each group's latest one. Sending it to the model is AiService's job, so the part that knows about
 * a particular AI provider stays in one place of its own.
 */
@Service
@RequiredArgsConstructor
public class SummaryService {

	/**
	 * The most messages one request sends: the newest ones for a first summary,
	 * or the newest since the saved summary for an update. Every message sent is
	 * paid for in tokens, and a message can be up to 4000 characters, so this is
	 * a ceiling on cost as much as on length.
	 */
	static final int MAX_MESSAGES = 200;

	// UTC, because the server does not know where each reader is. The model only
	// needs the order and the gaps between messages, which UTC keeps.
	private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
		.withZone(ZoneOffset.UTC);

	private final MessageRepository messageRepository;

	private final GroupSummaryRepository groupSummaryRepository;

	private final ConversationService conversationService;

	private final AiService aiService;

	/**
	 * The group's summary, brought up to date.
	 *
	 * Groups only: a one-to-one chat is short enough to read, and the volume of
	 * a busy group is what summaries are for. The check runs before anything is
	 * read, so a stranger gets the usual 404, a direct conversation gets 400,
	 * and the model is never called for either.
	 *
	 * There are three cases:
	 *  - no saved summary: summarize the newest messages from scratch;
	 *  - a saved one and nothing new since: hand it back, no model call;
	 *  - a saved one and newer messages: ask the model to fold the new messages
	 *    into it, so only what is new is sent.
	 * Either way a fresh result is saved together with the newest message it
	 * covers, which is where the next request picks up.
	 *
	 * Not @Transactional on purpose: the model call can take seconds, and a
	 * transaction would hold a database connection open for all of it. Each
	 * read and the final save run on their own.
	 */
	public SummaryResponse summarize(String subject, Long conversationId) {
		conversationService.requireGroupMembership(conversationId, Long.valueOf(subject));

		Limit limit = Limit.of(MAX_MESSAGES);
		GroupSummary saved = groupSummaryRepository.findById(conversationId).orElse(null);
		String summary;
		List<Message> newestFirst;

		if (saved == null) {
			newestFirst = messageRepository.findNewest(conversationId, limit);

			// Nothing to describe, and no reason to pay for a call that would say so.
			if (newestFirst.isEmpty()) {
				throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "There are no messages to summarize yet.");
			}

			summary = aiService.summarize(transcript(newestFirst, "(Earlier messages are not included.)"));
		} else {
			newestFirst = messageRepository.findNewerThan(conversationId, saved.getLastMessageId(), limit);

			if (newestFirst.isEmpty()) {
				return response(saved.getSummary());
			}

			summary = aiService.updateSummary(saved.getSummary(),
					transcript(newestFirst, "(Some messages since the previous summary are not included.)"));
		}

		groupSummaryRepository.save(GroupSummary.builder()
			.conversationId(conversationId)
			// The list is newest first, so its first message is the newest covered.
			.lastMessageId(newestFirst.get(0).getId())
			.summary(summary)
			.updatedAt(Instant.now())
			.build());

		return response(summary);
	}

	private static SummaryResponse response(String summary) {
		return SummaryResponse.builder().summary(summary).build();
	}

	/**
	 * Messages as one block of text, oldest first, one message per line:
	 *
	 *     [2026-10-03 14:05] alice: are we still on for friday?
	 *
	 * The list comes in newest first, as the queries return it. `gapNote` is put
	 * at the top when the list is a full batch, since there were probably more
	 * messages before it; saying so stops the model treating the first line it
	 * sees as where things began.
	 */
	static String transcript(List<Message> newestFirst, String gapNote) {
		StringBuilder transcript = new StringBuilder();

		if (newestFirst.size() == MAX_MESSAGES) {
			transcript.append(gapNote).append('\n');
		}

		// Walked from the end of the list, so the text reads oldest to newest
		// the way the chat does. The queries join the sender in, so reading every
		// author's name here costs no further queries.
		for (int i = newestFirst.size() - 1; i >= 0; i--) {
			Message message = newestFirst.get(i);
			transcript.append('[')
				.append(TIME.format(message.getCreatedAt()))
				.append("] ")
				.append(message.getSender().getUsername())
				.append(": ")
				.append(indentContinuationLines(message.getBody()))
				.append('\n');
		}

		return transcript.toString();
	}

	/**
	 * A message that runs over several lines gets its extra lines indented.
	 *
	 * Without this, someone could type a newline followed by
	 * "[2026-10-03 14:06] bob: ..." and their one message would look to the model
	 * like a second message from bob. With it, only a real message can start a
	 * line with a timestamp.
	 */
	private static String indentContinuationLines(String body) {
		return body.replace("\r\n", "\n").replace("\n", "\n    ");
	}
}
