package com.roommind.config;

import java.nio.charset.StandardCharsets;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import lombok.Getter;
import lombok.Setter;

/**
 * Everything under app.jwt in application.properties, in one place. Both halves
 * of the token flow read from here — AuthService when it signs a token, the
 * decoder in SecurityConfig when it checks one — so the two cannot drift apart.
 *
 * Getter and Setter rather than @Data on purpose: @Data would also generate a
 * toString containing the secret, and anything that logged this object would
 * put the signing key in the log file.
 */
@Configuration
@ConfigurationProperties(prefix = "app.jwt")
@Getter
@Setter
public class JwtConfig {

	private String secret;

	private String issuer;

	private long expiryMinutes;

	/**
	 * The signing key, derived from the configured secret. Keeping the
	 * conversion here means the raw string is not handled anywhere else.
	 * HS256 requires at least 32 bytes.
	 */
	public SecretKey getSecretKey() {
		return new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
	}
}
