package com.roommind.service;

import java.util.List;

import com.roommind.dto.PersonResponse;
import com.roommind.mapper.UserMapper;
import com.roommind.repository.UserRepository;

import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;

/**
 * Finding other people to talk to. There is no friend request step in RoomMind:
 * every account is visible to every signed-in account, and a conversation starts
 * with the first message rather than an invitation.
 */
@Service
@RequiredArgsConstructor
public class UserService {

	private final UserRepository userRepository;

	private final UserMapper userMapper;

	/**
	 * Everyone but you, alphabetically, narrowed to usernames beginning with
	 * `search` when one is given.
	 *
	 * Prefix rather than "contains" because that is what a name list is for —
	 * typing "an" should offer Anna, not everyone with an "an" buried in the
	 * middle of their handle. It is also the form an index on username can
	 * actually use once there are enough accounts to matter.
	 *
	 * You are left out because the page exists to message somebody, and there is
	 * nothing to be done with your own row.
	 */
	public List<PersonResponse> listPeople(String subject, String search) {
		Long currentUserId = Long.valueOf(subject);
		// No search term and a blank one mean the same thing to a person: show me
		// everybody. Trimming also stops a stray space from emptying the list.
		String prefix = search == null ? "" : search.trim();

		return userMapper.toPersonResponses(
			userRepository.findByUsernameStartingWithIgnoreCaseAndIdNotOrderByUsernameAsc(prefix, currentUserId));
	}
}
