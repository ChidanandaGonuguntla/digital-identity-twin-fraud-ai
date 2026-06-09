FROM node:20-alpine AS build
WORKDIR /app
COPY package.json package-lock.json* ./
RUN npm ci
COPY . .
ARG VITE_API_BASE_URL=/api/v1
ARG VITE_WS_URL=/ws/decisions
ARG VITE_USE_MOCK=false
ARG VITE_SECURITY_ENABLED=true
ARG VITE_APP_NAME=Digital Identity Twin
ARG VITE_GRAFANA_URL=http://localhost:3001
ENV VITE_API_BASE_URL=$VITE_API_BASE_URL
ENV VITE_WS_URL=$VITE_WS_URL
ENV VITE_USE_MOCK=$VITE_USE_MOCK
ENV VITE_SECURITY_ENABLED=$VITE_SECURITY_ENABLED
ENV VITE_APP_NAME=$VITE_APP_NAME
ENV VITE_GRAFANA_URL=$VITE_GRAFANA_URL
RUN npm run build

FROM nginx:1.27-alpine
RUN apk add --no-cache curl
COPY nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=build /app/dist /usr/share/nginx/html
EXPOSE 80
HEALTHCHECK --interval=30s --timeout=5s --start-period=10s --retries=3 CMD curl -fsS http://127.0.0.1/healthz || exit 1
