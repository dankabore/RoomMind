package com.roommind.config;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.openai.client.OpenAIClient;
import com.openai.client.okhttp.OpenAIOkHttpClient;

import lombok.Getter;
import lombok.Setter;

/**
 * Everything under app.openai in application.properties, and the one client
 * the rest of the app uses to talk to OpenAI.
 *
 * The key comes from the OPENAI_API_KEY environment variable (see
 * application.properties) so it never sits in the repository. Getter and Setter
 * rather than @Data for the same reason as JwtConfig: a generated toString
 * would print the key.
 */
@Configuration
@ConfigurationProperties(prefix = "app.openai")
@Getter
@Setter
public class OpenAiConfig {

	private String apiKey;

	/**
	 * Which model answers. In configuration rather than code because it is the
	 * main lever on cost and quality, and changing it should not need a
	 * rebuild.
	 */
	private String model;

	/**
	 * How long one attempt may take before giving up. The library's own default
	 * is ten minutes, far longer than anyone will watch a spinner for a summary.
	 */
	private Duration timeout;

	/**
	 * How many times a failed attempt is tried again. The library retries only
	 * failures that might pass on a second go (timeouts, dropped connections,
	 * "too busy", server errors), never a bad key or a bad request. Each retry
	 * can take the full timeout again, so the longest a person can wait is
	 * roughly timeout x (retries + 1).
	 */
	private int maxRetries;

	/**
	 * One client for the whole app. It holds a pool of HTTP connections, so
	 * making a new one per request would throw those away every time.
	 */
	@Bean
	public OpenAIClient openAiClient() {
		return OpenAIOkHttpClient.builder()
				.apiKey(apiKey)
				.timeout(timeout)
				.maxRetries(maxRetries)
				.build();
	}
}
