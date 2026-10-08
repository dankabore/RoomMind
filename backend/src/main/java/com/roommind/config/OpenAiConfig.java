package com.roommind.config;

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
	 * One client for the whole app. It holds a pool of HTTP connections, so
	 * making a new one per request would throw those away every time.
	 */
	@Bean
	public OpenAIClient openAiClient() {
		return OpenAIOkHttpClient.builder()
				.apiKey(apiKey)
				.build();
	}
}
