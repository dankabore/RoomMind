package com.roommind.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

import lombok.RequiredArgsConstructor;

/**
 * The live connection a chat screen keeps open so new messages reach it without
 * asking.
 *
 * A websocket on its own is just a pipe that carries text both ways. STOMP is a
 * small protocol on top of it that gives that text a shape — "connect",
 * "subscribe to this address", "here is a message for that address" — so
 * neither side has to invent one.
 *
 * Traffic only flows one way here: the server pushes, browsers listen. Sending
 * a message still goes through the ordinary POST endpoint, which already
 * validates, checks membership and saves. Accepting sends over the socket too
 * would mean a second path doing all of that.
 */
@Configuration
@EnableWebSocketMessageBroker
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

	/**
	 * Where a conversation's new messages are published, followed by its id:
	 * /topic/conversations/42. Everyone subscribed to that address receives each
	 * message saved into conversation 42.
	 */
	public static final String CONVERSATION_TOPIC = "/topic/conversations/";

	private final CorsConfig corsConfig;

	private final StompAuthInterceptor stompAuthInterceptor;

	@Override
	public void registerStompEndpoints(StompEndpointRegistry registry) {
		// ws://localhost:8080/ws. By default a websocket only accepts pages from
		// its own origin, and the Vite dev server is a different one, so it is
		// named here the same way the CORS rules name it.
		registry.addEndpoint("/ws").setAllowedOrigins(corsConfig.getAllowedOrigin());
	}

	@Override
	public void configureMessageBroker(MessageBrokerRegistry registry) {
		// The "simple broker" is an in-memory list of who is subscribed to which
		// address, kept inside this application. Enough for one server; several
		// servers behind a load balancer would need a shared broker instead,
		// because each would only know its own subscribers.
		registry.enableSimpleBroker("/topic");
	}

	/**
	 * Every frame a browser sends passes through the interceptor before anything
	 * acts on it. That is where tokens and memberships are checked.
	 */
	@Override
	public void configureClientInboundChannel(ChannelRegistration registration) {
		registration.interceptors(stompAuthInterceptor);
	}
}
