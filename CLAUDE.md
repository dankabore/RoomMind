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
  `repository`, `dto`, `entity`, `mapper`. No feature-named packages.
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

    config/      SecurityConfig, JwtConfig
    controller/  AuthController, HealthController
    service/     AuthService, JwtService
    repository/  UserRepository
    dto/         RegisterRequest, LoginRequest, UserResponse, TokenResponse
    entity/      User
    mapper/      UserMapper

Frontend under `frontend/src/`:

    lib/         api.ts (axios instance and error text), auth.ts (token
                 storage), forms.ts (field-error state and shared checks)
    pages/       LoginPage, RegisterPage — state, validation rules, submit
    components/  AuthCard, TextField, FormMessage, SubmitButton, RequireAuth
    App.tsx      the signed-in home page

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
  CORS allows the Vite origin.
- Phase 2 done: register, login, logout and `/api/auth/me` work end to end.
  Chat conversations and messages are the next feature.
- `V1__create_users_table.sql` is the only migration: id, email, username,
  password hash, created at. Display name and language arrive with the profile
  feature. `ddl-auto=validate`, so entities must match migrations.
- Email is the login identity and is stored lowercased; username is the public
  handle. Both unique.
- Access tokens only — signed HS256 with `app.jwt.secret`, issuer checked on the
  way back in. No refresh tokens; the user decided against them.
- Open endpoints: `/api/health`, `POST /api/auth/register`, `POST /api/auth/login`,
  and the `ERROR` dispatch. Everything else needs a bearer token.
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
