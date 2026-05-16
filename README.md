# URL Shortener

Shorten URLs, track clicks, analyze traffic — all self-hosted.

**Live demo:** [url-shortener-seven-ashy.vercel.app](https://url-shortener-seven-ashy.vercel.app)  
**API:** [url-shortener-upt2.onrender.com](https://url-shortener-upt2.onrender.com)

---

## Features

- **Short links** — 6-character alphanumeric codes, auto-https, SSRF-safe
- **Click analytics** — daily traffic, device/browser breakdown, top referrers
- **Rate limited** — 20 creates/min, 60 redirects/min per IP
- **Cached redirects** — Caffeine with 1-hour TTL for fast 302s
- **Dark mode** — automatic, respects system preference
- **Docker ready** — one-command production deploy with PostgreSQL

## Stack

| Layer | Tech |
|-------|------|
| Backend | Spring Boot 3.4.4, Java 21, Maven |
| Frontend | Next.js 15.3.1, React 19, Tailwind CSS v4, Recharts |
| Database | H2 (dev) / PostgreSQL 16 (prod) |
| Cache | Caffeine (in-memory, 1-hour TTL) |

## Quick Start

### Prerequisites
- Java 21+, Node.js 18+, Maven

### Backend (H2 dev mode)
```bash
cd backend
mvn spring-boot:run
# http://localhost:8080
```

### Frontend
```bash
cd frontend
npm install
npm run dev
# http://localhost:3000
```

## Deploy for Free

Services used:
- **Database:** [Neon](https://neon.tech) — free PostgreSQL
- **Backend:** [Render](https://render.com) — free web service (spins down after 15 min)
- **Frontend:** [Vercel](https://vercel.com) — free Next.js hosting
- **Keep awake:** [cron-job.org](https://cron-job.org) — pings every 15 min

### 1. Database (Neon)
1. Sign up at [neon.tech](https://neon.tech)
2. Create a project → copy connection string

### 2. Backend (Render)
1. Create a **Web Service** from your GitHub repo
2. Settings:
   - **Runtime:** Docker
   - **Root Directory:** (leave blank)
   - **Dockerfile Path:** `backend/Dockerfile`
   - **Docker Build Context:** `backend`
3. Environment variables:

| Variable | Value |
|----------|-------|
| `SPRING_PROFILES_ACTIVE` | `postgres` |
| `SPRING_DATASOURCE_URL` | `jdbc:postgresql://...?sslmode=require` |
| `SPRING_DATASOURCE_USERNAME` | your Neon username |
| `SPRING_DATASOURCE_PASSWORD` | your Neon password |
| `APP_BASE_URL` | `https://your-app.onrender.com` |
| `APP_CORS_ORIGINS` | `https://your-frontend.vercel.app` |

### 3. Frontend (Vercel)
1. Import your GitHub repo
2. **Root Directory:** `frontend`
3. Deploy
4. Update `API` constant in `frontend/src/app/page.tsx` to your Render URL

### 4. Keep Alive (cron-job.org)
Create a cron job hitting `https://your-app.onrender.com/actuator/health` every 15 min.

## API

| Method | Path | Description |
|--------|------|-------------|
| `POST` | `/api/urls` | Create short URL |
| `GET` | `/api/urls` | List all URLs (paginated) |
| `GET` | `/api/urls/{id}/analytics` | Click analytics |
| `DELETE` | `/api/urls/{id}` | Delete URL |
| `GET` | `/{shortCode}` | Redirect |
| `GET` | `/actuator/health` | Health check |

```bash
curl -X POST https://your-app.onrender.com/api/urls \
  -H "Content-Type: application/json" \
  -d '{"originalUrl":"https://example.com"}'
```

## Docker

```bash
docker compose up --build
```

Starts PostgreSQL + backend on port 8080.

## License

MIT
