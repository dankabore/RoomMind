package com.roommind.service;

import java.util.List;

import com.roommind.dto.PersonResponse;
import com.roommind.dto.UpdateProfileRequest;
import com.roommind.dto.UserResponse;
import com.roommind.entity.User;
import com.roommind.mapper.UserMapper;
import com.roommind.repository.UserRepository;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

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
			userRepository.findByUsernameStartingWithIgnoreCaseAndIdNotAndAssistantFalseOrderByUsernameAsc(prefix, currentUserId));
	}

	/**
	 * Your own profile. Always the caller's: the id comes from the token, so
	 * there is no way to ask for, or change, anyone else's.
	 */
	public UserResponse profile(String subject) {
		return userMapper.toResponse(findCaller(subject));
	}

	/**
	 * Replaces your display name and language. A blank display name is stored
	 * as null, so "not set" has one form in the database and the screen only
	 * has one case to fall back on.
	 */
	public UserResponse updateProfile(String subject, UpdateProfileRequest request) {
		User user = findCaller(subject);

		String displayName = request.getDisplayName() == null ? "" : request.getDisplayName().trim();
		user.setDisplayName(displayName.isEmpty() ? null : displayName);
		user.setLanguage(request.getLanguage());

		return userMapper.toResponse(userRepository.save(user));
	}

	// A valid token for a deleted account: the same 401 /api/auth/me gives, so
	// the screen sends the person back to log in.
	private User findCaller(String subject) {
		return userRepository.findById(Long.valueOf(subject))
			.orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "This account no longer exists."));
	}
}
