---
trigger: model_decision
description: Automatically activated when the agent modifies Dockerfile, docker-compose.yml, .env files, deployment scripts, or any infrastructure/configuration code in the hotel booking system.
---

# Docker & Deployment Standards — Hotel Booking System

## 1. Service Architecture

The system runs 5 containerized services defined in `docker-compose.yml`:

| Service Name | Image / Build | Internal Port | Host Port |
|---|---|---|---|
| `postgres_db` | `postgres:16-alpine` | `5432` | `5433` |
| `redis` | `redis:7-alpine` | `6379` | `6379` |
| `keycloak` | `quay.io/keycloak/keycloak:24.0.4` | `8080` | `8081` |
| `backend` | Build from `./backend/Dockerfile` | `8080` | `8080` |
| `frontend` | Build from `./frontend/Dockerfile` | `80` | `3000` |

## 2. Environment Variable Rules

- **Never hardcode secrets** in `Dockerfile`, `docker-compose.yml`, or source code
- All secrets and environment-specific configs must be in `.env` file (gitignored)
- Always provide `.env-example` with placeholder values and comments documenting each variable
- Use Docker Compose variable substitution (`${VARIABLE_NAME}`) to inject env vars into services
- Backend Spring Boot reads config via `application.yml` which reads from OS environment variables

## 3. Dockerfile Best Practices

### Backend (Spring Boot / Java 21)
- Use **multi-stage build**: `maven:3.9-eclipse-temurin-21` builder → `eclipse-temurin:21-jre-alpine` runtime
- Never copy entire project directory — only copy compiled `.jar` artifact into runtime stage
- Specify `EXPOSE 8080` and use `ENTRYPOINT ["java", "-jar", "app.jar"]`
- Pass JVM flags via `JAVA_OPTS` environment variable for tuning

### Frontend (React / Vite)
- Use **multi-stage build**: `node:20-alpine` builder → `nginx:alpine` runtime
- Build with `npm run build`, serve static files via Nginx
- Nginx config must proxy `/api` requests to the backend service

## 4. Health Checks

Every service must define a `healthcheck` in `docker-compose.yml`:

```yaml
# PostgreSQL
healthcheck:
  test: ["CMD-SHELL", "pg_isready -U ${POSTGRES_USER} -d ${POSTGRES_DB}"]
  interval: 10s
  timeout: 5s
  retries: 5

# Redis
healthcheck:
  test: ["CMD", "redis-cli", "ping"]
  interval: 10s
  timeout: 3s
  retries: 3

# Backend Spring Boot
healthcheck:
  test: ["CMD-SHELL", "curl -f http://localhost:8080/actuator/health || exit 1"]
  interval: 15s
  timeout: 10s
  retries: 5
  start_period: 60s
```

## 5. Service Dependencies

Services must declare `depends_on` with `condition: service_healthy`:
- `backend` depends on `postgres_db` (healthy) + `redis` (healthy) + `keycloak` (healthy)
- `frontend` depends on `backend` (healthy)

**Never** use `depends_on` without conditions — containers starting does not mean services are ready.

## 6. Volumes & Data Persistence

- Named volumes (`pg_data`, `redis_data`) must be used for persistent data — never bind-mount production DB data
- Database SQL scripts are mounted from `./database/` into PostgreSQL init directory (`/docker-entrypoint-initdb.d/`)
- SQL init scripts run **once** on first container creation — to re-run, delete the volume with `docker compose down -v`
- Never mount source code into backend/frontend containers in production builds

## 7. Network Configuration

- All services must be on a single dedicated internal bridge network (e.g., `hotel_network`)
- Services communicate using Docker service names as hostnames (e.g., `postgres_db:5432`, `redis:6379`)
- Only expose ports to host that are required for external access or development

## 8. Deployment Checklist

Before committing infrastructure changes:
- [ ] `docker compose config` passes validation without errors
- [ ] `.env-example` is updated if new variables are added
- [ ] No secrets are committed to version control
- [ ] All new services have health checks defined
- [ ] `depends_on` conditions are set correctly
- [ ] The system comes up cleanly with `docker compose up -d --build` on a clean machine
