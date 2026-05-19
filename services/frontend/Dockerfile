# syntax=docker/dockerfile:1.7

# ---------- builder ----------
FROM node:22-alpine AS builder
WORKDIR /workspace

# Install deps first so the layer caches across source-only changes.
COPY services/frontend/package.json services/frontend/package-lock.json ./
RUN npm ci --no-audit --no-fund

# Copy the actual source.
COPY services/frontend/tsconfig.json services/frontend/vite.config.ts \
     services/frontend/index.html ./
COPY services/frontend/src ./src
COPY services/frontend/tests ./tests

# `npm run build` runs `tsc -b && vite build` — same script the dev uses locally.
RUN npm run build

# ---------- runtime ----------
FROM nginx:1.27-alpine AS runtime
RUN apk add --no-cache wget

COPY services/frontend/nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=builder /workspace/dist /usr/share/nginx/html

EXPOSE 80
HEALTHCHECK --interval=10s --timeout=3s --retries=5 --start-period=10s \
    CMD wget -qO- http://localhost/healthz || exit 1
