package com.roommind.service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.roommind.dto.AddMemberRequest;
import com.roommind.dto.ConversationResponse;
import com.roommind.dto.ConversationSummaryResponse;
import com.roommind.dto.CreateGroupRequest;
import com.roommind.dto.GroupMemberResponse;
import com.roommind.dto.GroupResponse;
import com.roommind.dto.OpenDirectRequest;
import com.roommind.entity.Conversation;
import com.roommind.entity.ConversationMember;
import com.roommind.entity.Message;
import com.roommind.entity.User;
import com.roommind.enums.ConversationType;
import com.roommind.enums.MemberRole;
import com.roommind.mapper.MessageMapper;
import com.roommind.mapper.UserMapper;
import com.roommind.repository.ConversationMemberRepository;
import com.roommind.repository.ConversationRepository;
import com.roommind.repository.MessageRepository;
import com.roommind.repository.UserRepository;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import lombok.RequiredArgsConstructor;

/**
 * Opening conversations, listing them, running a group's membership, and
 * deciding who is allowed into one.
 */
@Service
@RequiredArgsConstructor
public class ConversationService {

	private final ConversationRepository conversationRepository;

	private final ConversationMemberRepository conversationMemberRepository;

	private final UserRepository userRepository;

	private final MessageRepository messageRepository;

	private final UserMapper userMapper;

	private final MessageMapper messageMapper;

	/**
	 * Hands back the conversation between the caller and one other person,
	 * creating it if this is the first time they have opened it.
	 *
	 * Find-or-create rather than plain create, because opening the same chat
	 * twice must land in the same place. If this only ever created, every visit
	 * would start a fresh thread and the history would scatter across them.
	 *
	 * @Transactional because a conversation with only one of its two members
	 * written is a conversation nobody can use. Either all three rows land or
	 * none do.
	 */
	@Transactional
	public ConversationResponse openDirect(String subject, OpenDirectRequest request) {
		Long callerId = Long.valueOf(subject);
		Long otherUserId = request.getUserId();

		if (callerId.equals(otherUserId)) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "You cannot start a conversation with yourself.");
		}

		User otherUser = userRepository.findById(otherUserId)
			.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "That account does not exist."));

		Conversation conversation = conversationRepository.findDirectBetween(callerId, otherUserId)
			.orElseGet(() -> createDirect(userRepository.getReferenceById(callerId), otherUser));

		return ConversationResponse.builder()
			.id(conversation.getId())
			.otherUser(userMapper.toPersonResponse(otherUser))
			.build();
	}

	/**
	 * The caller's conversations for the dashboard, most recently active first,
	 * each with the last thing said in it and whatever names it: the person on
	 * the other side of a direct conversation, or a group's name.
	 *
	 * Three queries however many conversations there are: the latest message in
	 * each, the groups that have no messages at all, and the people on the other
	 * side of all the direct ones. Asking once per conversation instead would
	 * cost two queries for every row on the screen.
	 *
	 * A direct conversation nobody has written in is left out — opening
	 * someone's chat creates one whether or not anything is said. An empty group
	 * is kept, because making one is deliberate and this list is the way back
	 * into it.
	 */
	public List<ConversationSummaryResponse> listFor(String subject) {
		Long userId = Long.valueOf(subject);

		List<Message> latestMessages = messageRepository.findLatestInEachConversationOf(userId);
		List<Conversation> emptyGroups = conversationRepository.findEmptyGroupsOf(userId);

		if (latestMessages.isEmpty() && emptyGroups.isEmpty()) {
			// Also spares the database a question about an empty list of ids.
			return List.of();
		}

		// Only the direct ones need a person fetched for them. A group names
		// itself, and asking for "the other members" of one would hand back
		// several people with nowhere to put them.
		List<Long> directIds = latestMessages.stream()
			.map(Message::getConversation)
			.filter(conversation -> conversation.getType() == ConversationType.DIRECT)
			.map(Conversation::getId)
			.toList();

		// Keyed by conversation, so each row below can find its person. toMap
		// refuses two people for one conversation, which is exactly right here:
		// only direct conversations are asked about, so a second person would
		// mean a direct conversation with three people in it.
		Map<Long, User> otherUsers = directIds.isEmpty()
			? Map.of()
			: conversationMemberRepository.findOtherMembers(directIds, userId)
				.stream()
				.collect(Collectors.toMap(member -> member.getConversation().getId(), ConversationMember::getUser));

		Stream<ConversationSummaryResponse> written = latestMessages.stream()
			.map(message -> row(message.getConversation(), otherUsers.get(message.getConversation().getId()),
					message));

		Stream<ConversationSummaryResponse> unwritten = emptyGroups.stream()
			.map(group -> row(group, null, null));

		// Sorted here rather than by the database, because the two halves come
		// from different queries. A group nobody has written in is placed by when
		// it was made, which is the only thing that has happened to it.
		return Stream.concat(written, unwritten)
			.sorted(Comparator.comparing(ConversationService::activeAt).reversed())
			.toList();
	}

	private ConversationSummaryResponse row(Conversation conversation, User otherUser, Message lastMessage) {
		return ConversationSummaryResponse.builder()
			.id(conversation.getId())
			.type(conversation.getType())
			.name(conversation.getName())
			.otherUser(otherUser == null ? null : userMapper.toPersonResponse(otherUser))
			.lastMessage(lastMessage == null ? null : messageMapper.toResponse(lastMessage))
			.createdAt(conversation.getCreatedAt())
			.build();
	}

	/** When a row last had anything happen to it: its last message, or its own start. */
	private static Instant activeAt(ConversationSummaryResponse row) {
		return row.getLastMessage() == null ? row.getCreatedAt() : row.getLastMessage().getCreatedAt();
	}

	/**
	 * Creates a group with the caller as its admin and, optionally, some people
	 * already in it. Everyone else can only be added later, by the admin — there
	 * is no way to join a group on your own.
	 *
	 * @Transactional for the same reason as openDirect: a group row without its
	 * membership rows is a group nobody, not even its creator, can see.
	 */
	@Transactional
	public GroupResponse createGroup(String subject, CreateGroupRequest request) {
		Long callerId = Long.valueOf(subject);

		User creator = userRepository.findById(callerId)
			.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "That account does not exist."));

		// A set, because the same person listed twice would otherwise become two
		// membership rows and two entries in the list. The creator is dropped
		// from it rather than refused: they are already going in, as the admin.
		Set<Long> memberIds = new LinkedHashSet<>(request.getMemberIds() == null ? List.of() : request.getMemberIds());
		memberIds.remove(callerId);

		List<User> members = userRepository.findAllById(memberIds);
		if (members.size() != memberIds.size()) {
			// One id in the list has no account. Naming which one would let
			// anyone map out the id range by watching the message change.
			throw new ResponseStatusException(HttpStatus.NOT_FOUND, "One of those accounts does not exist.");
		}

		Instant now = Instant.now();
		Conversation group = conversationRepository.save(Conversation.builder()
			.type(ConversationType.GROUP)
			.name(request.getName().trim())
			.createdAt(now)
			.build());

		List<ConversationMember> rows = new ArrayList<>();
		rows.add(member(group, creator, MemberRole.ADMIN, now));
		members.forEach(user -> rows.add(member(group, user, MemberRole.MEMBER, now)));
		conversationMemberRepository.saveAll(rows);

		return groupResponse(group);
	}

	/**
	 * A group and who is in it, for anyone who is in it. Ordinary members can
	 * read the list — they can see who is writing in the group anyway — but only
	 * the admin can change it.
	 */
	public GroupResponse readGroup(String subject, Long conversationId) {
		ConversationMember membership = conversationMemberRepository
			.findMembership(conversationId, Long.valueOf(subject))
			.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "That conversation does not exist."));

		Conversation conversation = membership.getConversation();
		if (conversation.getType() != ConversationType.GROUP) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "That conversation is not a group.");
		}

		return groupResponse(conversation);
	}

	/**
	 * Puts someone into a group. Only the group's admin can, which is the whole
	 * of how people get in: there is no request-to-join and no open invitation.
	 */
	@Transactional
	public GroupResponse addMember(String subject, Long conversationId, AddMemberRequest request) {
		Conversation group = requireGroupAdmin(conversationId, Long.valueOf(subject));

		User user = userRepository.findById(request.getUserId())
			.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "That account does not exist."));

		// 409 rather than quietly doing nothing: the admin asked for a change
		// that did not happen, and the screen should say why. The unique
		// constraint on the pair would catch it anyway, as a 500.
		if (conversationMemberRepository.existsByConversationIdAndUserId(conversationId, user.getId())) {
			throw new ResponseStatusException(HttpStatus.CONFLICT, "That person is already in this group.");
		}

		conversationMemberRepository.save(member(group, user, MemberRole.MEMBER, Instant.now()));

		return groupResponse(group);
	}

	/**
	 * Takes someone out of a group. Admin only, and never the admin themselves —
	 * a group with nobody in charge could never add or remove anyone again. An
	 * admin who wants out hands the role on first, which the leave endpoint will
	 * handle.
	 *
	 * The person's messages stay. They were said, and removing them would leave
	 * holes in everyone else's history.
	 */
	@Transactional
	public void removeMember(String subject, Long conversationId, Long userId) {
		Long callerId = Long.valueOf(subject);
		requireGroupAdmin(conversationId, callerId);

		if (callerId.equals(userId)) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
				"The admin cannot remove themselves. Hand the admin role to someone else first.");
		}

		ConversationMember membership = conversationMemberRepository.findMembership(conversationId, userId)
			.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "That person is not in this group."));

		conversationMemberRepository.delete(membership);
	}

	/**
	 * Stops here unless the caller is the admin of a group, and hands back the
	 * group if they are.
	 *
	 * The three answers are deliberately different. A stranger gets the same 404
	 * requireMember gives, so trying ids reveals nothing. A member who is not the
	 * admin gets 403, because they already know the group exists and hiding it
	 * from them would only be confusing. Pointing this at a direct conversation
	 * is a 400 — it exists and they are in it, but it has no membership to run.
	 */
	private Conversation requireGroupAdmin(Long conversationId, Long userId) {
		ConversationMember membership = conversationMemberRepository.findMembership(conversationId, userId)
			.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "That conversation does not exist."));

		Conversation conversation = membership.getConversation();
		if (conversation.getType() != ConversationType.GROUP) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "That conversation is not a group.");
		}

		if (membership.getRole() != MemberRole.ADMIN) {
			throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only the group's admin can do that.");
		}

		return conversation;
	}

	/**
	 * Stops here unless the caller is in the conversation. Every endpoint that
	 * touches a conversation's contents starts with this.
	 *
	 * The answer is 404 and not 403 on purpose. "Forbidden" would confirm the
	 * conversation exists, which is enough to work out who is talking to whom by
	 * trying ids one after another. "Not found" tells a stranger nothing, and
	 * reads the same as an id that was never real.
	 */
	public void requireMember(Long conversationId, Long userId) {
		if (!conversationMemberRepository.existsByConversationIdAndUserId(conversationId, userId)) {
			throw new ResponseStatusException(HttpStatus.NOT_FOUND, "That conversation does not exist.");
		}
	}

	private Conversation createDirect(User caller, User otherUser) {
		Instant now = Instant.now();
		Conversation conversation = conversationRepository.save(Conversation.builder()
			.type(ConversationType.DIRECT)
			.createdAt(now)
			.build());

		conversationMemberRepository.save(member(conversation, caller, MemberRole.MEMBER, now));
		conversationMemberRepository.save(member(conversation, otherUser, MemberRole.MEMBER, now));

		return conversation;
	}

	/**
	 * The group as its member list shows it. Read back from the database rather
	 * than assembled from whatever was just changed, so the answer is the real
	 * membership and not the caller's version of it.
	 */
	private GroupResponse groupResponse(Conversation group) {
		List<GroupMemberResponse> members = conversationMemberRepository.findMembersOf(group.getId())
			.stream()
			.map(membership -> GroupMemberResponse.builder()
				.id(membership.getUser().getId())
				.username(membership.getUser().getUsername())
				.role(membership.getRole())
				.build())
			.toList();

		return GroupResponse.builder()
			.id(group.getId())
			.name(group.getName())
			.members(members)
			.build();
	}

	private ConversationMember member(Conversation conversation, User user, MemberRole role, Instant joinedAt) {
		return ConversationMember.builder()
			.conversation(conversation)
			.user(user)
			.role(role)
			.joinedAt(joinedAt)
			.build();
	}
}
