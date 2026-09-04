# Implementation Plan

Each phase ends with something that visibly works. Do not start a phase before
the one above it runs.

## Phase 1 — The project runs locally

- [x] Generate Spring Boot 3 / Java 21 project (Maven) with web, security,
      oauth2-resource-server, data jpa, validation, websocket, postgresql and
      flyway
- [x] docker-compose file running PostgreSQL locally
- [x] Connect Spring to the database, confirm Flyway runs on startup
- [x] Generate React + TypeScript app with Vite
- [x] Add Tailwind, React Router, axios, TanStack Query
- [x] CORS config allowing the frontend origin
- [x] Health endpoint that the frontend calls and displays
- [x] .gitignore
- [ ] First commit

## Phase 2 — Users can register and log in

- [x] Migration: `users` table (id, email, username, password hash, created at);
      display name and language move to the profile migration
- [x] User entity and repository
- [x] Registration endpoint with validation, unique email and username
- [x] Spring Security config: stateless, BCrypt password hashing
- [x] JWT signing key from `app.jwt.secret`, overridable by `APP_JWT_SECRET`
- [x] Login endpoint returning an access token
- [x] Token validation on protected endpoints
- [x] `/api/auth/me` endpoint returning the current user
- [x] React: register page
- [x] React: login page
- [x] React: store the token, attach it to every request via an axios instance
- [x] React: axios interceptor sending expired or rejected tokens back to login —
      the route guard already checks the token on entry; the interceptor catches
      a token that expires while a loaded page keeps calling the API
- [x] React: protected routes
- [x] React: logout clears the token

## Phase 3 — Messages send and persist

- [ ] Migration: `conversations`, `conversation_members`, `messages`
- [ ] Entities and repositories
- [ ] People endpoint: list users, prefix search by username, exclude self
- [ ] Endpoint to open a direct conversation, creating it on first message
- [ ] Send message endpoint
- [ ] Fetch messages endpoint, paginated, 50 per page, oldest-scroll cursor
- [ ] Conversation list endpoint with last message preview
- [ ] Membership check on every conversation endpoint
- [ ] React: people page with search
- [ ] React: dashboard listing recent conversations
- [ ] React: chat view with message list and input
- [ ] React: load older messages on scroll up

## Phase 4 — Messages arrive live

- [ ] STOMP websocket config and endpoint
- [ ] Read and verify the access token from the STOMP CONNECT frame
- [ ] Reject the connection when the token is missing or invalid
- [ ] Subscription interceptor rejecting non-members of a conversation
- [ ] Broadcast each saved message to its conversation topic
- [ ] React: connect to STOMP after login, passing the token
- [ ] React: subscribe on opening a conversation, unsubscribe on leaving
- [ ] React: append incoming messages
- [ ] React: reconnect after disconnect
- [ ] React: avoid duplicating your own sent message

## Phase 5 — Group chats

- [ ] Migration: conversation type, group name, member role
- [ ] Create group endpoint, creator becomes admin
- [ ] Add member endpoint, admin only
- [ ] Remove member endpoint, admin only
- [ ] Leave endpoint for ordinary members
- [ ] Block admin from leaving while other members remain
- [ ] Transfer admin endpoint
- [ ] End the group when the last member, the admin, leaves
- [ ] React: create group
- [ ] React: member list
- [ ] React: add and remove members
- [ ] React: leave group
- [ ] React: transfer admin

## Phase 6 — Summarize a conversation

- [ ] Add Anthropic Java SDK, API key from environment
- [ ] Service that loads recent messages and formats them for the prompt
- [ ] Summarize endpoint, membership checked
- [ ] Cache the summary against the newest message id
- [ ] Handle API failure and timeout
- [ ] React: summarize button
- [ ] React: summary panel with loading and error states

## Phase 7 — Translation and @Ai

- [ ] Profile endpoint to read and update display name and language
- [ ] React: profile page
- [ ] Translate endpoint, target language from the caller's profile
- [ ] Cache translations by message and language
- [ ] React: translate button per message, toggle back to original
- [ ] Detect `@ai` in an incoming message
- [ ] Assistant service answering from conversation history
- [ ] Save the answer as a message authored by the assistant
- [ ] Broadcast the answer over STOMP like any other message
- [ ] React: render assistant messages distinctly

## Phase 8 — Deployed and demoable

- [ ] Testcontainers setup
- [ ] Repository and flow tests
- [ ] Tests proving a non-member cannot read or subscribe to a conversation
- [ ] Dockerfile for the backend
- [ ] Deploy backend and frontend with managed PostgreSQL
- [ ] Seed demo accounts and a sample conversation
- [ ] README with setup steps and screenshots

## Later

- [ ] Refresh tokens
- [ ] Online / offline presence
- [ ] Typing indicators
- [ ] Unread counts
- [ ] Read receipts
