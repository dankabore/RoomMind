package com.roommind.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import lombok.Getter;
import lombok.Setter;

/**
 * Everything under app.cors in application.properties. Two things read the
 * frontend's address: the CORS rules for ordinary requests in SecurityConfig,
 * and the origin check on the websocket handshake in WebSocketConfig. Reading
 * it from one place keeps them from disagreeing about which page may connect.
 */
@Configuration
@ConfigurationProperties(prefix = "app.cors")
@Getter
@Setter
public class CorsConfig {

	private String allowedOrigin;
}
