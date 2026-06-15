# Study App

A full-stack study and focus application with real-time chat, group management, session tracking, and study analytics.

**Tech stack**

- **Backend:** Java Spring Boot (see [backend/pom.xml](backend/pom.xml#L1)) — WebMVC, WebSocket, Spring Security, JPA, Redis, MongoDB, JWT, Bucket4j rate limiting. Java version: 25.
- **Frontend:** React + Vite (see [frontend/package.json](frontend/package.json#L1)) — React 19, react-router, i18n, axios.
- **Dev infra:** Docker Compose for local DBs ([docker-compose.dev.yml](docker-compose.dev.yml#L1)).

**Key features**

- Real-time chat & websockets (see `backend/src/main/java/com/namphong/backend`)
- Group creation, membership, and ranking
- Session-based study tracking and statistics
- Authentication & OAuth2 support
- Internationalization (English & Vietnamese)
- Rate limiting and security hardening

**Important files**

- Design system and UI tokens: [DESIGN.md](DESIGN.md#L1)
- Backend entrypoint: [backend/src/main/java/com/namphong/backend/BackendApplication.java](backend/src/main/java/com/namphong/backend/BackendApplication.java#L1)
- Backend config: [backend/src/main/resources/application.yml](backend/src/main/resources/application.yml#L1)
- Frontend README: [frontend/README.md](frontend/README.md#L1)
- Docker compose (dev): [docker-compose.dev.yml](docker-compose.dev.yml#L1)

Getting started (development)

1. Run local databases (recommended)

```bash
docker compose -f docker-compose.dev.yml up -d
```

2. Backend (Windows)

```powershell
cd backend
.\mvnw.cmd spring-boot:run
```

Or (Unix/macOS)

```bash
cd backend
./mvnw spring-boot:run
```

3. Frontend

```bash
cd frontend
npm install
npm run dev
```

4. Tests (backend)

```bash
cd backend
mvn test
```

Notes

- Environment variables for databases are referenced in `docker-compose.dev.yml` and `backend/src/main/resources/application.yml`.
- See [backend/HELP.md](backend/HELP.md#L1) for additional backend developer notes.

Contributing

- Open issues or PRs against the `main` branch. Follow code style in existing modules.

License

- Add a LICENSE file or replace this section with the chosen license.
