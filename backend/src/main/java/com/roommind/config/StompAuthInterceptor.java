package com.roommind.config;

import java.security.Principal;

import com.roommind.service.ConversationService;

import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import lombok.RequiredArgsConstructor;

/**
 * The websocket's equivalent of the security filter in front of the REST
 * endpoints. It sees every frame a browser sends and throws on the ones that
 * are not allowed. Spring answers a throw with a STOMP ERROR frame and closes
 * the connection.
 *
 * The reasons written below do not reach the browser yet: Spring's ERROR frame
 * carries a generic "Failed to send message" instead. They document why each
 * refusal happens, and can be passed through once the client has a use for
 * telling them apart.
 *
 * Three rules:
 * - CONNECT must carry a valid access token. The browser cannot attach one to
 *   the websocket handshake itself, so the STOMP client sends it as a header
 *   on this first frame instead.
 * - SUBSCRIBE is only to a conversation the caller is a member of.
 * - SEND is refused outright. Messages are sent through the POST endpoint;
 *   without this rule anyone connected could publish straight to a
 *   conversation's topic and put words in someone else's mouth.
 */
@Component
@RequiredArgsConstructor
public class StompAuthInterceptor implements ChannelInterceptor {

	private static final String BEARER_PREFIX = "Bearer ";

	// The same decoder the REST endpoints use, so a token is judged by exactly
	// the same rules — signature, expiry, issuer — whichever door it comes in by.
	private final JwtDecoder jwtDecoder;

	private final ConversationService conversationService;

	@Override
	public Message<?> preSend(Message<?> message, MessageChannel channel) {
		StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
		if (accessor == null || accessor.getCommand() == null) {
			// Heartbeats and other non-STOMP traffic carry no command to judge.
			return message;
		}

		switch (accessor.getCommand()) {
			case CONNECT -> authenticate(accessor);
			case SUBSCRIBE -> authorizeSubscription(accessor);
			case SEND -> throw new AccessDeniedException("Send messages through the API, not the socket.");
			default -> {
				// UNSUBSCRIBE and DISCONNECT need no permission.
			}
		}
		return message;
	}

	/**
	 * Checks the token and records who this connection belongs to. Spring keeps
	 * the user set here and attaches it to every later frame on the same
	 * connection, which is how SUBSCRIBE below knows who is asking.
	 *
	 * The token is only checked here, once. A connection opened with a token
	 * that later expires stays open until the browser drops it; the next
	 * reconnect needs a valid token again.
	 */
	private void authenticate(StompHeaderAccessor accessor) {
		String header = accessor.getFirstNativeHeader("Authorization");
		if (header == null || !header.startsWith(BEARER_PREFIX)) {
			throw new AccessDeniedException("A token is required to connect.");
		}

		try {
			Jwt jwt = jwtDecoder.decode(header.substring(BEARER_PREFIX.length()));
			// The same kind of object the REST side builds from a token, so its
			// getName() is the user id, just as jwt.getSubject() is in controllers.
			accessor.setUser(new JwtAuthenticationToken(jwt));
		}
		catch (JwtException ex) {
			// The decoder's own message can say which check failed; the client
			// needs to know only that it has to log in again.
			throw new AccessDeniedException("The token is invalid or has expired.");
		}
	}

	private void authorizeSubscription(StompHeaderAccessor accessor) {
		Principal user = accessor.getUser();
		if (user == null) {
			throw new AccessDeniedException("Connect before subscribing.");
		}

		Long conversationId = conversationIdFrom(accessor.getDestination());

		try {
			conversationService.requireMember(conversationId, Long.valueOf(user.getName()));
		}
		catch (ResponseStatusException ex) {
			// Same wording as the REST endpoints' 404, for the same reason: a
			// stranger should not be able to tell a private conversation from one
			// that does not exist.
			throw new AccessDeniedException("That conversation does not exist.");
		}
	}

	/**
	 * The id at the end of /topic/conversations/{id}. Any other address is
	 * refused, so nothing can be listened to that this class has not checked.
	 */
	private Long conversationIdFrom(String destination) {
		if (destination == null || !destination.startsWith(WebSocketConfig.CONVERSATION_TOPIC)) {
			throw new AccessDeniedException("That conversation does not exist.");
		}
		try {
			return Long.valueOf(destination.substring(WebSocketConfig.CONVERSATION_TOPIC.length()));
		}
		catch (NumberFormatException ex) {
			throw new AccessDeniedException("That conversation does not exist.");
		}
	}
}
