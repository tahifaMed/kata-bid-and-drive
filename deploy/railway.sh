#!/usr/bin/env bash
# One-time bootstrap of the BidAndDrive Railway project: Postgres, backend and frontend.
# Run it once after `railway login`. Re-running adds a second Postgres and services, so don't.
set -euo pipefail

PROJECT_NAME="${PROJECT_NAME:-bidanddrive}"
cd "$(dirname "$0")/.."

command -v railway >/dev/null || { echo "Railway CLI missing: npm install -g @railway/cli" >&2; exit 1; }
railway whoami >/dev/null 2>&1 || { echo "Not logged in: run railway login" >&2; exit 1; }

# Create and link the project, unless this directory is already linked to one
if ! railway status >/dev/null 2>&1; then
  railway init --name "$PROJECT_NAME"
fi

railway add --database postgres

# Postgres reference variables are resolved by Railway. The backend needs JDBC form.
railway add --service backend \
  --variables "PORT=8080" \
  --variables 'DB_URL=jdbc:postgresql://${{Postgres.PGHOST}}:${{Postgres.PGPORT}}/${{Postgres.PGDATABASE}}' \
  --variables 'DB_USERNAME=${{Postgres.PGUSER}}' \
  --variables 'DB_PASSWORD=${{Postgres.PGPASSWORD}}'

# The frontend reaches the backend over Railway private networking. The backend stays private.
railway add --service frontend \
  --variables 'BACKEND_URL=http://${{backend.RAILWAY_PRIVATE_DOMAIN}}:8080'

# Only the frontend gets a public URL
railway domain --service frontend

cat <<MSG

Project "$PROJECT_NAME" created. Services: Postgres, backend (private), frontend (public).

Next, so that pushes to main deploy automatically:
  1. Railway dashboard → project → Settings → Tokens → create a token for the production environment.
  2. gh secret set RAILWAY_TOKEN --repo tahifaMed/kata-bid-and-drive
  3. Merge a PR to main, or run the "deploy" workflow manually from the Actions tab.
MSG
