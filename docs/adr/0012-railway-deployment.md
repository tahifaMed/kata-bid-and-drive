# 0012: Deploy to Railway with a bootstrap script and push-to-main workflow

Status: Accepted

## Context
The app needs a hosted environment: Postgres, a Spring Boot backend and a Vue frontend. Railway has no single file that creates a whole project. Its config files (`railway.toml`) describe one service. Pushes to `main` must deploy without manual steps.

## Decision
- One-time setup is `deploy/railway.sh`, which uses the Railway CLI to create the project, Postgres, `backend` and `frontend` services and their variables.
- Each service builds from its own Dockerfile. `backend/railway.toml` and `frontend/railway.toml` hold the build and health settings.
- The frontend's nginx serves the static bundle and proxies `/api/` to the backend over Railway private networking. The browser sees one origin, so no CORS is needed and the relative `/api` client works unchanged.
- The backend has no public domain. Only the frontend is public.
- `.github/workflows/deploy.yml` runs `railway up` for both services on each push to `main` (and manually). It authenticates with a project token in the `RAILWAY_TOKEN` secret.
- The backend runs as one replica (ADR 0002).

## Consequences
- The `X-User-Id` identity header is still unauthenticated (ADR 0001). The public URL exposes it, so this deploy is for the kata and demos only.
- SSE (spec 009) will need `proxy_buffering off` and a long read timeout in `frontend/nginx.conf.template` when it lands.
- The nginx resolver `fd12::10` is Railway's private DNS. If it changes, `/api` returns 502 until the template is updated.
- Deploys go through GitHub Actions, so a `RAILWAY_TOKEN` secret must exist before the first push to `main`.
