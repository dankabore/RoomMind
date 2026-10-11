package com.roommind.service;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;

import com.roommind.config.OpenAiConfig;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import com.openai.client.OpenAIClient;
import com.openai.client.okhttp.OpenAIOkHttpClient;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * How AiService answers when OpenAI fails, tried against a fake OpenAI rather
 * than the real one.
 *
 * The fake is the small web server that comes with the JDK, listening on a
 * free local port. The real OpenAI client is pointed at it, so these tests also
 * check what the library actually throws for each failure — which is what the
 * error handling depends on — without a key, a network or any cost.
 */
class AiServiceTest {

	private HttpServer server;

	private OpenAiConfig config;

	// What the fake answers next, and how many requests it has seen.
	private volatile int status;

	private volatile String body;

	private volatile long delayMillis;

	private final AtomicInteger requests = new AtomicInteger();

	@BeforeEach
	void startFakeOpenAi() throws IOException {
		server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
		server.createContext("/v1/responses", this::answer);
		server.start();

		config = new OpenAiConfig();
		config.setApiKey("test-key");
		config.setModel("test-model");
		config.setTimeout(Duration.ofMillis(500));
	}

	@AfterEach
	void stopFakeOpenAi() {
		server.stop(0);
	}

	@Test
	void returnsTheTextOfTheAnswer() {
		replyWith(200, reply("""
			{"type": "output_text", "text": "  Friday is on.  ", "annotations": []}
			"""));

		assertThat(service().summarize("transcript")).isEqualTo("Friday is on.");
	}

	@Test
	void sendsNothingWithoutAKey() {
		config.setApiKey("");

		assertFails(() -> service().summarize("transcript"), HttpStatus.SERVICE_UNAVAILABLE,
				"AI features are not set up on this server.");
		assertThat(requests.get()).isZero();
	}

	@Test
	void aRejectedKeyIsAServerProblemNotTheUsers() {
		replyWith(401, error("invalid_api_key", "Incorrect API key provided."));

		assertFails(() -> service().summarize("transcript"), HttpStatus.SERVICE_UNAVAILABLE,
				"AI features are not set up correctly on this server.");
	}

	@Test
	void tooManyRequestsAsksToTryAgainLater() {
		replyWith(429, error("rate_limit_exceeded", "Rate limit reached."));

		assertFails(() -> service().summarize("transcript"), HttpStatus.SERVICE_UNAVAILABLE,
				"The AI service is busy right now. Try again in a minute.");
	}

	@Test
	void runningOutOfCreditIsNotPresentedAsBusy() {
		replyWith(429, error("insufficient_quota", "You exceeded your current quota."));

		assertFails(() -> service().summarize("transcript"), HttpStatus.SERVICE_UNAVAILABLE,
				"AI features are unavailable right now.");
	}

	@Test
	void aServerErrorIsABadGateway() {
		replyWith(500, error("server_error", "Something broke."));

		assertFails(() -> service().summarize("transcript"), HttpStatus.BAD_GATEWAY,
				"The AI service had a problem. Try again shortly.");
	}

	@Test
	void aSlowAnswerIsATimeout() {
		delayMillis = 2000;
		replyWith(200, reply("""
			{"type": "output_text", "text": "too late", "annotations": []}
			"""));

		assertFails(() -> service().summarize("transcript"), HttpStatus.GATEWAY_TIMEOUT,
				"The AI service took too long to answer. Try again.");
	}

	@Test
	void nobodyListeningCannotBeReached() {
		int port = server.getAddress().getPort();
		server.stop(0);

		assertFails(() -> service(port).summarize("transcript"), HttpStatus.BAD_GATEWAY,
				"Could not reach the AI service. Try again shortly.");
	}

	@Test
	void aRefusalIsNotReturnedAsASummary() {
		replyWith(200, reply("""
			{"type": "refusal", "refusal": "I can't help with that."}
			"""));

		assertFails(() -> service().summarize("transcript"), HttpStatus.BAD_GATEWAY,
				"This conversation could not be summarized.");
	}

	@Test
	void anEmptyAnswerIsNotReturnedAsASummary() {
		replyWith(200, reply("""
			{"type": "output_text", "text": "   ", "annotations": []}
			"""));

		assertFails(() -> service().updateSummary("old", "transcript"), HttpStatus.BAD_GATEWAY,
				"The AI service returned an empty answer. Try again.");
	}

	@Test
	void aServerErrorIsRetriedOnceWhenRetriesAreOn() {
		replyWith(500, error("server_error", "Something broke."));

		assertThatThrownBy(() -> service(server.getAddress().getPort(), 1).summarize("transcript"))
			.isInstanceOf(ResponseStatusException.class);
		assertThat(requests.get()).isEqualTo(2);
	}

	private AiService service() {
		return service(server.getAddress().getPort());
	}

	private AiService service(int port) {
		return service(port, 0);
	}

	// Retries are off unless a test asks for them, so each failure is seen once
	// and the tests stay fast.
	private AiService service(int port, int maxRetries) {
		OpenAIClient client = OpenAIOkHttpClient.builder()
			.apiKey("test-key")
			.baseUrl("http://localhost:" + port + "/v1")
			.timeout(config.getTimeout())
			.maxRetries(maxRetries)
			.build();
		return new AiService(client, config);
	}

	private void replyWith(int status, String body) {
		this.status = status;
		this.body = body;
	}

	private void answer(HttpExchange exchange) throws IOException {
		requests.incrementAndGet();
		try {
			Thread.sleep(delayMillis);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		}
		byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
		exchange.getResponseHeaders().add("Content-Type", "application/json");
		exchange.sendResponseHeaders(status, bytes.length);
		try (OutputStream out = exchange.getResponseBody()) {
			out.write(bytes);
		} catch (IOException ignored) {
			// The client gave up waiting, which is what the timeout test wants.
		}
	}

	private static String reply(String contentPart) {
		return """
			{"id": "resp_1", "object": "response", "status": "completed", "output": [
				{"type": "message", "id": "msg_1", "role": "assistant", "status": "completed",
				 "content": [%s]}
			]}
			""".formatted(contentPart);
	}

	private static String error(String code, String message) {
		return """
			{"error": {"message": "%s", "type": "error", "code": "%s", "param": null}}
			""".formatted(message, code);
	}

	private static void assertFails(Runnable call, HttpStatus status, String reason) {
		assertThatThrownBy(call::run)
			.isInstanceOfSatisfying(ResponseStatusException.class, exception -> {
				assertThat(exception.getStatusCode()).isEqualTo(status);
				assertThat(exception.getReason()).isEqualTo(reason);
			});
	}
}
