package com.roommind.mapper;

import java.util.List;

import com.roommind.dto.MessageResponse;
import com.roommind.entity.Message;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * Turns a stored message into the shape the API returns.
 *
 * Still a straight copy, just from one level down: the sender's id and name are
 * read through the message's link to their account rather than off the message
 * itself. The three @Mapping lines only spell out those paths — nothing is
 * calculated, renamed in meaning, or ignored.
 *
 * Callers must load messages with the sender joined in. These paths read that
 * link, and on a message fetched without it, each one is a separate trip to the
 * database.
 */
@Mapper(componentModel = "spring")
public interface MessageMapper {

	@Mapping(target = "conversationId", source = "conversation.id")
	@Mapping(target = "senderId", source = "sender.id")
	@Mapping(target = "senderUsername", source = "sender.username")
	MessageResponse toResponse(Message message);

	List<MessageResponse> toResponses(List<Message> messages);
}
