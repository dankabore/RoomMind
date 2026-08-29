package com.roommind.config;

import java.util.List;

import jakarta.servlet.DispatcherType;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import lombok.RequiredArgsConstructor;

@Configuration
@RequiredArgsConstructor
public class SecurityConfig {

	private final JwtConfig jwtConfig;

	@Value("${app.cors.allowed-origin}")
	private String allowedOrigin;

	@Bean
	public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
		http
			.cors(Customizer.withDefaults())
			.csrf(AbstractHttpConfigurer::disable)
			.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
			.authorizeHttpRequests(auth -> auth
				// When a controller throws, Spring forwards internally to /error to build
				// the response body. Without this line that forward is itself checked for
				// a token, so a 400 or 409 would leave here as a 401.
				.dispatcherTypeMatchers(DispatcherType.ERROR).permitAll()
				.requestMatchers("/api/health").permitAll()
				.requestMatchers(HttpMethod.POST, "/api/auth/register", "/api/auth/login").permitAll()
				.anyRequest().authenticated())
			// Reads the bearer token off the Authorization header, checks its
			// signature and expiry, and rejects the request if either is wrong.
			.oauth2ResourceServer(oauth2 -> oauth2.jwt(Customizer.withDefaults()));

		return http.build();
	}

	@Bean
	public CorsConfigurationSource corsConfigurationSource() {
		CorsConfiguration config = new CorsConfiguration();
		config.setAllowedOrigins(List.of(allowedOrigin));
		config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
		config.setAllowedHeaders(List.of("Authorization", "Content-Type"));

		UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
		source.registerCorsConfiguration("/**", config);
		return source;
	}

	// The same key signs the tokens this server hands out and checks the ones it
	// gets back, so there is no public/private keypair to manage.
	@Bean
	JwtEncoder jwtEncoder() {
		return NimbusJwtEncoder.withSecretKey(jwtConfig.getSecretKey()).build();
	}

	@Bean
	JwtDecoder jwtDecoder() {
		NimbusJwtDecoder decoder = NimbusJwtDecoder.withSecretKey(jwtConfig.getSecretKey()).build();
		// createDefaultWithIssuer keeps the expiry check and adds an issuer check,
		// so a token this server did not mint is rejected inside the filter,
		// before any controller runs.
		decoder.setJwtValidator(JwtValidators.createDefaultWithIssuer(jwtConfig.getIssuer()));
		return decoder;
	}

	// BCrypt deliberately takes a noticeable amount of time to hash, which is
	// what makes guessing stored passwords in bulk impractical.
	@Bean
	PasswordEncoder passwordEncoder() {
		return new BCryptPasswordEncoder();
	}
}
