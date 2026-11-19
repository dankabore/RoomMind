package com.roommind.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

/**
 * Switches on @Async: a method marked with it returns to its caller at once
 * and does its work on a background thread from the pool below.
 *
 * The assistant is the reason. The model takes seconds to answer, and the
 * person who typed @ai should get their own message back straight away
 * instead of waiting for the reply to it.
 *
 * The pool has to be declared here. Spring Boot normally provides one, but
 * steps aside when other thread pools exist, and the websocket broker brings
 * four. @Async then falls back to starting a new thread for every call, with
 * no limit at all.
 */
@Configuration
@EnableAsync
public class AsyncConfig {

	/**
	 * Four questions with the model at once, twenty more waiting. Beyond that
	 * a question is turned away rather than piling up paid calls; see
	 * MessageService.send.
	 *
	 * Named taskExecutor because that is the name @Async looks for.
	 */
	@Bean
	public ThreadPoolTaskExecutor taskExecutor() {
		ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
		executor.setCorePoolSize(4);
		executor.setMaxPoolSize(4);
		executor.setQueueCapacity(20);
		executor.setThreadNamePrefix("assistant-");
		return executor;
	}
}
