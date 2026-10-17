package com.roommind.service;

import java.util.Optional;

import com.roommind.dto.UpdateProfileRequest;
import com.roommind.entity.User;
import com.roommind.enums.Language;
import com.roommind.mapper.UserMapper;
import com.roommind.mapper.UserMapperImpl;
import com.roommind.repository.UserRepository;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * Editing your own profile. The interesting part is what gets stored for the
 * display name: "not set" must always end up as null, whichever way the
 * screen sends it, so later code has one case to check.
 */
@ExtendWith(MockitoExtension.class)
class UserServiceTest {

	@Mock
	private UserRepository userRepository;

	// The real generated mapper, so the test also proves the new fields reach
	// the response rather than stopping at the entity.
	@Spy
	private UserMapper userMapper = new UserMapperImpl();

	@InjectMocks
	private UserService userService;

	@Test
	void savesTrimmedNameAndLanguage() {
		User alice = alice();
		given(userRepository.findById(7L)).willReturn(Optional.of(alice));
		given(userRepository.save(any(User.class))).willAnswer(call -> call.getArgument(0));

		var response = userService.updateProfile("7", request("  Alice B.  ", Language.FR));

		assertThat(alice.getDisplayName()).isEqualTo("Alice B.");
		assertThat(alice.getLanguage()).isEqualTo(Language.FR);
		assertThat(response.getDisplayName()).isEqualTo("Alice B.");
		assertThat(response.getLanguage()).isEqualTo(Language.FR);
	}

	@Test
	void blankNameIsStoredAsNotSet() {
		User alice = alice();
		alice.setDisplayName("Old name");
		given(userRepository.findById(7L)).willReturn(Optional.of(alice));
		given(userRepository.save(any(User.class))).willAnswer(call -> call.getArgument(0));

		userService.updateProfile("7", request("   ", Language.EN));

		assertThat(alice.getDisplayName()).isNull();
	}

	@Test
	void missingNameClearsTheOldOne() {
		User alice = alice();
		alice.setDisplayName("Old name");
		given(userRepository.findById(7L)).willReturn(Optional.of(alice));
		given(userRepository.save(any(User.class))).willAnswer(call -> call.getArgument(0));

		userService.updateProfile("7", request(null, Language.EN));

		assertThat(alice.getDisplayName()).isNull();
	}

	@Test
	void deletedAccountIs401AndSavesNothing() {
		given(userRepository.findById(7L)).willReturn(Optional.empty());

		assertThatThrownBy(() -> userService.updateProfile("7", request("Alice", Language.EN)))
			.isInstanceOfSatisfying(ResponseStatusException.class,
				e -> assertThat(e.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED));
		verify(userRepository, never()).save(any());
	}

	private static User alice() {
		return User.builder().id(7L).username("alice").email("alice@x.com").language(Language.EN).build();
	}

	private static UpdateProfileRequest request(String displayName, Language language) {
		UpdateProfileRequest request = new UpdateProfileRequest();
		request.setDisplayName(displayName);
		request.setLanguage(language);
		return request;
	}
}
