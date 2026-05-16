# URL Shortener — Project Context

## Stack
- **Backend:** Spring Boot 3.4.4, Java 21, Maven, Spring Security
- **Frontend:** Next.js 15.3.1, React 19, Tailwind CSS v4, Recharts
- **Database:** H2 (dev), PostgreSQL 16 (prod)
- **Cache:** Caffeine (1-hour TTL, 10K max entries)
- **Auth:** JWT (jjwt 0.12.6), BCrypt passwords, per-user URL scoping

## Project Structure
```
url-shortener/
├── backend/                  # Spring Boot app
│   ├── pom.xml
│   ├── Dockerfile
│   └── src/main/
│       ├── java/com/urlshortener/
│       │   ├── UrlShortenerApplication.java
│       │   ├── auth/
│       │   │   ├── JwtUtil.java             # JWT generation + validation
│       │   │   └── JwtAuthFilter.java        # OncePerRequestFilter
│       │   ├── controller/
│       │   │   ├── AuthController.java       # POST /api/auth/login|register
│       │   │   ├── UrlApiController.java     # REST: CRUD + analytics
│       │   │   └── RedirectController.java   # GET /{shortCode} -> 302
│       │   ├── service/
│       │   │   └── UrlService.java           # Core logic
│       │   ├── model/
│       │   │   ├── User.java                # JPA entity (auth)
│       │   │   ├── ShortUrl.java            # JPA entity
│       │   │   └── ClickEvent.java          # JPA entity
│       │   ├── repository/                   # Spring Data JPA
│       │   ├── dto/                          # Records
│       │   └── config/
│       │       ├── SecurityConfig.java       # Spring Security (stateless, CORS)
│       │       ├── CorsConfig.java
│       │       ├── CacheConfig.java
│       │       ├── RateLimitFilter.java      # Sliding window rate limiter
│       │       └── GlobalExceptionHandler.java
│       └── resources/
│           ├── application.properties          # Dev defaults (H2)
│           └── application-postgres.properties # Prod (PostgreSQL)
├── frontend/                 # Next.js 15 app
│   ├── package.json
│   ├── next.config.ts
│   ├── tsconfig.json
│   ├── postcss.config.mjs
│   ├── .env.example
│   └── src/
│       ├── lib/auth.tsx       # AuthProvider + useAuth hook
│       └── app/
│           ├── layout.tsx
│           ├── page.tsx       # Dashboard (protected)
│           ├── login/page.tsx
│           ├── signup/page.tsx
│           └── globals.css
└── docker-compose.yml        # PostgreSQL + backend
```

## Live URLs
- **Backend:** https://url-shortener-upt2.onrender.com
- **Frontend:** https://url-shortener-seven-ashy.vercel.app

## Key Behaviors
- **Auth:** JWT-based. Register/login at `/api/auth/register` and `/api/auth/login`
- **Protected endpoints** (require `Authorization: Bearer <token>` header):
  - `GET /api/urls` — list own URLs
  - `GET /api/urls/{id}/analytics` — own URL analytics
  - `DELETE /api/urls/{id}` — delete own URL
- **Public endpoints** (no auth required):
  - `POST /api/urls` — create URL (optionally linked to user if token provided)
  - `GET /{shortCode}` — redirect
  - `/api/auth/login`, `/api/auth/register`, `/actuator/health`
- Users only see their own URLs. Anonymous URLs (created without auth) are invisible in the dashboard
- Short codes are 6-char alphanumeric (random, no custom codes)
- Auto-prepends `https://` if protocol missing
- Blocks private/internal IPs for SSRF protection
- Rate limit: 20 POST/min, 60 GET/min per IP
- Click tracking logs device, browser, OS, referer, IP
- Analytics returns last 30 days daily clicks, device/browser/referer breakdown
- Delete cascades to click events
- CORS configured for frontend origin

## Common Commands
```bash
# Backend dev
cd backend && mvn spring-boot:run

# Frontend dev
cd frontend && npm run dev

# Build backend
cd backend && mvn package -DskipTests

# Build frontend
cd frontend && npx next build

# Docker
docker compose up --build
```

## Env Vars (for Render/Neon)
Key env vars used by the backend:
- `SPRING_PROFILES_ACTIVE` -> `postgres`
- `SPRING_DATASOURCE_URL` -> Neon JDBC URL
- `SPRING_DATASOURCE_USERNAME` / `SPRING_DATASOURCE_PASSWORD`
- `APP_BASE_URL` -> backend public URL
- `APP_CORS_ORIGINS` -> frontend URL
- `APP_JWT_SECRET` -> JWT signing secret (min 32 chars)
- `NEXT_PUBLIC_API_URL` -> frontend env var for backend API URL

## Notes
- Rate limit filter uses a circular buffer sliding window (in-memory, per-IP)
- Cache is Caffeine, 1-hour write expiry, 10K entries
- JWT filter is @Order(2), runs after RateLimitFilter @Order(1)
- Frontend uses AuthProvider context; stores JWT + user in localStorage
- API URL in frontend is read from `NEXT_PUBLIC_API_URL` env var, falls back to hardcoded URL
- User passwords hashed with BCrypt
