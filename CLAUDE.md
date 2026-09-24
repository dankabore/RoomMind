# RoomMind

Real-time chat with AI summarization, translation and an @ai assistant. Spring
Boot backend, React frontend, PostgreSQL.

## Working rules

- Add a library, abstraction or configuration only when the feature being built
  right now cannot be done without it. If it can, leave it out and say when it
  will make sense.
- Build one feature at a time. Stop after it works so the user can review.
- Keep endpoints and controllers minimal — enough to prove the piece works.
- Not yet: websockets, roles and authorities, refresh tokens, state libraries.
  They arrive at the phase that needs them.
- The user is learning this stack. Explain choices in plain language, and do not
  assume a library name explains itself.
- Do not rewrite working code that has already been reviewed unless it is
  actually broken.
- Code the user pastes from another project is usually a style reference, not a
  spec. Ask what to take from it before copying its mechanism.

## Backend conventions

- Packages by layer under `com.roommind`: `config`, `controller`, `service`,
  `repository`, `dto`, `entity`, `enums`, `mapper`. No feature-named packages.
  Every enum lives in `enums`, whatever it belongs to — none are nested in the
  entity or DTO that uses them.
- Lombok for boilerplate: `@Getter`/`@Setter`/`@Builder`, and
  `@RequiredArgsConstructor` for injection instead of written constructors.
- No `@Data` on entities (its `equals`/`hashCode` covers database-assigned ids)
  or on anything holding a secret (its `toString` prints the field).
- A DTO for every request and response. Controllers never return entities.
- Controllers return `ResponseEntity` with an explicit status — 201 on create,
  200 on read. Services return the plain DTO and throw on failure; only the
  controller knows about HTTP.
- MapStruct mappers in `mapper/`, `@Mapper(componentModel = "spring")`, one
  interface per entity. Use one **only where the mapping is a straight field
  copy**. Keep it manual where values are normalised, hashed or generated — a
  mapper full of `ignore = true` is worse than a builder.
- Config values in one `@ConfigurationProperties` class per group under the
  `app.*` namespace, not scattered `@Value` strings. Never `spring.*`, which
  belongs to Boot.
- Tabs for indentation; imports grouped `java`, project, `org.springframework`.

## Documentation lookups

- The `context7` MCP server is configured. Use it to fetch current
  documentation before writing code against a library, rather than working
  from memory.
- This matters here in particular: the backend is on **Spring Boot 4 /
  Spring Security 7**, which is newer than most model training data and
  renamed several starters. Check the docs before writing security,
  websocket or Flyway configuration.
- Also worth checking there when the time comes: the Anthropic Java SDK,
  Flyway migration syntax, and STOMP setup.

## Documents

- `project-scope.md` — features and product decisions
- `tech-stack.md` — the stack
- `implementation-plan.md` — phases and tasks, with progress checked off

## Layout

- `backend/` — Spring Boot 4, Java 21 target, Maven wrapper
- `frontend/` — Vite, React 18, TypeScript
- `docker-compose.yml` — PostgreSQL

Backend packages under `backend/src/main/java/com/roommind/`:

    config/      SecurityConfig, JwtConfig, CorsConfig, WebSocketConfig,
                 StompAuthInterceptor
    controller/  AuthController, HealthController, UserController,
                 ConversationController, MessageController
    service/     AuthService, JwtService, UserService, ConversationService,
                 MessageService
    repository/  UserRepository, ConversationRepository,
                 ConversationMemberRepository, MessageRepository
    dto/         RegisterRequest, LoginRequest, UserResponse, TokenResponse,
                 PersonResponse, OpenDirectRequest, ConversationResponse,
                 SendMessageRequest, MessageResponse,
                 ConversationSummaryResponse, CreateGroupRequest,
                 AddMemberRequest, TransferAdminRequest, GroupResponse,
                 GroupMemberResponse
    entity/      User, Conversation, ConversationMember, Message
    enums/       ConversationType, MemberRole
    mapper/      UserMapper, MessageMapper

Frontend under `frontend/src/`:

    lib/         api.ts (axios instance and error text), auth.ts (token
                 storage), forms.ts (field-error state and shared checks),
                 chat.ts (chat types and the hooks that list, fetch, page,
                 send and listen live), groups.ts (making a group and
                 changing who is in it), socket.ts (the one STOMP connection
                 and the list of what open screens are subscribed to)
    pages/       DashboardPage — the signed-in home page at /, your
                 conversations and groups with a last-message preview;
                 NewGroupPage — /groups/new, name a group and pick who is in
                 it; GroupChatPage — /group/<conversation id>, one group with
                 its member panel;
                 LoginPage, RegisterPage — state, validation rules, submit;
                 PeoplePage — the searchable list of everyone;
                 ChatPage — one conversation, at /chat/<other user id>; only
                 resolves the conversation and wires lib/chat to components
    components/  AuthCard, TextField, FormMessage, SubmitButton, RequireAuth,
                 Avatar, ChatHeader, MessageList (owns the scroll
                 corrections), MessageComposer (owns the draft text),
                 LogoutButton (the button and its confirmation dialog),
                 GroupChatHeader, GroupMembers (the member panel),
                 PersonPicker (search and pick someone, used by both group
                 screens), ConfirmDialog (the "are you sure?" question the
                 member panel asks three times)

Migrations live in `backend/src/main/resources/db/migration/`.

## Running it

    docker compose up -d
    cd backend  && ./mvnw spring-boot:run     # http://localhost:8080
    cd frontend && npm run dev                # http://localhost:5173

## Machine specifics

- **Postgres runs on host port 5433**, not 5432. A PostgreSQL 18 Windows
  service already owns 5432 on this machine and wins the connection. Do not
  "fix" the port back to 5432.
- **The build runs on JDK 21**, the version `pom.xml` targets. `JAVA_HOME` is
  machine-wide and points at `C:\Program Files\Microsoft\jdk-21.0.8.9-hotspot`;
  IntelliJ's project SDK and language level are the same 21 (`ms-21`). It used
  to hold `C:\Program Files\Java\jdk-23` with four trailing spaces, which broke
  `./mvnw` from Git Bash with "JAVA_HOME is not defined correctly" — if that
  error ever returns, check the variable for trailing spaces first.
- JDK 17 and 23 are also installed and should stay unused here: 17 cannot
  compile for release 21 at all, and 23 is past its update window.
- Node is 24 (LTS). The frontend stays on Vite 5 / React 18, which were
  chosen under Node 18 and still build fine. No reason to upgrade them.
- **Lombok and MapStruct stay declared in `annotationProcessorPaths`** in
  `backend/pom.xml`, with `lombok-mapstruct-binding` between them. This was
  first needed because JDK 23 ignores annotation processors found only on the
  classpath; declaring them explicitly is correct on 21 as well, so leave it.
  Symptoms if it breaks: "cannot find symbol" on every generated getter, or
  mappers that compile but return empty objects.

## Current state

- Phase 1 done: both servers run, `/api/health` reports database connectivity,
  CORS allows the Vite origin. The endpoint still exists, but the home page no
  longer displays it — the dashboard replaced that card.
- Phase 2 done: register, login, logout and `/api/auth/me` work end to end.
- Phase 3 people feature: `GET /api/users` lists every other account
  alphabetically and narrows to usernames starting with `?search=`.
- Phase 3 messaging backend: `POST /api/conversations/direct` opens (and on
  first use creates) the one-to-one conversation with someone,
  `POST /api/conversations/{id}/messages` sends, and `GET` on the same path
  reads a page.
- Phase 3 chat screen: clicking someone on the people page opens
  `/chat/<their user id>`. The route names the person, not the
  conversation, because the conversation may not exist until the page
  opens — that keeps the URL reloadable without a lookup endpoint.
- Scrollback uses `useInfiniteQuery`. Its "next page" means further back in
  time; pages arrive newest-first and are reversed for display. A sent
  message is pushed into the cache rather than triggering a refetch, since
  refetching an infinite query re-requests every page it holds.
- Phase 3 conversation list: `GET /api/conversations` returns the caller's
  conversations, most recently active first, each with `otherUser` and the
  whole `lastMessage` (the screen shortens it, not the API).
- It is three queries however many conversations there are: the latest message
  in each (`max(id)` grouped by conversation), the caller's groups that have no
  messages, then the other members of all the direct ones at once.
- `listFor` gathers the other members with `toMap`, which throws if one
  conversation has two other people. It only asks about the direct
  conversations in the list, so that cannot happen; a group names itself and
  needs no such lookup.
- Phase 3 dashboard: `/` lists your conversations with the other person, the
  time of the last message and its start (`You:` when you sent it). Rows link
  to `/chat/<their user id>`, the same address the people page uses, and the
  chat header's Back now returns to `/`. Phase 3 is complete.
- Phase 4 live backend: STOMP over a plain websocket at `/ws` (no SockJS),
  in-memory simple broker on `/topic`. Each saved message is pushed to
  `/topic/conversations/<id>` as the same `MessageResponse` the POST returns.
- `/ws` is `permitAll` in the HTTP security chain because browsers cannot set
  headers on the handshake. The token arrives as an `Authorization: Bearer`
  header on the STOMP CONNECT frame instead, checked by the same `JwtDecoder`.
- `StompAuthInterceptor` is a plain `ChannelInterceptor`, not Spring Security's
  websocket module: that module is not installed and forces CSRF tokens on
  CONNECT. It refuses CONNECT without a valid token, SUBSCRIBE to anything but
  a conversation the caller belongs to, and every client SEND — sending stays
  on the POST endpoint, and an open SEND would let clients publish forged
  messages straight to a topic.
- A refused frame gets a STOMP ERROR and the connection closes. The ERROR says
  only "Failed to send message"; the interceptor's reasons do not reach the
  client. A custom error handler would pass them on, if the client needs them.
- The token is checked once, at CONNECT. A socket outlives its token's expiry
  until it drops; the reconnect needs a fresh token.
- The push happens after `save` returns, so it follows the commit. If
  `MessageService.send` becomes `@Transactional`, it must wait for the commit.
- The websocket origin check reads `app.cors.allowed-origin` through
  `CorsConfig`, the same value the CORS rules use.
- Verified with a hand-written STOMP script (scratchpad, not the repo): 11
  checks covering token, membership, forged SEND and wrong origin.
- Phase 4 live frontend: `@stomp/stompjs` over a native websocket. One
  connection per signed-in tab, a module-level client in `lib/socket.ts`.
  `RequireAuth` opens it once `/api/auth/me` confirms the token (later calls
  are no-ops); `LogoutButton` closes it. Leaving a page does not.
- `beforeConnect` reads the token from storage on every attempt, reconnects
  included, and stops trying if there is none.
- The library reconnects every 5 s but forgets subscriptions, so `socket.ts`
  keeps a list of listeners and resubscribes all of them on each connect.
- `useLiveMessages` in `ChatView` listens to `/topic/conversations/<id>`. On
  each (re)subscription it fetches the newest page and merges it, for messages
  sent during the gap. More than 50 missed leaves a hole until reopened.
- Every new message, sent or pushed, goes through `addMessage`, which skips
  ids already cached. That is what keeps your own message from appearing
  twice, since the backend pushes it back to you too.
- `getNextPageParam` tests `>= 50`, not `=== 50`: page one grows as messages
  are added, and exactly-fifty wrongly ended scrollback. This was a latent
  Phase 3 bug that live messages made common.
- `onStompError` calls `/api/auth/me`, so an expired token goes through the
  existing 401-to-login handling. ERROR frames carry no reason to check.
- No heartbeats: the simple broker has none configured, so a connection that
  dies silently (no close event) is not noticed until the browser notices.
  Configure broker heartbeats if that shows up.
- The dashboard is not live; it refetches when you return to it.
- Verified with a Node harness (scratchpad) rendering the real hooks against
  the backend, including a backend restart: 12 checks. A deliberately broken
  copy failed the five aimed at dedupe, ordering, scrollback and catch-up.
- Phase 5 group backend: `POST /api/conversations/groups` creates a group with
  the caller as admin, `POST /api/conversations/{id}/members` adds someone and
  `DELETE /api/conversations/{id}/members/{userId}` removes them, both admin
  only. Add and remove answer with the whole group so the screen can redraw its
  member list from the reply.
- `conversations.type` is `DIRECT` or `GROUP` and `conversation_members.role`
  is `ADMIN` or `MEMBER`, both `@Enumerated(STRING)` with check constraints in
  the migration. A group has a name; a direct conversation must not, since its
  name is whoever is reading it.
- `findDirectBetween` now also requires `type = DIRECT`. The member count of
  two alone stopped being enough the moment two-person groups were possible.
- `requireGroupAdmin` answers 404 to a stranger (same reason as
  `requireMember`), 403 to a member who is not the admin, and 400 when pointed
  at a direct conversation.
- The admin cannot remove themselves — leaving is its own endpoint. Adding
  someone already in the group is 409, not a silent success. Removing someone,
  or leaving, leaves the messages in place.
- Nothing enforces one admin per group in the database. A partial unique index
  would make the transfer order-dependent (demote before promote, or it fails),
  so the rule lives in the service, which swaps both roles in one transaction.
- Verified against a running backend with a curl script (scratchpad): 23 checks
  covering roles, duplicates, strangers, direct-vs-group and the dashboard.
- `GET /api/conversations/{id}/members` reads one group and its members. Any
  member may; the admin alone may change it.
- `GET /api/conversations` now returns groups as well as direct conversations.
  A row carries `type`, and exactly one of `otherUser` (direct) and `name`
  (group) is filled in. This changed a reviewed endpoint, deliberately: the
  home page is one list of everything, which is what the user chose.
- Phase 5 frontend: `/groups/new` names a group and picks who starts in it,
  `/group/<conversation id>` is the group chat. The group address is the
  conversation's id, unlike `/chat/<user id>`, because a group exists in its
  own right and there is no person to name it after.
- `lib/groups.ts` holds the group hooks; reading and sending a group's messages
  is no different from any other conversation, so that stays in `lib/chat.ts`.
  Live messages work in a group unchanged.
- `MessageList` gained `showSenders`: in a group the side a bubble sits on only
  says whether it is yours, so everyone else's needs a name above it.
- `PersonPicker` fetches the whole people list once and filters it in the
  browser, rather than searching per keystroke as the people page does. It is a
  short list in a panel, not the directory.
- The member panel's buttons are drawn only for the admin, but the backend
  refuses either way; hiding them is convenience, not the rule.
- A new group is on the home page list straight away, with "No messages yet"
  where the preview goes. Creating one still goes straight into the group.
- `POST /api/conversations/{id}/leave` leaves a group and `PUT
  /api/conversations/{id}/admin` hands the role to another member. PUT, because
  a group has one admin and sending it twice leaves the same person in the job.
- An admin with anyone else still in the group is refused with 409 and a reason
  naming the fix; they hand the role on first. An admin who is the last one in
  may leave, and the group ends with them: its messages, the membership row and
  the conversation are deleted, in that order, with a flush between the
  membership and the conversation so Hibernate cannot send them the other way
  round and hit the foreign key.
- The member panel is where all of this lives on screen: Make admin and Remove
  on each other member's row, Leave group at the foot, each behind the same
  confirmation dialog. An admin who cannot leave yet is told why instead of
  being given a button that would be refused.
- `ConfirmDialog` was pulled out once that panel needed three of these.
  `LogoutButton` still has its own copy, which was left alone.
- Verified against a running backend: 24 more checks covering leaving, the
  admin's block, the handover and the group ending with its last member. All
  three group scripts (23 + 13 + 24) pass together.
- Phase 5 is complete. Not yet, and not planned for it: an admin's account
  being deleted, which the rules assume never happens.
- Migrations: `V1__create_users_table.sql` (id, email, username, password
  hash, created at), `V2__create_conversations_and_messages.sql`
  (conversations, conversation_members, messages) and
  `V3__add_group_chats.sql` (conversation type and name, member role).
  Display name and language arrive with the profile feature.
  `ddl-auto=validate`, so entities must match migrations.
- Email is the login identity and is stored lowercased; username is the public
  handle. Both unique.
- Access tokens only — signed HS256 with `app.jwt.secret`, issuer checked on the
  way back in. No refresh tokens; the user decided against them.
- Open endpoints: `/api/health`, `POST /api/auth/register`, `POST /api/auth/login`,
  and the `ERROR` dispatch. Everything else needs a bearer token, `/api/users`
  included — no rule was added for it.
- `PersonResponse` is how other people appear: id and username only. Everyone
  signed in can read that list, so it must not carry emails the way
  `UserResponse` does.
- The people list is unpaginated. Message history is: 50 per read, walked
  backwards with `?before=<oldest message id on screen>`. Paging on the id
  rather than an offset means new arrivals cannot shift the window and
  duplicate or skip a message.
- `ConversationService.requireMember` guards every endpoint that touches a
  conversation's contents, and answers **404, not 403** — a 403 would confirm
  the conversation exists and let someone map out who talks to whom by trying
  ids.
- `findDirectBetween` requires a member count of exactly two, so a group
  containing both people can never be mistaken for their private conversation.
- Messages must be loaded with the sender joined in (`join fetch m.sender`).
  `MessageMapper` reads the sender's name through that link, so without the
  join a 50-message page becomes 51 queries. Measured: a 30-message read is
  2 queries.
- `POST /api/conversations/direct` answers 200, not the usual 201-on-create,
  because it is find-or-create and the caller does not act differently on
  whether a row was written.
- Opening a direct conversation creates it even if nothing is ever sent, and
  the list endpoint leaves those out: with no messages there is no latest
  message, so they drop out of that query on their own. Empty groups are the
  exception and are fetched separately — making a group is deliberate, and the
  list is the way back into it. Their row has a null `lastMessage` and is
  placed by the conversation's `createdAt`, which every row now carries.
- Token validation is Spring Security's `oauth2ResourceServer`, not a
  hand-written filter. There is no `UserDetailsService` or
  `AuthenticationProvider`: `AuthService` checks the password itself.
- Errors come back as RFC 9457 problem details
  (`spring.mvc.problemdetails.enabled`), so the reason on a
  `ResponseStatusException` reaches the browser as `detail`. `errorMessage()` in
  `lib/api.ts` is what reads it.
- The token lives in `localStorage` under `roommind.token`. An axios request
  interceptor attaches it; logging out just deletes it and clears the query
  cache, since the backend holds no session.
- `RequireAuth` wraps protected routes and calls `/api/auth/me` before rendering,
  so a token that expired between visits sends the person back to login.
- Verification script for the whole auth flow lives in the session scratchpad,
  not the repo. Rewrite it if it is needed again.
