# Campus Recycle Rewards — Spring Boot Backend

A scalable Spring Boot backend for the Campus Recycle Rewards platform, handling GitHub OAuth2 login, JWT auth, and a bridge to a Python microservice.

## Run & Operate

- Spring Boot starts via the `API Server` workflow automatically
- `mvn -f artifacts/spring-boot/pom.xml --no-transfer-progress spring-boot:run` — run manually
- `pnpm run typecheck` — typecheck Node.js packages only
- Required env: `DATABASE_URL`, `GITHUB_CLIENT_ID`, `GITHUB_CLIENT_SECRET`, `SESSION_SECRET`
- Optional env: `PYTHON_BACKEND_URL` — URL of the Python microservice (default: `http://localhost:8000`)

## Stack

- **Runtime:** Java 19 (GraalVM), Spring Boot 3.2.5
- **Auth:** GitHub OAuth2 + JWT (stateless, scalable)
- **DB:** PostgreSQL + Spring Data JPA + Hibernate (auto DDL)
- **Python bridge:** WebClient (reactive HTTP client)
- **Build:** Maven (no wrapper needed — GraalVM includes mvn)

## Where things live

- `artifacts/spring-boot/` — Spring Boot Maven project (the real backend)
- `artifacts/spring-boot/src/main/java/com/campusrecycle/` — all Java source
  - `config/` — SecurityConfig, DataSourceConfig, AppProperties, WebClientConfig
  - `security/` — JwtTokenProvider, JwtAuthenticationFilter, OAuth2SuccessHandler
  - `model/` — User entity
  - `repository/` — UserRepository (Spring Data JPA)
  - `service/` — UserService, PythonBackendService
  - `controller/` — HealthController, AuthController, UserController, PythonProxyController
  - `dto/` — UserDto, AuthResponse
- `artifacts/spring-boot/src/main/resources/application.yml` — all config

## API Endpoints

| Method | Path | Auth | Description |
|--------|------|------|-------------|
| GET | `/api/healthz` | Public | Health check |
| GET | `/api/auth/login/github` | Public | Returns GitHub OAuth redirect URL |
| GET | `/api/oauth2/authorization/github` | Public | Initiates GitHub OAuth flow |
| GET | `/api/auth/me` | JWT | Current user profile |
| POST | `/api/auth/verify` | JWT | Verify a token |
| POST | `/api/auth/logout` | JWT | Logout (client clears token) |
| GET | `/api/users/me` | JWT | My profile |
| GET | `/api/users/leaderboard` | JWT | Top 20 users by points |
| POST | `/api/users/me/points` | JWT | Add points to self |
| GET/POST/PUT | `/api/python/{path}` | JWT | Proxy requests to Python backend |

## Auth Flow

1. Frontend redirects user to `/api/oauth2/authorization/github`
2. User logs in with GitHub
3. Spring redirects to `https://campus-recycle-rewards--ctu01.replit.app/auth/callback?token=<JWT>`
4. Frontend stores JWT, sends as `Authorization: Bearer <token>` on every request

## Architecture decisions

- **Stateless JWT** — no server-side sessions; horizontal scaling works out of the box
- **Custom DataSourceConfig** — converts Replit's `postgresql://user:pass@host/db` URL to proper JDBC format with HikariCP
- **WebClient bridge** — async, non-blocking HTTP proxy to the Python backend via `PYTHON_BACKEND_URL`
- **CORS** — configured for the frontend origin and localhost dev ports
- **Hibernate auto-DDL** — `ddl-auto: update` creates/migrates the `users` table automatically on startup

## User preferences

- Student project — keep dependencies minimal and free
- GitHub OAuth (not Google) for login
- Must be scalable and able to talk to a secondary Python backend

## Gotchas

- `DataSourceConfig` converts the Replit postgres URL to JDBC format automatically — don't set `spring.datasource.url` directly
- GitHub OAuth callback URL must be registered in the GitHub OAuth App settings
- The JWT secret comes from `SESSION_SECRET` — if it changes, all existing tokens are invalidated
- After GitHub login success, users are redirected to `/auth/callback?token=...` on the frontend
