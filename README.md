# RoomMind

Real-time chat with AI summarization, translation and an @ai assistant.

- `project-scope.md` — features and product decisions
- `tech-stack.md` — the stack
- `implementation-plan.md` — phases and tasks

## Running it locally

Three things run at once, in three terminals.

**1. Database**

    docker compose up -d

Postgres listens on **port 5433** on the host, because port 5432 is already
taken by the PostgreSQL 18 service installed on this machine.

**2. Backend**

    cd backend
    ./mvnw spring-boot:run

Serves on http://localhost:8080.

**3. Frontend**

    cd frontend
    npm run dev

Serves on http://localhost:5173.

Open http://localhost:5173 and the page should report the backend and the
database as healthy.

## Handy commands

    docker compose down          # stop the database
    docker compose down -v       # stop it and delete all data
    docker compose exec db psql -U roommind roommind   # SQL prompt

## Notes

- `JAVA_HOME` on this machine has trailing spaces, which breaks `./mvnw` from
  Git Bash. Either fix the environment variable or prefix the command:
  `JAVA_HOME="C:\Program Files\Java\jdk-23" ./mvnw ...`
- The backend currently permits every request. Authentication arrives in
  Phase 2.
