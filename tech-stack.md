# Tech Stack

## Frontend

- React
- TypeScript
- Vite
- React Router
- axios
- TanStack Query
- Tailwind
- STOMP client for websockets
- Access token kept in localStorage

## Backend

- Spring Boot 3
- Java 21
- Spring Security with JWT access tokens
- spring-boot-starter-oauth2-resource-server for issuing and validating tokens
- CORS enabled for the frontend origin
- Spring WebSocket with STOMP, token sent on the STOMP CONNECT frame

## Database

- PostgreSQL
- Flyway for migrations

## AI

- Anthropic Java SDK (`com.anthropic`)
- Claude Opus 5 (`claude-opus-5`)
- No vector database, conversation history goes straight into the prompt

## Testing

- JUnit
- Testcontainers

## Deployment

- Docker
- Railway, Render or Fly.io
