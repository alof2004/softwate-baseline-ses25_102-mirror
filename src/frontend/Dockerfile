# Stage 1 — build static assets
FROM node:20-alpine AS build
WORKDIR /app
COPY package*.json ./
RUN npm ci
COPY . .
ARG VITE_API_BASE_URL
ARG VITE_KEYCLOAK_URL
ENV VITE_API_BASE_URL=$VITE_API_BASE_URL
ENV VITE_KEYCLOAK_URL=$VITE_KEYCLOAK_URL
RUN npm run build

# Stage 2 — serve with Nginx
FROM nginx:1.28-alpine
RUN apk update \
    && apk del nginx-module-acme nginx-module-geoip nginx-module-image-filter nginx-module-njs nginx-module-xslt 2>/dev/null || true \
    && apk upgrade --no-cache \
    && apk add --no-cache 'libcrypto3>=3.5.6-r0' 'libssl3>=3.5.6-r0' 'musl>=1.2.5-r23' 'musl-utils>=1.2.5-r23' 'zlib>=1.3.2-r0' || true
COPY --from=build /app/dist /usr/share/nginx/html
COPY nginx.conf /etc/nginx/nginx.conf
EXPOSE 80
HEALTHCHECK --interval=30s --timeout=5s --start-period=10s --retries=3 \
    CMD wget -qO- http://localhost:80/ || exit 1
USER nginx
