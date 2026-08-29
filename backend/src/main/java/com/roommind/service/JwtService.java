package com.roommind.service;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

import com.roommind.config.JwtConfig;
import com.roommind.dto.TokenResponse;
import com.roommind.entity.User;

import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;

/**
 * The one place that mints tokens. Nothing else needs to know what goes into a
 * token or how long it lasts — callers hand over a user and get a signed token
 * back.
 *
 * There is no parse or verify method here on purpose: the decoder configured in
 * SecurityConfig already does that on every request, before any of this code
 * runs. A second implementation would be a second thing to keep correct.
 */
@Service
@RequiredArgsConstructor
public class JwtService {

	private final JwtEncoder jwtEncoder;

	private final JwtConfig jwtConfig;

	public TokenResponse generateToken(User user) {
		Instant issuedAt = Instant.now();
		Instant expiresAt = issuedAt.plus(jwtConfig.getExpiryMinutes(), ChronoUnit.MINUTES);

		JwtClaimsSet claims = JwtClaimsSet.builder()
			.issuer(jwtConfig.getIssuer())
			.subject(user.getId().toString())
			.issuedAt(issuedAt)
			.expiresAt(expiresAt)
			.claim("username", user.getUsername())
			.build();

		JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
		String token = jwtEncoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();

		return TokenResponse.builder()
			.token(token)
			.expiresIn(Duration.between(issuedAt, expiresAt).toSeconds())
			.build();
	}
}
