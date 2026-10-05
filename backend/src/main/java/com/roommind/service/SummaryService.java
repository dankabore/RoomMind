package com.roommind.service;

import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;

import com.roommind.entity.Message;
import com.roommind.repository.MessageRepository;

import org.springframework.data.domain.Limit;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;

/**
 * Turning a conversation into something Claude can summarize.
 *
 * This class only gathers and lays out the text. Sending it to Claude is not
 * done here, so the part that knows about a particular AI provider stays in one
 * place of its own.
 */
@Service
@RequiredArgsConstructor
public class SummaryService {

	/**
	 * How far back a summary reads. Every message sent is paid for in tokens on
	 * every summary, and a message can be up to 4000 characters, so this is a
	 * ceiling on cost as much as on length. Two hundred covers a busy day in a
	 * group without letting a years-old chat send everything it holds.
	 */
	static final int MAX_MESSAGES = 200;

	// UTC, because the server does not know where each reader is. Claude only
	// needs the order and the gaps between messages, which UTC keeps.
	private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
		.withZone(ZoneOffset.UTC);

	private final MessageRepository messageRepository;

	private final ConversationService conversationService;

	/**
	 * The most recent messages in a conversation as one block of text, oldest
	 * first, one message per line:
	 *
	 *     [2026-10-03 14:05] alice: are we still on for friday?
	 *
	 * Empty when the conversation has no messages; what to tell the caller then
	 * is the endpoint's decision, not this method's.
	 */
	public String transcript(String subject, Long conversationId) {
		conversationService.requireMember(conversationId, Long.valueOf(subject));

		// findNewest joins the sender in, so reading every author's name below
		// costs no further queries.
		List<Message> newestFirst = messageRepository.findNewest(conversationId, Limit.of(MAX_MESSAGES));

		StringBuilder transcript = new StringBuilder();

		// A full batch means there were probably more before it. Saying so stops
		// the summary from presenting the start of what it was given as the start
		// of the conversation.
		if (newestFirst.size() == MAX_MESSAGES) {
			transcript.append("(Earlier messages are not included.)\n");
		}

		// Walked from the end of the list, so the text reads oldest to newest
		// the way the chat does.
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
	 * "[2026-10-03 14:06] bob: ..." and their one message would look to Claude
	 * like a second message from bob. With it, only a real message can start a
	 * line with a timestamp.
	 */
	private static String indentContinuationLines(String body) {
		return body.replace("\r\n", "\n").replace("\n", "\n    ");
	}
}
