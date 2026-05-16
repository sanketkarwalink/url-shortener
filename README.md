# URL Shortener

Shorten URLs, track clicks, analyze traffic — all self-hosted.

**Live demo:** [url-shortener-seven-ashy.vercel.app](https://url-shortener-seven-ashy.vercel.app)  
**API:** [url-shortener-upt2.onrender.com](https://url-shortener-upt2.onrender.com)

---

## Features

- **Auth** — register/login with JWT or Google OAuth. Each user sees only their own URLs
- **Short links** — 6-character alphanumeric codes, auto-https, SSRF-safe
- **Click analytics** — daily traffic, device/browser breakdown, top referrers
- **Rate limited** — 20 creates/min, 60 redirects/min per IP
- **Admin dashboard** — admins can view all URLs, see global stats, and moderate
- **Dark mode** — automatic, respects system preference
- **Docker ready** — one-command production deploy with PostgreSQL

## Stack

| Layer | Tech |
|-------|------|
| Backend | Spring Boot 3.4.4, Java 21, Maven, Spring Security |
| Frontend | Next.js 15.3.1, React 19, Tailwind CSS v4, Recharts |
| Database | H2 (dev) / PostgreSQL 16 (prod) |
| Cache | Caffeine (in-memory, 1-hour TTL) |
| Auth | JWT (jjwt), BCrypt passwords, Google OAuth 2.0 |

## Quick Start

### Prerequisites
- Java 21+, Node.js 18+, Maven

### Backend (H2 dev mode)
```bash
cd backend
GOOGLE_CLIENT_ID=your-client-id.apps.googleusercontent.com mvn spring-boot:run
# http://localhost:8080
```

> `GOOGLE_CLIENT_ID` is optional for development — Google sign-in will be disabled without it.

### Frontend
```bash
cd frontend
cp .env.example .env.local
# Edit .env.local with your Google Client ID
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

### 4. Environment Variables

**Backend (Render):**

| Variable | Value |
|----------|-------|
| `SPRING_PROFILES_ACTIVE` | `postgres` |
| `SPRING_DATASOURCE_URL` | `jdbc:postgresql://...?sslmode=require` |
| `SPRING_DATASOURCE_USERNAME` | Neon username |
| `SPRING_DATASOURCE_PASSWORD` | Neon password |
| `APP_BASE_URL` | `https://your-app.onrender.com` |
| `APP_CORS_ORIGINS` | `https://your-frontend.vercel.app` |
| `APP_JWT_SECRET` | A random string at least 32 characters long |
| `GOOGLE_CLIENT_ID` | Your Google OAuth client ID |

> To make yourself admin, run this SQL in your Neon console:
> ```sql
> ALTER TABLE users ADD COLUMN admin BOOLEAN DEFAULT FALSE;
> UPDATE users SET admin = TRUE WHERE email = 'your@email.com';
> ```

**Frontend (Vercel):**

| Variable | Value |
|----------|-------|
| `NEXT_PUBLIC_API_URL` | `https://your-app.onrender.com` |
| `NEXT_PUBLIC_GOOGLE_CLIENT_ID` | Your Google OAuth client ID |

### 5. Keep Alive (cron-job.org)
Create a cron job hitting `https://your-app.onrender.com/actuator/health` every 15 min.

## API

| Method | Path | Auth | Description |
|--------|------|------|-------------|
| `POST` | `/api/auth/register` | No | Register account |
| `POST` | `/api/auth/login` | No | Login, get JWT |
| `POST` | `/api/auth/google` | No | Sign in with Google (send ID token) |
| `POST` | `/api/urls` | Optional | Create short URL |
| `GET` | `/api/urls` | Yes | List URLs (`?all=true` for admin view) |
| `GET` | `/api/urls/admin/stats` | Yes (admin) | Global stats (users, URLs, clicks) |
| `GET` | `/api/urls/admin/users` | Yes (admin) | List all users with URL/click counts |
| `DELETE` | `/api/urls/admin/users/{id}` | Yes (admin) | Delete user and all their URLs |
| `GET` | `/api/urls/{id}/analytics` | Yes | Click analytics |
| `DELETE` | `/api/urls/{id}` | Yes | Delete URL |
| `GET` | `/{shortCode}` | No | Redirect |
| `GET` | `/actuator/health` | No | Health check |

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
