package com.roommind.service;

import java.time.Instant;
import java.util.Locale;

import com.roommind.dto.LoginRequest;
import com.roommind.dto.RegisterRequest;
import com.roommind.dto.TokenResponse;
import com.roommind.dto.UserResponse;
import com.roommind.entity.User;
import com.roommind.mapper.UserMapper;
import com.roommind.repository.UserRepository;

import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import lombok.RequiredArgsConstructor;

/**
 * Everything the accounts feature actually decides: who may register, whether a
 * password is right, and which account a request belongs to. Making the token
 * itself is JwtService's job; the controller above deals with HTTP.
 */
@Service
@RequiredArgsConstructor
public class AuthService {

	private final UserRepository userRepository;

	private final PasswordEncoder passwordEncoder;

	private final JwtService jwtService;

	private final UserMapper userMapper;

	/**
	 * Creates an account. Deliberately does not return a token: registering and
	 * signing in stay separate steps, so there is only one place that issues one.
	 */
	public UserResponse register(RegisterRequest request) {
		String email = normalise(request.getEmail());

		if (userRepository.existsByEmail(email)) {
			throw new ResponseStatusException(HttpStatus.CONFLICT, "That email is already registered.");
		}
		if (userRepository.existsByUsername(request.getUsername())) {
			throw new ResponseStatusException(HttpStatus.CONFLICT, "That username is already taken.");
		}

		User user = User.builder()
			.email(email)
			.username(request.getUsername())
			.passwordHash(passwordEncoder.encode(request.getPassword()))
			.createdAt(Instant.now())
			.build();

		return userMapper.toResponse(userRepository.save(user));
	}

	/**
	 * Checks the password and hands back a signed token. An unknown email and a
	 * wrong password give the identical 401, so the response cannot be used to
	 * discover which addresses have accounts.
	 */
	public TokenResponse login(LoginRequest request) {
		User user = userRepository.findByEmail(normalise(request.getEmail()))
			.filter(candidate -> passwordEncoder.matches(request.getPassword(), candidate.getPasswordHash()))
			.orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Email or password is incorrect."));

		return jwtService.generateToken(user);
	}

	/**
	 * Looks up the account a verified token belongs to. Spring Security has
	 * already checked the signature and expiry before this is reached.
	 */
	public UserResponse currentUser(String subject) {
		User user = userRepository.findById(Long.valueOf(subject))
			.orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "This account no longer exists."));

		return userMapper.toResponse(user);
	}

	// Email addresses are treated case-insensitively, so store and look them up
	// in one form. Without this, Ada@x.com and ada@x.com become two accounts.
	private String normalise(String email) {
		return email.trim().toLowerCase(Locale.ROOT);
	}
}
