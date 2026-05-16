# URL Shortener — Project Context

## Stack
- **Backend:** Spring Boot 3.4.4, Java 21, Maven
- **Frontend:** Next.js 15.3.1, React 19, Tailwind CSS v4, Recharts
- **Database:** H2 (dev), PostgreSQL 16 (prod)
- **Cache:** Caffeine (1-hour TTL, 10K max entries)

## Project Structure
```
url-shortener/
├── backend/                  # Spring Boot app
│   ├── pom.xml
│   ├── Dockerfile
│   └── src/main/
│       ├── java/com/urlshortener/
│       │   ├── UrlShortenerApplication.java
│       │   ├── controller/
│       │   │   ├── UrlApiController.java     # REST: CRUD + analytics
│       │   │   └── RedirectController.java   # GET /{shortCode} -> 302
│       │   ├── service/
│       │   │   └── UrlService.java           # Core logic
│       │   ├── model/
│       │   │   ├── ShortUrl.java             # JPA entity
│       │   │   └── ClickEvent.java           # JPA entity
│       │   ├── repository/                   # Spring Data JPA
│       │   ├── dto/                          # Records
│       │   └── config/
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
│   └── src/app/
│       ├── layout.tsx
│       ├── page.tsx          # SPA: form, list, analytics modal
│       └── globals.css
└── docker-compose.yml        # PostgreSQL + backend
```

## Live URLs
- **Backend:** https://url-shortener-upt2.onrender.com
- **Frontend:** https://url-shortener-seven-ashy.vercel.app

## Key Behaviors
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

## Notes
- Rate limit filter uses a circular buffer sliding window (in-memory, per-IP)
- Cache is Caffeine, 1-hour write expiry, 10K entries
- No auth/accounts — single-user by design
- API constant in `page.tsx:8` must point to backend URL
