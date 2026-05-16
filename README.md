# URL Shortener

Self-hosted URL shortener with click analytics — Spring Boot backend + Next.js frontend.

## Features

- Shorten long URLs into short, shareable links
- Click analytics: daily traffic, device/browser breakdown, referrers
- Automatic protocol handling (prepends `https://` if missing)
- Blocks shortlinks pointing to private/internal IPs (SSRF protection)
- Rate limiting (20 creates/min, 60 redirects/min per IP)
- Caching with Caffeine (1-hour TTL)
- Dark mode UI
- Docker support for production with PostgreSQL

## Architecture

```
url-shortener/
├── backend/          # Spring Boot 3.4.4 + Java 21
│   ├── src/main/java/com/urlshortener/
│   │   ├── controller/   # REST API + redirect endpoints
│   │   ├── service/      # Business logic (create, resolve, analytics)
│   │   ├── model/        # JPA entities (ShortUrl, ClickEvent)
│   │   ├── repository/   # Spring Data JPA repos
│   │   ├── dto/          # Request/response records
│   │   └── config/       # CORS, cache, rate limiting, error handling
│   └── src/main/resources/
│       ├── application.properties          # Dev config (H2)
│       └── application-postgres.properties # Prod config (PostgreSQL)
├── frontend/         # Next.js 15 + React 19 + Tailwind CSS v4
│   └── src/app/
│       ├── page.tsx       # Main SPA (create, list, analytics)
│       ├── layout.tsx     # Root layout
│       └── globals.css    # Tailwind + custom theme
└── docker-compose.yml    # PostgreSQL + backend
```

## Quick Start (Development)

### Prerequisites

- Java 21+
- Node.js 18+
- Maven

### Backend

```bash
cd backend
mvn spring-boot:run
```

Starts on `http://localhost:8080` with an H2 file database.

### Frontend

```bash
cd frontend
npm install
npm run dev
```

Opens at `http://localhost:3000`. The frontend API URL is configured in `page.tsx` as `http://localhost:8080`.

## Deploy for Free

### 1. Database — Neon (Free PostgreSQL)

1. Sign up at [neon.tech](https://neon.tech)
2. Create a project, copy the connection string

### 2. Backend — Render

1. Push this repo to GitHub
2. On [render.com](https://render.com), create a **Web Service**
3. Connect your GitHub repo
4. Settings:
   - **Root Directory:** `backend`
   - **Build Command:** `mvn package -DskipTests`
   - **Start Command:** `java -jar target/url-shortener-0.0.1.jar`
   - **Environment Variables:**
     - `SPRING_PROFILES_ACTIVE`: `postgres`
     - `SPRING_DATASOURCE_URL`: your Neon connection string
     - `SPRING_DATASOURCE_USERNAME`: your Neon username
     - `SPRING_DATASOURCE_PASSWORD`: your Neon password
     - `APP_BASE_URL`: `https://your-app.onrender.com`
     - `APP_CORS_ORIGINS`: `https://your-frontend.vercel.app`

> Render free tier sleeps after 15 min of inactivity. First request after sleep takes ~30s.

### 3. Frontend — Vercel

1. Push the repo to GitHub
2. On [vercel.com](https://vercel.com), import the repo
3. Settings:
   - **Root Directory:** `frontend`
   - **Framework Preset:** Next.js
4. After deploy, update the `API` constant in `frontend/src/app/page.tsx` to your Render URL, or set `NEXT_PUBLIC_API_URL` environment variable (you'll need to modify the code to read it)

Alternatively, just change the `API` constant at the top of `page.tsx`:

```ts
const API = "https://your-app.onrender.com";
```

Then redeploy.

### 4. Custom Domain (Optional)

Both Vercel and Render support custom domains in their free tiers.

## API Endpoints

| Method | Path | Description |
|--------|------|-------------|
| `POST` | `/api/urls` | Create short URL |
| `GET` | `/api/urls` | List all URLs (paginated) |
| `GET` | `/api/urls/{id}/analytics` | Click analytics |
| `DELETE` | `/api/urls/{id}` | Delete URL |
| `GET` | `/{shortCode}` | Redirect to original URL |
| `GET` | `/actuator/health` | Health check |

### Create a short URL

```bash
curl -X POST https://your-app.onrender.com/api/urls \
  -H "Content-Type: application/json" \
  -d '{"originalUrl":"https://example.com"}'
```

Response:
```json
{
  "id": 1,
  "originalUrl": "https://example.com",
  "shortCode": "Ab3Xyz",
  "shortUrl": "https://your-app.onrender.com/Ab3Xyz",
  "createdAt": "2026-05-16T12:00:00",
  "clickCount": 0
}
```

## Docker (Production)

```bash
docker compose up --build
```

This starts PostgreSQL + the backend. Requires Docker.

## License

MIT
