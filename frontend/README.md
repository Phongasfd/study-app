# Study App frontend

React 19 single-page app (Vite, React Router 7, Axios, i18next in Vietnamese and English) for the Study App. The backend lives in [`../backend`](../backend); the [root README](../README.md) describes the whole system.

## Pages

| Page | What it does |
| --- | --- |
| `Home` | Focus timer (start, stop, subjects) and today's study time |
| `Groups` | Create, search, discover and join study groups |
| `GroupInfo` | Members, live leaderboard and chat over one WebSocket (`/ws?groupId=…`); reconnects with exponential backoff and jitter after a deploy or a dropped connection |
| `Stats` | Weekly and monthly study time, by subject |
| `Login`, `Signup` | E-mail and password, or Google OAuth2 |

## How it talks to the backend

- REST through Axios with `withCredentials: true`: the backend sets `httpOnly` JWT cookies, so the app never sees a token.
- CSRF: the backend's `XSRF-TOKEN` cookie is sent back in the `X-XSRF-TOKEN` header on state-changing requests.
- The WebSocket URL is derived from `VITE_API_BASE_URL` (`https` becomes `wss`), so REST and the socket always hit the same host.

## Run

```bash
npm ci
VITE_API_BASE_URL=http://localhost:8080/api npm run dev   # http://localhost:5173
npm run lint
npm run build                                           # production bundle in dist/
```

The production image is built from the `Dockerfile` with `VITE_API_BASE_URL` as a build argument and served by Nginx (see `docker-compose.prod.yml`).
