# RoomMind

Real-time chat with AI summarization, translation and an @ai assistant. Spring
Boot backend, React frontend, PostgreSQL.

## Working rules

- Add a library, abstraction or configuration only when the feature being built
  right now cannot be done without it. If it can, leave it out and say when it
  will make sense.
- Build one feature at a time. Stop after it works so the user can review.
- Keep endpoints and controllers minimal — enough to prove the piece works.
- Not yet: websockets, advanced security config, state libraries, styling
  frameworks. They arrive at the phase that needs them.
- The user is learning this stack. Explain choices in plain language, and do not
  assume a library name explains itself.
- Do not rewrite working code that has already been reviewed unless it is
  actually broken.

## Documents

- `project-scope.md` — features and product decisions
- `tech-stack.md` — the stack
- `implementation-plan.md` — phases and tasks, with progress checked off

## Layout

- `backend/` — Spring Boot 4, Java 21 target, Maven wrapper
- `frontend/` — Vite, React 18, TypeScript
- `docker-compose.yml` — PostgreSQL

## Running it

    docker compose up -d
    cd backend  && ./mvnw spring-boot:run     # http://localhost:8080
    cd frontend && npm run dev                # http://localhost:5173

## Machine specifics

- **Postgres runs on host port 5433**, not 5432. A PostgreSQL 18 Windows
  service already owns 5432 on this machine and wins the connection. Do not
  "fix" the port back to 5432.
- **`JAVA_HOME` has trailing spaces** (`C:\Program Files\Java\jdk-23    `),
  which breaks `./mvnw` from Git Bash. Prefix commands with
  `JAVA_HOME="C:\Program Files\Java\jdk-23"` until the variable is fixed.
- Node is 18, so the frontend is pinned to Vite 5 / React 18.

## Current state

- Phase 1 done: both servers run, `/api/health` reports database connectivity,
  CORS allows the Vite origin.
- Every endpoint is permit-all. Authentication is Phase 2.
- No Flyway migrations exist yet. The first one creates `users`.
- Installed but not yet used, from the initial scaffold: React Router, TanStack
  Query, axios, Tailwind, Spring Security OAuth2 resource server. Do not build
  on them ahead of need.
