package com.roommind.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * Switches on @Async: a method marked with it returns to its caller at once
 * and does its work on a background thread from a pool Spring Boot provides.
 *
 * The assistant is the reason. The model takes seconds to answer, and the
 * person who typed @ai should get their own message back straight away
 * instead of waiting for the reply to it.
 */
@Configuration
@EnableAsync
public class AsyncConfig {
}
