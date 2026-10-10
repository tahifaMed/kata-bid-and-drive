# syntax=docker/dockerfile:1

# Build stage: install dependencies and produce the static bundle
FROM node:24-alpine AS build
WORKDIR /app

COPY package.json package-lock.json ./
RUN npm ci

COPY . .
RUN npm run build-only

# Runtime stage: nginx serves the bundle and proxies /api to the backend
FROM nginx:1.27-alpine

COPY --from=build /app/dist /usr/share/nginx/html
COPY nginx.conf.template /etc/nginx/templates/default.conf.template

# Only substitute our variables, so nginx's own $variables are left alone
ENV NGINX_ENVSUBST_FILTER=^(PORT|BACKEND_URL)$
ENV PORT=8080
EXPOSE 8080
